package com.myo.blog.entity.params;

import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : myo
 * @create 2023/8/3 23:30
 */
@Data
public class CaptchaParam {
    private String verCode; // 验证码
    private String verKey; // 验证码 key
}
