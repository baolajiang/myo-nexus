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

/**
 * 定时任务管理服务实现类
 *
 * 实现了两个接口：
 * - SysTaskService：对外暴露任务管理的业务接口（增删查改、手动执行等）
 * - CommandLineRunner：Spring Boot 启动完成后自动回调 run() 方法，
 *   用于在应用启动时将数据库中所有已启用的定时任务注册到调度器中
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTaskServiceImpl implements SysTaskService, CommandLineRunner {

    // 引用 SysTaskMapper，用于对 sys_task 表进行增删改查（任务配置信息）
    private final SysTaskMapper sysTaskMapper;

    // 引用 SysTaskLogMapper，用于对 sys_task_log 表进行查询（任务执行日志）
    private final SysTaskLogMapper sysTaskLogMapper;

    // 引用 CronTaskRegistrar（定时任务注册器），负责将任务注册到 Spring 调度线程池
    // 以及在任务启用/禁用时动态添加或移除调度
    private final CronTaskRegistrar cronTaskRegistrar;

    // 引用业务线程池 jobTaskExecutor（定义在 TaskSchedulerConfig 中）
    // 手动执行一次任务时，将任务提交到此线程池异步执行，与调度线程池隔离互不影响
    // 注意：当前容器中只有一个 ThreadPoolTaskExecutor 类型的 Bean，Spring 按类型自动匹配注入；
    // 若将来新增同类型 Bean，需加 @Qualifier("jobTaskExecutor") 明确指定，否则启动报错
    private final ThreadPoolTaskExecutor jobTaskExecutor;

    /**
     * 应用启动时自动执行，将数据库中所有状态为"启用"的定时任务注册到调度器
     *
     * 由 CommandLineRunner 接口触发，在 Spring 容器完全初始化后自动调用。
     * 每个任务单独 try-catch，保证单个任务加载失败不会影响其他任务和整个应用的启动。
     */
    @Override
    public void run(String... args) {
        // 查询数据库中所有 status = 1（启用）的定时任务
        LambdaQueryWrapper<SysTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysTask::getStatus, 1);
        List<SysTask> taskList = sysTaskMapper.selectList(queryWrapper);

        for (SysTask task : taskList) {
            try {
                // 将任务注册到调度线程池，按 cron 表达式定时触发
                cronTaskRegistrar.addCronTask(task.getId(), task.getTaskName(), task.getBeanName(),
                        task.getMethodName(), task.getCronExpression(), task.getTaskParam());
                log.info("[定时任务] 启动加载成功：{}", task.getTaskName());
            } catch (Exception e) {
                // 单个任务加载失败（如 cron 表达式非法、Bean 不存在等）只打警告日志
                // 不向上抛异常，避免一个坏任务导致整个应用启动失败
                log.warn("[定时任务] 启动加载失败，任务：{}，原因：{}", task.getTaskName(), e.getMessage());
            }
        }
    }

    /**
     * 分页查询所有定时任务列表
     * 按创建时间倒序排列，最新创建的任务排在最前面
     *
     * @param pageParams 分页参数，包含页码和每页条数
     * @return 分页结果，包含任务列表和总条数
     */
    @Override
    public Result getTaskList(PageParams pageParams) {
        // 构建分页对象，传入当前页码和每页条数
        Page<SysTask> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());
        LambdaQueryWrapper<SysTask> queryWrapper = new LambdaQueryWrapper<>();
        // 按创建时间倒序，保证列表展示顺序稳定
        queryWrapper.orderByDesc(SysTask::getCreateDate);
        sysTaskMapper.selectPage(page, queryWrapper);
        return Result.success(page);
    }

    /**
     * 启用或禁用定时任务
     *
     * 修改数据库中的任务状态后，同步操作调度器：
     * - 启用（status=1）：将任务注册到调度器，开始按 cron 定时执行
     * - 禁用（status=0）：从调度器中移除任务，停止定时执行
     *
     * @param taskId 任务 ID
     * @param status 目标状态，1=启用，0=禁用
     */
    @Override
    public Result changeStatus(Long taskId, Integer status) {
        // 参数校验：status 只允许传 0 或 1，防止非法值写入数据库
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail(ErrorCode.PARAMS_ERROR.getCode(), "status 参数非法，只允许传 0 或 1");
        }

        // 查询任务是否存在
        SysTask task = sysTaskMapper.selectById(taskId);
        if (task == null) {
            return Result.fail(20001, "任务不存在");
        }

        // 更新数据库中的任务状态
        task.setStatus(status);
        sysTaskMapper.updateById(task);

        // 同步操作调度器，保证数据库状态与调度器状态始终一致
        if (status == 1) {
            // 启用：注册到调度器，立即按 cron 表达式开始调度
            cronTaskRegistrar.addCronTask(task.getId(), task.getTaskName(), task.getBeanName(),
                    task.getMethodName(), task.getCronExpression(), task.getTaskParam());
        } else {
            // 禁用：从调度器中移除，停止后续触发
            cronTaskRegistrar.removeCronTask(taskId);
        }
        return Result.success(null);
    }

    /**
     * 手动立即执行一次定时任务
     *
     * 不走调度器，直接将任务提交到业务线程池立即异步执行一次。
     * 前端可以在弹窗中临时传入参数覆盖数据库中配置的默认参数；
     * 如果前端没有传参，则沿用数据库中已保存的任务参数。
     *
     * @param taskParams 前端传来的任务信息，必须包含 id，taskParam 可选（临时覆盖参数）
     * @return 操作结果，成功时返回"执行指令已下发"（任务异步执行，此时可能尚未完成）
     */
    @Override
    public Result runTaskOnce(SysTask taskParams) {
        // 根据前端传来的 ID 从数据库查出完整的任务配置
        SysTask sysTask = sysTaskMapper.selectById(taskParams.getId());
        if (sysTask == null) {
            return Result.fail(ErrorCode.PARAMS_ERROR.getCode(), "任务不存在");
        }

        // 决定本次执行使用哪个参数：
        // 前端弹窗有输入 → 用前端的（临时覆盖）；前端没有输入 → 用数据库里保存的默认参数
        String paramToRun = StringUtils.isNotBlank(taskParams.getTaskParam())
                ? taskParams.getTaskParam()
                : sysTask.getTaskParam();

        try {
            // 构建任务执行器 SchedulingRunnable，内部封装了：
            // 反射调用目标方法、超时控制（5分钟）、失败自动重试（最多3次）、失败告警邮件等完整逻辑
            SchedulingRunnable task = new SchedulingRunnable(
                    sysTask.getId(),         // 任务 ID，用于记录执行日志与关联任务
                    sysTask.getTaskName(),   // 任务名称，用于日志打印和告警邮件展示
                    sysTask.getBeanName(),   // Spring Bean 名称，反射时从容器中取出目标对象
                    sysTask.getMethodName(), // 要调用的方法名（无参或带 String 参数）
                    paramToRun,              // 本次执行使用的参数（前端临时参数优先于数据库配置）
                    jobTaskExecutor          // 业务线程池，任务在此线程池中异步执行，不占用调度线程
            );

            // 提交到业务线程池异步执行，当前线程立即返回，不等待任务完成
            jobTaskExecutor.execute(task);
            return Result.success("执行指令已下发");
        } catch (Exception e) {
            return Result.fail(500, "执行任务异常：" + e.getMessage());
        }
    }

    /**
     * 分页查询定时任务执行日志
     *
     * 支持按任务 ID 过滤，查询指定任务的历史执行记录。
     * 不传 taskId 则返回所有任务的执行日志。
     * 按执行时间倒序排列，最新的执行记录排在最前面。
     *
     * @param pageParams 分页参数，包含页码和每页条数
     * @param taskId     任务 ID，传 null 则查询全部任务的日志
     * @return 分页结果，包含日志列表和总条数
     */
    @Override
    public Result getTaskLogList(PageParams pageParams, Long taskId) {
        // 构建分页对象
        Page<SysTaskLog> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());
        LambdaQueryWrapper<SysTaskLog> queryWrapper = new LambdaQueryWrapper<>();

        // taskId 不为空时，只查询该任务的日志；为空时查询所有任务的日志
        if (taskId != null) {
            queryWrapper.eq(SysTaskLog::getTaskId, taskId);
        }

        // 按执行时间倒序，最新记录排在最前
        queryWrapper.orderByDesc(SysTaskLog::getCreateDate);
        sysTaskLogMapper.selectPage(page, queryWrapper);
        return Result.success(page);
    }
}