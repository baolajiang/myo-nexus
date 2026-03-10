package com.myo.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.dao.pojo.SysLog;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;

public interface SysLogService {
    /**
     * 分页查询操作日志
     */
    Result listLog(PageParams pageParams);


    // 备份并清理过期日志
    void backupAndCleanLogs(Long expireTime);

}