package com.myo.blog.dao.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class SysLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long createDate;

    private String ip;

    private String method;

    private String module;

    private String nickname;

    private String operation;

    private String params;

    private Long time;

    private String userid;

    private Integer status; // 0成功，1失败

    private String errorMsg;

    private String result;

    private String traceId;
}
