package com.myo.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.dao.mapper.SysTaskLogMapper;
import com.myo.blog.dao.mapper.SysTaskMapper;
import com.myo.blog.dao.pojo.SysTask;
import com.myo.blog.dao.pojo.SysTaskLog;
import com.myo.blog.entity.ErrorCode;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.service.SysTaskService;
import com.myo.blog.task.core.CronTaskRegistrar;
import com.myo.blog.task.core.SchedulingRunnable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysTaskServiceImpl implements SysTaskService, CommandLineRunner {

    private final SysTaskMapper sysTaskMapper;
    private final SysTaskLogMapper sysTaskLogMapper;
    private final CronTaskRegistrar cronTaskRegistrar;
    private final ThreadPoolTaskExecutor taskExecutor;

    /**
     * 应用启动时，加载所有启用的定时任务
     */
    @Override
    public void run(String... args) {
        LambdaQueryWrapper<SysTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysTask::getStatus, 1);
        List<SysTask> taskList = sysTaskMapper.selectList(queryWrapper);
        for (SysTask task : taskList) {
            try {
                cronTaskRegistrar.addCronTask(task.getId(), task.getTaskName(), task.getBeanName(),
                        task.getMethodName(), task.getCronExpression(), task.getTaskParam());
                log.info("[定时任务] 启动加载成功：{}", task.getTaskName());
            } catch (Exception e) {
                // 单个任务加载失败不影响其他任务和应用启动
                log.warn("[定时任务] 启动加载失败，任务：{}，原因：{}", task.getTaskName(), e.getMessage());
            }
        }
    }

    /**
     * 获取所有定时任务列表
     */
    @Override
    public Result getTaskList(PageParams pageParams) {
        Page<SysTask> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());
        LambdaQueryWrapper<SysTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(SysTask::getCreateDate);
        sysTaskMapper.selectPage(page, queryWrapper);
        return Result.success(page);
    }

    /**
     * 启用/禁用定时任务
     */
    @Override
    public Result changeStatus(Long taskId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail(ErrorCode.PARAMS_ERROR.getCode(), "status 参数非法，只允许传 0 或 1");
        }
        SysTask task = sysTaskMapper.selectById(taskId);
        if (task == null) {
            return Result.fail(20001, "任务不存在");
        }
        task.setStatus(status);
        sysTaskMapper.updateById(task);

        if (status == 1) {
            cronTaskRegistrar.addCronTask(task.getId(), task.getTaskName(), task.getBeanName(),
                    task.getMethodName(), task.getCronExpression(), task.getTaskParam());
        } else {
            cronTaskRegistrar.removeCronTask(taskId);
        }
        return Result.success(null);
    }

    /**
     * 手动执行一次定时任务
     */
    @Override
    public Result runTaskOnce(SysTask taskParams) {
        SysTask sysTask = sysTaskMapper.selectById(taskParams.getId());
        if (sysTask == null) {
            return Result.fail(ErrorCode.PARAMS_ERROR.getCode(), "任务不存在");
        }

        String paramToRun = StringUtils.isNotBlank(taskParams.getTaskParam())
                ? taskParams.getTaskParam()
                : sysTask.getTaskParam();

        try {
            // 【修复】补齐新构造函数所需的 taskExecutor 和 sysTaskLogMapper 两个参数
            SchedulingRunnable task = new SchedulingRunnable(
                    sysTask.getId(),
                    sysTask.getTaskName(),
                    sysTask.getBeanName(),
                    sysTask.getMethodName(),
                    paramToRun,
                    taskExecutor
            );

            taskExecutor.execute(task);
            return Result.success("执行指令已下发");
        } catch (Exception e) {
            return Result.fail(500, "执行任务异常：" + e.getMessage());
        }
    }

    /**
     * 获取定时任务执行日志列表
     */
    @Override
    public Result getTaskLogList(PageParams pageParams, Long taskId) {
        Page<SysTaskLog> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());
        LambdaQueryWrapper<SysTaskLog> queryWrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            queryWrapper.eq(SysTaskLog::getTaskId, taskId);
        }
        queryWrapper.orderByDesc(SysTaskLog::getCreateDate);
        sysTaskLogMapper.selectPage(page, queryWrapper);
        return Result.success(page);
    }
}