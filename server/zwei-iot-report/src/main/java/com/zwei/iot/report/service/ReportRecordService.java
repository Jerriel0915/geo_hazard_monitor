package com.zwei.iot.report.service;

import com.zwei.common.core.page.PageResult;
import com.zwei.iot.report.domain.ReportRecord;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.domain.dto.ReportRecordDetailVO;
import com.zwei.iot.report.domain.dto.ReportRecordPageDTO;
import com.zwei.iot.report.domain.dto.ReportRecordVO;
import com.zwei.iot.report.mapper.ReportRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportRecordService {

    private final ReportRecordMapper mapper;

    public ReportRecordService(ReportRecordMapper mapper) {
        this.mapper = mapper;
    }

    public PageResult<ReportRecordVO> page(ReportRecordPageDTO params) {
        int pageNum = params.getPageNum() == null ? 1 : params.getPageNum();
        int pageSize = params.getPageSize() == null ? 20 : params.getPageSize();

        long total = mapper.countPageList(
                params.getType(), params.getHazardPointId(),
                params.getPeriodStart(), params.getPeriodEnd(),
                params.getStatus(), params.getKeyword());

        List<ReportRecord> all = mapper.selectPageList(
                params.getType(), params.getHazardPointId(),
                params.getPeriodStart(), params.getPeriodEnd(),
                params.getStatus(), params.getKeyword());

        List<ReportRecordVO> rows = all.stream()
                .skip((long) (pageNum - 1) * pageSize)
                .limit(pageSize)
                .map(ReportRecordService::toVO)
                .collect(Collectors.toList());

        PageResult<ReportRecordVO> result = new PageResult<>();
        result.setRows(rows);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    public ReportRecordDetailVO detail(Long id) {
        ReportRecord r = mapper.selectById(id);
        if (r == null) {
            return null;
        }
        ReportRecordDetailVO vo = new ReportRecordDetailVO();
        copyBase(r, vo);
        vo.setContent(r.getContent());
        return vo;
    }

    public boolean remove(Long id) {
        return mapper.updateDeleteFlag(id, 1) > 0;
    }

    /**
     * 幂等检查: 查找同 type+隐患点+时间段 已成功生成的报告 ID。
     *
     * @return 已存在则返回其 id, 否则返回 null
     */
    public Long findExisting(Integer type, Long hazardPointId, LocalDate periodStart, LocalDate periodEnd) {
        ReportRecord r = mapper.selectExistingSuccess(type, hazardPointId, periodStart, periodEnd);
        return r == null ? null : r.getId();
    }

    static void copyBase(ReportRecord r, ReportRecordVO vo) {
        vo.setId(r.getId());
        vo.setType(r.getType());
        vo.setTypeDesc(r.getType() == null ? "" : ReportType.fromCode(r.getType()).desc());
        vo.setPeriodStart(r.getPeriodStart());
        vo.setPeriodEnd(r.getPeriodEnd());
        vo.setHazardPointId(r.getHazardPointId());
        vo.setHazardPointCode(r.getHazardPointCode());
        vo.setHazardPointName(r.getHazardPointName());
        vo.setReportName(r.getReportName());
        vo.setStatus(r.getStatus());
        vo.setStatusDesc(statusDesc(r.getStatus()));
        vo.setErrorMsg(r.getErrorMsg());
        vo.setCreateTime(r.getCreateTime());
    }

    static ReportRecordVO toVO(ReportRecord r) {
        ReportRecordVO vo = new ReportRecordVO();
        copyBase(r, vo);
        return vo;
    }

    static String statusDesc(Integer s) {
        if (s == null) {
            return "";
        }
        return switch (s) {
            case 1 -> "生成中";
            case 2 -> "已生成";
            case 3 -> "生成失败";
            default -> "";
        };
    }
}
