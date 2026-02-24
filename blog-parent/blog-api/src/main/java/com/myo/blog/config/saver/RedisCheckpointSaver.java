package com.myo.blog.config.saver;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 基于 Redis 的 AI 对话检查点持久化实现
 * 
 * 实现了框架的 {@link BaseCheckpointSaver} 接口，将 AI Agent 每一轮对话产生的
 * 检查点（Checkpoint）序列化后存入 Redis，替代默认的 JVM 内存存储（MemorySaver）。

 * 为什么要替换掉 MemorySaver？
 * MemorySaver 把数据存在 JVM 堆内存里，服务重启就全部丢失，也无法在多节点间共享。
 * 用 Redis 存储后，服务重启和水平扩容都不会影响用户的对话上下文。

 * Redis 数据结构：
 *   Key:   ai:agent:checkpoints:{threadId}
 *   Value: 整个 LinkedList<Checkpoint> 序列化后的 JSON 字符串
 *   TTL:   120 分钟（可按需调整）

 * 为什么用 RedisTemplate&lt;String, String&gt;？
 * Spring 默认的 RedisTemplate 使用 JDK 序列化，存到 Redis 里是乱码，
 * 而且与 fastjson2 的反序列化不兼容。改用纯字符串模板，自己掌控序列化全过程，
 * 不受任何框架默认配置干扰。

 * 滑动窗口记忆瘦身：
 * 每次写入时，若历史检查点超过 20 条，自动删除最旧的，防止 Redis 单个 Key 的
 * Value 无限膨胀，也避免 AI 上下文过长导致 Token 超限。
 */
@Slf4j
public class RedisCheckpointSaver implements BaseCheckpointSaver {

    /**
     * 使用纯字符串类型的 RedisTemplate，自己负责序列化与反序列化，
     * 避免 Spring 默认 JDK 序列化带来的乱码和兼容问题。
     */
    private final RedisTemplate<String, String> redisTemplate;

    /** Redis Key 前缀，用于与业务数据隔离，一眼就能在 Redis 里找到 AI 相关的 key */
    private static final String KEY_PREFIX = "ai:agent:checkpoints:";

    /** 检查点数据在 Redis 中的存活时间（分钟），超时自动清理，防止内存泄漏 */
    private static final long EXPIRE_MINUTES = 120;

    /** 滑动窗口最大历史条数，超出后自动丢弃最旧的检查点 */
    private static final int MAX_HISTORY = 20;

    public RedisCheckpointSaver(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // =========================================================================
    // 私有工具方法
    // =========================================================================

    /**
     * 根据 RunnableConfig 中的 threadId 拼出 Redis Key。
     * threadId 是会话的唯一标识，不同管理员的对话通过它隔离，互不干扰。
     * 如果没有 threadId，则使用 "$default" 作为兜底。
     */
    private String getRedisKey(RunnableConfig config) {
        String threadId = (String) config.threadId().orElse("$default");
        return KEY_PREFIX + threadId;
    }

    /**
     * 从 Redis 读取并反序列化检查点列表。
     *
     * 反序列化关键配置解析：
     * 
     *   {@code autoTypeFilter}：fastjson2 的安全白名单。
     *       序列化时写入了每个对象的完整类名（@type），反序列化时 fastjson2
     *       会根据类名反射创建对象。若不设白名单，默认拒绝所有自动类型，
     *       这里放行三个包前缀：
     *       
     *         {@code org.springframework.ai.} — Spring AI 的消息对象（UserMessage 等）
     *         {@code com.alibaba.cloud.ai.}  — 框架底层的 Checkpoint 等对象
     *         {@code java.util.}              — ArrayList、LinkedList 等集合类
     *       
     *   
     *   {@code SupportAutoType}：开启自动类型识别，让 fastjson2 能根据 JSON
     *       里的 @type 字段还原出正确的子类，而不是一律用父类接收。
     *   {@code FieldBased}：终极兼容手段。直接读写底层字段，完全绕过
     *       getter/setter/构造函数。对于那些没有无参构造函数或 getter 的框架内部类尤为重要。
     * 
     *
     * @return 检查点链表，头部是最新的，尾部是最旧的；Redis 无数据时返回空链表
     */
    @SuppressWarnings("unchecked")
    private LinkedList<Checkpoint> getCheckpointsFromRedis(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json != null && !json.trim().isEmpty()) {
            return JSON.parseObject(json, LinkedList.class,
                    JSONReader.autoTypeFilter(
                            "org.springframework.ai.",
                            "com.alibaba.cloud.ai.",
                            "java.util."
                    ),
                    JSONReader.Feature.SupportAutoType,
                    JSONReader.Feature.FieldBased);
        }
        return new LinkedList<>();
    }

    /**
     * 将检查点列表序列化并写入 Redis，同时刷新过期时间。

     * 序列化关键配置解析：

     *   {@code WriteClassName}：在 JSON 中写入每个对象的完整类名（@type 字段），
     *       这是反序列化时能还原出正确子类的前提。没有它，读回来的对象类型会丢失。
     *   {@code FieldBased}：同反序列化一侧，直接序列化底层字段，
     *       绕过框架内部类可能残缺的 getter，保证数据完整写出。
     * 
     */
    private void saveCheckpointsToRedis(String key, LinkedList<Checkpoint> checkpoints) {
        String json = JSON.toJSONString(checkpoints,
                JSONWriter.Feature.WriteClassName,
                JSONWriter.Feature.FieldBased);
        // set 的同时续期，保证活跃会话的数据不会因为 TTL 到期而意外丢失
        redisTemplate.opsForValue().set(key, json, EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    // =========================================================================
    // BaseCheckpointSaver 接口实现
    // =========================================================================

    /**
     * 列出指定会话（threadId）的所有历史检查点，供框架回放或调试使用。
     * 返回不可变集合，防止外部代码意外修改列表。
     */
    @Override
    public Collection<Checkpoint> list(RunnableConfig config) {
        String key = getRedisKey(config);
        return Collections.unmodifiableCollection(getCheckpointsFromRedis(key));
    }

    /**
     * 获取单个检查点，这是框架恢复上下文时最常调用的方法。

     * 有两种模式：

     *   config 中指定了 checkPointId → 按 ID 精确查找，用于"时光旅行"（回到某一步）
     *   config 中没有 checkPointId → 取链表头部，即最新的检查点，用于正常续对话
     * 
     */
    @Override
    public Optional<Checkpoint> get(RunnableConfig config) {
        String key = getRedisKey(config);
        LinkedList<Checkpoint> checkpoints = getCheckpointsFromRedis(key);

        if (checkpoints.isEmpty()) {
            return Optional.empty();
        }

        if (config.checkPointId().isPresent()) {
            // 按 ID 查找，用于回滚到历史某个状态
            String checkPointId = (String) config.checkPointId().get();
            return checkpoints.stream()
                    .filter(c -> c.getId().equals(checkPointId))
                    .findFirst();
        } else {
            // 取头部 = 最新的检查点，正常对话续接时走这里
            return Optional.ofNullable(checkpoints.peek());
        }
    }

    /**
     * 写入一个新的检查点，每轮对话结束后由框架自动调用。
     *
     * 同样有两种模式：
     * 
     *   config 中有 checkPointId → 更新已有检查点（原地替换），用于框架内部修正状态
     *   config 中没有 checkPointId → 新检查点压入链表头部，触发滑动窗口瘦身，
     *       并返回带有新 checkPointId 的 config，供框架下一步使用
     * 
     *
     * @return 携带了本次新检查点 ID 的 RunnableConfig，框架凭此串联多轮对话
     */
    @Override
    public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
        String key = getRedisKey(config);
        LinkedList<Checkpoint> checkpoints = getCheckpointsFromRedis(key);

        if (config.checkPointId().isPresent()) {
            // ---- 更新模式：找到对应 ID 的检查点，原地替换 ----
            String checkPointId = (String) config.checkPointId().get();
            int index = IntStream.range(0, checkpoints.size())
                    .filter(i -> checkpoints.get(i).getId().equals(checkPointId))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format("Checkpoint with id %s not found!", checkPointId)));

            checkpoints.set(index, checkpoint);
            saveCheckpointsToRedis(key, checkpoints);
            return config;

        } else {
            // ---- 新增模式：最新检查点压入头部 ----
            checkpoints.push(checkpoint);

            // 滑动窗口瘦身：超过 MAX_HISTORY 条则持续移除尾部（最旧的）
            // 目的：① 控制 Redis 单个 Key 的数据量  ② 防止 AI 上下文过长导致 Token 超限
            while (checkpoints.size() > MAX_HISTORY) {
                Checkpoint removed = checkpoints.pollLast();
                log.info("触发滑动窗口记忆瘦身，移除最老的检查点：{}", removed.getId());
            }

            saveCheckpointsToRedis(key, checkpoints);
            // 把新 checkPointId 写回 config，框架下一步会用到它
            return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
        }
    }

    /**
     * 释放（清除）指定会话的所有检查点，用于对话结束或主动重置上下文。
     *
     * 将当前数据快照打包成 {@link Tag} 返回给框架（框架可能用于审计或备份），
     * 然后从 Redis 中彻底删除该 Key，释放内存。
     *
     * @return Tag 对象，包含 threadId 和被删除前的检查点列表快照
     */
    @Override
    public Tag release(RunnableConfig config) throws Exception {
        String key = getRedisKey(config);
        LinkedList<Checkpoint> checkpoints = getCheckpointsFromRedis(key);
        String threadId = (String) config.threadId().orElse("$default");

        // 先拿到快照，再删除，保证 Tag 里的数据完整
        Tag tag = new Tag(threadId, checkpoints);
        redisTemplate.delete(key);
        return tag;
    }
}