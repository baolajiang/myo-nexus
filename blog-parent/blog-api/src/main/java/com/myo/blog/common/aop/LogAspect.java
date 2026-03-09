package com.myo.blog.common.aop;

import com.alibaba.fastjson.JSON;
import com.myo.blog.dao.mapper.SysLogMapper;
import com.myo.blog.dao.pojo.SysLog;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.service.ThreadService;
import com.myo.blog.utils.HttpContextUtils;
import com.myo.blog.utils.IpUtils;
import com.myo.blog.utils.UserThreadLocal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.UUID;

@Component
@Aspect
@Slf4j
@RequiredArgsConstructor
public class LogAspect {

    private static final String TRACE_ID = "TRACE_ID";


    private final ThreadService threadService;


    private final SysLogMapper sysLogMapper;

    @Pointcut("@annotation(com.myo.blog.common.aop.LogAnnotation)")
    public void logPointCut() {}

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put(TRACE_ID, traceId);

        Object result = null;
        Throwable error = null;
        try {
            // 执行业务逻辑
            result = point.proceed();
            return result;
        } catch (Throwable e) {
            // 如果报错了，把异常抓起来记在小本本上，然后再原样抛出去
            error = e;
            throw e;
        } finally {
            // 无论成功失败，都记录日志并异步存入数据库
            long time = System.currentTimeMillis() - beginTime;
            recordLog(point, time, result, error, traceId);
            MDC.remove(TRACE_ID);
        }
    }

    private void recordLog(ProceedingJoinPoint joinPoint, long time, Object result, Throwable error, String traceId) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogAnnotation logAnnotation = method.getAnnotation(LogAnnotation.class);

        SysLog sysLog = new SysLog();
        sysLog.setTraceId(traceId);
        sysLog.setTime(time);
        sysLog.setCreateDate(System.currentTimeMillis());

        // 1. 模块与操作描述
        sysLog.setModule(logAnnotation.module());
        sysLog.setOperation(logAnnotation.operator());

        // 2. 请求信息
        sysLog.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        if (request != null) {
            sysLog.setIp(IpUtils.getIpAddr(request));
        }

        // 3. 参数脱敏与截断
        Object[] args = joinPoint.getArgs();
        String params = JSON.toJSONString(args);
        if (params.contains("password")) {
            params = "****** (Desensitized)";
        }
        if (params.length() > 5000) {
            params = params.substring(0, 5000) + "...";
        }
        sysLog.setParams(params);

        // 4. 记录操作人 (如果已经登录)
        SysUser user = UserThreadLocal.get();
        if (user != null) {
            sysLog.setUserid(user.getId());
            sysLog.setNickname(user.getNickname());
        } else {
            sysLog.setUserid("0");
            sysLog.setNickname("未登录访客");
        }

        // 5. 记录执行状态和结果
        if (error != null) {
            sysLog.setStatus(1); // 1 代表失败
            sysLog.setErrorMsg(error.getMessage());
        } else {
            sysLog.setStatus(0); // 0 代表成功
            String resultStr = JSON.toJSONString(result);
            if (resultStr != null && resultStr.length() > 5000) {
                resultStr = resultStr.substring(0, 5000) + "...";
            }
            sysLog.setResult(resultStr);
        }

        // 6. 交给线程池异步落库，绝不阻塞主线程！
        threadService.saveLog(sysLogMapper, sysLog);
    }
}