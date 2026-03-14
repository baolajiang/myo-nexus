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

/**
 * 定时任务注册器
 *
 * 负责管理所有定时任务的生命周期：注册、启动、停止、销毁。
 * 实现了 DisposableBean 接口，在 Spring 容器关闭时自动取消所有正在运行的任务，
 * 防止应用关闭时任务仍在后台执行造成数据异常。
 *
 * 核心设计：调度线程池（threadPoolTaskScheduler）只负责按 cron 时间点触发任务，
 * 真正的业务逻辑交给业务线程池（jobTaskExecutor）执行，两者职责分离互不干扰。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CronTaskRegistrar implements DisposableBean {

    // 引用调度线程池（定义在 TaskSchedulerConfig 中）
    // 职责单一：只负责按 cron 表达式到点触发任务，本身不执行任何业务逻辑
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    // 引用业务线程池 jobTaskExecutor（定义在 TaskSchedulerConfig 中）
    // 构造 SchedulingRunnable 时传入，任务的真正业务逻辑在此线程池中执行
    private final ThreadPoolTaskExecutor jobTaskExecutor;

    // 缓存所有已注册任务的调度句柄（taskId -> ScheduledFuture）
    // 使用 ConcurrentHashMap 保证多线程并发注册/移除任务时的线程安全
    // ScheduledFuture 是任务的控制句柄，持有它才能在需要时取消任务
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * 注册并启动一个定时任务
     *
     * 如果该任务 ID 已经在运行，会先取消旧任务再重新注册，
     * 保证同一个任务不会被重复调度（常见于修改 cron 表达式后重新启用）。
     *
     * @param taskId         任务唯一 ID，用于缓存和取消任务
     * @param taskName       任务名称，用于日志展示
     * @param beanName       目标 Spring Bean 的名称，反射调用时从容器中取出
     * @param methodName     目标方法名（无参或带 String 参数）
     * @param cronExpression cron 表达式，定义任务的触发时间规则
     * @param taskParam      任务执行参数，传 null 或空字符串则调用无参方法
     */
    public void addCronTask(Long taskId, String taskName, String beanName, String methodName,
                            String cronExpression, String taskParam) {
        // 如果该任务已在运行，先移除旧的调度，避免同一任务被重复触发
        if (scheduledTasks.containsKey(taskId)) {
            removeCronTask(taskId);
        }

        // 构建任务执行器，封装了反射调用、超时控制、失败重试、告警通知等完整逻辑
        SchedulingRunnable task = new SchedulingRunnable(
                taskId, taskName, beanName, methodName, taskParam, jobTaskExecutor
        );

        // 将任务提交给调度线程池，按 cron 表达式定时触发
        // CronTrigger 负责解析 cron 表达式并计算下次触发时间
        // schedule() 返回 ScheduledFuture，持有它后续才能取消任务
        ScheduledFuture<?> future = threadPoolTaskScheduler.schedule(task, new CronTrigger(cronExpression));

        // 缓存调度句柄，供后续禁用任务时取消调度使用
        if (future != null) {
            scheduledTasks.put(taskId, future);
        }
    }

    /**
     * 移除并停止一个定时任务
     *
     * 从缓存中取出调度句柄并调用 cancel(true)，
     * true 表示如果任务正在执行中，向其发送中断信号尝试终止。
     *
     * @param taskId 要停止的任务 ID
     */
    public void removeCronTask(Long taskId) {
        // remove() 同时完成"从缓存中删除"和"拿到句柄"两个操作，保证原子性
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            // cancel(true)：取消调度，并向正在执行的任务线程发送中断信号
            future.cancel(true);
        }
    }

    /**
     * Spring 容器关闭时自动调用，清理所有正在运行的定时任务
     *
     * 由 DisposableBean 接口触发，在应用优雅停机时执行。
     * 取消所有任务的调度并清空缓存，防止容器关闭后任务仍在后台游离运行。
     */
    @Override
    public void destroy() {
        for (ScheduledFuture<?> future : scheduledTasks.values()) {
            // 逐一取消所有已注册任务的调度
            future.cancel(true);
        }
        // 清空缓存，释放内存
        scheduledTasks.clear();
        log.info("[定时任务] 所有定时任务已随容器关闭而清理完毕");
    }
}