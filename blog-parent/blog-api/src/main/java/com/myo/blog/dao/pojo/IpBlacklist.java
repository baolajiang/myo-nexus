package com.myo.blog.dao.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * IP 黑名单实体类，对应 myo_ip_blacklist 表
 * 记录所有被封禁的 IP 信息，配合 Redis 实现实时拦截
 */
@Data
@TableName("myo_ip_blacklist")
public class IpBlacklist {

    // 主键，自增
    @TableId(type = IdType.AUTO)
    private Long id;

    // 封禁的 IP 地址，支持 IPv4 和 IPv6，表中有唯一索引
    private String ip;

    // 封禁时间戳（毫秒）
    private Long createDate;

    // 封禁原因，例如：管理员手动封禁、1小时内触发限流120次
    private String reason;

    // IP 归属地，通过 ip2region 查询，例如：中国-广东-深圳
    private String ipLocation;

    // 封禁类型：1=手动封禁，2=限流触发自动封禁，3=AI风控触发封禁
    private Integer banType;

    // 操作人 ID，管理员手动封禁时填当前用户 ID，系统自动封禁填 "SYSTEM"
    private String operatorId;

    // 封禁到期时间戳（毫秒），NULL 表示永久封禁
    private Long expireTime;

    // 封禁状态：1=封禁中，0=已解封
    private Integer status;

    // 触发违规次数，主要记录限流触发场景下的累计违规次数
    // 来源：RateLimitAspect 中 Redis VIOLATION:{ip} 的计数值
    private Integer violationCount;
}