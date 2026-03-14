package com.myo.blog.task.core;

import com.myo.blog.dao.mapper.SysTaskLogMapper;
import com.myo.blog.dao.pojo.SysTaskLog;
import com.myo.blog.service.MailService;
import com.myo.blog.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.*;

// 定时任务包装器，支持动态参数
@Slf4j
public class SchedulingRunnable implements Runnable {

    private final Long taskId;
    private final String taskName;
    private final String beanName;
    private final String methodName;
    private final String taskParam;
    // 业务线程池：真正执行任务逻辑的地方，与调度线程池隔离
    private final ThreadPoolTaskExecutor taskExecutor;

    public SchedulingRunnable(Long taskId, String taskName, String beanName, String methodName,
                              String taskParam, ThreadPoolTaskExecutor taskExecutor) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.beanName = beanName;
        this.methodName = methodName;
        this.taskParam = taskParam;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void run() {
        log.info("定时任务开始执行 - 任务名称：[{}]", taskName);
        long startTime = System.currentTimeMillis();

        SysTaskLog taskLog = new SysTaskLog();
        taskLog.setTaskId(taskId);
        taskLog.setTaskName(taskName);
        taskLog.setBeanName(beanName);
        taskLog.setCreateDate(startTime);

        int maxRetries = 3;
        long timeoutMinutes = 5;
        Exception lastException = null;
        boolean shouldAlert = true;

        for (int i = 1; i <= maxRetries; i++) {
            // future 声明在 try 外，submit 动作放在 try 内
            // 这样 RejectedExecutionException 也能被 catch 接住，触发重试和告警
            Future<?> future = null;
            try {
                // submit 返回原生 FutureTask，cancel(true) 会向底层线程发送真实中断信号
                future = taskExecutor.submit(() -> invokeTargetMethod());
                future.get(timeoutMinutes, TimeUnit.MINUTES);
                taskLog.setStatus(0);
                lastException = null;
                break;

            } catch (TimeoutException te) {
                future.cancel(true);
                lastException = new RuntimeException("任务执行超时，已强行阻断 (" + timeoutMinutes + " 分钟)");

            } catch (Exception e) {
                // 剥洋葱：拿到最真实的业务异常
                Throwable rootCause = e;
                if (rootCause instanceof ExecutionException) {
                    rootCause = rootCause.getCause();
                }
                if (rootCause instanceof java.lang.reflect.InvocationTargetException) {
                    rootCause = ((java.lang.reflect.InvocationTargetException) rootCause).getTargetException();
                }

                lastException = rootCause instanceof Exception ? (Exception) rootCause : new RuntimeException(rootCause);

                String exceptionName = rootCause.getClass().getSimpleName().toLowerCase();
                boolean isParamError = rootCause instanceof IllegalArgumentException ||
                        exceptionName.contains("json") || exceptionName.contains("parse");

                if (isParamError) {
                    log.error("【系统拦截】任务 [{}] 触发了不可恢复的参数异常 ({})，全局引擎已强制阻断重试！",
                            taskName, rootCause.getMessage());
                    shouldAlert = false;
                    break;
                }
            }

            if (i < maxRetries) {
                log.warn("任务 [{}] 第 {} 次执行失败，5秒后准备重试... 原因: {}", taskName, i, lastException.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (lastException != null) {
            log.error("定时任务最终执行失败 - 任务名称：[{}]", taskName, lastException);
            taskLog.setStatus(1);
            String errorMsg = lastException.toString();
            taskLog.setErrorInfo(errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg);

            if (shouldAlert) {
                try {
                    MailService mailService = SpringContextUtils.getBean(MailService.class);
                    Environment env = SpringContextUtils.getBean(Environment.class);
                    String fromEmail = env.getProperty("spring.mail.username");
                    String subject = "【系统告警】定时任务执行失败 - " + taskName;
                    String content = String.format(
                            "您的博客系统发生定时任务执行异常！\n\n任务ID：%d\n任务名称：%s\n调用目标：%s\n已重试次数：%d\n报错信息：\n%s\n\n请及时登录后台排查。",
                            taskId, taskName, beanName, maxRetries, errorMsg);
                    mailService.sendMailAsync(fromEmail, subject, content);
                } catch (Exception mailEx) {
                    log.error("发送任务失败告警邮件异常", mailEx);
                }
            }
        }

        long times = System.currentTimeMillis() - startTime;
        taskLog.setCostTime(times);
        SysTaskLogMapper sysTaskLogMapper = SpringContextUtils.getBean(SysTaskLogMapper.class);
        sysTaskLogMapper.insert(taskLog);
        log.info("定时任务执行结束 - 任务名称：[{}]，总耗时：{} 毫秒", taskName, times);
    }

    // 通过反射调用目标 Bean 的方法
    private void invokeTargetMethod() {
        try {
            Object target = SpringContextUtils.getBean(beanName);
            Method method;
            if (StringUtils.isNotBlank(taskParam)) {
                method = ReflectionUtils.findMethod(target.getClass(),
                        StringUtils.isNotBlank(methodName) ? methodName : "run", String.class);
                if (method == null) {
                    throw new NoSuchMethodException("未找到带 String 参数的目标方法：" + methodName);
                }
                ReflectionUtils.makeAccessible(method);
                method.invoke(target, taskParam);
            } else {
                method = ReflectionUtils.findMethod(target.getClass(),
                        StringUtils.isNotBlank(methodName) ? methodName : "run");
                if (method == null) {
                    throw new NoSuchMethodException("未找到无参的目标方法：" + methodName);
                }
                ReflectionUtils.makeAccessible(method);
                method.invoke(target);
            }
        } catch (Exception e) {
            // 包装成 RuntimeException 向上传递给 Future
            throw new RuntimeException(e);
        }
    }
}