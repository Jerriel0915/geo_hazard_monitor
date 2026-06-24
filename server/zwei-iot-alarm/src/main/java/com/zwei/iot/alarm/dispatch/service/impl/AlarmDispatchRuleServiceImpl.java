package com.zwei.iot.alarm.dispatch.service.impl;

import com.zwei.common.core.domain.entity.SysDept;
import com.zwei.common.core.domain.entity.SysRole;
import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.common.exception.ServiceException;
import com.zwei.common.utils.SecurityUtils;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleDevice;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleHazardPoint;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleRecipient;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleCreateRequest;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleDetailVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleItemVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleQuery;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleDeviceMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleHazardPointMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleRecipientMapper;
import com.zwei.iot.alarm.dispatch.service.IAlarmDispatchRuleService;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.service.IDeviceService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.service.IHazardPointService;
import com.zwei.system.service.ISysDeptService;
import com.zwei.system.service.ISysRoleService;
import com.zwei.system.service.ISysUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知规则 Service 实现
 */
@Service
public class AlarmDispatchRuleServiceImpl implements IAlarmDispatchRuleService {

    @Autowired private AlarmDispatchRuleMapper ruleMapper;
    @Autowired private AlarmDispatchRuleHazardPointMapper hpMapper;
    @Autowired private AlarmDispatchRuleDeviceMapper deviceMapper;
    @Autowired private AlarmDispatchRuleRecipientMapper recipientMapper;
    @Autowired private ISysRoleService roleService;
    @Autowired private ISysDeptService deptService;
    @Autowired private ISysUserService userService;
    @Autowired private IHazardPointService hazardPointService;
    @Autowired private IDeviceService deviceService;

    private static final String WILDCARD = "*";

    // ============= 列表 =============
    @Override
    public List<AlarmDispatchRuleItemVO> selectList(AlarmDispatchRuleQuery query) {
        // 查主表全量匹配结果（前端客户端分页）
        AlarmDispatchRule where = new AlarmDispatchRule();
        where.setName(query.getName());
        where.setEventType(query.getEventType());
        where.setIsEnabled(query.getIsEnabled());
        where.setDelFlag(0);
        List<AlarmDispatchRule> all = ruleMapper.selectListByWhere(where);
        if (all.isEmpty()) return Collections.emptyList();

        List<Long> ruleIds = all.stream().map(AlarmDispatchRule::getId).toList();

        // 批量查关联（避免 N+1）
        List<AlarmDispatchRuleHazardPoint> hpRows = hpMapper.selectByRuleIds(ruleIds);
        List<AlarmDispatchRuleDevice> devRows = deviceMapper.selectByRuleIds(ruleIds);
        Map<Long, List<AlarmDispatchRuleRecipient>> recipMap = groupRecip(
            recipientMapper.selectByRuleIds(ruleIds));

        // 批量解析 ID→名称（列表展示用）
        Map<String, String> hpNameMap = resolveHazardPointNames(hpRows);
        Map<String, String> devNameMap = resolveDeviceNames(devRows);

        Map<Long, List<AlarmDispatchRuleHazardPoint>> hpMap = groupHp(hpRows);
        Map<Long, List<AlarmDispatchRuleDevice>> devMap = groupDev(devRows);

        // 装配 VO
        return all.stream().map(rule -> toItemVO(
            rule, hpMap.getOrDefault(rule.getId(), Collections.emptyList()),
            devMap.getOrDefault(rule.getId(), Collections.emptyList()),
            recipMap.getOrDefault(rule.getId(), Collections.emptyList()),
            hpNameMap, devNameMap
        )).collect(Collectors.toList());
    }

    // ============= 详情 =============
    @Override
    public AlarmDispatchRuleDetailVO selectDetail(Long id) {
        AlarmDispatchRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new ServiceException("通知规则不存在: " + id);
        }

        AlarmDispatchRuleDetailVO vo = new AlarmDispatchRuleDetailVO();
        vo.setId(rule.getId());
        vo.setName(rule.getName());
        vo.setEventType(rule.getEventType());
        vo.setAlarmLevels(splitCsv(rule.getAlarmLevels()));
        vo.setChannels(splitCsv(rule.getChannels()));
        vo.setIsEnabled(rule.getIsEnabled());
        vo.setRemark(rule.getRemark());
        vo.setCreateTime(rule.getCreateTime());
        vo.setCreateBy(rule.getCreateBy());

        // 隐患点
        List<AlarmDispatchRuleHazardPoint> hps = hpMapper.selectByRuleId(id);
        vo.setHazardPointIds(hps.stream()
            .map(AlarmDispatchRuleHazardPoint::getHazardPointId).toList());

        // 设备
        List<AlarmDispatchRuleDevice> devs = deviceMapper.selectByRuleId(id);
        vo.setDeviceIds(devs.stream()
            .map(AlarmDispatchRuleDevice::getDeviceId).toList());

        // 接收人
        List<AlarmDispatchRuleRecipient> recips = recipientMapper.selectByRuleId(id);
        AlarmDispatchRuleDetailVO.RecipientDetail rd = new AlarmDispatchRuleDetailVO.RecipientDetail();
        rd.setHasWildcardRole(false);
        rd.setHasWildcardDept(false);
        rd.setHasWildcardUser(false);
        for (AlarmDispatchRuleRecipient r : recips) {
            switch (r.getRecipientType()) {
                case "ROLE" -> {
                    if (WILDCARD.equals(r.getRecipientId())) rd.setHasWildcardRole(true);
                }
                case "DEPT" -> {
                    if (WILDCARD.equals(r.getRecipientId())) rd.setHasWildcardDept(true);
                }
                case "USER" -> {
                    if (WILDCARD.equals(r.getRecipientId())) rd.setHasWildcardUser(true);
                }
            }
        }
        vo.setRecipients(rd);

        return vo;
    }

    // ============= 创建 =============
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int create(AlarmDispatchRuleCreateRequest req) {
        AlarmDispatchRule rule = new AlarmDispatchRule();
        rule.setName(req.getName());
        rule.setEventType(req.getEventType());
        rule.setAlarmLevels(joinCsv(req.getAlarmLevels()));
        rule.setChannels(joinCsv(req.getChannels()));
        rule.setIsEnabled(req.getIsEnabled() != null ? req.getIsEnabled() : 1);
        rule.setDelFlag(0);
        rule.setRemark(req.getRemark());
        rule.setCreateBy(SecurityUtils.getUsername());
        rule.setCreateTime(new Date());

        ruleMapper.insert(rule);

        Long ruleId = rule.getId();
        saveRelations(ruleId, req);
        return 1;
    }

    // ============= 更新 =============
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(Long id, AlarmDispatchRuleCreateRequest req) {
        AlarmDispatchRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new ServiceException("通知规则不存在: " + id);
        }
        rule.setName(req.getName());
        rule.setEventType(req.getEventType());
        rule.setAlarmLevels(joinCsv(req.getAlarmLevels()));
        rule.setChannels(joinCsv(req.getChannels()));
        rule.setIsEnabled(req.getIsEnabled());
        rule.setRemark(req.getRemark());
        rule.setUpdateBy(SecurityUtils.getUsername());
        rule.setUpdateTime(new Date());
        ruleMapper.updateById(rule);

        // 先删后插
        hpMapper.deleteByRuleId(id);
        deviceMapper.deleteByRuleId(id);
        recipientMapper.deleteByRuleId(id);
        saveRelations(id, req);
        return 1;
    }

    // ============= 删除 =============
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(Long id) {
        ruleMapper.logicDeleteById(id);
        hpMapper.deleteByRuleId(id);
        deviceMapper.deleteByRuleId(id);
        recipientMapper.deleteByRuleId(id);
        return 1;
    }

    // ============= 启停 =============
    @Override
    public int toggleEnabled(Long id, Integer isEnabled) {
        return ruleMapper.updateEnabled(id, isEnabled);
    }

    // ============= 接收人选项 =============
    @Override
    public RecipientOptions selectRecipientOptions() {
        List<AlarmDispatchRuleDetailVO.RoleOption> roles = roleService.selectRoleAll().stream()
            .map(r -> {
                AlarmDispatchRuleDetailVO.RoleOption o = new AlarmDispatchRuleDetailVO.RoleOption();
                o.setId(String.valueOf(r.getRoleId()));
                o.setName(r.getRoleName());
                return o;
            }).toList();

        List<AlarmDispatchRuleDetailVO.DeptOption> depts = deptService.selectDeptList(new SysDept()).stream()
            .map(d -> {
                AlarmDispatchRuleDetailVO.DeptOption o = new AlarmDispatchRuleDetailVO.DeptOption();
                o.setId(String.valueOf(d.getDeptId()));
                o.setName(d.getDeptName());
                o.setParentId(d.getParentId() == null ? null : String.valueOf(d.getParentId()));
                return o;
            }).toList();

        List<AlarmDispatchRuleDetailVO.UserOption> users = userService.selectUserList(new SysUser()).stream()
            .map(u -> {
                AlarmDispatchRuleDetailVO.UserOption o = new AlarmDispatchRuleDetailVO.UserOption();
                o.setId(String.valueOf(u.getUserId()));
                o.setName(u.getUserName());
                return o;
            }).toList();

        return new RecipientOptions(roles, depts, users);
    }

    // ============= 私有辅助 =============

    private void saveRelations(Long ruleId, AlarmDispatchRuleCreateRequest req) {
        // 隐患点（THRESHOLD / COMPREHENSIVE）
        boolean isAlarmType = "THRESHOLD".equals(req.getEventType())
            || "COMPREHENSIVE".equals(req.getEventType());
        if (isAlarmType && req.getHazardPointIds() != null) {
            List<String> normalized = normalizeWildcard(req.getHazardPointIds());
            if (!normalized.isEmpty()) {
                hpMapper.batchInsert(normalized.stream().map(hp -> {
                    AlarmDispatchRuleHazardPoint e = new AlarmDispatchRuleHazardPoint();
                    e.setRuleId(ruleId);
                    e.setHazardPointId(hp);
                    return e;
                }).toList());
            }
        }
        // 设备（仅 OFFLINE）
        if ("OFFLINE".equals(req.getEventType()) && req.getDeviceIds() != null) {
            List<String> normalized = normalizeWildcard(req.getDeviceIds());
            if (!normalized.isEmpty()) {
                deviceMapper.batchInsert(normalized.stream().map(d -> {
                    AlarmDispatchRuleDevice e = new AlarmDispatchRuleDevice();
                    e.setRuleId(ruleId);
                    e.setDeviceId(d);
                    return e;
                }).toList());
            }
        }
        // 接收人
        if (req.getRecipients() != null) {
            List<AlarmDispatchRuleRecipient> list = new ArrayList<>();
            buildRecipients(ruleId, "ROLE", req.getRecipients().getRoleIds(), list);
            buildRecipients(ruleId, "DEPT", req.getRecipients().getDeptIds(), list);
            buildRecipients(ruleId, "USER", req.getRecipients().getUserIds(), list);
            if (!list.isEmpty()) recipientMapper.batchInsert(list);
        }
    }

    private void buildRecipients(Long ruleId, String type, List<String> ids,
                                  List<AlarmDispatchRuleRecipient> out) {
        if (ids == null || ids.isEmpty()) return;
        List<String> normalized = normalizeWildcard(ids);
        for (String id : normalized) {
            AlarmDispatchRuleRecipient r = new AlarmDispatchRuleRecipient();
            r.setRuleId(ruleId);
            r.setRecipientType(type);
            r.setRecipientId(id);
            out.add(r);
        }
    }

    /**
     * 通配符归一化：列表含 "*" 时只保留 "*"（与其他具体项互斥）。
     * 同时去重、去空。
     */
    private List<String> normalizeWildcard(List<String> ids) {
        if (ids == null) return Collections.emptyList();
        Set<String> seen = new LinkedHashSet<>();
        for (String id : ids) {
            if (StringUtils.isBlank(id)) continue;
            seen.add(id.trim());
        }
        if (seen.contains(WILDCARD)) {
            return Collections.singletonList(WILDCARD);
        }
        return new ArrayList<>(seen);
    }

    private AlarmDispatchRuleItemVO toItemVO(AlarmDispatchRule rule,
            List<AlarmDispatchRuleHazardPoint> hps,
            List<AlarmDispatchRuleDevice> devs,
            List<AlarmDispatchRuleRecipient> recips,
            Map<String, String> hpNameMap,
            Map<String, String> devNameMap) {
        AlarmDispatchRuleItemVO vo = new AlarmDispatchRuleItemVO();
        vo.setId(rule.getId());
        vo.setName(rule.getName());
        vo.setEventType(rule.getEventType());
        vo.setAlarmLevels(splitCsv(rule.getAlarmLevels()));
        vo.setChannels(splitCsv(rule.getChannels()));
        vo.setIsEnabled(rule.getIsEnabled());
        vo.setCreateTime(rule.getCreateTime());
        vo.setCreateBy(rule.getCreateBy());
        vo.setRemark(rule.getRemark());

        // 隐患点
        boolean hpAll = hps.stream().anyMatch(h -> WILDCARD.equals(h.getHazardPointId()));
        vo.setHazardPointAll(hpAll);
        if (!hpAll) {
            vo.setHazardPointNames(hps.stream()
                .map(h -> hpNameMap.getOrDefault(h.getHazardPointId(), h.getHazardPointId()))
                .toList());
        }

        // 设备
        boolean devAll = devs.stream().anyMatch(d -> WILDCARD.equals(d.getDeviceId()));
        vo.setDeviceAll(devAll);
        if (!devAll) {
            vo.setDeviceNames(devs.stream()
                .map(d -> devNameMap.getOrDefault(d.getDeviceId(), d.getDeviceId()))
                .toList());
        }

        // 接收人
        long roleCnt = recips.stream().filter(r -> "ROLE".equals(r.getRecipientType())).count();
        long deptCnt = recips.stream().filter(r -> "DEPT".equals(r.getRecipientType())).count();
        long userCnt = recips.stream().filter(r -> "USER".equals(r.getRecipientType())).count();
        boolean recipAll = recips.stream().anyMatch(r -> WILDCARD.equals(r.getRecipientId()));
        vo.setRecipientAll(recipAll);
        if (!recipAll) {
            vo.setRecipientSummary(
                roleCnt + " 角色 / " + deptCnt + " 部门 / " + userCnt + " 人");
        }
        return vo;
    }

    private Map<Long, List<AlarmDispatchRuleHazardPoint>> groupHp(
            List<AlarmDispatchRuleHazardPoint> list) {
        return list.stream().collect(
            Collectors.groupingBy(AlarmDispatchRuleHazardPoint::getRuleId));
    }

    private Map<Long, List<AlarmDispatchRuleDevice>> groupDev(
            List<AlarmDispatchRuleDevice> list) {
        return list.stream().collect(
            Collectors.groupingBy(AlarmDispatchRuleDevice::getRuleId));
    }

    /**
     * 解析隐患点 ID → 名称映射（列表展示用）。
     * 只处理非通配符的数值型 ID；通配符 "*" 由调用方自行判断。
     * 利用 IHazardPointService 的 @Cacheable("hazardPoint") 减少重复查询。
     */
    private Map<String, String> resolveHazardPointNames(List<AlarmDispatchRuleHazardPoint> rows) {
        Set<String> ids = rows.stream()
            .map(AlarmDispatchRuleHazardPoint::getHazardPointId)
            .filter(id -> !WILDCARD.equals(id))
            .collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        Map<String, String> result = new HashMap<>(ids.size());
        for (String id : ids) {
            try {
                HazardPoint hp = hazardPointService.selectHazardPointById(Long.parseLong(id));
                if (hp != null) result.put(id, hp.getName());
            } catch (NumberFormatException ignored) {
                // 非数值 ID 直接跳过，列表展示回退为原 ID
            }
        }
        return result;
    }

    /**
     * 解析设备 ID → 名称映射（列表展示用）。
     */
    private Map<String, String> resolveDeviceNames(List<AlarmDispatchRuleDevice> rows) {
        Set<String> ids = rows.stream()
            .map(AlarmDispatchRuleDevice::getDeviceId)
            .filter(id -> !WILDCARD.equals(id))
            .collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        Map<String, String> result = new HashMap<>(ids.size());
        for (String id : ids) {
            try {
                Device dev = deviceService.selectDeviceById(Long.parseLong(id));
                if (dev != null) result.put(id, dev.getName());
            } catch (NumberFormatException ignored) {
                // 非数值 ID 直接跳过
            }
        }
        return result;
    }

    private Map<Long, List<AlarmDispatchRuleRecipient>> groupRecip(
            List<AlarmDispatchRuleRecipient> list) {
        return list.stream().collect(
            Collectors.groupingBy(AlarmDispatchRuleRecipient::getRuleId));
    }

    private List<String> splitCsv(String csv) {
        if (StringUtils.isBlank(csv)) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private String joinCsv(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }
}
