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

/**
 * 友情链接存活检测任务
 *
 * 定时对数据库中所有友情链接发送 HTTP HEAD 请求，
 * 检测对方网站是否仍然可以正常访问。
 * 发现失效链接后抛出异常，触发 SchedulingRunnable 的告警机制，
 * 自动发送邮件通知管理员及时处理。
 */
@Slf4j
@Component("linkCheckTask")
@RequiredArgsConstructor
public class LinkCheckTask {

    // 引用友链 Mapper，用于查询数据库中所有友情链接
    private final LinkMapper linkMapper;

    /**
     * 无参方法：供定时任务自动调度使用
     * 默认 HTTP 连接超时时间为 5000 毫秒
     */
    public void run() {
        executeCheck(5000);
    }

    /**
     * 有参方法：数据库中配置了 taskParam 时调用此方法（自动调度和手动执行都可能走这里）
     *
     * 支持一个 JSON 参数：
     * - timeout：HTTP 连接超时时间（毫秒），默认 5000
     *
     * 示例参数：{"timeout": 3000}
     *
     * @param param JSON 格式的参数字符串
     */
    public void run(String param) {
        log.info("[友链检测任务] 接收到动态参数：{}", param);
        int timeout = 5000; // 默认超时 5000 毫秒

        if (org.springframework.util.StringUtils.hasText(param)) {
            // 格式校验：参数必须包含 {，否则不是 JSON 格式
            if (!param.contains("{")) {
                throw new IllegalArgumentException("参数必须是 JSON 格式");
            }
            // 用正则从 JSON 中提取 timeout 的值，支持 "timeout": 3000 格式
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"timeout\"\\s*:\\s*(\\d+)").matcher(param);
            if (matcher.find()) {
                timeout = Integer.parseInt(matcher.group(1));
            }
        }
        executeCheck(timeout);
    }

    /**
     * 核心检测逻辑：逐一检测所有友链是否存活
     *
     * 检测完毕后，如果发现失效链接，抛出异常触发 SchedulingRunnable 的告警机制，
     * 自动发送邮件通知管理员，邮件中包含所有失效链接的名称和 URL。
     *
     * @param timeout HTTP 连接和读取的超时时间（毫秒）
     */
    private void executeCheck(int timeout) {
        log.info("[友链检测任务] 开始执行... 当前设定的检测超时时间为: {} ms", timeout);

        // 查询数据库中所有友情链接
        List<Link> linkList = linkMapper.selectList(new LambdaQueryWrapper<>());
        if (linkList.isEmpty()) {
            log.info("[友链检测任务] 暂无友情链接需要检测");
            return;
        }

        int deadCount = 0;
        StringBuilder deadLinksInfo = new StringBuilder();

        for (Link link : linkList) {
            boolean isAlive = checkUrl(link.getUrl(), timeout);
            if (!isAlive) {
                log.error("[友链检测任务] 发现失效友链: 网站名称={}, URL={}", link.getName(), link.getUrl());
                deadCount++;
                // 收集失效链接信息，用于邮件告警内容
                deadLinksInfo.append(link.getName()).append(" (").append(link.getUrl()).append(")\n");
            }
        }

        log.info("[友链检测任务] 执行完毕。共检测 {} 个链接，发现 {} 个失效链接。",
                linkList.size(), deadCount);

        // 有失效链接时抛出异常，SchedulingRunnable 捕获后会发送告警邮件通知管理员
        if (deadCount > 0) {
            throw new RuntimeException("检测到 " + deadCount + " 个失效友链，请及时清理或联系站长！\n失效名单：\n" + deadLinksInfo);
        }
    }

    /**
     * 向指定 URL 发送 HTTP HEAD 请求，判断网站是否存活
     *
     * 使用 HEAD 请求而非 GET，只获取响应头不下载页面内容，速度更快、流量更少。
     * 伪装成普通浏览器的 User-Agent，防止被对方的防爬虫机制拦截返回误判的 403。
     *
     * 存活判定规则：
     * - 200~399：正常响应，网站存活
     * - 401：需要密码验证，但服务器本身是活的
     * - 403：被防火墙拦截，但服务器本身是活的
     * - 其他状态码或连接异常：判定为失效
     *
     * @param urlString 要检测的 URL
     * @param timeout   连接和读取的超时时间（毫秒）
     * @return true = 网站存活，false = 网站失效
     */
    private boolean checkUrl(String urlString, int timeout) {
        if (!StringUtils.hasText(urlString)) {
            return false;
        }

        // 容错：数据库中部分友链可能忘记填写 http:// 前缀，自动补全
        if (!urlString.startsWith("http")) {
            urlString = "http://" + urlString;
        }

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 使用 HEAD 方法，只请求响应头，不下载页面正文，速度极快
            connection.setRequestMethod("HEAD");
            // 伪装成普通浏览器，避免被对方防爬虫规则误判为机器人而返回 403
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Connection", "keep-alive");
            // 应用动态传入的超时时间，防止单个慢速网站拖慢整批检测
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            int responseCode = connection.getResponseCode();
            return (responseCode >= 200 && responseCode < 400)
                    || responseCode == 401  // 需要认证，但服务器存活
                    || responseCode == 403; // 被防火墙拦截，但服务器存活
        } catch (Exception e) {
            // 域名解析失败、连接超时、网络不通等异常，均视为网站失效
            return false;
        }
    }
}