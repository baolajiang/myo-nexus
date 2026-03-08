package com.myo.blog.controller;

import com.myo.blog.common.aop.RateLimit;
import com.myo.blog.service.LoginService;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.LoginParam;
import com.myo.blog.utils.IpUtils;
import com.myo.blog.utils.HttpContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("register")
/**
 * 注册控制器
 */
public class RegisterController {

    private final LoginService loginService;

    // 1分钟1次，防止邮件轰炸
    @RateLimit(time = 60, count = 1, msg = "验证码发送太频繁，请稍后再试")
    @PostMapping("sendCode")
    public Result sendCode(@RequestBody LoginParam loginParam) {
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IpUtils.getIpAddr(request);
        String email = loginParam.getEmail();

        log.info("验证码发送请求开始 - IP: {}, 邮箱: {}", ip, email);

        try {
            Result result = loginService.sendEmailCode(email);

            if (result.isSuccess()) {
                log.info("验证码发送成功 - IP: {}, 邮箱: {}", ip, email);
            } else {
                log.warn("验证码发送失败 - IP: {}, 邮箱: {}, 错误码: {}, 错误信息: {}",
                        ip, email, result.getCode(), result.getMsg());
            }

            return result;
        } catch (Exception e) {
            log.error("验证码发送异常 - IP: {}, 邮箱: {}, 异常信息: {}", ip, email, e.getMessage(), e);
            return Result.fail(500, "系统异常，请稍后重试");
        }
    }

    // 1分钟5次，防止批量注册
    @RateLimit(time = 60, count = 5, msg = "注册太频繁")
    @PostMapping
    public Result register(@RequestBody LoginParam loginParam){
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IpUtils.getIpAddr(request);
        String account = loginParam.getAccount();
        String email = loginParam.getEmail();

        log.info("注册请求开始 - IP: {}, 账号: {}, 邮箱: {}", ip, account, email);

        try {
            Result result = loginService.register(loginParam);

            if (result.isSuccess()) {
                log.info("注册成功 - IP: {}, 账号: {}, 邮箱: {}", ip, account, email);
            } else {
                log.warn("注册失败 - IP: {}, 账号: {}, 邮箱: {}, 错误码: {}, 错误信息: {}",
                        ip, account, email, result.getCode(), result.getMsg());
            }

            return result;
        } catch (Exception e) {
            log.error("注册异常 - IP: {}, 账号: {}, 邮箱: {}, 异常信息: {}",
                    ip, account, email, e.getMessage(), e);
            return Result.fail(500, "系统异常，请稍后重试");
        }
    }
}