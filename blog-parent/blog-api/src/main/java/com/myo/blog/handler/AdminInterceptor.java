package com.myo.blog.handler;

import com.alibaba.fastjson.JSON;
import com.myo.blog.dao.mapper.SysUserMapper;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.entity.ErrorCode;
import com.myo.blog.entity.Result;
import com.myo.blog.utils.UserThreadLocal;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

// 管理员拦截器
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {
    // 2. 注入 SysUserMapper
    private final SysUserMapper sysUserMapper;
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        SysUser sysUser = UserThreadLocal.get();
        if (sysUser == null) {
            return sendError(response, ErrorCode.NO_LOGIN.getCode(), "未登录");
        }


        // 去数据库查这个用户拥有的所有权限
        List<String> permissions = sysUserMapper.findPermissionsByUserId(sysUser.getId());

        // 如果权限列表为空，说明是普通用户（在数据库设计时没给普通用户分配任何权限）
        if (permissions == null || permissions.isEmpty()) {
            return sendError(response, ErrorCode.NO_PERMISSION.getCode(), "无权访问，需要管理员权限");
        }

        // 有权限，放行
        return true;
    }
    // 辅助方法：返回 JSON 格式的错误信息
    private boolean sendError(HttpServletResponse response, int code, String msg) throws Exception {
        Result result = Result.fail(code, msg);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().print(JSON.toJSONString(result));
        return false;
    }
}