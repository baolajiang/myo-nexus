package com.myo.blog.handler;

import com.alibaba.fastjson.JSON;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.service.LoginService;
import com.myo.blog.utils.UserThreadLocal;
import com.myo.blog.entity.ErrorCode;
import com.myo.blog.entity.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
@Component
@Slf4j
// 身份认证拦截器：负责校验Token并将用户信息放入本地线程
public class AuthInterceptor implements HandlerInterceptor {

    private final LoginService loginService;

    @Override
// 调用时间：Controller方法处理之前,进行预处理
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
            return true;
        }

        if (!(handler instanceof HandlerMethod)){
            return true;
        }

        // 获取到Authorization中的token
        String token = request.getHeader("Authorization");

        // 判断是否为空
        if (StringUtils.isBlank(token)){
            Result result = Result.fail(ErrorCode.NO_LOGIN.getCode(), "未登录");
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().print(JSON.toJSONString(result));
            return false;
        }

        // 验证 Token
        SysUser sysUser = loginService.checkToken(token);

        // 判断 Token 是否有效
        if (sysUser == null){
            Result result = Result.fail(ErrorCode.NO_LOGIN.getCode(), "未登录");
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().print(JSON.toJSONString(result));
            return false;
        }

        // 登录验证成功，放行，将用户信息放入ThreadLocal
        UserThreadLocal.put(sysUser);
        return true;
    }

    @Override
// 多用于清理资源,在Controller方法处理完成后执行
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 防止内存泄漏
        UserThreadLocal.remove();
    }
}