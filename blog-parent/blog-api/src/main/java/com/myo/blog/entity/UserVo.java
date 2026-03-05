package com.myo.blog.entity;

import lombok.Data;
//作用：用于前端展示用户公开信息
@Data
public class UserVo {

    private String nickname;//昵称

    private String account;//账号名

    private String avatar;//头像

    private String id;//id

    private String email;//邮箱

    private Integer sex;//性别 0未知 1男 2女

    private String mobilePhoneNumber;//手机号

}
