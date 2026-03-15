package com.myo.blog.service;

import com.myo.blog.entity.Result;

import java.util.Map;

/**
 * IP 黑名单管理 Service 接口
 */
public interface IpBlacklistService {

    /**
     * 分页查询 IP 黑名单列表
     * 支持按 IP 模糊搜索、封禁类型、状态筛选
     *
     * @param params 包含 page、pageSize、ip、banType、status
     */
    Result listIp(Map<String, Object> params);

    /**
     * 封禁 IP
     * 同步写入 MySQL 和 Redis，拦截器实时生效
     * 支持永久封禁（expireTime=null）和临时封禁（expireTime 有值）
     * 若 IP 已存在则更新状态，不存在则新增
     *
     * @param params 包含 ip、reason、expireTime（可选）
     */
    Result banIp(Map<String, Object> params);

    /**
     * 解封 IP
     * 同步删除 Redis 封禁键，更新数据库状态为已解封
     *
     * @param ip 要解封的 IP 地址
     */
    Result unbanIp(String ip);

    /**
     * 彻底删除 IP 黑名单记录
     * 同步删除 MySQL 记录和 Redis 封禁键
     *
     * @param id 记录 ID
     */
    Result deleteIp(Long id);
}