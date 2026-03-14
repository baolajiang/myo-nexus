package com.myo.blog.admin;

import com.myo.blog.common.aop.LogAnnotation;
import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.entity.ErrorCode;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.entity.params.UserParam;
import com.myo.blog.service.CommentsService;
import com.myo.blog.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUsersController {
    private final SysUserService sysUserService;

    /**
     * 查看用户列表
     * 只要有 'user:list' 权限就能看
     */
    @RequirePermission("user:list")
    @PostMapping("/list")
    public Result UserList(@RequestBody PageParams pageParams) {

        return sysUserService.UserList(pageParams);
    }

    /**
     * 修改用户账号状态 (封禁/解封)
     * 只有拥有 'user:ban' 权限的管理员才能调用
     */
    @RequirePermission("user:ban")
    @PostMapping("/status")
    @LogAnnotation(module = "用户管理", operator = "更新用户状态")
    public Result updateUserStatus(@RequestBody UserParam userParam) {
        // 直接调用 Service 层的新方法
        return sysUserService.updateUserStatus(userParam);
    }
    /**
     * 更新用户信息 (用于编辑功能，可修改昵称、邮箱、手机号、状态等)
     */
    @PostMapping("/update")
    @RequirePermission("user:edit")
    @LogAnnotation(module = "用户管理", operator = "编辑用户资料")
    public Result updateUser(@RequestBody UserParam userParam) {
        // 复用 Service 层已有的 updateUser 方法
        // 该方法会处理基本信息更新，如果状态改为封禁(99)还会自动踢下线
        int count = sysUserService.updateUser(userParam);
        if (count > 0) {
            return Result.success("更新成功");
        }
        return Result.fail(ErrorCode.OPERATION_FAILED.getCode(), "更新失败");
    }



}
