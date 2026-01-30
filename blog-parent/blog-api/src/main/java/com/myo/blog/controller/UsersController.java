package com.myo.blog.controller;

import com.myo.blog.service.SysUserService;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.UserParam;
import com.myo.blog.utils.IpUtils;
import com.myo.blog.utils.HttpContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("users")
public class UsersController {

    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate; // 注入 Redis

    //根据token查询当前用户信息
    @GetMapping("currentUser")
    public Result currentUser(@RequestHeader("Authorization") String token){
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IpUtils.getIpAddr(request);
        log.info("查询当前用户信息 - IP: {}", ip);

        try {
            Result result = sysUserService.findUserByToken(token);
            if (result.isSuccess()) {
                log.debug("当前用户信息查询成功 - IP: {}", ip);
            } else {
                log.warn("当前用户信息查询失败 - IP: {}, 错误码: {}", ip, result.getCode());
            }
            return result;
        } catch (Exception e) {
            log.error("查询当前用户信息异常 - IP: {}, 异常信息: {}", ip, e.getMessage(), e);
            return Result.fail(500, "系统异常");
        }
    }
    @GetMapping("info/{id}")
    public Result getUserPublicInfo(@PathVariable("id") String id) {
        System.out.println(id);
        System.out.println("根据ID查询用户信息: " + sysUserService.findUserVoById(id));
        return Result.success(sysUserService.findUserVoById(id));
    }

    //根据用户名查询账号信息
    @PostMapping("queryUserByAccount")
    public Result findUserByAccount(@RequestBody UserParam user){
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IpUtils.getIpAddr(request);
        String account = user.getAccount();
        log.info("根据账号查询用户信息 - IP: {}, 账号: {}", ip, account);

        try {
            Result result = Result.success(sysUserService.findUserByAccount(user));
            log.debug("用户信息查询成功 - IP: {}, 账号: {}", ip, account);
            return result;
        } catch (Exception e) {
            log.error("用户信息查询异常 - IP: {}, 账号: {}, 异常信息: {}", ip, account, e.getMessage(), e);
            return Result.fail(500, "系统异常");
        }
    }

    //修改个人资料
    @PostMapping("updateUser")
    public Result updateUser(@RequestBody UserParam userParam,@RequestHeader("Authorization") String token){
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IpUtils.getIpAddr(request);
        String account = userParam.getAccount();

        log.info("修改用户资料 - IP: {}, 账号: {}", ip, account);

        try {
            int i = sysUserService.updateUser(userParam);
            if(i>0){
                log.info("用户资料修改成功 - IP: {}, 账号: {}, 影响行数: {}", ip, account, i);
                if (token != null) {
                    // 删除 Redis 缓存
                    redisTemplate.delete("TOKEN_" + token);
                }
            }


            return Result.success(i);
        } catch (Exception e) {
            log.error("用户资料修改异常 - IP: {}, 账号: {}, 异常信息: {}", ip, account, e.getMessage(), e);
            return Result.fail(500, "系统异常");
        }
    }

    //修改个人头像
    @PostMapping("updateUserAvatar")
    public Result updateUserAvatar(@RequestBody UserParam userParam){
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IpUtils.getIpAddr(request);
        String account = userParam.getAccount();
        log.info("修改用户头像 - IP: {}, 账号: {}", ip, account);

        try {
            int i = sysUserService.updateUserAvatar(userParam);
            log.info("用户头像修改成功 - IP: {}, 账号: {}, 影响行数: {}", ip, account, i);
            return Result.success(i);
        } catch (Exception e) {
            log.error("用户头像修改异常 - IP: {}, 账号: {}, 异常信息: {}", ip, account, e.getMessage(), e);
            return Result.fail(500, "系统异常");
        }
    }
}