package com.myo.blog.task.jobs;

import com.myo.blog.dao.mapper.ArticleMapper;
import com.myo.blog.dao.pojo.Article;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
// 文章浏览量同步任务，用于定时同步 Redis 中的浏览量到 MySQL 数据库
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncTask {

    private final StringRedisTemplate stringRedisTemplate;

    private final ArticleMapper articleMapper;

    private static final String VIEW_COUNT_KEY = "blog:article:viewCount";

    public void run() {
        log.info("[浏览量同步任务] 开始执行...");

        // 1. 把 Redis 里所有的文章浏览量一次性全拉出来
        Map<Object, Object> viewCountMap = stringRedisTemplate.opsForHash().entries(VIEW_COUNT_KEY);

        if (viewCountMap.isEmpty()) {
            log.info("[浏览量同步任务] Redis中暂无数据，无需同步。");
            return;
        }

        int successCount = 0;

        // 2. 遍历这些数据，挨个更新到 MySQL
        for (Map.Entry<Object, Object> entry : viewCountMap.entrySet()) {
            try {
                String articleId = entry.getKey().toString();
                Integer viewCount = Integer.valueOf(entry.getValue().toString());

                // 构造一个只包含 ID 和 最新浏览量 的实体去更新，效率最高
                Article article = new Article();
                article.setId(articleId);
                article.setViewCounts(viewCount);

                int updated = articleMapper.updateById(article);
                if (updated > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("[浏览量同步任务] 同步单篇文章失败, ArticleID: {}", entry.getKey(), e);
            }
        }

        log.info("[浏览量同步任务] 执行完毕。共成功同步 {} 篇文章。", successCount);
    }
}