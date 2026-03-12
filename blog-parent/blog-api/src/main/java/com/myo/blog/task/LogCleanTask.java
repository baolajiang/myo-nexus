package com.myo.blog.task;

import com.myo.blog.service.SysLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanTask {


    private final SysLogService sysLogService;

    // 现在由 CronTaskRegistrar 注册和管理定时任务
    public void run() {
        // 30 天前的时间戳
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        log.info("[归档任务] 触发，清理 {} 毫秒以前的日志", thirtyDaysAgo);
        sysLogService.backupAndCleanLogs(thirtyDaysAgo);
    }
}