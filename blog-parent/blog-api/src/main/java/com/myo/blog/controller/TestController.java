package com.myo.blog.controller;

import com.myo.blog.common.aop.RateLimit; // 记得引入限流注解
import com.myo.blog.entity.Result;
import com.myo.blog.utils.IpUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController

@RequestMapping("test")
public class TestController {

    // 原有的测试接口保持不变
    @GetMapping
    public Result test(){
        return Result.success(null);
    }


}