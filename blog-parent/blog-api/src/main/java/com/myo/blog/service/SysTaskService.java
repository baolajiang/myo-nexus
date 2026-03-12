package com.myo.blog.service;

import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;

public interface SysTaskService {

    Result getTaskList(PageParams pageParams);

    Result changeStatus(Long taskId, Integer status);

    Result runTaskOnce(Long taskId);

    Result getTaskLogList(PageParams pageParams, Long taskId);
}