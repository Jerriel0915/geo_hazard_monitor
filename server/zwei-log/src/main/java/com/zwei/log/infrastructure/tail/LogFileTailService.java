package com.zwei.log.infrastructure.tail;

import com.zwei.log.infrastructure.config.LogModuleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 日志文件追踪服务 — 使用 RandomAccessFile 轮询 tail 日志文件，广播新行给所有 SSE 订阅者
 *
 * @author zwei
 */
@Service
public class LogFileTailService implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(LogFileTailService.class);
    private static final int POLL_INTERVAL_MS = 500;
    private static final int MAX_READ_SIZE = 2 * 1024 * 1024; // 2MB per poll

    private static final DateTimeFormatter LOG_TS =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("Asia/Shanghai"));

    private final LogModuleProperties properties;
    private final long replayWindowMinutes;
    private final int replayMinLines;
    private final List<SseEmitter> subscribers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "log-file-tailer");
        t.setDaemon(true);
        return t;
    });

    private RandomAccessFile raf;
    private long lastPosition;
    private String currentFilePath;

    public LogFileTailService(LogModuleProperties properties) {
        this.properties = properties;
        this.replayWindowMinutes = properties.getConsoleReplayWindowMinutes();
        this.replayMinLines = properties.getConsoleReplayMinLines();
    }

    @Override
    public void afterPropertiesSet() {
        String path = properties.getConsoleLogPath();
        if (path == null || path.isBlank()) {
            path = "./logs/sys-all.log";
        }
        this.currentFilePath = path;
        openFile();
        scheduler.scheduleWithFixedDelay(this::poll, POLL_INTERVAL_MS, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void openFile() {
        try {
            if (raf != null) {
                raf.close();
            }
        } catch (IOException ignored) {
        }

        try {
            File file = new File(currentFilePath);
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                file.createNewFile();
            }
            raf = new RandomAccessFile(file, "r");
            raf.seek(file.length());
            lastPosition = file.length();
        } catch (IOException e) {
            log.warn("Failed to open log file: {}", currentFilePath, e);
            raf = null;
        }
    }

    private void poll() {
        try {
            if (raf == null) {
                openFile();
                if (raf == null) {
                    return;
                }
            }

            File file = new File(currentFilePath);
            long fileLength = file.length();

            // File rotation: file shrunk or was replaced
            if (fileLength < lastPosition) {
                log.info("Log file rotation detected, reopening: {}", currentFilePath);
                openFile();
                return;
            }

            if (fileLength <= lastPosition) {
                return;
            }

            raf.seek(lastPosition);
            long bytesToRead = Math.min(fileLength - lastPosition, MAX_READ_SIZE);
            byte[] buffer = new byte[(int) bytesToRead];
            int bytesRead = raf.read(buffer);
            if (bytesRead <= 0) {
                return;
            }

            String text = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            String[] lines = text.split("\\r?\\n");

            // Snapshot subscribers to avoid concurrent modification during iteration
            List<SseEmitter> snapshot = List.copyOf(subscribers);
            if (snapshot.isEmpty() || lines.length == 0) {
                lastPosition = raf.getFilePointer();
                return;
            }

            for (String line : lines) {
                if (line.isEmpty()) {
                    continue;
                }
                broadcast(line, snapshot);
            }

            lastPosition = raf.getFilePointer();
        } catch (IOException e) {
            log.warn("Error polling log file: {}", e.getMessage());
            try {
                openFile();
            } catch (Exception ignored) {
            }
        }
    }

    private void broadcast(String line, List<SseEmitter> targets) {
        for (SseEmitter emitter : targets) {
            try {
                emitter.send(SseEmitter.event()
                    .name("line")
                    .data(line));
            } catch (Exception e) {
                subscribers.remove(emitter);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public SseEmitter subscribe(SseEmitter emitter) {
        return subscribe(emitter, replayWindowMinutes);
    }

    public SseEmitter subscribe(SseEmitter emitter, long windowMinutes) {
        List<String> replayLines = readLinesSince(windowMinutes, replayMinLines);
        try {
            for (String line : replayLines) {
                emitter.send(SseEmitter.event().name("line").data(line));
            }
            emitter.send(SseEmitter.event()
                .name("ready")
                .data(Map.of("replayCount", replayLines.size(), "windowMinutes", windowMinutes)));
        } catch (Exception e) {
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
            }
            return emitter;
        }

        emitter.onCompletion(() -> subscribers.remove(emitter));
        emitter.onTimeout(() -> subscribers.remove(emitter));
        emitter.onError(ex -> subscribers.remove(emitter));

        subscribers.add(emitter);
        return emitter;
    }

    // ---------------------------------------------------------------------------
    // Time-based replay
    // ---------------------------------------------------------------------------

    List<String> readLinesSince(long windowMinutes, int minLines) {
        List<String> result = new ArrayList<>();
        try {
            File file = new File(currentFilePath);
            if (!file.exists() || file.length() == 0) {
                return result;
            }
            Instant cutoff = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);
            long startPos = findWindowStart(file, cutoff, minLines);
            result = forwardRead(file, startPos);
        } catch (Exception e) {
            log.warn("Time-based replay failed, falling back to line-based: {}", e.getMessage());
            return readLastLines(minLines);
        }
        return result;
    }

    /**
     * 从文件尾部反向扫描，定位时间窗口起始字节偏移。
     */
    private long findWindowStart(File file, Instant cutoff, int minLines) throws IOException {
        int linesScanned = 0;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long fileLen = file.length();
            long pos = fileLen;
            byte[] buf = new byte[8192];
            StringBuilder tail = new StringBuilder();

            while (pos > 0) {
                int readSize = (int) Math.min(buf.length, pos);
                pos -= readSize;
                raf.seek(pos);
                int n = raf.read(buf, 0, readSize);
                if (n <= 0) break;

                String block = new String(buf, 0, n, StandardCharsets.UTF_8) + tail;
                String[] lines = block.split("\\n", -1);

                tail.setLength(0);
                tail.append(lines[0]); // partial line, carry to next chunk

                // Track byte offset of each line within the block
                int[] offsets = new int[lines.length];
                int off = 0;
                for (int j = 0; j < lines.length; j++) {
                    offsets[j] = off;
                    off += lines[j].length() + 1; // +1 for the newline
                }

                for (int i = lines.length - 1; i >= 1; i--) {
                    String line = lines[i].trim();
                    if (line.isEmpty()) continue;
                    linesScanned++;

                    Instant ts = parseLogTimestamp(line);
                    if (ts != null && ts.isBefore(cutoff) && linesScanned >= minLines) {
                        return pos + offsets[i] + lines[i].length() + 1;
                    }
                }
            }
        }
        return 0;
    }

    Instant parseLogTimestamp(String line) {
        if (line == null || line.length() < 23) return null;
        try {
            return Instant.from(LOG_TS.parse(line.substring(0, 23)));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private List<String> forwardRead(File file, long startOffset) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            reader.skip(startOffset);
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    result.add(line);
                }
            }
        }
        return result;
    }

    // ---------------------------------------------------------------------------
    // Fallback: line-based replay
    // ---------------------------------------------------------------------------

    private List<String> readLastLines(int count) {
        List<String> result = new ArrayList<>();
        try {
            File file = new File(currentFilePath);
            if (!file.exists() || file.length() == 0) {
                return result;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String[] ring = new String[count];
                int idx = 0;
                int total = 0;
                String line;
                while ((line = reader.readLine()) != null) {
                    ring[idx % count] = line;
                    idx++;
                    total++;
                }
                int start = Math.max(0, total - count);
                for (int i = start; i < total; i++) {
                    result.add(ring[i % count]);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read last lines for replay: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        try {
            if (raf != null) {
                raf.close();
            }
        } catch (IOException ignored) {
        }
    }

    int getSubscriberCount() {
        return subscribers.size();
    }
}
