package com.myo.blog.task.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 定时任务线程池配置类
 *
 * 定义了两个线程池，职责严格分离：
 * - threadPoolTaskScheduler：调度线程池，只负责"打铃"（按 cron 触发任务），不干业务
 * - jobTaskExecutor：业务线程池，负责真正执行任务的业务逻辑
 *
 * 分离的原因：如果调度线程池同时执行业务逻辑，一旦某个耗时任务占用调度线程，
 * 其他所有定时任务都无法按时触发，整个调度系统就会假死。
 */
@Configuration
public class TaskSchedulerConfig {

    /**
     * 调度线程池：只负责按 cron 时间点触发任务，不执行业务逻辑

     * 线程数设为 10，因为触发动作本身极其轻量（只是往业务线程池提交一个任务），
     * 10 个线程足以支撑同时触发大量定时任务。

     * 拒绝策略使用 CallerRunsPolicy：
     * 调度线程池满的概率极低（触发动作很快），即使偶尔发生，
     * 让调用方（主线程）直接执行触发动作也不会造成系统级问题。
     */
    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // 调度线程池大小，10 个线程足够同时触发多个定时任务
        scheduler.setPoolSize(10);
        // 线程名称前缀，方便在日志和线程 dump 中快速识别调度线程
        scheduler.setThreadNamePrefix("scheduler-");
        // 应用关闭时等待正在触发中的任务提交完毕再关闭线程池，不强制中断
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        // 最多等待 60 秒，超时后强制关闭，防止应用停机卡住
        scheduler.setAwaitTerminationSeconds(60);
        // 拒绝策略：线程池满时由调用方线程直接执行，不丢弃触发动作
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return scheduler;
    }

    /**
     * 业务执行线程池：真正执行定时任务业务逻辑的地方
     *
     * 与调度线程池完全隔离，业务任务无论执行多久都不会影响调度线程按时触发。
     *
     * 线程数参数说明：
     * - corePoolSize(10)：常驻线程数，长期保活，应对日常并发任务
     * - maxPoolSize(50)：线程数上限，队列满后才会扩展到此上限
     * - queueCapacity(200)：核心线程全忙时的等待队列容量，缓冲突发流量
     * - keepAliveSeconds(60)：超出核心数的临时线程空闲 60 秒后自动销毁，节省资源
     *
     * 拒绝策略使用 AbortPolicy（直接抛异常）：
     * 当 50 个线程全满且队列 200 也满时，新任务提交会抛出 RejectedExecutionException。
     * SchedulingRunnable 的 catch(Exception e) 会接住此异常并触发重试，
     * 同时保护调度线程不被拉去执行业务逻辑，防止调度系统瘫痪。
     */
    @Bean
    public ThreadPoolTaskExecutor jobTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 常驻核心线程数
        executor.setCorePoolSize(10);
        // 线程数最大上限（核心线程全忙且队列也满时，才会创建新线程直到此上限）
        executor.setMaxPoolSize(50);
        // 等待队列容量（核心线程全忙时，新任务先进队列排队）
        executor.setQueueCapacity(200);
        // 非核心线程空闲存活时间（秒）
        executor.setKeepAliveSeconds(60);
        // 线程名称前缀，方便在日志和线程 dump 中快速识别业务执行线程
        executor.setThreadNamePrefix("任务-exec-");
        // 应用关闭时等待队列中的任务执行完毕再关闭，保证任务不丢失
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 最多等待 60 秒，超时后强制关闭，防止应用停机卡住
        executor.setAwaitTerminationSeconds(60);
        // 拒绝策略：线程池和队列全满时直接抛出 RejectedExecutionException，
        // 由 SchedulingRunnable 的异常处理机制接住并触发重试，绝不让调度线程来干业务的活
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 手动调用 initialize() 完成线程池的初始化，@Bean 方式必须显式调用
        executor.initialize();
        return executor;
    }
}