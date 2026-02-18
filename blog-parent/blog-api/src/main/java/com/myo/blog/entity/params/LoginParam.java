package com.myo.blog.entity.params;

import lombok.Data;

@Data
public class LoginParam {

    private String account;

    private String password;

    private String nickname;

    private String email;

    private String code; // 验证码字段
}
