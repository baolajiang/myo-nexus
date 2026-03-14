package com.myo.blog.task.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CronTaskRegistrar implements DisposableBean {

    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    // 注入业务线程池，传给 SchedulingRunnable 执行真正的业务逻辑
    private final ThreadPoolTaskExecutor jobTaskExecutor;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void addCronTask(Long taskId, String taskName, String beanName, String methodName,
                            String cronExpression, String taskParam) {
        if (scheduledTasks.containsKey(taskId)) {
            removeCronTask(taskId);
        }

        SchedulingRunnable task = new SchedulingRunnable(
                taskId, taskName, beanName, methodName, taskParam, jobTaskExecutor
        );

        ScheduledFuture<?> future = threadPoolTaskScheduler.schedule(task, new CronTrigger(cronExpression));
        if (future != null) {
            scheduledTasks.put(taskId, future);
        }
    }

    public void removeCronTask(Long taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public void destroy() {
        for (ScheduledFuture<?> future : scheduledTasks.values()) {
            future.cancel(true);
        }
        scheduledTasks.clear();
    }
}