package com.myo.blog.task.jobs;

import com.myo.blog.dao.mapper.ArticleMapper;
import com.myo.blog.dao.pojo.Article;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 文章浏览量同步任务，用于定时同步 Redis 中的浏览量到 MySQL 数据库
@Slf4j
@Component("viewCountSyncTask")
@RequiredArgsConstructor
public class ViewCountSyncTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final ArticleMapper articleMapper;

    private static final String VIEW_COUNT_KEY = "blog:article:viewCount";

    // 1. 无参方法：供定时任务默认调度使用
    public void run() {
        // 默认执行全量同步
        executeSync(true, null);
    }

    // 2. 有参方法：供前端“执行一次”时动态传参使用
    public void run(String param) {
        log.info("[浏览量同步任务] 手动触发，接收到动态参数：{}", param);
        //校验参数格式是否正确
        if (org.springframework.util.StringUtils.hasText(param)) {
            String trimmedParam = param.trim();
            if (!trimmedParam.startsWith("{") || !trimmedParam.endsWith("}")) {
                throw new IllegalArgumentException("参数格式错误：必须是标准 JSON 格式！");
            }
        }
        boolean forceSync = true;
        String targetArticleId = null;

        if (StringUtils.hasText(param)) {
            // 如果参数里明确写了 forceSync: false，则关闭强制全量同步
            if (param.contains("\"forceSync\":false") || param.contains("\"forceSync\": false")) {
                forceSync = false;
            }
            Matcher matcher = Pattern.compile("\"articleId\"\\s*:\\s*\"([^\"]+)\"").matcher(param);
            if (matcher.find()) {
                targetArticleId = matcher.group(1);
            }

        }

        executeSync(forceSync, targetArticleId);
    }

    // 3. 核心同步逻辑
    private void executeSync(boolean forceSync, String targetArticleId) {
        log.info("[浏览量同步任务] 开始执行... 强制全量: {}, 指定文章ID: {}", forceSync, targetArticleId != null ? targetArticleId : "无");

        Map<Object, Object> viewCountMap = stringRedisTemplate.opsForHash().entries(VIEW_COUNT_KEY);

        if (viewCountMap.isEmpty()) {
            log.info("[浏览量同步任务] Redis中暂无数据，无需同步。");
            return;
        }

        int successCount = 0;
        int errorCount = 0;
        StringBuilder errorInfo = new StringBuilder();

        for (Map.Entry<Object, Object> entry : viewCountMap.entrySet()) {
            String articleId = entry.getKey().toString();

            // 如果指定了单篇文章，则跳过其他文章
            if (StringUtils.hasText(targetArticleId) && !targetArticleId.equals(articleId)) {
                continue;
            }

            try {
                Integer viewCount = Integer.valueOf(entry.getValue().toString());

                // 校验浏览量是否有效，避免无意义的数据库操作
                if (viewCount == null || viewCount <= 0) {
                    continue;
                }

                Article article = new Article();
                article.setId(articleId);
                article.setViewCounts(viewCount);

                int updated = articleMapper.updateById(article);
                if (updated > 0) {
                    successCount++;
                    // [同步到数据库成功后，立刻清理掉 Redis 里的这条记录！
                    // 这样下次只要没人访问这篇文章，就不会产生多余的同步操作
                    stringRedisTemplate.opsForHash().delete(VIEW_COUNT_KEY, articleId);
                } else {
                    // 如果 updated 为 0，说明数据库里没有这篇文章（可能被删除了），顺手清理 Redis 里的脏数据
                    log.warn("[浏览量同步任务] 数据库中未找到文章，准备清理 Redis 中的脏数据: {}", articleId);
                    stringRedisTemplate.opsForHash().delete(VIEW_COUNT_KEY, articleId);
                }

            } catch (Exception e) {
                errorCount++;
                errorInfo.append("ID: ").append(articleId).append("，原因: ").append(e.getMessage()).append("\n");
                log.error("[浏览量同步任务] 同步单篇文章失败, ArticleID: {}", articleId, e);
            }
        }

        log.info("[浏览量同步任务] 执行完毕。成功同步 {} 篇，清理/失败 {} 篇。", successCount, errorCount);

        //触发邮件告警联动
        if (errorCount > 0) {
            throw new RuntimeException("有 " + errorCount + " 篇文章的浏览量同步异常！\n异常详情：\n" + errorInfo.toString());
        }
    }
}