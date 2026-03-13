package com.myo.blog.task.core;

import com.myo.blog.dao.mapper.SysTaskLogMapper;
import com.myo.blog.dao.pojo.SysTaskLog;
import com.myo.blog.service.MailService;
import com.myo.blog.utils.SpringContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@RequiredArgsConstructor
public class SchedulingRunnable implements Runnable {

    private final Long taskId;
    private final String taskName;
    private final String beanName;
    private final String methodName;
    // 支持动态参数
    private final String taskParam;

    @Override
    public void run() {
        log.info("定时任务开始执行 - 任务名称：[{}]", taskName);
        long startTime = System.currentTimeMillis();

        SysTaskLog taskLog = new SysTaskLog();
        taskLog.setTaskId(taskId);
        taskLog.setTaskName(taskName);
        taskLog.setBeanName(beanName);
        taskLog.setCreateDate(startTime);

        int maxRetries = 3; // 最大重试次数
        long timeoutMinutes = 5; // 默认任务超时时间为5分钟
        Exception lastException = null;

        for (int i = 1; i <= maxRetries; i++) {
            FutureTask<Void> futureTask = null;
            try {
                // 1. 从 Spring 容器中获取目标 Bean
                Object target = SpringContextUtils.getBean(beanName);

                // 2. 定义具体的执行逻辑（使用 Callable 方便被 Future 包装进行超时控制）
                Callable<Void> taskCallable = () -> {
                    Method method;
                    // 判断是否有动态参数，如果有，寻找带 String 参数的方法
                    if (StringUtils.isNotBlank(taskParam)) {
                        method = ReflectionUtils.findMethod(target.getClass(), StringUtils.isNotBlank(methodName) ? methodName : "run", String.class);
                        if (method == null) {
                            throw new NoSuchMethodException("未找到带 String 参数的目标方法：" + methodName);
                        }
                        ReflectionUtils.makeAccessible(method);
                        method.invoke(target, taskParam); // 传入动态参数执行
                    } else {
                        // 寻找无参方法
                        method = ReflectionUtils.findMethod(target.getClass(), StringUtils.isNotBlank(methodName) ? methodName : "run");
                        if (method == null) {
                            throw new NoSuchMethodException("未找到无参的目标方法：" + methodName);
                        }
                        ReflectionUtils.makeAccessible(method);
                        method.invoke(target); // 无参执行
                    }
                    return null;
                };

                // 3. 使用 FutureTask 包装，并放入新线程执行，以实现超时阻断机制
                futureTask = new FutureTask<>(taskCallable);
                Thread executeThread = new Thread(futureTask);
                executeThread.start();

                // 4. 限时等待执行结果
                futureTask.get(timeoutMinutes, TimeUnit.MINUTES);

                // 执行成功，记录状态并跳出重试循环
                taskLog.setStatus(0);
                lastException = null;
                break;

            } catch (TimeoutException te) {
                // 【核心机制：超时阻断】
                lastException = new RuntimeException("任务执行超时，已强行阻断 (" + timeoutMinutes + " 分钟)");
                if (futureTask != null) {
                    futureTask.cancel(true); // 强行中断死锁的子线程
                }
            } catch (Exception e) {
                lastException = e;
            }

            // 【核心机制：失败重试】还没达到最大次数，等待5秒后重试
            if (i < maxRetries) {
                log.warn("任务 [{}] 第 {} 次执行失败，5秒后准备重试... 原因: {}", taskName, i, lastException.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 如果全部重试完还是报错，进入失败和告警逻辑
        if (lastException != null) {
            log.error("定时任务最终执行失败 - 任务名称：[{}]", taskName, lastException);
            taskLog.setStatus(1);
            String errorMsg = lastException.toString();
            taskLog.setErrorInfo(errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg);

            // 发送告警邮件
            try {
                MailService mailService = SpringContextUtils.getBean(MailService.class);
                // 动态获取发件邮箱，修复 @Value 为 null 的 bug
                Environment env = SpringContextUtils.getBean(Environment.class);
                String fromEmail = env.getProperty("spring.mail.username");

                String subject = "【系统告警】定时任务执行失败 - " + taskName;
                String content = String.format("您的博客系统发生定时任务执行异常！\n\n任务ID：%d\n任务名称：%s\n调用目标：%s\n已重试次数：%d\n报错信息：\n%s\n\n请及时登录后台排查。",
                        taskId, taskName, beanName, maxRetries, errorMsg);

                mailService.sendMailAsync(fromEmail, subject, content);
            } catch (Exception mailEx) {
                log.error("发送任务失败告警邮件异常", mailEx);
            }
        }

        // 无论成功还是失败，最终记录耗时并保存入库
        long times = System.currentTimeMillis() - startTime;
        taskLog.setCostTime(times);
        SysTaskLogMapper sysTaskLogMapper = SpringContextUtils.getBean(SysTaskLogMapper.class);
        sysTaskLogMapper.insert(taskLog);
        log.info("定时任务执行结束 - 任务名称：[{}]，总耗时：{} 毫秒", taskName, times);
    }
}