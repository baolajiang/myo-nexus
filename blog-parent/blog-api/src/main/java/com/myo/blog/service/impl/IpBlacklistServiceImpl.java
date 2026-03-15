package com.myo.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.dao.mapper.IpBlacklistMapper;
import com.myo.blog.dao.pojo.IpBlacklist;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.entity.Result;
import com.myo.blog.service.IpBlacklistService;
import com.myo.blog.utils.IpUtils;
import com.myo.blog.utils.UserThreadLocal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * IP 黑名单管理 Service 实现类
 *
 * 所有写操作均同步维护 Redis，保证 IpBlackListInterceptor 拦截实时生效。
 * 操作顺序：写操作先改数据库再改 Redis，删操作先删数据库再删 Redis。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpBlacklistServiceImpl implements IpBlacklistService {

    // 引用 IP 黑名单 Mapper，负责对 myo_ip_blacklist 表进行增删改查
    private final IpBlacklistMapper ipBlacklistMapper;

    // 引用 Redis 模板，封禁/解封时同步操作，保证拦截器实时生效
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 分页查询 IP 黑名单列表
     * 支持 IP 模糊搜索、封禁类型筛选、状态筛选，按封禁时间倒序
     */
    @Override
    public Result listIp(Map<String, Object> params) {
        // 解析分页参数，提供默认值防止空指针
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int pageSize = params.get("pageSize") != null ? Integer.parseInt(params.get("pageSize").toString()) : 10;
        String ip = params.get("ip") != null ? params.get("ip").toString() : null;
        Integer banType = params.get("banType") != null ? Integer.parseInt(params.get("banType").toString()) : null;
        Integer status = params.get("status") != null ? Integer.parseInt(params.get("status").toString()) : null;

        Page<IpBlacklist> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<IpBlacklist> queryWrapper = new LambdaQueryWrapper<>();

        // IP 地址模糊搜索，支持输入部分 IP 片段查询
        if (StringUtils.isNotBlank(ip)) {
            queryWrapper.like(IpBlacklist::getIp, ip);
        }
        // 封禁类型精确筛选：1=手动 2=限流触发 3=AI风控
        if (banType != null) {
            queryWrapper.eq(IpBlacklist::getBanType, banType);
        }
        // 状态精确筛选：1=封禁中 0=已解封
        if (status != null) {
            queryWrapper.eq(IpBlacklist::getStatus, status);
        }
        // 按封禁时间倒序，最新封禁的排在最前，方便管理员处理最近的异常
        queryWrapper.orderByDesc(IpBlacklist::getCreateDate);

        Page<IpBlacklist> result = ipBlacklistMapper.selectPage(pageParam, queryWrapper);
        return Result.success(result);
    }

    /**
     * 封禁 IP
     *
     * 处理逻辑：
     * 1. 查询 IP 归属地（失败不影响封禁流程）
     * 2. 判断该 IP 是否已在黑名单中
     *    - 已存在：更新状态为封禁中，刷新原因、时间、操作人
     *    - 不存在：新增一条封禁记录
     * 3. 同步写入 Redis，拦截器立即生效
     */
    @Override
    public Result banIp(Map<String, Object> params) {
        String ip = params.get("ip") != null ? params.get("ip").toString().trim() : null;
        if (StringUtils.isBlank(ip)) {
            return Result.fail(400, "IP 地址不能为空");
        }

        String reason = params.get("reason") != null ? params.get("reason").toString() : "管理员手动封禁";
        Long expireTime = params.get("expireTime") != null
                ? Long.parseLong(params.get("expireTime").toString()) : null;

        // 获取当前操作人，用于记录是哪个管理员执行了封禁
        SysUser currentUser = UserThreadLocal.get();
        String operatorId = currentUser != null ? currentUser.getId() : "SYSTEM";

        // 查询 IP 归属地，失败时静默处理不中断封禁流程
        String ipLocation = null;
        try {
            ipLocation = IpUtils.getCityInfo(ip);
        } catch (Exception e) {
            log.warn("[IP封禁] 查询归属地失败，IP: {}", ip);
        }

        // 检查该 IP 是否已存在于黑名单，避免重复插入（表上有唯一索引）
        IpBlacklist existing = ipBlacklistMapper.selectOne(
                new LambdaQueryWrapper<IpBlacklist>().eq(IpBlacklist::getIp, ip)
        );

        if (existing != null) {
            // 已存在：更新为封禁状态，刷新封禁原因和操作人
            LambdaUpdateWrapper<IpBlacklist> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(IpBlacklist::getIp, ip)
                    .set(IpBlacklist::getStatus, 1)
                    .set(IpBlacklist::getReason, reason)
                    .set(IpBlacklist::getExpireTime, expireTime)
                    .set(IpBlacklist::getOperatorId, operatorId)
                    .set(IpBlacklist::getBanType, 1) // 手动封禁
                    .set(IpBlacklist::getCreateDate, System.currentTimeMillis());
            ipBlacklistMapper.update(null, updateWrapper);
        } else {
            // 不存在：新增封禁记录
            IpBlacklist blacklist = new IpBlacklist();
            blacklist.setIp(ip);
            blacklist.setReason(reason);
            blacklist.setIpLocation(ipLocation);
            blacklist.setBanType(1); // 手动封禁
            blacklist.setOperatorId(operatorId);
            blacklist.setExpireTime(expireTime);
            blacklist.setStatus(1);
            blacklist.setViolationCount(0);
            blacklist.setCreateDate(System.currentTimeMillis());
            ipBlacklistMapper.insert(blacklist);
        }

        // 同步写入 Redis，IpBlackListInterceptor 会立即拦截该 IP 的所有请求
        redisTemplate.opsForValue().set("BAN:IP:" + ip, "Manual Ban by " + operatorId);
        log.info("[IP封禁] 手动封禁成功，IP: {}，操作人: {}", ip, operatorId);
        return Result.success("封禁成功");
    }

    /**
     * 解封 IP
     * 更新数据库状态为已解封，同步删除 Redis 封禁键，拦截器立即失效
     */
    @Override
    public Result unbanIp(String ip) {
        ip = ip.trim();
        if (StringUtils.isBlank(ip)) {
            return Result.fail(400, "IP 地址不能为空");
        }

        // 更新数据库：status 改为 0（已解封）
        LambdaUpdateWrapper<IpBlacklist> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(IpBlacklist::getIp, ip)
                .set(IpBlacklist::getStatus, 0);
        ipBlacklistMapper.update(null, updateWrapper);

        // 同步删除 Redis 封禁键，拦截器立即放行该 IP
        redisTemplate.delete("BAN:IP:" + ip);
        log.info("[IP解封] 解封成功，IP: {}", ip);
        return Result.success("解封成功");
    }

    /**
     * 彻底删除 IP 黑名单记录
     * 先查出 IP 地址，再删数据库记录，最后删 Redis 封禁键
     */
    @Override
    public Result deleteIp(Long id) {
        IpBlacklist record = ipBlacklistMapper.selectById(id);
        if (record == null) {
            return Result.fail(404, "记录不存在");
        }

        // 删除数据库记录
        ipBlacklistMapper.deleteById(id);

        // 同步删除 Redis 封禁键，避免记录删了但 Redis 仍拦截的情况
        redisTemplate.delete("BAN:IP:" + record.getIp());
        log.info("[IP删除] 删除记录成功，IP: {}", record.getIp());
        return Result.success("删除成功");
    }
}