package com.myo.blog.admin;

import com.myo.blog.entity.Result;
import com.myo.blog.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/monitor")
@RequiredArgsConstructor
/**
 * 监控控制器
 */
public class MonitorController {


    private final MonitorService monitorService;

    @GetMapping("/cache")
    public Result getCacheInfo() {
        return monitorService.getCacheInfo();
    }
}