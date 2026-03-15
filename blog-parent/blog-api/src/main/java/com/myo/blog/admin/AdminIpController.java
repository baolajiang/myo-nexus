package com.myo.blog.admin;

import com.myo.blog.common.aop.LogAnnotation;
import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.entity.Result;
import com.myo.blog.service.IpBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * IP 黑名单管理后台接口
 * 只负责接收请求和返回结果，业务逻辑全部交给 IpBlacklistService 处理
 */
@RestController
@RequestMapping("/admin/ip")
@RequiredArgsConstructor
public class AdminIpController {

    // 引用 IP 黑名单 Service，所有业务逻辑在此处理
    private final IpBlacklistService ipBlacklistService;

    /**
     * 分页查询 IP 黑名单列表
     * 支持按 IP、封禁类型、状态筛选
     */
    @PostMapping("/list")
    @RequirePermission("ip:list")
    public Result list(@RequestBody Map<String, Object> params) {
        return ipBlacklistService.listIp(params);
    }

    /**
     * 手动封禁 IP
     * 支持永久封禁和临时封禁，同步写入 MySQL 和 Redis
     */
    @PostMapping("/ban")
    @RequirePermission("ip:edit")
    @LogAnnotation(module = "IP黑名单", operator = "手动封禁IP")
    public Result ban(@RequestBody Map<String, Object> params) {
        return ipBlacklistService.banIp(params);
    }

    /**
     * 解封 IP
     * 同步删除 Redis 封禁键，更新数据库状态为已解封
     */
    @PostMapping("/unban")
    @RequirePermission("ip:edit")
    @LogAnnotation(module = "IP黑名单", operator = "解封IP")
    public Result unban(@RequestBody String ip) {
        return ipBlacklistService.unbanIp(ip);
    }

    /**
     * 彻底删除 IP 黑名单记录
     * 同步删除 MySQL 记录和 Redis 封禁键
     */
    @PostMapping("/delete/{id}")
    @RequirePermission("ip:edit")
    @LogAnnotation(module = "IP黑名单", operator = "删除IP记录")
    public Result delete(@PathVariable("id") Long id) {
        return ipBlacklistService.deleteIp(id);
    }
}