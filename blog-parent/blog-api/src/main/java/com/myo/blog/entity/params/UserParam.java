package com.myo.blog.entity.params;

import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : myo
 * @create 2023/8/16 22:17
 * 作用：接收前端传递的用户数据，包括注册、修改资料等操作
 */
@Data
public class UserParam {
    private String id;
    private String account;//用户名
    private String password;//密码
    private String nickname;//昵称
    private String avatar;//头像
    private String email;//邮箱
    private String mobilePhoneNumber;//电话
    private Integer sex;//性别 0未知 1男 2女
    private String remark;//备注
    private String status;//状态 0-正常 1-警告 99-封禁
}
