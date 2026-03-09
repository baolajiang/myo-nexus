package com.myo.blog.config;

import com.myo.blog.handler.AdminInterceptor;
import com.myo.blog.handler.IpBlackListInterceptor;
import com.myo.blog.handler.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 全局配置
 * 拦截器执行顺序（按注册顺序依次执行）：
 *   IpBlackListInterceptor — 拦截所有请求，黑名单 IP 直接拒绝，最先执行
 *   LoginInterceptor — 验证登录状态，将 SysUser 存入 UserThreadLocal
 *   AdminInterceptor — 验证管理员权限，只作用于 /admin/** 路径
 *
 */
@Configuration
@RequiredArgsConstructor
public class WebMVCConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    private final IpBlackListInterceptor ipBlackListInterceptor;

    /**
     * AdminInterceptor 负责校验管理员权限，
     * 必须在 LoginInterceptor 之后注册，保证 UserThreadLocal 中已有当前用户信息
     */
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://myo.pub",
                        "https://admin.myo.pub",
                        "http://localhost:48082",
                        "http://localhost:48182"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 第一道：IP 黑名单拦截器，拦截所有请求，黑名单直接拒绝
        registry.addInterceptor(ipBlackListInterceptor)
                .addPathPatterns("/**");

        // 第二道：登录拦截器，验证 token 并将用户信息存入 UserThreadLocal
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/test")
                .addPathPatterns("/comments/create/change")
                .addPathPatterns("/articles/publish")
                .addPathPatterns("/articles/update")
                .addPathPatterns("/articles/delete")
                .addPathPatterns("/articles/my")
                .addPathPatterns("/admin/**")
                .addPathPatterns("/categorys/**")
                .addPathPatterns("/tags/**")
                .addPathPatterns("/login/ticket");

        // 第三道：管理员权限拦截器，必须在 LoginInterceptor 之后注册
        // 只拦截 /admin/** 路径，普通用户访问后台接口会被拦截
        // 注意：此处 AdminInterceptor 依赖 UserThreadLocal 中已存在的用户信息，
        // 故顺序不可颠倒
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
    }
}