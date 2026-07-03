package com.zwei.framework.web.service;

import java.util.Set;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.zwei.common.constant.CacheConstants;
import com.zwei.common.constant.Constants;
import com.zwei.common.constant.UserConstants;
import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.common.core.domain.model.LoginUser;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.common.exception.ServiceException;
import com.zwei.common.exception.user.BlackListException;
import com.zwei.common.exception.user.CaptchaException;
import com.zwei.common.exception.user.CaptchaExpireException;
import com.zwei.common.exception.user.UserNotExistsException;
import com.zwei.common.exception.user.UserPasswordNotMatchException;
import com.zwei.common.utils.DateUtils;
import com.zwei.common.utils.MessageUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.common.utils.ip.IpUtils;
import com.zwei.framework.security.context.AuthenticationContextHolder;
import com.zwei.log.application.service.LogCenterService;
import com.zwei.log.domain.LogAttributes;
import com.zwei.log.domain.enums.AuthEventType;
import com.zwei.log.domain.enums.LogExecutionStatus;
import com.zwei.log.domain.model.LogAuthRecord;
import com.zwei.system.service.ISysConfigService;
import com.zwei.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录校验方法
 *
 * @author zwei
 */
@Component
public class SysLoginService
{
    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private LogCenterService logCenterService;

    @Autowired
    private SysSmsCodeService sysSmsCodeService;

    @Autowired
    private SysPermissionService permissionService;

    /**
     * 登录验证
     *
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     * @param uuid 唯一标识
     * @param rememberMe 记住我
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid, Boolean rememberMe)
    {
        // 验证码校验
        validateCaptcha(username, code, uuid);
        // 登录前置校验
        loginPreCheck(username, password);
        // 用户验证
        Authentication authentication = null;
        try
        {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(authenticationToken);
        }
        catch (Exception e)
        {
            if (e instanceof BadCredentialsException)
            {
                publishLoginFailure(username, MessageUtils.message("user.password.not.match"));
                throw new UserPasswordNotMatchException();
            }
            else
            {
                publishLoginFailure(username, e.getMessage());
                throw new ServiceException(e.getMessage());
            }
        }
        finally
        {
            AuthenticationContextHolder.clearContext();
        }
        publishAuthRecord(username, AuthEventType.LOGIN_SUCCESS, LogExecutionStatus.SUCCESS.name(),
            MessageUtils.message("user.login.success"), null);
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser.getUserId());
        // 生成token
        return tokenService.createToken(loginUser, rememberMe);
    }

    /**
     * 短信验证码登录
     *
     * @param phone      手机号
     * @param smsCode    短信验证码
     * @param rememberMe 记住我
     * @return token
     */
    public String loginBySms(String phone, String smsCode, Boolean rememberMe)
    {
        sysSmsCodeService.verifyCode(phone, smsCode);
        checkLoginBlackList(phone);
        SysUser user = loadUserByPhone(phone);

        Set<String> permissions = permissionService.getMenuPermission(user);
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getDeptId(), user, permissions);
        recordLoginInfo(loginUser.getUserId());

        publishAuthRecord(phone, AuthEventType.LOGIN_SUCCESS, LogExecutionStatus.SUCCESS.name(),
            MessageUtils.message("user.login.success"), null, "SMS");

        return tokenService.createToken(loginUser, rememberMe);
    }

    private void checkLoginBlackList(String identifier) {
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            publishLoginFailure(identifier, MessageUtils.message("login.blocked"));
            throw new BlackListException();
        }
    }

    private SysUser loadUserByPhone(String phone) {
        SysUser user = userService.selectUserByPhone(phone);
        if (StringUtils.isNull(user))
        {
            publishLoginFailure(phone, MessageUtils.message("user.not.exists"));
            throw new UserNotExistsException();
        }
        if ("1".equals(user.getDelFlag()))
        {
            publishLoginFailure(phone, MessageUtils.message("user.password.delete"));
            throw new UserNotExistsException();
        }
        if ("1".equals(user.getStatus()))
        {
            publishLoginFailure(phone, MessageUtils.message("user.blocked"));
            throw new BlackListException();
        }
        return user;
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
            String captcha = redisCache.getCacheObject(verifyKey);
            if (captcha == null)
            {
                publishLoginFailure(username, MessageUtils.message("user.jcaptcha.expire"));
                throw new CaptchaExpireException();
            }
            redisCache.deleteObject(verifyKey);
            if (StringUtils.isEmpty(code) || !code.equalsIgnoreCase(captcha))
            {
                publishLoginFailure(username, MessageUtils.message("user.jcaptcha.error"));
                throw new CaptchaException();
            }
        }
    }

    /**
     * 登录前置校验
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            publishLoginFailure(username, MessageUtils.message("not.null"));
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            publishLoginFailure(username, MessageUtils.message("user.password.not.match"));
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            publishLoginFailure(username, MessageUtils.message("user.password.not.match"));
            throw new UserPasswordNotMatchException();
        }
        // IP黑名单校验
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            publishLoginFailure(username, MessageUtils.message("login.blocked"));
            throw new BlackListException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId)
    {
        userService.updateLoginInfo(userId, IpUtils.getIpAddr(), DateUtils.getNowDate());
    }

    private void publishLoginFailure(String username, String message) {
        publishAuthRecord(username, AuthEventType.LOGIN_FAIL, LogExecutionStatus.FAIL.name(), message, "LOGIN_FAIL");
    }

    private void publishAuthRecord(String username, AuthEventType eventType, String resultStatus, String message, String failureCode) {
        publishAuthRecord(username, eventType, resultStatus, message, failureCode, "PASSWORD");
    }

    private void publishAuthRecord(String username, AuthEventType eventType, String resultStatus, String message, String failureCode, String authChannel) {
        LogAuthRecord record = new LogAuthRecord();
        record.setUsername(username);
        record.setAuthEventType(eventType.name());
        record.setAuthChannel(authChannel);
        record.setResultStatus(resultStatus);
        record.setFailureMessage(message);
        record.setFailureCode(failureCode);
        record.setClientIp(IpUtils.getIpAddr());
        record.setClientLocation(com.zwei.common.utils.ip.AddressUtils.getRealAddressByIP(record.getClientIp()));
        HttpServletRequest request = com.zwei.common.utils.ServletUtils.getRequest();
        if (request != null) {
            record.setRequestUri(request.getRequestURI());
            record.setRequestMethod(request.getMethod());
            record.setUserAgent(StringUtils.substring(request.getHeader("User-Agent"), 0, 512));
            Object traceId = request.getAttribute(LogAttributes.TRACE_ID);
            Object requestId = request.getAttribute(LogAttributes.REQUEST_ID);
            if (traceId != null) {
                record.setTraceId(traceId.toString());
            }
            if (requestId != null) {
                record.setRequestId(requestId.toString());
            }
        }
        logCenterService.publishAuth(record);
    }
}
