package com.myo.blog.config.saver;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class RedisCheckpointSaver implements BaseCheckpointSaver {

    // 核心降维打击：改用最纯粹的 <String, String> 模板，不受任何框架默认配置干扰
    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "ai:agent:checkpoints:";
    private static final long EXPIRE_MINUTES = 120;

    public RedisCheckpointSaver(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getRedisKey(RunnableConfig config) {
        String threadId = (String) config.threadId().orElse("$default");
        return KEY_PREFIX + threadId;
    }

    // 手动反序列化
    @SuppressWarnings("unchecked")
    private LinkedList<Checkpoint> getCheckpointsFromRedis(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json != null && !json.trim().isEmpty()) {
            return JSON.parseObject(json, LinkedList.class,
                    // 加上这行白名单过滤器，只有匹配这些包名的类才允许被反射创建
                    //代码原理解析：
                    //在 JSONReader.autoTypeFilter 里面加了三个前缀。
                    //第一个是 org.springframework.ai.，用于放行 Spring AI 原生的 UserMessage 等消息对象。
                    //第二个是 com.alibaba.cloud.ai.，用于放行阿里框架底层的 Checkpoint 检查点对象。
                    //第三个是 java.util.，用于放行底层的集合类比如 ArrayList 等。
                    JSONReader.autoTypeFilter(
                            "org.springframework.ai.",
                            "com.alibaba.cloud.ai.",
                            "java.util."
                    ),

                    JSONReader.Feature.SupportAutoType,
                    // 终极大杀器：绕过变态构造函数，直接基于底层字段反序列化！
                    JSONReader.Feature.FieldBased);
        }
        return new LinkedList<>();
    }

    // 手动序列化
    private void saveCheckpointsToRedis(String key, LinkedList<Checkpoint> checkpoints) {
        String json = JSON.toJSONString(checkpoints,
                JSONWriter.Feature.WriteClassName,
                // 终极大杀器：无视一切 getter/setter，直接提取底层字段序列化！
                JSONWriter.Feature.FieldBased);

        redisTemplate.opsForValue().set(key, json, EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public Collection<Checkpoint> list(RunnableConfig config) {
        String key = getRedisKey(config);
        return Collections.unmodifiableCollection(getCheckpointsFromRedis(key));
    }

    @Override
    public Optional<Checkpoint> get(RunnableConfig config) {
        String key = getRedisKey(config);
        LinkedList<Checkpoint> checkpoints = getCheckpointsFromRedis(key);

        if (checkpoints.isEmpty()) {
            return Optional.empty();
        }

        if (config.checkPointId().isPresent()) {
            String checkPointId = (String) config.checkPointId().get();
            return checkpoints.stream()
                    .filter(c -> c.getId().equals(checkPointId))
                    .findFirst();
        } else {
            return Optional.ofNullable(checkpoints.peek());
        }
    }

    @Override
    public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
        String key = getRedisKey(config);
        LinkedList<Checkpoint> checkpoints = getCheckpointsFromRedis(key);

        if (config.checkPointId().isPresent()) {
            String checkPointId = (String) config.checkPointId().get();
            int index = IntStream.range(0, checkpoints.size())
                    .filter(i -> checkpoints.get(i).getId().equals(checkPointId))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException(String.format("Checkpoint with id %s not found!", checkPointId)));

            checkpoints.set(index, checkpoint);
            saveCheckpointsToRedis(key, checkpoints);
            return config;
        } else {
            checkpoints.push(checkpoint);
            saveCheckpointsToRedis(key, checkpoints);
            return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
        }
    }

    @Override
    public Tag release(RunnableConfig config) throws Exception {
        String key = getRedisKey(config);
        LinkedList<Checkpoint> checkpoints = getCheckpointsFromRedis(key);
        String threadId = (String) config.threadId().orElse("$default");

        Tag tag = new Tag(threadId, checkpoints);
        redisTemplate.delete(key);
        return tag;
    }
}