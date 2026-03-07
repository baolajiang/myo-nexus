package com.myo.blog.common.aop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.IpBlacklistMapper;
import com.myo.blog.dao.pojo.IpBlacklist;
import com.myo.blog.entity.ErrorCode;
import com.myo.blog.entity.Result;
import com.myo.blog.service.MailService; // 引入邮件服务
import com.myo.blog.utils.HttpContextUtils;
import com.myo.blog.utils.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
/**
 * 自定义限流切面类
 * 用于在方法上添加限流功能，防止单位时间内请求次数超过设定的阈值。
 * 1. 可以自定义限流时间（秒）
 * 2. 可以自定义限流次数
 * 3. 可以自定义提示信息
 * 4. 可以自定义管理员邮箱（默认发送给配置的发件人）
 */
public class RateLimitAspect {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 注入邮件服务
    @Autowired
    private MailService mailService;

    // 注入 IP 黑名单映射器
    @Autowired
    private IpBlacklistMapper ipBlacklistMapper;

    // 读取管理员邮箱（这里默认发给自己，也可以配置其他邮箱）
    @Value("${spring.mail.username}")
    private String adminEmail;

    @Around("@annotation(com.myo.blog.common.aop.RateLimit)")
    public Object interceptor(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit != null) {
            HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
            String ipAddr = IpUtils.getIpAddr(request);
            String methodName = method.getName();

            // 检查是否已被封禁
            if (Boolean.TRUE.equals(redisTemplate.hasKey("BAN:IP:" + ipAddr))) {
                return Result.fail(ErrorCode.IP_BANNED.getCode(), ErrorCode.IP_BANNED.getMsg());
            }

            // 原子计数
            String limitKey = "LIMIT:" + ipAddr + ":" + methodName;
            Long currentCount = redisTemplate.opsForValue().increment(limitKey);

            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(limitKey, rateLimit.time(), TimeUnit.SECONDS);
            }

            // 触发限流
            if (currentCount != null && currentCount > rateLimit.count()) {
                log.warn("用户 [{}] 触发接口 [{}] 限流警告 (当前第 {} 次)", ipAddr, methodName, currentCount);

                // --- 恶意攻击统计 ---
                String violationKey = "VIOLATION:" + ipAddr;
                Long violations = redisTemplate.opsForValue().increment(violationKey);

                if (violations != null && violations == 1) {
                    redisTemplate.expire(violationKey, 1, TimeUnit.HOURS);
                }

                // --- 【核心逻辑】判定永久封禁并通知 ---
                if (violations != null && violations >= 100) {
                    log.error(">>> 用户 [{}] 恶意攻击(1小时内累计{}次)，执行永久封禁！", ipAddr, violations);

                    String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    String banInfo = String.format("Time:%s, Count:%d, Reason:Malicious Attack", nowStr, violations);

                    // 执行封禁
                    redisTemplate.opsForValue().set("BAN:IP:" + ipAddr, banInfo);


                    // 将 IP 加入黑名单（先检查是否已存在）
                    try {
                        // 先查询是否已存在
                        Long count = ipBlacklistMapper.selectCount(new LambdaQueryWrapper<IpBlacklist>().eq(IpBlacklist::getIp, ipAddr));
                        if (count == 0) {
                            IpBlacklist blacklist = new IpBlacklist();
                            blacklist.setIp(ipAddr);
                            blacklist.setCreateDate(System.currentTimeMillis());
                            blacklist.setReason("触发限流自动封禁: 1小时内" + violations + "次");
                            ipBlacklistMapper.insert(blacklist);
                            log.info(">>> IP [{}] 已成功加入MySQL黑名单", ipAddr);
                        } else {
                            log.info(">>> IP [{}] 已在MySQL黑名单中，跳过重复插入", ipAddr);
                        }
                    } catch (Exception e) {
                        log.error("IP黑名单入库失败: {}", e.getMessage());
                    }

                    redisTemplate.delete(violationKey);

                    // 发送管理员报警邮件
                    sendAlertEmail(ipAddr, violations, nowStr);

                    return Result.fail(ErrorCode.IP_BANNED.getCode(), ErrorCode.IP_BANNED.getMsg());
                }

                return Result.fail(ErrorCode.RISK_CONTROL.getCode(), rateLimit.msg());
            }
        }
        return joinPoint.proceed();
    }

    /**
     * 封装发送报警邮件的逻辑
     */
    private void sendAlertEmail(String ip, Long count, String time) {
        // 使用异步线程发送，防止阻塞正常的拦截逻辑（虽然 MailService 已经是异步的，但这里处理逻辑解耦更好）
        String subject = "【高危警告】系统自动封禁恶意IP: " + ip;
        String content = String.format(
                "尊敬的管理员：\n\n系统检测到高频恶意请求，已触发自动防御机制。\n\n" +
                        "--------------------------------\n" +
                        "● 封禁对象: %s\n" +
                        "● 封禁时间: %s\n" +
                        "● 违规统计: 1小时内累计触发 %d 次限流\n" +
                        "● 处理结果: 已永久封禁 (Redis Key: BAN:IP:%s)\n" +
                        "--------------------------------\n\n" +
                        "如需解封，请访问管理后台或手动删除 Redis Key。",
                ip, time, count, ip
        );

        // 调用之前写好的 MailService
        mailService.sendMailAsync(adminEmail, subject, content);
    }
}