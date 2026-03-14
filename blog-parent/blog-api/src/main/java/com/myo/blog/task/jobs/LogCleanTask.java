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


    // ==========================================
    // 1. 无参方法（保持不变，供定时调度使用）
    // ==========================================
    public void run() {
        log.info("[系统日志清理任务] 触发，开始清理过期日志...");
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        executeClean(thirtyDaysAgo);
    }

    // ==========================================
    // 2. 带参数的方法（供前端点击"执行一次"时传参使用）
    // ==========================================
    public void run(String param) {
        log.info("[系统日志清理任务] 接收到动态参数：{}", param);
        // 如果前端传了 "dadwa"，这里直接抛出 NumberFormatException
        // 底层引擎会自动识别出它是非法参数，直接阻断并报警！不需要你写任何 try-catch！
        int daysToClean = org.springframework.util.StringUtils.hasText(param) ? Integer.parseInt(param.trim()) : 30;
        executeClean(System.currentTimeMillis() - ((long) daysToClean * 24 * 60 * 60 * 1000));
    }

    // ==========================================
    // 3. 提取出的核心清理逻辑（复用代码）
    // ==========================================
    private void executeClean(long timeAgo) {
        try {
            // 清理系统操作日志
            sysLogService.backupAndCleanLogs(timeAgo);

            // 清理定时任务调度日志
            LambdaQueryWrapper<SysTaskLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.lt(SysTaskLog::getCreateDate, timeAgo);
            int deletedCount = sysTaskLogMapper.delete(queryWrapper);

            log.info("[系统日志清理任务] 执行完毕。共清理了 {} 条过期的任务调度日志。", deletedCount);
        } catch (Exception e) {
            log.error("[系统日志清理任务] 执行过程中发生异常", e);
            throw new RuntimeException("日志清理执行失败", e);
        }
    }


}