package com.zwei.iot.report.service;

import com.zwei.common.redis.DistributedLock;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.datasource.ReportDataAssembler;
import com.zwei.iot.report.domain.ReportRecord;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.mapper.ReportRecordMapper;
import com.zwei.iot.report.render.ReportRenderer;
import com.zwei.iot.report.support.ReportPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * 报告生成编排服务。
 * <ol>
 *   <li>Redis 分布式锁兜底多实例并发</li>
 *   <li>按 hp 串行处理 (避免 IoTDB 并发压力)</li>
 *   <li>单 hp 失败不影响其他</li>
 *   <li>幂等: 同 type+hp+period 已有成功记录 -> 跳过</li>
 *   <li>不自动重试, 失败记 status=3</li>
 * </ol>
 */
@Service
public class ReportGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationService.class);
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    private final ReportRecordMapper mapper;
    private final ReportDataAssembler assembler;
    private final IHazardPointQueryService hazardQuery;
    private final DistributedLock lock;
    private final List<ReportRenderer> renderers;

    public ReportGenerationService(ReportRecordMapper mapper,
                                    ReportDataAssembler assembler,
                                    IHazardPointQueryService hazardQuery,
                                    DistributedLock lock,
                                    List<ReportRenderer> renderers) {
        this.mapper = mapper;
        this.assembler = assembler;
        this.hazardQuery = hazardQuery;
        this.lock = lock;
        this.renderers = renderers;
    }

    /** 以当天为参考日期 → 生成上一周期报告 (定时任务入口)。 */
    public void generateAll(ReportType type) {
        generateAll(type, LocalDate.now());
    }

    /** 以指定日期为参考日期 → 生成上一周期报告 (手动触发入口，兼容补跑历史数据)。 */
    public void generateAll(ReportType type, LocalDate referenceDate) {
        ReportPeriod period = ReportPeriod.previous(type, referenceDate);
        log.info("[report] start type={} period={}~{} refDate={}", type, period.start(), period.end(), referenceDate);

        String lockKey = "report:gen:" + type.code() + ":" + period.key();
        DistributedLock.LockToken token = lock.tryLock(lockKey, LOCK_TTL);
        if (!token.acquired()) {
            log.info("[report] another instance is running, skip");
            return;
        }

        int success = 0, fail = 0;
        try {
            List<HazardPointBrief> hps = hazardQuery.listMonitoring();
            log.info("[report] hps count={}", hps.size());
            for (HazardPointBrief hp : hps) {
                try {
                    generateOne(type, period, hp);
                    success++;
                } catch (DuplicateKeyException e) {
                    log.info("[report] skip (duplicate) hp={} type={}", hp.id(), type);
                } catch (Exception e) {
                    fail++;
                    log.error("[report] fail hp={} type={} reason={}", hp.id(), type, e.getMessage(), e);
                }
            }
            log.info("[report] done type={} total={} success={} fail={}", type, hps.size(), success, fail);
        } finally {
            lock.unlock(lockKey, token);
        }
    }

    public void generateOne(ReportType type, ReportPeriod period, HazardPointBrief hp) {
        ReportRecord existing = mapper.selectExistingSuccess(type.code(), hp.id(), period.start(), period.end());
        if (existing != null) {
            log.info("[report] skip exists hp={} type={} period={}", hp.id(), type, period.key());
            return;
        }

        ReportRecord placeholder = new ReportRecord();
        placeholder.setTemplateId(null);
        placeholder.setTemplateName(type.name().toLowerCase());
        placeholder.setType(type.code());
        placeholder.setPeriodStart(period.start());
        placeholder.setPeriodEnd(period.end());
        placeholder.setHazardPointId(hp.id());
        placeholder.setHazardPointCode(hp.code());
        placeholder.setHazardPointName(hp.name());
        placeholder.setReportName(hp.name() + " - 监测" + type.desc()
            + " (" + period.start() + "~" + period.end() + ")");
        placeholder.setStatus(1);
        placeholder.setDelFlag(0);
        mapper.insert(placeholder);
        Long id = placeholder.getId();

        try {
            ReportContext ctx = assembler.build(type, period, hp);
            ReportRenderer renderer = findRenderer(type);
            String html = renderer.render(ctx);
            mapper.updateStatusAndContent(id, 2, html, null);
        } catch (Exception e) {
            mapper.updateStatusAndContent(id, 3, null, truncate(e.getMessage(), 1000));
            throw e;
        }
    }

    private ReportRenderer findRenderer(ReportType type) {
        return renderers.stream()
            .filter(r -> r.type() == type)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("无匹配渲染器: " + type));
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
