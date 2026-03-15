package com.myo.blog.dao.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.baomidou.mybatisplus.annotation.TableField;
@Data
public class SysUser {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String account;

    private Integer sex; // 0未知 1男 2女

    private String avatar;

    private Long createDate;

    private Integer deleted;

    private String email;

    private Long lastLogin;

    private String mobilePhoneNumber;

    private String nickname;

    private String password;

    private String salt;

    private Integer status;// 0：正常，1：警告，99：被禁用


    private String lastIpaddr;

    private String remark;// 备注

    @TableField(exist = false) // 表示数据库表里没有这一列
    private Boolean online;


}
