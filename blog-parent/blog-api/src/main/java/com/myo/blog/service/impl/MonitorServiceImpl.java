package com.myo.blog.service.impl;

import com.myo.blog.entity.Result;
import com.myo.blog.service.MonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MonitorServiceImpl implements MonitorService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public Result getCacheInfo() {
        // 1. 获取 Redis 服务器基本信息
        Properties info = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info());

        // 2. 获取命令统计
        Properties commandStats = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info("commandstats"));

        // 3. 获取 Key 数量
        Object dbSize = redisTemplate.execute((RedisCallback<Object>) connection -> connection.dbSize());

        Map<String, Object> result = new HashMap<>();
        result.put("info", info);
        result.put("dbSize", dbSize);

        // ================= 大厂核心指标计算区 =================
        if (info != null) {
            // A. 计算缓存命中率 (Hit Rate)
            long hits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
            long misses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
            double hitRate = 0.0;
            if (hits + misses > 0) {
                hitRate = ((double) hits / (hits + misses)) * 100;
            }
            // 保留两位小数
            result.put("hitRate", Math.round(hitRate * 100.0) / 100.0);

            // B. 实时 QPS (每秒处理指令数)
            result.put("qps", info.getProperty("instantaneous_ops_per_sec", "0"));

            // C. 网络吞吐量 (实时网络接收/发送带宽 KB/s)
            result.put("netInput", info.getProperty("instantaneous_input_kbps", "0"));
            result.put("netOutput", info.getProperty("instantaneous_output_kbps", "0"));
        }
        // =====================================================

        // 4. 解析命令统计 (供圆环图使用)
        List<Map<String, Object>> pieList = new ArrayList<>();
        if (commandStats != null) {
            commandStats.stringPropertyNames().forEach(key -> {
                Map<String, Object> data = new HashMap<>();
                String property = commandStats.getProperty(key);
                data.put("name", key.replace("cmdstat_", ""));
                String calls = property.substring(property.indexOf("calls=") + 6, property.indexOf(",usec="));
                data.put("value", Integer.parseInt(calls));
                pieList.add(data);
            });
        }
        result.put("commandStats", pieList);

        return Result.success(result);
    }
}