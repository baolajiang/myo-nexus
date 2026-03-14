package com.myo.blog.task.jobs;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.SysTaskLogMapper;
import com.myo.blog.dao.pojo.SysTaskLog;
import com.myo.blog.service.SysLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 系统日志清理任务
 *
 * 定时清理数据库中两类过期日志，防止日志表无限膨胀占用存储空间：
 * 1. 系统操作日志（sys_log 表）：通过 SysLogService.backupAndCleanLogs() 先备份再清理
 * 2. 定时任务执行日志（sys_task_log 表）：直接删除过期记录
 *
 * 默认清理 30 天前的日志，可通过参数动态指定天数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanTask {

    // 引用系统操作日志服务，负责对 sys_log 表进行备份并清理过期记录
    private final SysLogService sysLogService;

    // 引用定时任务执行日志 Mapper，用于删除 sys_task_log 表中的过期记录
    private final SysTaskLogMapper sysTaskLogMapper;

    /**
     * 无参方法：供定时任务自动调度使用
     * 默认清理 30 天前的日志
     */
    public void run() {
        log.info("[系统日志清理任务] 触发，开始清理过期日志...");
        // 计算 30 天前的时间戳作为清理基准线
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        executeClean(thirtyDaysAgo);
    }

    /**
     * 有参方法：数据库中配置了 taskParam 时调用此方法（自动调度和手动执行都可能走这里）
     * 参数为保留天数，超过该天数的日志将被清理
     *
     * 传入非数字字符串时会抛出 NumberFormatException，
     * SchedulingRunnable 识别后自动阻断重试并静默处理，不发送告警邮件。
     *
     * @param param 保留天数，如 "7" 表示清理 7 天前的日志，不传则默认 30 天
     */
    public void run(String param) {
        log.info("[系统日志清理任务] 接收到动态参数：{}", param);
        // 有参数则解析为天数，无参数则默认 30 天
        int daysToClean = org.springframework.util.StringUtils.hasText(param)
                ? Integer.parseInt(param.trim()) : 30;
        executeClean(System.currentTimeMillis() - ((long) daysToClean * 24 * 60 * 60 * 1000));
    }

    /**
     * 核心清理逻辑：清理指定时间点之前的所有过期日志
     *
     * @param timeAgo 清理基准时间戳，早于此时间的日志将被清理
     */
    private void executeClean(long timeAgo) {
        try {
            // 清理系统操作日志（先备份到归档表，再删除原表中的过期记录）
            sysLogService.backupAndCleanLogs(timeAgo);

            // 清理定时任务执行日志，删除 createDate 早于 timeAgo 的记录
            LambdaQueryWrapper<SysTaskLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.lt(SysTaskLog::getCreateDate, timeAgo);
            int deletedCount = sysTaskLogMapper.delete(queryWrapper);

            log.info("[系统日志清理任务] 执行完毕。共清理了 {} 条过期的任务调度日志。", deletedCount);
        } catch (Exception e) {
            log.error("[系统日志清理任务] 执行过程中发生异常", e);
            // 向上抛出异常，触发 SchedulingRunnable 的重试和告警机制
            throw new RuntimeException("日志清理执行失败", e);
        }
    }
}