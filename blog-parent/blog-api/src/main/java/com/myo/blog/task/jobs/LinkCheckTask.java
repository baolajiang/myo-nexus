package com.myo.blog.task.jobs;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.LinkMapper;
import com.myo.blog.dao.pojo.Link;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
// 友链检测任务
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkCheckTask {


    private final LinkMapper linkMapper;

    public void run() {
        log.info("[友链检测任务] 开始执行...");

        // 1. 查询出所有的友情链接
        List<Link> linkList = linkMapper.selectList(new LambdaQueryWrapper<>());
        if (linkList.isEmpty()) {
            log.info("[友链检测任务] 暂无友情链接需要检测");
            return;
        }

        int deadCount = 0;

        // 2. 遍历检测每个链接是否存活
        for (Link link : linkList) {
            boolean isAlive = checkUrl(link.getUrl());
            if (!isAlive) {
                // 如果网站挂了，记录下它的名字和链接
                log.error("[友链检测任务] 发现失效友链: 网站名称={}, URL={}", link.getName(), link.getUrl());
                deadCount++;

                // 进阶玩法：如果你以后给 myo_link 表加了 status 字段，这里可以直接 update 数据库把这个友链隐藏掉
            }
        }

        log.info("[友链检测任务] 执行完毕。共检测 {} 个链接，发现 {} 个失效链接。", linkList.size(), deadCount);
    }

    // 使用原生的 HttpURLConnection 检测 URL 是否能正常访问
    private boolean checkUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 设置请求方式为 HEAD，只获取响应头，不下载网页内容，速度极快
            connection.setRequestMethod("HEAD");
            // 设置超时时间为 5 秒
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            // 只要返回 200 到 399 之间的状态码，都算作网站正常存活
            return (responseCode >= 200 && responseCode < 400);
        } catch (Exception e) {
            return false;
        }
    }
}