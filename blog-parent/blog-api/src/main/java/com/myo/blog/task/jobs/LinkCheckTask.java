package com.myo.blog.task.jobs;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.LinkMapper;
import com.myo.blog.dao.pojo.Link;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 友链检测任务
@Slf4j
@Component("linkCheckTask")
@RequiredArgsConstructor
public class LinkCheckTask {

    private final LinkMapper linkMapper;

    // 1. 无参方法：供定时任务默认调度使用
    public void run() {
        // 默认超时时间 5000 毫秒
        executeCheck(5000);
    }

    // 2. 有参方法：供前端“执行一次”时动态传参使用
    public void run(String param) {
        log.info("[友链检测任务] 手动触发，接收到动态参数：{}", param);
        int timeout = 5000;

        if (org.springframework.util.StringUtils.hasText(param)) {
            // 只需要做最基础的业务防御
            if (!param.contains("{")) {
                // 手动抛出 IllegalArgumentException，引擎立刻能识别！
                throw new IllegalArgumentException("参数必须是 JSON 格式");
            }
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"timeout\"\\s*:\\s*(\\d+)").matcher(param);
            if (matcher.find()) {
                timeout = Integer.parseInt(matcher.group(1));
            }
        }
        executeCheck(timeout);
    }

    // 3. 核心检测逻辑
    private void executeCheck(int timeout) {
        log.info("[友链检测任务] 开始执行... 当前设定的检测超时时间为: {} ms", timeout);

        // 查询出所有的友情链接
        List<Link> linkList = linkMapper.selectList(new LambdaQueryWrapper<>());
        if (linkList.isEmpty()) {
            log.info("[友链检测任务] 暂无友情链接需要检测");
            return;
        }

        int deadCount = 0;
        StringBuilder deadLinksInfo = new StringBuilder();

        // 遍历检测每个链接是否存活
        for (Link link : linkList) {
            boolean isAlive = checkUrl(link.getUrl(), timeout);
            if (!isAlive) {
                log.error("[友链检测任务] 发现失效友链: 网站名称={}, URL={}", link.getName(), link.getUrl());
                deadCount++;
                deadLinksInfo.append(link.getName()).append(" (").append(link.getUrl()).append(")\n");
            }
        }

        log.info("[友链检测任务] 执行完毕。共检测 {} 个链接，发现 {} 个失效链接。", linkList.size(), deadCount);

        // 【梦幻联动】如果我们之前配置的邮件告警系统生效了，这里发现死链直接抛出异常，
        // 底层的 SchedulingRunnable 捕获到异常后，会自动给你发一封邮件，告诉你谁的博客挂了！
        if (deadCount > 0) {
            throw new RuntimeException("检测到 " + deadCount + " 个失效友链，请及时清理或联系站长！\n失效名单：\n" + deadLinksInfo.toString());
        }
    }

    // 4. 发送 HTTP 请求检测
    private boolean checkUrl(String urlString, int timeout) {
        if (!StringUtils.hasText(urlString)) {
            return false;
        }

        // 容错处理：修复部分友链在数据库里忘记写 http:// 的问题
        if (!urlString.startsWith("http")) {
            urlString = "http://" + urlString;
        }

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 设置请求方式为 HEAD，只获取响应头，不下载网页内容，速度极快
            connection.setRequestMethod("HEAD");

            // 【核心优化】伪装成普通浏览器，防止被对方防爬虫防火墙拦截返回 403
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Connection", "keep-alive");

            // 应用动态传入的超时时间
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            int responseCode = connection.getResponseCode();
            // 只要返回 200 到 399 之间的状态码，或者 401(需要密码)/403(被完全锁死的严格防火墙但服务器活着)，都算作网站正常存活
            return (responseCode >= 200 && responseCode < 400) || responseCode == 401 || responseCode == 403;
        } catch (Exception e) {
            // 捕获异常（如域名解析失败、连接超时等）说明网站确实打不开
            return false;
        }
    }
}