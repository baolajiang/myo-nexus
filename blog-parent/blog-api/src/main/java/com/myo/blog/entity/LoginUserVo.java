package com.myo.blog.entity;

import lombok.Data;
//作用：登录后返回给前端的用户信息
@Data
public class LoginUserVo {

    private String id;

    private String account;

    private String nickname;//昵称

    private String avatar;//头像

    private Integer sex;//性别 0未知 1男 2女

    private String email;//邮箱

    private String mobilePhoneNumber;//手机号
}
