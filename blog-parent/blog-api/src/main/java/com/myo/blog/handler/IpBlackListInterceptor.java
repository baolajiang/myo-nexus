package com.myo.blog.handler;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.IpBlacklistMapper;
import com.myo.blog.dao.pojo.IpBlacklist;
import com.myo.blog.entity.ErrorCode;
import com.myo.blog.entity.Result;
import com.myo.blog.utils.IpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * IP 黑名单拦截器 (高可用版)
 * 策略：Redis 优先，数据库兜底
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpBlackListInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, String> redisTemplate;

    private final IpBlacklistMapper ipBlacklistMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ipAddr = IpUtils.getIpAddr(request);
        boolean isBanned = false;

        try {
            // 1. 优先查 Redis (速度快)
            if (redisTemplate.hasKey("BAN:IP:" + ipAddr)) {
                isBanned = true;
            }
        } catch (Exception e) {
            // 2. 【关键】如果 Redis 挂了(连接失败等)，不要让系统崩，而是降级查 MySQL (安全兜底),即使Redis挂了，系统仍能正常工作
            log.error("Redis服务异常，自动降级查询数据库黑名单: {}", e.getMessage());
            try {
                Long count = ipBlacklistMapper.selectCount(new LambdaQueryWrapper<IpBlacklist>().eq(IpBlacklist::getIp, ipAddr));
                if (count > 0) {
                    isBanned = true;
                }
            } catch (Exception dbError) {
                // 如果连数据库都挂了，那就彻底没办法了，只能放行（保证服务可用性）或者报错
                log.error("数据库查询失败: {}", dbError.getMessage());
            }
        }

        // 3. 执行拦截
        if (isBanned) {
            response.setContentType("application/json;charset=utf-8");
            PrintWriter out = response.getWriter();
            out.write(JSON.toJSONString(Result.fail(ErrorCode.IP_BANNED.getCode(), "您的IP已被系统永久封禁")));
            out.flush();
            out.close();
            return false;
        }

        return true;
    }
}