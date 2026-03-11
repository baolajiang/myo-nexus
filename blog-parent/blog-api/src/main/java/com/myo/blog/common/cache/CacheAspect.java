package com.myo.blog.common.cache;

import com.alibaba.fastjson.JSON;
import com.myo.blog.entity.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 基于 Redis 的方法级缓存切面，配合 {@link Cache} 注解使用。
 *
 * 使用方式：在 Service 方法上加 @Cache(name="article", expire=60000)
 * 切面会自动拦截，实现"先查缓存，命中直接返回，未命中查库并写入缓存"。
 *
 * Redis Key 格式：{name}::{类名}::{方法名}::{参数MD5}
 * 示例：article::ArticleServiceImpl::listArticle::a1b2c3d4
 *
 * 解决的三大缓存问题：
 *   - 缓存穿透：DB 无数据时写入 "NULL" 占位符，2分钟后过期
 *   - 缓存击穿：分布式锁 + UUID + Lua 原子释放，防止热点 key 过期时大量请求同时打库
 *   - 缓存雪崩：过期时间加 0~30s 随机抖动，避免大量 key 同时失效
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class CacheAspect {

    private final RedisTemplate<String, String> redisTemplate;

    // -------------------------------------------------------------------------
    // 常量配置
    // -------------------------------------------------------------------------

    /** 空值占位符。DB 无数据时存入此值，防止缓存穿透 */
    private static final String NULL_VALUE = "NULL";

    /** 分布式锁 key 前缀，与业务 key 区分开 */
    private static final String LOCK_PREFIX = "lock::";

    /** 分布式锁持有超时时间。超时自动释放，防止线程崩溃后死锁 */
    private static final Duration LOCK_EXPIRE = Duration.ofSeconds(10);

    /** 空值占位符的过期时间。不宜太长，避免长期占用内存 */
    private static final Duration NULL_EXPIRE = Duration.ofMinutes(2);

    /** 雪崩抖动上限（毫秒）。实际过期时间 = expire + [0, 30s) 随机值 */
    private static final long JITTER_MAX_MS = 30_000L;

    /** 未抢到锁时的单次等待时间（毫秒） */
    private static final long LOCK_WAIT_MS = 50L;

    /**
     * 抢锁最大重试次数。
     * 50次 × 50ms = 最长等待约 2.5 秒，超出后降级直接查库
     */
    private static final int MAX_RETRY_COUNT = 50;

    /**
     * Lua 脚本：原子性地"判断 + 删除"锁，防止误删其他线程的锁。
     *
     * 为什么必须用 Lua？
     *   如果"判断值"和"删除"是两个 Redis 命令，中间可能被打断：
     *   线程A判断完、还没删除时锁超时 → 线程B拿到锁 → 线程A再删就误删了线程B的锁。
     *   Lua 脚本在 Redis 中是原子执行的，整个过程不会被其他命令插入。
     *
     * 逻辑：GET key == uuid ? DEL key : 不操作
     *
     * 提为 static final，避免每次释放锁时重复创建对象（高并发下有意义）
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] " +
                        "then return redis.call('del', KEYS[1]) " +
                        "else return 0 end"
        );
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    // -------------------------------------------------------------------------
    // 切点 & 主流程
    // -------------------------------------------------------------------------

    /** 切点：拦截所有标注了 @Cache 注解的方法 */
    @Pointcut("@annotation(com.myo.blog.common.cache.Cache)")
    public void pt() {}

    /**
     * 环绕通知：缓存读写主逻辑。
     *
     * 流程：
     *   1. 构造 Redis Key
     *   2. 查缓存：命中则直接返回（含空值占位符处理）
     *   3. 未命中：进入加锁重建流程
     *   4. 异常兜底：降级直接执行真实方法，保证业务可用
     */
    @Around("pt()")
    public Object around(ProceedingJoinPoint pjp) {
        try {
            Method method = getMethod(pjp);
            Cache cacheAnnotation = method.getAnnotation(Cache.class);
            String redisKey = buildRedisKey(pjp, method, cacheAnnotation);

            // 查缓存
            String redisValue = redisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isNotEmpty(redisValue)) {
                // 命中空值占位符：DB 本来就没数据，直接返回空，不打库
                if (NULL_VALUE.equals(redisValue)) {
                    log.debug("[缓存] 命中空值占位符，key={}", redisKey);
                    return Result.success(null);
                }
                log.debug("[缓存] 命中，key={}", redisKey);
                return JSON.parseObject(redisValue, Result.class);
            }

            // 缓存未命中，加分布式锁后重建
            return loadWithLock(pjp, redisKey, cacheAnnotation.expire());

        } catch (Throwable throwable) {
            // 兜底降级：无论是 Redis 故障、反射出错还是业务异常，都不能让接口 500
            // 策略：绕过缓存，直接执行真实方法
            log.error("[缓存] 切面异常，降级直接执行，原因：{}", throwable.getMessage(), throwable);
            try {
                return pjp.proceed();
            } catch (Throwable e) {
                log.error("[缓存] 降级执行也失败", e);
                return Result.fail(-999, "系统错误");
            }
        }
    }

    // -------------------------------------------------------------------------
    // 加锁重建缓存
    // -------------------------------------------------------------------------

    /**
     * 加分布式锁后重建缓存，防止缓存击穿。
     *
     * 流程：
     *   while 循环尝试抢锁（最多 MAX_RETRY_COUNT 次）
     *     ├─ 抢到锁 → 双重检查 → 查库 → 写缓存 → Lua 释放锁
     *     └─ 未抢到 → 等待 50ms → 重试
     *   超过重试次数 → 降级直接查库
     *
     * @param pjp      连接点
     * @param redisKey 业务缓存 key
     * @param expire   注解配置的过期时间（毫秒）
     */
    private Object loadWithLock(ProceedingJoinPoint pjp, String redisKey, long expire) throws Throwable {
        String lockKey = LOCK_PREFIX + redisKey;

        // 优化：lockValue 提到循环外，只生成一次
        // 同一次"抢锁任务"始终使用同一个 UUID，重试时不需要换新值
        String lockValue = UUID.randomUUID().toString();

        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {

            // 尝试加锁：SET lockKey lockValue NX PX 10000（原子操作）
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, LOCK_EXPIRE);

            if (Boolean.TRUE.equals(locked)) {
                // 抢到锁，由本线程负责重建缓存
                try {
                    // 双重检查：等锁期间其他线程可能已经写好缓存了，避免重复查库
                    String redisValue = redisTemplate.opsForValue().get(redisKey);
                    if (StringUtils.isNotEmpty(redisValue)) {
                        if (NULL_VALUE.equals(redisValue)) return Result.success(null);
                        return JSON.parseObject(redisValue, Result.class);
                    }

                    // 执行真实业务方法 ，获取业务方法的返回值,
                    // 带有@Cacheable注解的方法放行，去db查询，
                    // 将查询结果写入缓存，没有就返回null，写入空值占位符，
                    // 后续请求直接返回null，不去db查询，避免缓存穿透。
                    Object proceed = pjp.proceed();

                    // 判断是否为空数据，决定写正常缓存还是空值占位符，
                    // 为空数据时，写入空值占位符，较短过期时间，后续请求直接找这个key返回null，避免缓存穿透。
                    // 不为空数据时，写入正常缓存，较长过期时间,并且为了避免缓存雪崩，在注解配置的过期时间上加随机抖动，错开失效时间
                    if (isEmptyResult(proceed)) {
                        // 防穿透：DB 也没数据，写空值占位符，较短过期时间
                        log.debug("[缓存] DB 无数据，写入空值占位符，key={}", redisKey);
                        redisTemplate.opsForValue().set(redisKey, NULL_VALUE, NULL_EXPIRE);
                    } else {
                        // 防雪崩：在注解配置的过期时间上加随机抖动，错开失效时间
                        long jitter = (long) (Math.random() * JITTER_MAX_MS);
                        Duration finalExpire = Duration.ofMillis(expire + jitter);
                        redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(proceed), finalExpire);
                        log.debug("[缓存] 写入缓存，key={}，过期={}ms（含抖动）", redisKey, finalExpire.toMillis());
                    }

                    return proceed;

                } finally {
                    // 用 Lua 脚本原子释放锁：先比对 UUID，是自己的才删除
                    // 防止场景：线程A锁超时自动释放 → 线程B拿到锁 → 线程A走finally误删线程B的锁
                    redisTemplate.execute(
                            UNLOCK_SCRIPT,
                            Collections.singletonList(lockKey),
                            lockValue
                    );
                }
            }

            // 未抢到锁，等待后重试
            retryCount++;
            log.debug("[缓存] 未抢到锁，等待{}ms后重试({}/{})，key={}", LOCK_WAIT_MS, retryCount, MAX_RETRY_COUNT, redisKey);
            Thread.sleep(LOCK_WAIT_MS);
        }

        // 超过最大重试次数，降级直接查库，保证业务可用
        log.warn("[缓存] 等待锁超时（{}次），降级直接查库，key={}", MAX_RETRY_COUNT, redisKey);
        return pjp.proceed();
    }

    // -------------------------------------------------------------------------
    // 工具方法
    // -------------------------------------------------------------------------

    /**
     * 判断业务返回值是否为"空数据"。
     *
     * 项目中 Service 层统一返回 Result 对象，proceed 不会是 null，
     * 必须拆开 Result 判断里面的 data 字段。
     * 非 Result 类型时保守返回 false（认为有数据），不缓存空值。
     */
    private boolean isEmptyResult(Object proceed) {
        if (proceed instanceof Result result) {
            return result.getData() == null;
        }
        return false;
    }

    /**
     * 构造 Redis Key。
     * 格式：{name}::{类名}::{方法名}::{参数MD5}
     *
     * 参数部分经过 MD5：① 防止 key 过长 ② 防止参数含特殊字符
     * 无参数时使用固定字符串 "noargs"
     */
    private String buildRedisKey(ProceedingJoinPoint pjp, Method method, Cache annotation) {
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        Object[] args = pjp.getArgs();
        String paramsMd5 = "noargs";
        if (args != null && args.length > 0) {
            StringBuilder params = new StringBuilder();
            for (Object arg : args) {
                params.append(arg != null ? JSON.toJSONString(arg) : "null");
            }
            paramsMd5 = DigestUtils.md5Hex(params.toString());
        }

        return annotation.name() + "::" + className + "::" + methodName + "::" + paramsMd5;
    }

    /**
     * 从 AOP 连接点获取被拦截的 Method 对象。
     *
     * 使用 MethodSignature 而不是手动反射，安全且准确。
     * 手动反射的问题：通过 args[i].getClass() 推断参数类型，
     * 多态时会出错（方法定义 List，实际传 ArrayList，反射找不到方法）。
     */
    private Method getMethod(ProceedingJoinPoint pjp) {
        return ((MethodSignature) pjp.getSignature()).getMethod();
    }
}