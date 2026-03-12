package com.myo.blog.task;

import com.myo.blog.dao.mapper.SysTaskLogMapper;
import com.myo.blog.dao.pojo.SysTaskLog;
import com.myo.blog.utils.SpringContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
// 定时任务执行器，用于在指定时间点执行指定的 Bean 方法
@Slf4j
@RequiredArgsConstructor
public class SchedulingRunnable implements Runnable {

    private final Long taskId;
    private final String taskName;
    private final String beanName;
    private final String methodName;



    @Override
    public void run() {
        log.info("定时任务开始执行 - 任务名称：[{}]", taskName);
        long startTime = System.currentTimeMillis();

        SysTaskLog taskLog = new SysTaskLog();
        taskLog.setTaskId(taskId);
        taskLog.setTaskName(taskName);
        taskLog.setBeanName(beanName);
        taskLog.setCreateDate(startTime);

        try {
            // 1. 从 Spring 容器中获取目标 Bean
            Object target = SpringContextUtils.getBean(beanName);

            // 2. 利用反射获取目标方法
            Method method = null;
            if (StringUtils.isNotBlank(methodName)) {
                method = ReflectionUtils.findMethod(target.getClass(), methodName);
            } else {
                method = ReflectionUtils.findMethod(target.getClass(), "run"); // 默认找 run 方法
            }

            if (method == null) {
                throw new NoSuchMethodException("未找到目标方法：" + methodName);
            }

            // 3. 取消 Java 语言访问检查以提升反射速度，并执行方法
            ReflectionUtils.makeAccessible(method);
            method.invoke(target);

            // 4. 执行成功，记录状态
            taskLog.setStatus(0);

        } catch (Exception e) {
            log.error("定时任务执行失败 - 任务名称：[{}]", taskName, e);
            // 5. 执行失败，记录状态和错误信息
            taskLog.setStatus(1);
            String errorMsg = e.toString();
            if (errorMsg.length() > 2000) {
                errorMsg = errorMsg.substring(0, 2000);
            }
            taskLog.setErrorInfo(errorMsg);
        } finally {
            // 6. 计算耗时并保存入库
            long times = System.currentTimeMillis() - startTime;
            taskLog.setCostTime(times);

            // 从 Spring 容器拿 Mapper 保存日志
            SysTaskLogMapper sysTaskLogMapper = SpringContextUtils.getBean(SysTaskLogMapper.class);
            sysTaskLogMapper.insert(taskLog);

            log.info("定时任务执行结束 - 任务名称：[{}]，耗时：{} 毫秒", taskName, times);
        }
    }
}