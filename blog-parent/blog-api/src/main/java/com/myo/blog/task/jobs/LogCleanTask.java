package com.myo.blog.task.jobs;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.SysTaskLogMapper;
import com.myo.blog.dao.pojo.SysTaskLog;
import com.myo.blog.service.SysLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 系统日志清理任务，用于定时清理过期的系统操作日志和任务调度日志
@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanTask {

    private final SysLogService sysLogService;
    // 注入任务调度日志的 Mapper
    private final SysTaskLogMapper sysTaskLogMapper;

    public void run() {
        // 30 天前的时间戳
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        log.info("[系统日志清理任务] 触发，开始清理过期日志...");

        try {
            // 1. 清理系统操作日志
            sysLogService.backupAndCleanLogs(thirtyDaysAgo);

            // 2. 清理定时任务调度日志
            LambdaQueryWrapper<SysTaskLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.lt(SysTaskLog::getCreateDate, thirtyDaysAgo);
            int deletedCount = sysTaskLogMapper.delete(queryWrapper);

            log.info("[系统日志清理任务] 执行完毕。共清理了 {} 条过期的任务调度日志。", deletedCount);
        } catch (Exception e) {
            log.error("[系统日志清理任务] 执行过程中发生异常", e);
            // 抛出异常，让外层的 SchedulingRunnable 捕获，从而触发我们刚写好的邮件告警
            throw new RuntimeException("日志清理执行失败", e);
        }
    }
}