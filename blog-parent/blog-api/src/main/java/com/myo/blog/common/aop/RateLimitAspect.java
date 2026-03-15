package com.myo.blog.common.aop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.IpBlacklistMapper;
import com.myo.blog.dao.pojo.IpBlacklist;
import com.myo.blog.entity.ErrorCode;
import com.myo.blog.entity.Result;
import com.myo.blog.service.MailService;
import com.myo.blog.utils.HttpContextUtils;
import com.myo.blog.utils.IpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 自定义限流切面
 *
 * 拦截所有标注了 @RateLimit 注解的方法，基于 Redis 对每个 IP + 接口方法名的组合进行计数限流。
 *
 * 三层防御机制：
 * 1. 【实时拦截】：每次请求先查 Redis BAN:IP 键，已封禁直接拒绝，不走后续逻辑
 * 2. 【计数限流】：单位时间内超过阈值，返回限流提示，并累计违规次数到 VIOLATION:{ip}
 * 3. 【自动封禁】：1小时内违规累计达到 100 次，触发永久封禁并同步写入 MySQL + 发送告警邮件
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    // Redis 模板，用于限流计数、违规统计、封禁标记的读写
    private final RedisTemplate<String, String> redisTemplate;

    // 邮件服务，触发永久封禁时异步发送告警邮件给管理员
    private final MailService mailService;

    // IP 黑名单 Mapper，触发永久封禁时将 IP 持久化到 MySQL
    private final IpBlacklistMapper ipBlacklistMapper;

    // 管理员邮箱，从配置文件读取，告警邮件的收件人
    @Value("${spring.mail.username}")
    private String adminEmail;

    /**
     * 限流主拦截逻辑
     * 拦截所有标注了 @RateLimit 注解的方法，执行三层防御
     */
    @Around("@annotation(com.myo.blog.common.aop.RateLimit)")
    public Object interceptor(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit != null) {
            HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
            String ipAddr = IpUtils.getIpAddr(request);
            String methodName = method.getName();

            // ===== 第一层：黑名单快速拦截 =====
            // 场景：该 IP 之前已被系统判定为恶意攻击，写入了 Redis BAN:IP:{ip} 封禁标记
            // 做法：直接拒绝，不走任何后续逻辑，性能最优（一次 Redis 读操作搞定）
            // 效果：被封禁的 IP 每次请求都在这里被挡住，完全无法访问任何接口
            if (redisTemplate.hasKey("BAN:IP:" + ipAddr)) {
                return Result.fail(ErrorCode.IP_BANNED.getCode(), ErrorCode.IP_BANNED.getMsg());
            }

            // ===== 第二层：计数限流 =====
            // 场景：该 IP 未被封禁，但单位时间内请求次数过多，触发限流保护
            // 做法：用 Redis Key（LIMIT:{ip}:{方法名}）对每个 IP 的每个接口独立计数
            //       - 不同接口独立计数，互不影响（登录接口触发限流不影响评论接口）
            //       - 窗口时间由 @RateLimit(time=xx) 控制，窗口过期后计数自动清零重来
            //       - increment 是 Redis 原子操作，高并发下计数绝对准确
            // 效果：例如 @RateLimit(time=60, count=5) 表示 60 秒内超过 5 次就触发限流
            //       正常用户 60 秒内请求 ≤5 次，完全不受影响；
            //       第 6、7、8...次请求被拦截并返回限流提示
            String limitKey = "LIMIT:" + ipAddr + ":" + methodName;
            Long currentCount = redisTemplate.opsForValue().increment(limitKey);

            // 第一次计数时才设置过期时间（窗口起点），后续请求不重置，保证窗口固定
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(limitKey, rateLimit.time(), TimeUnit.SECONDS);
            }

            // 超过限流阈值，进入违规统计流程
            if (currentCount != null && currentCount > rateLimit.count()) {
                log.warn("用户 [{}] 触发接口 [{}] 限流警告 (当前第 {} 次)", ipAddr, methodName, currentCount);

                // ===== 第三层：违规累计统计，达到阈值触发永久封禁 =====
                // 场景：该 IP 不只是偶尔触发限流，而是在 1 小时内多次持续触发，累计违规超过 100 次
                // 做法：用 Redis Key（VIOLATION:{ip}）统计该 IP 1 小时内累计触发限流的总次数
                //       - 只有被第二层拦截（即真正触发限流）才会累加，正常请求绝不会计入
                //       - 1 小时窗口从【第一次违规时刻】开始计时，窗口到期自动清零重来
                //       - 窗口内累计超过 100 次即触发永久封禁：写入 Redis + 持久化 MySQL + 发告警邮件
                //
                // 举例：
                //   第  1 分钟 → 违规 55 次，VIOLATION = 55，1小时窗口从此刻开始计时
                //   第 10 分钟 → 再违规 40 次，VIOLATION = 55 + 40 = 95
                //   第 55 分钟 → 再违规  6 次，VIOLATION = 95 + 6 = 101 → 触发永久封禁！
                //
                // 注意边界：若第 1 分钟违规后，等到第 62 分钟再违规，
                //           1小时窗口已过期，VIOLATION 自动清零，不会被封
                String violationKey = "VIOLATION:" + ipAddr;
                Long violations = redisTemplate.opsForValue().increment(violationKey);

                // 第一次违规时设置 1 小时过期，窗口内累计，超时自动清零
                if (violations != null && violations == 1) {
                    redisTemplate.expire(violationKey, 1, TimeUnit.HOURS);
                }

                // 1小时内累计违规达到 100 次，判定为恶意攻击，触发永久封禁
                if (violations != null && violations >= 100) {
                    log.error(">>> 用户 [{}] 恶意攻击(1小时内累计{}次)，执行永久封禁！", ipAddr, violations);

                    String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    String banInfo = String.format("Time:%s, Count:%d, Reason:Malicious Attack", nowStr, violations);

                    // 写入 Redis 封禁标记，后续所有请求在第一层直接拦截
                    redisTemplate.opsForValue().set("BAN:IP:" + ipAddr, banInfo);

                    // 持久化到 MySQL，保证 Redis 重启后封禁不失效
                    try {
                        // 先查是否已存在，避免唯一索引冲突报错
                        Long count = ipBlacklistMapper.selectCount(
                                new LambdaQueryWrapper<IpBlacklist>().eq(IpBlacklist::getIp, ipAddr)
                        );
                        if (count == 0) {
                            IpBlacklist blacklist = new IpBlacklist();
                            blacklist.setIp(ipAddr);
                            blacklist.setCreateDate(System.currentTimeMillis());
                            blacklist.setReason("触发限流自动封禁: 1小时内" + violations + "次");
                            blacklist.setBanType(2);                            // 限流触发自动封禁
                            blacklist.setOperatorId("SYSTEM");                  // 系统自动执行
                            blacklist.setStatus(1);                             // 封禁中
                            blacklist.setViolationCount(violations.intValue()); // 记录违规次数
                            // 查询 IP 归属地，失败时静默处理，不影响封禁主流程
                            try {
                                blacklist.setIpLocation(IpUtils.getCityInfo(ipAddr));
                            } catch (Exception ignored) {}
                            ipBlacklistMapper.insert(blacklist);
                            log.info(">>> IP [{}] 已成功加入MySQL黑名单", ipAddr);
                        } else {
                            // 已存在说明之前被封禁过（如 Redis 重启后重新触发），跳过重复插入
                            log.info(">>> IP [{}] 已在MySQL黑名单中，跳过重复插入", ipAddr);
                        }
                    } catch (Exception e) {
                        // 入库失败只记录日志，不影响 Redis 封禁的实时生效
                        log.error("IP黑名单入库失败: {}", e.getMessage());
                    }

                    // 封禁完成后清空违规计数，防止解封后计数仍是满的导致立刻再被封
                    redisTemplate.delete(violationKey);

                    // 异步发送告警邮件通知管理员，不阻塞当前拦截逻辑
                    sendAlertEmail(ipAddr, violations, nowStr);

                    return Result.fail(ErrorCode.IP_BANNED.getCode(), ErrorCode.IP_BANNED.getMsg());
                }

                // 未达到封禁阈值，返回限流提示，用户稍后可重试
                return Result.fail(ErrorCode.RISK_CONTROL.getCode(), rateLimit.msg());
            }
        }
        // 未触发限流，放行请求，执行真实业务逻辑
        return joinPoint.proceed();
    }

    /**
     * 异步发送永久封禁告警邮件
     * 邮件内容包含封禁 IP、触发时间、违规次数、处理结果和解封说明
     *
     * @param ip    被封禁的 IP 地址
     * @param count 1小时内累计违规次数
     * @param time  封禁触发时间字符串
     */
    private void sendAlertEmail(String ip, Long count, String time) {
        String subject = "【高危警告】系统自动封禁恶意IP: " + ip;
        String content = String.format(
                "尊敬的管理员：\n\n系统检测到高频恶意请求，已触发自动防御机制。\n\n" +
                        "--------------------------------\n" +
                        "● 封禁对象: %s\n" +
                        "● 封禁时间: %s\n" +
                        "● 违规统计: 1小时内累计触发 %d 次限流\n" +
                        "● 处理结果: 已永久封禁 (Redis Key: BAN:IP:%s)\n" +
                        "--------------------------------\n\n" +
                        "如需解封，请访问管理后台 IP 黑名单页面操作。",
                ip, time, count, ip
        );
        // MailService 内部已是异步执行，不会阻塞当前线程
        mailService.sendMailAsync(adminEmail, subject, content);
    }
}