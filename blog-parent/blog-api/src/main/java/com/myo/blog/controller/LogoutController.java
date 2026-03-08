package com.myo.blog.controller;

import com.myo.blog.service.LoginService;
import com.myo.blog.entity.Result;
import com.myo.blog.utils.IpUtils;
import com.myo.blog.utils.HttpContextUtils;
import com.myo.blog.dao.pojo.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("logout")
@RequiredArgsConstructor
/**
 * 退出登录控制器
 */
public class LogoutController {

    private final LoginService loginService;

    @GetMapping
    public Result logout(@RequestHeader("Authorization") String token){
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IpUtils.getIpAddr(request);

        // 记录退出登录请求开始
        log.info("用户退出登录请求开始 - IP: {}, Token: {}", ip, token != null ? "已提供" : "为空");

        try {
            // 先检查token有效性，获取用户信息
           SysUser sysUser = loginService.checkToken(token);
            if (sysUser != null) {
                log.info("退出登录用户信息 - 用户ID: {}, 账号: {}, 昵称: {}, IP: {}",
                        sysUser.getId(), sysUser.getAccount(), sysUser.getNickname(), ip);
            } else {
                log.warn("退出登录时用户信息获取失败 - Token无效或已过期, IP: {}", ip);
            }

            Result result = loginService.logout(token);
            if (result.isSuccess()) {
                if (sysUser != null) {
                    log.info("用户退出登录成功 - 用户ID: {}, 账号: {}, 昵称: {}, IP: {}",
                            sysUser.getId(), sysUser.getAccount(), sysUser.getNickname(), ip);
                } else {
                    log.info("用户退出登录成功 - IP: {}", ip);
                }
            } else {
                log.warn("用户退出登录失败 - IP: {}, 错误码: {}, 错误信息: {}",
                        ip, result.getCode(), result.getMsg());
            }
            return result;
        } catch (Exception e) {
            log.error("用户退出登录异常 - IP: {}, 异常信息: {}", ip, e.getMessage(), e);
            return Result.fail(500, "系统异常");
        }
    }
}