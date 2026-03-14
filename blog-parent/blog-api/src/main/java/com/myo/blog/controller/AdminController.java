package com.myo.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.common.aop.LogAnnotation;
import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.dao.mapper.IpBlacklistMapper;
import com.myo.blog.dao.pojo.IpBlacklist;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.entity.ErrorCode;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.entity.params.UserParam;

import com.myo.blog.service.CommentsService;
import com.myo.blog.service.SysLogService;
import com.myo.blog.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
// 管理员接口
@RestController
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {

    private final RedisTemplate<String, String> redisTemplate;


    private final IpBlacklistMapper ipBlacklistMapper;



    /**
     * 1. 手动封禁 IP
     */
    @PostMapping("ban")
    @RequirePermission("ip:edit")
    @LogAnnotation(module = "系统管理", operator = "封禁用户")
    public Result banIp(@RequestParam String ip) {
        // 1. 写入 Redis (立刻生效)
        redisTemplate.opsForValue().set("BAN:IP:" + ip, "Manual Ban by Admin");

        // 2. 写入 MySQL (持久化)
        // 先判断是否存在，防止重复插入报错
        Long count = ipBlacklistMapper.selectCount(new LambdaQueryWrapper<IpBlacklist>().eq(IpBlacklist::getIp, ip));
        if (count == 0) {
            IpBlacklist blacklist = new IpBlacklist();
            blacklist.setIp(ip);
            blacklist.setCreateDate(System.currentTimeMillis());
            blacklist.setReason("管理员手动封禁");
            ipBlacklistMapper.insert(blacklist);
        }

        return Result.success("已成功封禁 IP: " + ip);
    }

    /**
     * 2. 手动解封 IP
     * 同时删除 Redis 和 MySQL 中的记录
     */
    @PostMapping("unban")
    @RequirePermission("ip:edit")
    @LogAnnotation(module = "系统管理", operator = "解封用户")
    public Result unbanIp(@RequestParam String ip) {
        // 1. 删 Redis
        Boolean redisDeleted = redisTemplate.delete("BAN:IP:" + ip);

        // 2. 删 MySQL
        int dbDeleted = ipBlacklistMapper.delete(new LambdaQueryWrapper<IpBlacklist>().eq(IpBlacklist::getIp, ip));

        if (Boolean.TRUE.equals(redisDeleted) || dbDeleted > 0) {
            return Result.success("已成功解除 IP [" + ip + "] 的封禁");
        }
        return Result.fail(ErrorCode.OPERATION_FAILED.getCode(), "该 IP 未被封禁或已解封");
    }

    /**
     * 3. 查询黑名单列表 (支持分页)
     * 方便管理员在后台查看当前封禁了哪些人
     */
    @PostMapping("blacklist")
    public Result listBlacklist(@RequestBody PageParams pageParams) {
        // 创建分页对象 (当前页, 每页条数)
        Page<IpBlacklist> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());

        // 执行查询 (按时间倒序排列，新封的在前面)
        LambdaQueryWrapper<IpBlacklist> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(IpBlacklist::getCreateDate);

        Page<IpBlacklist> ipBlacklistPage = ipBlacklistMapper.selectPage(page, queryWrapper);

        // 直接返回分页结果
        return Result.success(ipBlacklistPage);
    }




}