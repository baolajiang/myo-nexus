package com.myo.blog.task.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
// 定时任务线程池配置
@Configuration
public class TaskSchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // 设置线程池大小，根据你的任务数量调整
        scheduler.setPoolSize(20);
        // 设置线程名称前缀，排查日志时方便认出来
        scheduler.setThreadNamePrefix("task-");
        // 优雅等待所有任务结束后再关闭线程池
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        return scheduler;
    }
}