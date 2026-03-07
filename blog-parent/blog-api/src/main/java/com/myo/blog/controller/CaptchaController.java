package com.myo.blog.controller;

import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.CaptchaParam;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * Created by IntelliJ IDEA.
 *
 * @Author : myo
 * @create 2023/8/3 21:39
 */
@RestController
@RequestMapping("yzm")
public class CaptchaController {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    // 生成验证码
    @PostMapping("/captchaClass")
    public Result captchaClass() throws Exception {
        // easy-captcha 1.6.2 可能在某些环境下会有字体问题，但在 base64 模式下通常可用
        SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 5);
        /**
         * TYPE_DEFAULT	数字和字母混合
         * TYPE_ONLY_NUMBER	纯数字
         * TYPE_ONLY_CHAR	纯字母
         * TYPE_ONLY_UPPER	纯大写字母
         * TYPE_ONLY_LOWER	纯小写字母
         * TYPE_NUM_AND_UPPER	数字和大写字母
         * **/
        specCaptcha.setCharType(Captcha.TYPE_DEFAULT);
        String verCode = specCaptcha.text().toLowerCase();
        String key = "yzm:" + UUID.randomUUID().toString().replace("-", "");

        // 存入redis并设置过期时间为2分钟
        redisTemplate.opsForValue().set(key, verCode, 2, TimeUnit.MINUTES);

        Map<String, Object> map = new HashMap<>();
        map.put("key", key);
        map.put("image", specCaptcha.toBase64());
        return Result.success(map);
    }
    // 登录时验证验证码
    @PostMapping("/login")
    public Result login(@RequestBody CaptchaParam captcha){
        String verKey = captcha.getVerKey();
        String verCode = captcha.getVerCode();
        // 获取redis中的验证码
        String redisCode = redisTemplate.opsForValue().get(verKey);

        if(redisCode == null){
            return Result.success("验证码已过期");
        }

        if (verCode == null || !redisCode.equals(verCode.trim().toLowerCase())) {
            return Result.success("验证码不正确");
        }

        // 验证成功，删除验证码
        redisTemplate.delete(verKey);


        return Result.success("验证码正确");
    }
}