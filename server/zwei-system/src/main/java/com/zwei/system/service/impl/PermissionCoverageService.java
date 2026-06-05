package com.zwei.system.service.impl;

import com.zwei.system.mapper.SysMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 权限覆盖分析服务。
 * <p>
 * 对比代码中 {@code @PreAuthorize} 注解声明的权限字符串与
 * 数据库 {@code sys_menu.perms} 表中的记录，识别缺失的菜单项。
 */
@Service
public class PermissionCoverageService {

    private final RequestMappingHandlerMapping handlerMapping;
    private final SysMenuMapper menuMapper;

    private static final Pattern PREAUTHORIZE_PATTERN =
            Pattern.compile("@ss\\.hasPermi\\('([^']+)'\\)");

    @Autowired
    public PermissionCoverageService(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
                                      SysMenuMapper menuMapper) {
        this.handlerMapping = handlerMapping;
        this.menuMapper = menuMapper;
    }

    /**
     * 权限覆盖报告。
     *
     * @return 代码中声明的权限、数据库中已注册的权限、仅在代码中的权限
     */
    public CoverageReport getCoverageReport() {
        Set<String> codePerms = scanCodePermissions();
        List<String> dbPerms = menuMapper.selectMenuPerms();
        Set<String> dbPermSet = dbPerms != null
                ? dbPerms.stream().filter(Objects::nonNull).collect(Collectors.toSet())
                : Collections.emptySet();

        Set<String> missingInDb = new LinkedHashSet<>(codePerms);
        missingInDb.removeAll(dbPermSet);

        return new CoverageReport(new ArrayList<>(codePerms), new ArrayList<>(dbPermSet),
                new ArrayList<>(missingInDb));
    }

    /**
     * 批量注册缺失的权限到菜单表。
     * <p>
     * 每条权限作为"系统监控"（parent_id=2）下的按钮（F）记录写入。
     *
     * @param perms 权限字符串列表
     * @return 实际写入的条数
     */
    public int batchRegister(List<String> perms) {
        if (perms == null || perms.isEmpty()) return 0;
        Set<String> existing = new HashSet<>(menuMapper.selectMenuPerms());
        int count = 0;
        for (String perm : perms) {
            if (perm == null || perm.isBlank() || existing.contains(perm)) continue;
            com.zwei.common.core.domain.entity.SysMenu menu = new com.zwei.common.core.domain.entity.SysMenu();
            menu.setParentId(2L); // 系统监控
            menu.setMenuName(perm.substring(perm.lastIndexOf(':') + 1));
            menu.setPerms(perm);
            menu.setMenuType("F");
            menu.setStatus("0");
            menu.setVisible("0");
            menu.setIsFrame("1");
            menu.setIsCache("0");
            menu.setOrderNum(0);
            menu.setCreateBy("system");
            menuMapper.insertMenu(menu);
            count++;
        }
        return count;
    }

    private Set<String> scanCodePermissions() {
        Set<String> perms = new LinkedHashSet<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        for (HandlerMethod handlerMethod : handlerMethods.values()) {
            PreAuthorize annotation = handlerMethod.getMethodAnnotation(PreAuthorize.class);
            if (annotation != null) {
                Matcher matcher = PREAUTHORIZE_PATTERN.matcher(annotation.value());
                while (matcher.find()) {
                    perms.add(matcher.group(1));
                }
            }
        }
        return perms;
    }

    public static class CoverageReport {
        public List<String> codePerms;
        public List<String> dbPerms;
        public List<String> missingInDb;

        public CoverageReport(List<String> codePerms, List<String> dbPerms, List<String> missingInDb) {
            this.codePerms = codePerms;
            this.dbPerms = dbPerms;
            this.missingInDb = missingInDb;
        }
    }
}
