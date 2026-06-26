package com.zwei.framework.plugin;

import com.zwei.common.core.domain.BaseEntity;
import com.zwei.common.core.domain.model.LoginUser;
import com.zwei.common.utils.SecurityUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * MyBatis 审计字段自动填充插件。
 * <p>
 * 在 INSERT / UPDATE 执行前，自动为继承 {@link BaseEntity} 的参数注入审计字段：
 * <ul>
 *   <li>INSERT：若 createBy / createTime 为空则自动填充</li>
 *   <li>UPDATE：若 updateBy 为空则自动填充；若 updateTime 为空则自动填充</li>
 * </ul>
 * <p>
 * 若当前线程无认证用户（如后台任务 / 系统调用），则跳过填充，不影响原有逻辑。
 * <p>
 * 注册方式：在 {@code MyBatisConfig.sqlSessionFactory()} 中通过
 * {@code sessionFactory.setPlugins(auditFieldInterceptor())} 注册。
 *
 * @author zwei
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class AuditFieldInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(AuditFieldInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object parameter = invocation.getArgs()[1];
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];

        if (parameter instanceof BaseEntity entity) {
            String username = resolveUsername();
            if (username != null) {
                SqlCommandType commandType = ms.getSqlCommandType();
                if (commandType == SqlCommandType.INSERT) {
                    fillCreateAudit(entity, username);
                } else if (commandType == SqlCommandType.UPDATE) {
                    fillUpdateAudit(entity, username);
                }
            }
        }

        return invocation.proceed();
    }

    /**
     * 安全获取当前用户名，未认证时返回 null（后台任务场景）。
     */
    private String resolveUsername() {
        try {
            LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
            return loginUser != null ? loginUser.getUsername() : null;
        } catch (Exception e) {
            log.debug("无法获取当前登录用户，跳过审计字段自动填充: {}", e.getMessage());
            return null;
        }
    }

    private void fillCreateAudit(BaseEntity entity, String username) {
        if (entity.getCreateBy() == null || entity.getCreateBy().isBlank()) {
            entity.setCreateBy(username);
        }
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(new Date());
        }
    }

    private void fillUpdateAudit(BaseEntity entity, String username) {
        if (entity.getUpdateBy() == null || entity.getUpdateBy().isBlank()) {
            entity.setUpdateBy(username);
        }
        if (entity.getUpdateTime() == null) {
            entity.setUpdateTime(new Date());
        }
    }
}
