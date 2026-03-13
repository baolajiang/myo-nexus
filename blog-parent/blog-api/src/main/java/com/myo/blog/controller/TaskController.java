package com.myo.blog.controller;

import com.myo.blog.common.aop.LogAnnotation;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.service.SysTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/task")
@RequiredArgsConstructor
public class TaskController {

    private final SysTaskService sysTaskService;

    @PostMapping("/list")
    public Result getTaskList(@RequestBody PageParams pageParams) {
        return sysTaskService.getTaskList(pageParams);
    }

    @PostMapping("/log/list")
    public Result getTaskLogList(@RequestBody Map<String, Object> params) {
        PageParams pageParams = new PageParams();
        if (params.get("page") != null) pageParams.setPage(Integer.parseInt(params.get("page").toString()));
        if (params.get("pageSize") != null) pageParams.setPageSize(Integer.parseInt(params.get("pageSize").toString()));

        Long taskId = null;
        if (params.get("taskId") != null) {
            taskId = Long.parseLong(params.get("taskId").toString());
        }
        return sysTaskService.getTaskLogList(pageParams, taskId);
    }

    @PostMapping("/status")
    @LogAnnotation(module="系统任务", operator="启停任务")
    public Result changeStatus(@RequestBody Map<String, Object> params) {
        Long taskId = Long.parseLong(params.get("taskId").toString());
        Integer status = Integer.parseInt(params.get("status").toString());
        return sysTaskService.changeStatus(taskId, status);
    }

    @PostMapping("/run")
    @LogAnnotation(module="系统任务", operator="手动执行一次")
    public Result runTaskOnce(@RequestBody com.myo.blog.dao.pojo.SysTask sysTask) {

        return sysTaskService.runTaskOnce(sysTask);
    }
}