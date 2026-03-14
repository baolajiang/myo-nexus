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

/**
 * 收藏夹网站存活检测任务（智能升级版）
 *
 * 定时对数据库中所有收藏链接发送 HTTP HEAD 请求，并根据检测结果自动维护状态：
 * 1. 正常 → 失效：自动将 status 改为 0（前台隐藏），并发送告警邮件通知管理员
 * 2. 失效 → 正常：自动将 status 改为 1（重新展示），实现死链自动复活
 *
 * 链接状态约定：1 = 正常展示，0 = 已下架（失效）
 */
@Slf4j
@Component("linkCheckTask")
@RequiredArgsConstructor
public class LinkCheckTask {

    // 引用收藏链接 Mapper，用于查询所有链接和更新链接状态
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
        log.info("[收藏夹检测任务] 接收到动态参数：{}", param);
        int timeout = 5000;

        if (StringUtils.hasText(param)) {
            // 格式校验：参数必须包含 {，否则不是 JSON 格式
            if (!param.contains("{")) {
                throw new IllegalArgumentException("参数必须是 JSON 格式");
            }
            // 用正则从 JSON 中提取 timeout 的值
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"timeout\"\\s*:\\s*(\\d+)").matcher(param);
            if (matcher.find()) {
                timeout = Integer.parseInt(matcher.group(1));
            }
        }
        executeCheck(timeout);
    }

    /**
     * 核心检测逻辑：逐一检测所有链接，自动下架失效链接、自动恢复复活链接
     *
     * 只有发现「新失效」的链接时才抛出异常，触发 SchedulingRunnable 的告警机制，
     * 自动发送邮件通知管理员；链接自动恢复属于好消息，不需要告警。
     *
     * @param timeout HTTP 连接和读取的超时时间（毫秒）
     */
    private void executeCheck(int timeout) {
        log.info("[收藏夹检测任务] 开始执行... 当前设定的检测超时时间为: {} ms", timeout);

        // 查询所有收藏链接，包含正常（status=1）和已下架（status=0）的，全部重新检测
        List<Link> linkList = linkMapper.selectList(new LambdaQueryWrapper<>());
        if (linkList.isEmpty()) {
            log.info("[收藏夹检测任务] 暂无链接需要检测");
            return;
        }

        int newlyDeadCount = 0;              // 本次新发现的失效链接数量
        int recoveryCount = 0;               // 本次自动恢复的链接数量
        StringBuilder deadLinksInfo = new StringBuilder(); // 失效链接名单，用于告警邮件内容

        for (Link link : linkList) {
            boolean isAlive = checkUrl(link.getUrl(), timeout);
            // 防御性处理：status 为 null 时视为正常状态（1），避免空指针
            Integer currentStatus = link.getStatus() == null ? 1 : link.getStatus();

            if (!isAlive && currentStatus == 1) {
                // 状态变化：正常 → 失效（网站刚刚挂了）
                log.error("[收藏夹检测任务] 发现失效链接，已自动下架: 网站名称={}, URL={}",
                        link.getName(), link.getUrl());

                // 自动将数据库中该链接的状态改为 0（下架），前台不再展示
                Link updateLink = new Link();
                updateLink.setId(link.getId());
                updateLink.setStatus(0);
                linkMapper.updateById(updateLink);

                newlyDeadCount++;
                deadLinksInfo.append(link.getName()).append(" (").append(link.getUrl()).append(")\n");

            } else if (isAlive && currentStatus == 0) {
                // 状态变化：失效 → 正常（网站恢复访问）
                log.info("[收藏夹检测任务] 发现链接恢复访问，已自动重新上架: 网站名称={}, URL={}",
                        link.getName(), link.getUrl());

                // 自动将数据库中该链接的状态改回 1（上架），前台重新展示
                Link updateLink = new Link();
                updateLink.setId(link.getId());
                updateLink.setStatus(1);
                linkMapper.updateById(updateLink);

                recoveryCount++;
            }
            // 正常且状态为1，或失效且状态已经是0，均无需操作
        }

        log.info("[收藏夹检测任务] 执行完毕。共检测 {} 个链接，新发现 {} 个失效，{} 个恢复正常。",
                linkList.size(), newlyDeadCount, recoveryCount);

        // 只有发现新失效链接时才抛异常，触发告警邮件；自动恢复属于正常结果，不告警
        if (newlyDeadCount > 0) {
            throw new RuntimeException("检测到 " + newlyDeadCount + " 个收藏网站刚刚失效，系统已自动将其从前台隐藏！\n本次自动下架名单：\n" + deadLinksInfo);
        }
    }

    /**
     * 向指定 URL 发送 HTTP HEAD 请求，判断网站是否存活
     *
     * 使用 HEAD 请求只获取响应头，不下载页面内容，速度快、流量少。
     * 伪装成普通浏览器的 User-Agent，防止被对方防爬虫机制误拦截。
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
        // 容错：数据库中部分链接可能忘记填写 http:// 前缀，自动补全
        if (!urlString.startsWith("http")) {
            urlString = "http://" + urlString;
        }

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 使用 HEAD 方法，只请求响应头，不下载页面正文
            connection.setRequestMethod("HEAD");
            // 伪装成普通浏览器，避免被对方防爬虫规则误判为机器人
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
            // 域名解析失败、连接超时、网络不通等，均视为网站失效
            return false;
        }
    }
}