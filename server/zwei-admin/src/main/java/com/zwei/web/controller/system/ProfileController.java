package com.zwei.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.zwei.common.annotation.Log;
import com.zwei.common.config.RuoYiConfig;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.common.core.domain.model.SysProfileResponse;
import com.zwei.common.core.domain.model.SysProfileUpdateRequest;
import com.zwei.common.core.domain.model.SysUserPasswordRequest;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.DateUtils;
import com.zwei.common.utils.SecurityUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.common.utils.file.FileUploadUtils;
import com.zwei.common.utils.file.FileUtils;
import com.zwei.common.utils.file.MimeTypeUtils;
import com.zwei.framework.web.service.TokenService;
import com.zwei.system.service.ISysUserService;

/**
 * 个人中心
 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    @GetMapping
    public AjaxResult profile()
    {
        SysUser currentUser = getLoginUser().getUser();
        return success(SysProfileResponse.fromUser(
                currentUser,
                userService.selectUserRoleGroup(getUsername()),
                userService.selectUserPostGroup(getUsername())));
    }

    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult updateProfile(@Validated @RequestBody SysProfileUpdateRequest request)
    {
        SysUser currentUser = getLoginUser().getUser();
        request.applyTo(currentUser);
        if (StringUtils.isNotEmpty(currentUser.getPhonenumber()) && !userService.checkPhoneUnique(currentUser))
        {
            return error("修改用户'" + getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(currentUser.getEmail()) && !userService.checkEmailUnique(currentUser))
        {
            return error("修改用户'" + getUsername() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser) > 0)
        {
            tokenService.setLoginUser(getLoginUser());
            return success();
        }
        return error("修改个人信息异常，请联系管理员");
    }

    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/password")
    public AjaxResult updatePassword(@Validated @RequestBody SysUserPasswordRequest request)
    {
        Long userId = getUserId();
        SysUser user = userService.selectUserById(userId);
        String password = user.getPassword();
        if (!SecurityUtils.matchesPassword(request.getOldPassword(), password))
        {
            return error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(request.getNewPassword(), password))
        {
            return error("新密码不能与旧密码相同");
        }
        String newPassword = SecurityUtils.encryptPassword(request.getNewPassword());
        if (userService.resetUserPwd(userId, newPassword) > 0)
        {
            getLoginUser().getUser().setPwdUpdateDate(DateUtils.getNowDate());
            getLoginUser().getUser().setPassword(newPassword);
            tokenService.setLoginUser(getLoginUser());
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "avatarfile", required = false) MultipartFile avatarFile) throws Exception
    {
        MultipartFile actualFile = file != null ? file : avatarFile;
        if (actualFile != null && !actualFile.isEmpty())
        {
            Long userId = getUserId();
            String avatar = FileUploadUtils.upload(RuoYiConfig.getAvatarPath(), actualFile, MimeTypeUtils.IMAGE_EXTENSION, true);
            if (userService.updateUserAvatar(userId, avatar))
            {
                String oldAvatar = getLoginUser().getUser().getAvatar();
                if (StringUtils.isNotEmpty(oldAvatar))
                {
                    FileUtils.deleteFile(RuoYiConfig.getProfile() + FileUtils.stripPrefix(oldAvatar));
                }
                getLoginUser().getUser().setAvatar(avatar);
                tokenService.setLoginUser(getLoginUser());
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }
}
