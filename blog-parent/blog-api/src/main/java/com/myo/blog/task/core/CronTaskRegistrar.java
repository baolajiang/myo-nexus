package com.myo.blog.task.core;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
// 定时任务注册器，用于注册和管理定时任务
@Component
@RequiredArgsConstructor
public class CronTaskRegistrar implements DisposableBean {


    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    // 缓存正在运行的任务句柄，用于后续取消任务
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // 添加并启动定时任务
    public void addCronTask(Long taskId, String taskName, String beanName, String methodName, String cronExpression, String taskParam) {
        // 如果任务已经在跑了，先把它取消掉
        if (scheduledTasks.containsKey(taskId)) {
            removeCronTask(taskId);
        }

        // 创建任务包装器，支持动态参数
        SchedulingRunnable task = new SchedulingRunnable(taskId, taskName, beanName, methodName, taskParam);

        // 交给线程池调度
        ScheduledFuture<?> future = threadPoolTaskScheduler.schedule(task, new CronTrigger(cronExpression));

        if (future != null) {
            scheduledTasks.put(taskId, future);
        }
    }

    // 移除并停止定时任务
    public void removeCronTask(Long taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(true);
        }
    }

    // 销毁时清理所有任务
    @Override
    public void destroy() {
        for (ScheduledFuture<?> future : scheduledTasks.values()) {
            future.cancel(true);
        }
        scheduledTasks.clear();
    }
}