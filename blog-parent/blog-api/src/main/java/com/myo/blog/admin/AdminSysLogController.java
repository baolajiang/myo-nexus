package com.myo.blog.admin;


import com.myo.blog.common.aop.LogAnnotation;
import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/sysLog")
@RequiredArgsConstructor
public class AdminSysLogController {
    private final SysLogService sysLogService;

    /**
     * 分頁查詢操作日誌
     */
    @PostMapping("/list")
    @RequirePermission("sys:log:list") // 確保你有對應的權限標識
    public Result listLog(@RequestBody PageParams pageParams) {
        return sysLogService.listLog(pageParams);
    }

    @PostMapping("/upload")
    @LogAnnotation(module="系统日志", operator="条件导出日志到R2")
    public Result uploadLog(@RequestBody PageParams pageParams) {
        // 交给 Service 层处理具体的打包和上传逻辑
        return sysLogService.exportLogToR2(pageParams);
    }
}
