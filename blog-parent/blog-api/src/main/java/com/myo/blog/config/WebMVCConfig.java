package com.myo.blog.config;

import com.myo.blog.handler.AdminInterceptor;
import com.myo.blog.handler.IpBlackListInterceptor;
import com.myo.blog.handler.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 全局 Web 配置
 *
 * 核心职责：
 * 1. 配置 CORS 跨域规则，保障前后端分离架构下的安全访问
 * 2. 注册并编排全局拦截器，构建系统的三道安全防线
 *
 * 拦截器防线（严格按注册顺序执行）：
 * 【最外层】IpBlackListInterceptor：流量清洗，识别并阻断黑名单 IP 的所有请求
 * 【核心层】AuthInterceptor：身份鉴权，校验 Token 并在 UserThreadLocal 挂载当前用户上下文
 * 【最内层】AdminInterceptor：越权防护，基于已挂载的用户信息，校验 /admin/** 路径的管理员权限
 */
@Configuration
@RequiredArgsConstructor
public class WebMVCConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final IpBlackListInterceptor ipBlackListInterceptor;
    private final AdminInterceptor adminInterceptor;

    /**
     * 跨域资源共享（CORS）配置
     *
     * - 采用白名单机制（allowedOrigins），仅放行指定的生产与开发环境域名
     * - 放行 OPTIONS 预检请求以及标准 RESTful 动作
     * - 开启 allowCredentials，允许携带凭证（如 Cookie）
     * - 预检结果缓存 1 小时（maxAge），大幅降低浏览器 OPTIONS 请求带来的网络开销
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://myo.pub",          // 线上前台域名
                        "https://admin.myo.pub",    // 线上后台域名
                        "http://localhost:48082",   // 本地前台开发环境
                        "http://localhost:48182"    // 本地后台开发环境
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许所有 HTTP 方法
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(true)// 允许携带凭证（如 Cookie）
                .maxAge(3600);// 预检结果缓存 1 小时
    }

    /**
     * 注册 HTTP 请求拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 第一道防线：IP 黑名单无差别拦截
        registry.addInterceptor(ipBlackListInterceptor)
                .addPathPatterns("/**");// 对所有路径应用 IP 黑名单拦截

        // 第二道防线：登录鉴权与上下文挂载
        registry.addInterceptor(authInterceptor)
                // --- 必须登录才能访问的私有区域 ---
                .addPathPatterns("/test")
                .addPathPatterns("/comments/create/change")
                .addPathPatterns("/articles/publish")
                .addPathPatterns("/articles/update")
                .addPathPatterns("/articles/delete")
                .addPathPatterns("/articles/my")
                .addPathPatterns("/admin/**")
                .addPathPatterns("/login/ticket")
                // 【注意】：如果有 /categorys/create 这种写操作，必须先把大区锁住
                 .addPathPatterns("/categorys/**")
                 .addPathPatterns("/tags/**")

                // --- 无需登录即可访问的公共展示区域 ---
                // 【提示】：只有上方 addPathPatterns 锁定过的大区，才需要在这里开特批。
                // 如果上面没锁 /categorys/**，这里的排除其实是多余的，可酌情删除。
                .excludePathPatterns("/categorys")
                .excludePathPatterns("/categorys/detail")
                .excludePathPatterns("/categorys/detail/**")
                .excludePathPatterns("/tags")
                .excludePathPatterns("/tags/detail")
                .excludePathPatterns("/tags/detail/**");

        // 第三道防线：后台敏感接口的超级权限校验
        // 依赖 AuthInterceptor 提前写入的 UserContext
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
    }
}