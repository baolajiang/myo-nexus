package com.myo.blog.task.core;

import com.myo.blog.dao.mapper.SysTaskLogMapper;
import com.myo.blog.dao.pojo.SysTaskLog;
import com.myo.blog.service.MailService;
import com.myo.blog.utils.SpringContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.*;

/**
 * 定时任务执行包装器
 *
 * 每一个定时任务都会被包装成一个 SchedulingRunnable 实例，
 * 由调度线程池（threadPoolTaskScheduler）按 cron 时间点触发 run() 方法。
 *
 * 核心能力：
 * 1. 将真正的业务逻辑提交到业务线程池（jobTaskExecutor）异步执行，调度线程不阻塞
 * 2. 超时控制：任务执行超过 5 分钟自动中断，防止僵尸任务长期占用线程
 * 3. 失败重试：业务异常时最多重试 3 次，每次间隔 5 秒
 * 4. 智能阻断：识别参数格式错误（IllegalArgumentException / json / parse 异常），
 *    直接阻断重试并静默处理，不发送告警邮件（因为是人为操作失误，不是系统故障）
 * 5. 失败告警：非参数错误导致的最终失败，发送告警邮件通知管理员
 * 6. 执行日志：无论成功还是失败，最终都将执行结果和耗时写入 sys_task_log 表
 */
@Slf4j
@RequiredArgsConstructor
public class SchedulingRunnable implements Runnable {

    // 任务唯一 ID，用于关联 sys_task_log 执行日志记录
    private final Long taskId;

    // 任务名称，用于日志打印和告警邮件展示
    private final String taskName;

    // 目标 Spring Bean 的名称，运行时通过 SpringContextUtils.getBean() 从容器中取出目标对象
    private final String beanName;

    // 目标方法名，通过反射在目标 Bean 上调用此方法
    // 如果 taskParam 不为空，则寻找带 String 参数的同名方法；否则寻找无参方法
    private final String methodName;

    // 任务执行参数，对应目标方法的 String 入参
    // 为空时调用无参方法 run()，不为空时调用有参方法 run(String param)
    private final String taskParam;

    // 业务线程池（jobTaskExecutor，定义在 TaskSchedulerConfig 中）
    // 任务的真正业务逻辑在此线程池中执行，与调度线程池完全隔离
    // 使用 submit() 提交，返回原生 FutureTask，cancel(true) 可向底层线程发送真实中断信号
    private final ThreadPoolTaskExecutor taskExecutor;



    /**
     * 调度线程池触发此方法，完整执行一次任务的调度流程：
     * 提交业务线程 → 限时等待 → 超时中断 / 异常重试 → 记录日志 → 失败告警
     */
    @Override
    public void run() {
        log.info("定时任务开始执行 - 任务名称：[{}]", taskName);
        long startTime = System.currentTimeMillis();

        // 初始化本次执行的日志对象，最终无论成功失败都会写入数据库
        SysTaskLog taskLog = new SysTaskLog();
        taskLog.setTaskId(taskId);
        taskLog.setTaskName(taskName);
        taskLog.setBeanName(beanName);
        taskLog.setCreateDate(startTime);

        int maxRetries = 3;        // 最大重试次数（含第一次执行，共尝试 3 次）
        long timeoutMinutes = 5;   // 单次执行超时时间（分钟），超时后强制中断
        Exception lastException = null;  // 记录最后一次异常，用于日志和告警
        boolean shouldAlert = true;      // 告警开关，参数错误时关闭，不发送告警邮件

        for (int i = 1; i <= maxRetries; i++) {
            // future 声明在 try 外，是为了让 TimeoutException 的 catch 块能访问到它执行 cancel
            // submit 放在 try 内，是为了让线程池满时抛出的 RejectedExecutionException
            // 也能被下面的 catch(Exception e) 接住，触发重试逻辑，而不是直接击穿整个方法
            Future<?> future = null;
            try {
                // 将业务逻辑提交到业务线程池异步执行
                // submit() 返回原生 FutureTask，持有它才能做超时控制和强制中断
                future = taskExecutor.submit(() -> invokeTargetMethod());

                // 限时等待业务线程执行完毕，超过 timeoutMinutes 分钟抛出 TimeoutException
                future.get(timeoutMinutes, TimeUnit.MINUTES);

                // 执行成功，记录成功状态，清空异常，跳出重试循环
                taskLog.setStatus(0);
                lastException = null;
                break;

            } catch (TimeoutException te) {
                // 超时：调用 cancel(true) 向业务线程发送中断信号，由线程池负责回收线程
                // 防止超时任务死占线程池资源导致线程泄漏
                future.cancel(true);
                lastException = new RuntimeException("任务执行超时，已强行阻断 (" + timeoutMinutes + " 分钟)");

            } catch (Exception e) {
                // 剥洋葱：ExecutionException 是 Future.get() 的外壳，
                // InvocationTargetException 是反射调用的外壳，
                // 依次剥掉这两层，拿到最真实的业务异常
                Throwable rootCause = e;
                if (rootCause instanceof ExecutionException) {
                    rootCause = rootCause.getCause();
                }
                if (rootCause instanceof java.lang.reflect.InvocationTargetException) {
                    rootCause = ((java.lang.reflect.InvocationTargetException) rootCause).getTargetException();
                }

                lastException = rootCause instanceof Exception ? (Exception) rootCause : new RuntimeException(rootCause);

                // 智能阻断：判断是否为参数格式错误导致的不可恢复异常
                // 判定规则：IllegalArgumentException（含 NumberFormatException）
                // 或异常类名中包含 json / parse 关键字，均认定为参数错误
                String exceptionName = rootCause.getClass().getSimpleName().toLowerCase();
                boolean isParamError = rootCause instanceof IllegalArgumentException ||
                        exceptionName.contains("json") || exceptionName.contains("parse");

                if (isParamError) {
                    // 参数错误是人为操作失误，不是系统故障，阻断重试并关闭告警开关
                    log.error("【系统拦截】任务 [{}] 触发了不可恢复的参数异常 ({})，全局引擎已强制阻断重试！",
                            taskName, rootCause.getMessage());
                    shouldAlert = false;
                    break;
                }
                // 非参数错误：继续循环执行重试逻辑
            }

            // 还没到最大重试次数，等待 5 秒后进行下一次重试
            if (i < maxRetries) {
                log.warn("任务 [{}] 第 {} 次执行失败，5秒后准备重试... 原因: {}", taskName, i, lastException.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    // 休眠期间收到中断信号（如应用关闭），恢复中断标志并停止重试
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 所有重试均失败，记录失败状态并决定是否发送告警邮件
        if (lastException != null) {
            log.error("定时任务最终执行失败 - 任务名称：[{}]", taskName, lastException);
            taskLog.setStatus(1); // 1 = 执行失败
            String errorMsg = lastException.toString();
            // 错误信息超过 2000 字符时截断，防止数据库字段溢出
            taskLog.setErrorInfo(errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg);

            // shouldAlert = true 表示是系统级故障（非参数错误），需要发邮件通知管理员
            if (shouldAlert) {
                try {
                    // 从 Spring 容器获取邮件服务（SchedulingRunnable 是 new 出来的，无法直接注入）
                    MailService mailService = SpringContextUtils.getBean(MailService.class);
                    // 从环境配置中动态读取发件邮箱，避免 @Value 在非 Bean 类中注入为 null 的问题
                    Environment env = SpringContextUtils.getBean(Environment.class);
                    String fromEmail = env.getProperty("spring.mail.username");
                    String subject = "【系统告警】定时任务执行失败 - " + taskName;
                    String content = String.format(
                            "您的博客系统发生定时任务执行异常！\n\n任务ID：%d\n任务名称：%s\n调用目标：%s\n已重试次数：%d\n报错信息：\n%s\n\n请及时登录后台排查。",
                            taskId, taskName, beanName, maxRetries, errorMsg);
                    // 异步发送邮件，不阻塞当前线程
                    mailService.sendMailAsync(fromEmail, subject, content);
                } catch (Exception mailEx) {
                    // 发邮件失败只记录日志，不影响后续的日志落库
                    log.error("发送任务失败告警邮件异常", mailEx);
                }
            }
        }

        // 无论成功还是失败，记录本次执行的总耗时并将日志写入数据库
        long times = System.currentTimeMillis() - startTime;
        taskLog.setCostTime(times);
        // 从 Spring 容器获取 Mapper（同上，SchedulingRunnable 是 new 出来的，无法直接注入）
        SysTaskLogMapper sysTaskLogMapper = SpringContextUtils.getBean(SysTaskLogMapper.class);
        sysTaskLogMapper.insert(taskLog);
        log.info("定时任务执行结束 - 任务名称：[{}]，总耗时：{} 毫秒", taskName, times);
    }

    /**
     * 通过反射调用目标 Bean 的方法
     *
     * 根据 taskParam 是否有值，决定调用有参还是无参版本的目标方法：
     * - taskParam 不为空 → 调用 methodName(String param)
     * - taskParam 为空   → 调用 methodName()
     *
     * 所有受检异常都包装成 RuntimeException 向上抛出，
     * 由外层 Future.get() 的 catch(ExecutionException) 统一捕获处理。
     */
    private void invokeTargetMethod() {
        try {
            // 从 Spring 容器中取出目标 Bean（如 logCleanTask、databaseBackupTask 等）
            Object target = SpringContextUtils.getBean(beanName);
            Method method;

            if (StringUtils.isNotBlank(taskParam)) {
                // 有参数：查找带 String 类型参数的目标方法
                method = ReflectionUtils.findMethod(target.getClass(),
                        StringUtils.isNotBlank(methodName) ? methodName : "run", String.class);
                if (method == null) {
                    throw new NoSuchMethodException("未找到带 String 参数的目标方法：" + methodName);
                }
                // makeAccessible：解除 private/protected 访问限制，确保反射调用不报 IllegalAccessException
                ReflectionUtils.makeAccessible(method);
                method.invoke(target, taskParam);
            } else {
                // 无参数：查找无参的目标方法
                method = ReflectionUtils.findMethod(target.getClass(),
                        StringUtils.isNotBlank(methodName) ? methodName : "run");
                if (method == null) {
                    throw new NoSuchMethodException("未找到无参的目标方法：" + methodName);
                }
                ReflectionUtils.makeAccessible(method);
                method.invoke(target);
            }
        } catch (Exception e) {
            // 将所有受检异常包装成 RuntimeException，
            // 使其能穿透 Runnable/Callable 的方法签名，被外层 Future.get() 的 ExecutionException 捕获
            throw new RuntimeException(e);
        }
    }
}