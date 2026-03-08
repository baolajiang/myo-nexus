package com.myo.blog.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.IpBlacklistMapper;
import com.myo.blog.dao.pojo.IpBlacklist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * 项目启动时执行：将 MySQL 数据库中的黑名单同步到 Redis 缓存中
 * 功能：在项目启动时，从数据库加载所有 IP 黑名单，并将其同步到 Redis 缓存中
 * 优势：启动时加载，后续请求直接从 Redis 读取，避免数据库查询延迟
 * 注意：如果 Redis 重启，需要重新加载黑名单到 Redis 缓存中
 */
public class IpBlacklistRunner implements CommandLineRunner {

    private final IpBlacklistMapper ipBlacklistMapper;

    private final RedisTemplate<String, String> redisTemplate;

    // 定义标记 Key，用来判断 Redis 是否重启过
    public static final String BLACKLIST_MARKER_KEY = "BAN:IS_LOADED";

    @Override
    public void run(String... args) throws Exception {
        loadBlacklist();
    }

    /**
     * 公共方法：加载黑名单并设置标记
     */
    public void loadBlacklist() {
        log.info(">>> 开始加载 IP 黑名单到 Redis...");

        List<IpBlacklist> blacklists = ipBlacklistMapper.selectList(new LambdaQueryWrapper<>());

        for (IpBlacklist record : blacklists) {
            String key = "BAN:IP:" + record.getIp();
            redisTemplate.opsForValue().set(key, "From DB: " + record.getReason());
        }

        // 【关键一步】插上旗帜，表示数据已加载
        // 不设置过期时间，除非 Redis 重启，否则它一直都在
        redisTemplate.opsForValue().set(BLACKLIST_MARKER_KEY, "1");

        log.info(">>> IP 黑名单加载完毕，共 {} 条，且已设置存活标记", blacklists.size());
    }
}