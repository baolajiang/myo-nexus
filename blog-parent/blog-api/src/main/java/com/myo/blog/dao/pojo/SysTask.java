package com.myo.blog.dao.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("myo_sys_task")
public class SysTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskName;
    private String taskGroup;
    private String beanName;
    private String methodName;
    private String cronExpression;
    private Integer status;
    private String remark;
    private Long createDate;
    private String taskParam;//支持动态执行参数
}