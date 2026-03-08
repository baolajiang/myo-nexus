package com.myo.blog.controller;

import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.MessageParam;
import com.myo.blog.entity.params.QRcodeParar;
import com.myo.blog.service.MessageService;
import com.myo.blog.utils.IpUtils;
import com.myo.blog.utils.QRcodeUtils;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("utils")
@RequiredArgsConstructor
public class UtilController {
    private final MessageService messageService;

    //获取访问设备
    @GetMapping("getUserAgent")
    public Result getUserAgent(HttpServletRequest req){
        String ip = IpUtils.getIpAddr(req);
        log.debug("获取用户代理信息 - IP: {}", ip);
        UserAgent useragent = IpUtils.getUserAgent(req);
        log.debug("用户代理信息获取成功 - IP: {}, 设备: {}", ip, useragent.getBrowser().getName());
        return Result.success(useragent);
    }

    //城市信息
    @GetMapping("getCityInfo")
    public Result getCityInfo(HttpServletRequest req){
        String ip = IpUtils.getIpAddr(req);
        log.debug("获取城市信息 - IP: {}", ip);
        String hostName = IpUtils.getCityInfo(ip);
        log.info("城市信息获取成功 - IP: {}, 城市: {}", ip, hostName);
        return Result.success(hostName);
    }

    //获取主机名
    @GetMapping("getHostName")
    public Result currentUser(HttpServletRequest req){
        String ip = IpUtils.getIpAddr(req);
        log.debug("获取主机名 - IP: {}", ip);
        String hostName = req.getRemoteHost();
        if(hostName.equals("hecs-79668")){
            hostName = IpUtils.getIpAddr(req);
        }
        log.info("主机名获取成功 - IP: {}, 主机名: {}", ip, hostName);
        return Result.success(hostName);
    }

    //当前ip
    @GetMapping("getIpAddress")
    public Result getIpAddress(HttpServletRequest req){
        String ip = IpUtils.getIpAddr(req);
        log.info("获取IP地址 - 客户端IP: {}", ip);
        return Result.success(ip);
    }


    //生成二维码
    //type 1:自定义配置 2:自定义二维码 3:自定义二维码+自定义二维码
    @PostMapping("/create")
    public void contextLoads(HttpServletResponse response, @RequestBody QRcodeParar qr) throws IOException {
        String content = qr.getContent();
        int type = qr.getType();
        log.info("生成二维码 - 内容: {}, 类型: {}", content, type);

        if(type == 1){
            QRcodeUtils.testCustomConfig(response, qr);
        } else if(type == 2){
            QRcodeUtils.testCustomCodeEyes(response, qr);
        } else if(type == 3){
            QRcodeUtils.testYuanXing(response, qr);
        }
        log.debug("二维码生成完成 - 内容: {}", content);
    }

    //站点信息
    @GetMapping("getWebinfo")
    public Result getWebinfo(){
        log.debug("获取站点信息");
        return Result.success("");
    }
}