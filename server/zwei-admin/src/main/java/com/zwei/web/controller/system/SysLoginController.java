package com.zwei.web.controller.system;

import com.zwei.common.annotation.Anonymous;
import com.zwei.common.annotation.RateLimiter;
import com.zwei.common.annotation.RepeatSubmit;
import com.zwei.common.constant.Constants;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.domain.entity.SysMenu;
import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.common.core.domain.model.LoginBody;
import com.zwei.common.core.domain.model.LoginUser;
import com.zwei.common.core.text.Convert;
import com.zwei.common.enums.LimitType;
import com.zwei.common.utils.DateUtils;
import com.zwei.common.utils.SecurityUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.framework.web.service.SysLoginService;
import com.zwei.framework.web.service.SysPermissionService;
import com.zwei.framework.web.service.SysSmsCodeService;
import com.zwei.framework.web.service.TokenService;
import com.zwei.system.service.ISysConfigService;
import com.zwei.system.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 登录验证
 * 
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/auth")
public class SysLoginController
{
    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysSmsCodeService smsCodeService;

    /**
     * 发送短信验证码
     *
     * @param body 请求体 { phone }
     * @return 结果
     */
    @Anonymous
    @RateLimiter(time = 60, count = 3, limitType = LimitType.IP)
    @PostMapping("/sms")
    public AjaxResult sendSms(@RequestBody Map<String, String> body)
    {
        String phone = body.get("phone");
        if (StringUtils.isEmpty(phone))
        {
            return AjaxResult.error("手机号不能为空");
        }
        smsCodeService.sendCode(phone);
        return AjaxResult.success("验证码已发送");
    }

    /**
     * 登录方法
     *
     * @param loginBody 登录信息
     * @return 结果
     */
    @RepeatSubmit
    @RateLimiter(time = 60, count = 5, limitType = LimitType.IP)
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        Map<String, Object> data = new HashMap<>();
        Integer expiresIN = loginBody.getRememberMe() ? 7 * 24 * 60 * 60 : 20 * 60;
        String token;
        // 短信验证码登录
        if (StringUtils.isNotEmpty(loginBody.getSmsCode()))
        {
            token = loginService.loginBySms(loginBody.getPhone(), loginBody.getSmsCode(), loginBody.getRememberMe());
        }
        else
        {
            // 账号密码登录
            token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
                    loginBody.getUuid(), loginBody.getRememberMe());
        }

        data.put(Constants.TOKEN, token);
        data.put(Constants.EXPIRES_IN, expiresIN);
        return AjaxResult.success("登陆成功", data);
    }

    /**
     * 获取用户信息
     * 
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        if (!loginUser.getPermissions().equals(permissions))
        {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        ajax.put("pwdChrtype", getSysAccountChrtype());
        ajax.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
        ajax.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        return ajax;
    }

    /**
     * 获取路由信息
     * 
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }

    // 获取用户密码自定义配置规则
    public String getSysAccountChrtype()
    {
        return Convert.toStr(configService.selectConfigByKey("sys.account.chrtype"), "0");
    }

    // 检查初始密码是否提醒修改
    public boolean initPasswordIsModify(Date pwdUpdateDate)
    {
        Integer initPasswordModify = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return initPasswordModify != null && initPasswordModify == 1 && pwdUpdateDate == null;
    }

    // 检查密码是否过期
    public boolean passwordIsExpiration(Date pwdUpdateDate)
    {
        Integer passwordValidateDays = Convert.toInt(configService.selectConfigByKey("sys.account.passwordValidateDays"));
        if (passwordValidateDays != null && passwordValidateDays > 0)
        {
            if (StringUtils.isNull(pwdUpdateDate))
            {
                // 如果从未修改过初始密码，直接提醒过期
                return true;
            }
            Date nowDate = DateUtils.getNowDate();
            return DateUtils.differentDaysByMillisecond(nowDate, pwdUpdateDate) > passwordValidateDays;
        }
        return false;
    }
}
