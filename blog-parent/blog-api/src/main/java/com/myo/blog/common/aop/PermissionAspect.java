package com.myo.blog.common.aop;

import com.myo.blog.dao.mapper.SysUserMapper;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.entity.ErrorCode;
import com.myo.blog.entity.Result;
import com.myo.blog.service.SysUserService;
import com.myo.blog.utils.UserThreadLocal;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限切面类
 * 作用：拦截带有 @RequirePermission 注解的方法，进行权限校验
 */
@Aspect     // 1. 声明这是一个切面类
@Component  // 2. 将该类交给 Spring 容器管理（实例化为 Bean）
public class PermissionAspect {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 定义切点 (Pointcut)
     * 意思就是：瞄准所有头上贴了 @RequirePermission 注解的方法
     * 只要方法上有这个注解，就会触发下面的 around 逻辑
     */
    @Pointcut("@annotation(com.myo.blog.common.aop.RequirePermission)")
    public void permissionPointCut() {}



    @Around("permissionPointCut() && @annotation(requirePermission)")
    public Object around(ProceedingJoinPoint point, RequirePermission requirePermission) throws Throwable {

        SysUser sysUser = UserThreadLocal.get();

        if (sysUser == null) {
            return Result.fail(ErrorCode.NO_LOGIN.getCode(), "未登录");
        }

        // ============================================================
        // 【删除】原来的第二步：开后门（超级管理员特权）
        // 因为站长现在也有具体的权限列表了，不需要这个特殊的 if 判断了
        // 直接删掉下面这段 if 代码
        // if (sysUser.getAdmin() != null && sysUser.getAdmin() == 1) { ... }
        // ============================================================

        // 第三步：获取方法要求的权限
        String requiredPermission = requirePermission.value();

        // 第四步：查钥匙
        List<String> userPermissions = sysUserService.findPermissionsByUserId(sysUser.getId());

        // 第五步：比对
        if (userPermissions.contains(requiredPermission)) {
            return point.proceed();
        } else {
            return Result.fail(ErrorCode.NO_PERMISSION.getCode(), "无权访问");
        }
    }
}