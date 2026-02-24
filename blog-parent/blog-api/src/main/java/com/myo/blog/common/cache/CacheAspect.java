package com.myo.blog.common.cache;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myo.blog.entity.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 基于 Redis 的方法级缓存切面
 * 配合自定义注解 {@link Cache} 使用。只需在 Service 方法上加
 * {@code @Cache(name="xxx", expire=60000)}，该切面就会自动拦截方法调用，
 * 实现"先查 Redis，有则直接返回缓存，无则执行方法并将结果写入 Redis"的逻辑。
 * 缓存 Key 的构成规则
 *   {注解name} :: {类名} :: {方法名} :: {参数的MD5}
 *   例：article::ArticleServiceImpl::listArticle::a1b2c3d4...
 * 参数部分经过 MD5 处理，既避免 key 过长，也防止参数中的特殊字符导致 Redis 取不到值。
 */
@Aspect    // 声明这是一个 AOP 切面类
@Component // 注册为 Spring Bean，让 Spring 能管理它
@Slf4j     // 注入 log 对象，方便打印日志
@RequiredArgsConstructor
public class CacheAspect {

    private static final ObjectMapper objectMapper = new ObjectMapper();


    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 切点：拦截所有标注了 @Cache 注解的方法。
     * 后续的 @Around 通知通过引用 pt() 来复用这个切点定义。
     */
    @Pointcut("@annotation(com.myo.blog.common.cache.Cache)")
    public void pt() {}

    /**
     * 环绕通知：在目标方法执行前后都能介入，是实现缓存逻辑的核心。
     * 执行流程：
     *   拼接缓存 Key（类名 + 方法名 + 参数MD5）
     *   查 Redis：命中则直接返回缓存结果，不再执行真实方法
     *   未命中：调用真实方法，将结果写入 Redis 并设置过期时间
     *   任何异常均兜底返回系统错误，避免因缓存逻辑崩溃影响业务
     * 
     *
     * @param pjp 连接点，封装了被拦截方法的所有信息（类、方法名、参数等），
     *            调用 pjp.proceed() 才会真正执行目标方法
     */
    @Around("pt()")
    public Object around(ProceedingJoinPoint pjp) {
        try {
            Signature signature = pjp.getSignature();

            // ----------------------------------------------------------------
            // 第一步：收集被拦截方法的基本信息，用于拼接唯一的缓存 Key
            // ----------------------------------------------------------------

            // 被拦截的类名，例如 "ArticleServiceImpl"
            String className = pjp.getTarget().getClass().getSimpleName();

            // 被拦截的方法名，例如 "listArticle"
            String methodName = signature.getName();

            // 方法的实际入参，用于拼接 key（同一个方法、不同参数应该缓存不同结果）
            Object[] args = pjp.getArgs();
            Class[] parameterTypes = new Class[args.length];

            // 把所有参数序列化为 JSON 字符串拼在一起
            String params = "";
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    params += JSON.toJSONString(args[i]);
                    parameterTypes[i] = args[i].getClass();
                } else {
                    parameterTypes[i] = null;
                }
            }

            // 对参数字符串做 MD5：
            // ① 防止参数太长导致 Redis Key 超长
            // ② 防止参数里有特殊字符（如空格、冒号）导致 Key 格式混乱
            if (StringUtils.isNotEmpty(params)) {
                params = DigestUtils.md5Hex(params);
            }

            // ----------------------------------------------------------------
            // 第二步：读取方法上的 @Cache 注解，获取缓存配置
            // ----------------------------------------------------------------

            // 通过反射找到目标方法对象
            Method method = pjp.getSignature().getDeclaringType().getMethod(methodName, parameterTypes);

            // 取出方法上的 @Cache 注解
            Cache annotation = method.getAnnotation(Cache.class);

            // 从注解中读取缓存过期时间（毫秒）和缓存名称前缀
            long expire = annotation.expire();
            String name = annotation.name();

            // ----------------------------------------------------------------
            // 第三步：拼接 Redis Key，查询缓存
            // ----------------------------------------------------------------

            // 最终 Key 格式：article::ArticleServiceImpl::listArticle::a1b2c3...
            String redisKey = name + "::" + className + "::" + methodName + "::" + params;

            String redisValue = redisTemplate.opsForValue().get(redisKey);

            // 缓存命中：直接把 JSON 反序列化成 Result 对象返回，不执行真实方法
            if (StringUtils.isNotEmpty(redisValue)) {
                log.debug("缓存命中，key={}", redisKey);
                Result result = JSON.parseObject(redisValue, Result.class);
                return result;
            }

            // ----------------------------------------------------------------
            // 第四步：缓存未命中，执行真实的业务方法
            // ----------------------------------------------------------------

            // pjp.proceed() 是真正调用目标方法的地方，相当于放行
            Object proceed = pjp.proceed();

            // 将方法返回值序列化为 JSON 字符串，存入 Redis，并设置过期时间
            redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(proceed), Duration.ofMillis(expire));
            log.debug("结果已写入缓存，key={}，过期时间={}ms", redisKey, expire);

            return proceed;

        } catch (Throwable throwable) {
            // 无论是反射出错、Redis 连接失败还是业务方法抛异常，
            // 统一在这里捕获并打印堆栈，返回系统错误，防止异常向上传播导致接口 500
            throwable.printStackTrace();
        }

        return Result.fail(-999, "系统错误");
    }
}