package com.myo.blog.controller;

import com.myo.blog.common.aop.RateLimit;
import com.myo.blog.dao.mapper.SysUserMapper;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.entity.ErrorCode;
import com.myo.blog.service.LoginService;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.LoginParam;
import com.myo.blog.utils.IpUtils;
import com.myo.blog.utils.HttpContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("login")
public class LoginController {

    @Autowired
    private LoginService loginService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    // 注入 SysUserMapper 用于查询权限
    @Autowired
    private SysUserMapper sysUserMapper;

    // 防止暴力破解，1分钟限制5次
    @RateLimit(time = 60, count = 5, msg = "账号或密码错误次数过多，请1分钟后再试")
    @PostMapping
    public Result login(@RequestBody LoginParam loginParam){
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IpUtils.getIpAddr(request);
        String account = loginParam.getAccount();

        log.info("登录请求开始 - IP: {}, 账号: {}", ip, account);

        try {
            Result result = loginService.login(loginParam);

            if (result.isSuccess()) {
                log.info("登录成功 - IP: {}, 账号: {}", ip, account);
            } else {
                log.warn("登录失败 - IP: {}, 账号: {}, 错误码: {}, 错误信息: {}",
                        ip, account, result.getCode(), result.getMsg());
            }

            return result;
        } catch (Exception e) {
            log.error("登录异常 - IP: {}, 账号: {}, 异常信息: {}", ip, account, e.getMessage(), e);
            return Result.fail(500, "系统异常，请稍后重试");
        }
    }


    /**
     * 获取一次性票据 (Ticket)
     * 通过查询权限列表来验证是否为管理员
     */
    @PostMapping("ticket")
    public Result getTicket(@RequestHeader("Authorization") String token) {
        // 1. 校验 Token
        SysUser user = loginService.checkToken(token);
        if (user == null) {
            return Result.fail(ErrorCode.NO_LOGIN.getCode(), "未登录");
        }

        // 2.去数据库查该用户的权限列表
        // 既然没有 字段，那就看他有没有分配过权限。
        // 如果 permissions 为空，说明他是普通用户，不能进后台。
        List<String> permissions = sysUserMapper.findPermissionsByUserId(user.getId());

        if (permissions == null || permissions.isEmpty()) {
            return Result.fail(ErrorCode.NO_PERMISSION.getCode(), "您没有访问后台的权限");
        }

        // 3. 生成并存储 Ticket
        String ticket = "TICKET_" + UUID.randomUUID().toString().replace("-", "");

        // 存入 Redis：Key=票据, Value=Token, 有效期=30秒
        redisTemplate.opsForValue().set(ticket, token, 30, TimeUnit.SECONDS);

        return Result.success(ticket);
    }

    /**
     * 票据兑换 Token
     * 后台项目启动时调用，用票据换回真正的 Token
     */
    @PostMapping("ticket/exchange")
    public Result exchangeToken(@RequestBody String ticket) {
        // 简单处理引号（防止前端传过来带双引号的 JSON 字符串）
        String cleanTicket = ticket.replace("\"", "");

        // 去 Redis 查 Token
        String token = redisTemplate.opsForValue().get(cleanTicket);

        if (token == null) {
            return Result.fail(90004, "票据无效或已过期");
        }

        // 【关键】兑换成功后立即销毁，防止二次使用
        redisTemplate.delete(cleanTicket);

        return Result.success(token);
    }
}