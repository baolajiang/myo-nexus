package com.myo.blog.config;

import com.myo.blog.handler.IpBlackListInterceptor;
import com.myo.blog.handler.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMVCConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;//日志拦截
    // 注入黑名单拦截器
    @Autowired
    private IpBlackListInterceptor ipBlackListInterceptor;
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        /* 解决跨域问题 **/
        registry.addMapping("/**")
                // 允许的源（域名）
                .allowedOrigins("https://myo.pub", "https://admin.myo.pub", "http://localhost:48082", "http://localhost:48182")
                // 允许的请求方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许的请求头
                .allowedHeaders("*")
                // 是否允许凭据（cookies）
                .allowCredentials(true)
                // 预检请求的有效期（秒）
                .maxAge(3600);
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /* 拦截器配置 */
        //拦截test接口，拦截器主要用途：进行用户登录状态的拦截，日志的拦截等。
        // 1.注册黑名单拦截器 (放在最前面，先过这一关)
        // addPathPatterns("/**") 表示拦截所有接口
        registry.addInterceptor(ipBlackListInterceptor)
                .addPathPatterns("/**");
        registry.addInterceptor(loginInterceptor)
                /*指定拦截器要拦截的请求(支持*通配符)*/
                /*.addPathPatterns("/util/*")*/
                .addPathPatterns("/test")
                .addPathPatterns("/comments/create/change")
                .addPathPatterns("/articles/publish")
                .addPathPatterns("/admin/**") // 保护后台接口
                .addPathPatterns("/login/ticket"); // 保护获取票据的接口！
    }
}
