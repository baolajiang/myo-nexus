package com.myo.blog.dao.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("myo_sys_task_log")
public class SysTaskLog {

    // 换成 AUTO，交给 MySQL 底层去自增，保证绝对不会出现 null 报错
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private String taskName;
    private String beanName;
    private Integer status;
    private String errorInfo;
    private Long costTime;
    private Long createDate;
}