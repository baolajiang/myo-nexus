package com.myo.blog.entity.params;

import lombok.Data;

@Data
public class LoginParam {

    private String account;// 账号

    private String password;// 密码

    private String nickname;// 昵称

    private String email;// 邮箱

    private String avatar;// 头像


    private Integer sex; // 0未知 1男 2女

    private String code; // 验证码字段
}
