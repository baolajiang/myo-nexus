package com.myo.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.dao.mapper.SysTaskLogMapper;
import com.myo.blog.dao.mapper.SysTaskMapper;
import com.myo.blog.dao.pojo.SysTask;
import com.myo.blog.dao.pojo.SysTaskLog;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.service.SysTaskService;
import com.myo.blog.task.core.CronTaskRegistrar;
import com.myo.blog.task.core.SchedulingRunnable;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class SysTaskServiceImpl implements SysTaskService, CommandLineRunner {

    private final SysTaskMapper sysTaskMapper;

    private final SysTaskLogMapper sysTaskLogMapper;

    private final CronTaskRegistrar cronTaskRegistrar;

    private final Executor taskExecutor;


    @Override
    public void run(String... args) {
        LambdaQueryWrapper<SysTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysTask::getStatus, 1);
        List<SysTask> taskList = sysTaskMapper.selectList(queryWrapper);
        for (SysTask task : taskList) {
            cronTaskRegistrar.addCronTask(task.getId(), task.getTaskName(), task.getBeanName(), task.getMethodName(), task.getCronExpression());
        }
    }

    @Override
    public Result getTaskList(PageParams pageParams) {
        Page<SysTask> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());
        LambdaQueryWrapper<SysTask> queryWrapper = new LambdaQueryWrapper<>();
        sysTaskMapper.selectPage(page, queryWrapper);
        return Result.success(page);
    }

    @Override
    public Result changeStatus(Long taskId, Integer status) {
        SysTask task = sysTaskMapper.selectById(taskId);
        if (task == null) {
            return Result.fail(20001, "任务不存在");
        }
        task.setStatus(status);
        sysTaskMapper.updateById(task);

        if (status == 1) {
            cronTaskRegistrar.addCronTask(task.getId(), task.getTaskName(), task.getBeanName(), task.getMethodName(), task.getCronExpression());
        } else {
            cronTaskRegistrar.removeCronTask(taskId);
        }
        return Result.success(null);
    }

    @Override
    public Result runTaskOnce(Long taskId) {
        SysTask task = sysTaskMapper.selectById(taskId);
        if (task == null) {
            return Result.fail(20001, "任务不存在");
        }
        SchedulingRunnable schedulingRunnable = new SchedulingRunnable(task.getId(), task.getTaskName(), task.getBeanName(), task.getMethodName());
        taskExecutor.execute(schedulingRunnable);
        return Result.success(null);
    }

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