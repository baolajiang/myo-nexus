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
            cronTaskRegistrar.addCronTask(task.getId(), task.getTaskName(), task.getBeanName(), task.getMethodName(), task.getCronExpression(), task.getTaskParam());
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
            cronTaskRegistrar.addCronTask(task.getId(), task.getTaskName(), task.getBeanName(), task.getMethodName(), task.getCronExpression(), task.getTaskParam());
        } else {
            cronTaskRegistrar.removeCronTask(taskId);
        }
        return Result.success(null);
    }

    @Override
    public Result runTaskOnce(SysTask taskParams) {
        // 1. 根據前端傳來的 ID 去資料庫查出原本的任務信息
        SysTask sysTask = sysTaskMapper.selectById(taskParams.getId());
        if (sysTask == null) {
            return Result.fail(ErrorCode.PARAMS_ERROR.getCode(), "任务不存在");
        }
        // 2. 【核心邏輯】判斷前端有沒有臨時傳參數過來
        // 如果前端在彈窗裡輸入了參數，就用前端的；如果沒輸入，就用資料庫裡配置的參數
        String paramToRun = org.apache.commons.lang3.StringUtils.isNotBlank(taskParams.getTaskParam())
                ? taskParams.getTaskParam()
                : sysTask.getTaskParam();
        try {
            // 3. 組裝 Runnable，注意這裡要把 paramToRun 作為最後一個參數傳進去
            SchedulingRunnable task = new SchedulingRunnable(
                    sysTask.getId(),
                    sysTask.getTaskName(),
                    sysTask.getBeanName(),
                    sysTask.getMethodName(),
                    paramToRun
            );

            // 4. 扔進線程池執行 (直接使用你類裡已經注入的 taskExecutor)
            taskExecutor.execute(task);

            return Result.success("执行指令已下发");
        } catch (Exception e) {
            return Result.fail(500, "执行任务异常：" + e.getMessage());
        }
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