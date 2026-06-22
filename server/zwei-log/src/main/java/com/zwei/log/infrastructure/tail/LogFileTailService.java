package com.zwei.log.infrastructure.tail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.zwei.log.infrastructure.config.LogModuleProperties;

/**
 * 日志文件追踪服务 — 使用 RandomAccessFile 轮询 tail 日志文件，广播新行给所有 SSE 订阅者
 *
 * @author zwei
 */
@Service
public class LogFileTailService implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(LogFileTailService.class);
    private static final int POLL_INTERVAL_MS = 500;
    private static final int INITIAL_REPLAY_LINES = 50;
    private static final int MAX_READ_SIZE = 2 * 1024 * 1024; // 2MB per poll

    private final LogModuleProperties properties;
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
        List<String> replayLines = readLastLines(INITIAL_REPLAY_LINES);
        try {
            for (String line : replayLines) {
                emitter.send(SseEmitter.event().name("line").data(line));
            }
            emitter.send(SseEmitter.event()
                .name("ready")
                .data(Collections.singletonMap("replayCount", replayLines.size())));
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

    private List<String> readLastLines(int count) {
        List<String> result = new ArrayList<>();
        try {
            File file = new File(currentFilePath);
            if (!file.exists() || file.length() == 0) {
                return result;
            }

            RandomAccessFile reader = new RandomAccessFile(file, "r");
            long pos = file.length() - 1;
            int linesFound = 0;
            StringBuilder sb = new StringBuilder();

            while (pos >= 0 && linesFound < count) {
                reader.seek(pos);
                char c = (char) reader.readByte();
                if (c == '\n' && sb.length() > 0) {
                    result.add(0, sb.reverse().toString());
                    sb.setLength(0);
                    linesFound++;
                } else if (c != '\r') {
                    sb.append(c);
                }
                pos--;
            }
            if (sb.length() > 0 && linesFound < count) {
                result.add(0, sb.reverse().toString());
            }
            reader.close();
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
