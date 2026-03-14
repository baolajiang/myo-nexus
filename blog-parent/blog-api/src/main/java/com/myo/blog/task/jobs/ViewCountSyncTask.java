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

/**
 * 文章浏览量同步任务
 *
 * 文章被访问时，浏览量先写入 Redis Hash（高性能），
 * 由本任务定时将 Redis 中的浏览量批量同步到 MySQL，减少数据库写压力。
 *
 * Redis 数据结构：Hash
 *   key：blog:article:viewCount
 *   field：文章 ID
 *   value：当前浏览量
 *
 * 同步成功后立即删除 Redis 中对应的记录，
 * 下次有人访问该文章时才会重新写入，避免无意义的重复同步。
 */
@Slf4j
@Component("viewCountSyncTask")
@RequiredArgsConstructor
public class ViewCountSyncTask {

    // 引用 Redis 客户端，用于读取和清理 Hash 中的浏览量数据
    private final StringRedisTemplate stringRedisTemplate;

    // 引用文章 Mapper，用于将浏览量更新到 MySQL 的 article 表
    private final ArticleMapper articleMapper;

    // Redis Hash 的 key，所有文章的浏览量都存在这个 Hash 下
    private static final String VIEW_COUNT_KEY = "blog:article:viewCount";

    /**
     * 无参方法：供定时任务自动调度使用
     * 默认执行全量同步，同步 Redis 中所有文章的浏览量到数据库
     */
    public void run() {
        executeSync(true, null);
    }

    /**
     * 有参方法：数据库中配置了 taskParam 时调用此方法（自动调度和手动执行都可能走这里）
     *
     * 支持两个 JSON 参数：
     * - forceSync：是否强制全量同步，默认 true；传 false 时只同步指定文章
     * - articleId：指定同步某一篇文章的 ID，不传则同步全部
     *
     * 示例参数：{"forceSync": false, "articleId": "123"}
     *
     * @param param JSON 格式的参数字符串
     */
    public void run(String param) {
        // 注意：此方法不仅在手动触发时调用，数据库中配置了 taskParam 的定时任务也会走这里
        log.info("[浏览量同步任务] 接收到动态参数：{}", param);

        // 参数格式校验：必须是标准 JSON 格式（以 { 开头、} 结尾）
        if (org.springframework.util.StringUtils.hasText(param)) {
            String trimmedParam = param.trim();
            if (!trimmedParam.startsWith("{") || !trimmedParam.endsWith("}")) {
                throw new IllegalArgumentException("参数格式错误：必须是标准 JSON 格式！");
            }
        }

        boolean forceSync = true;       // 默认全量同步
        String targetArticleId = null;  // 默认不限定文章

        if (StringUtils.hasText(param)) {
            // 用字符串匹配检测是否关闭了全量同步（兼容有无空格的写法）
            if (param.contains("\"forceSync\":false") || param.contains("\"forceSync\": false")) {
                forceSync = false;
            }
            // 用正则提取 articleId 的值，支持 "articleId": "123" 格式
            Matcher matcher = Pattern.compile("\"articleId\"\\s*:\\s*\"([^\"]+)\"").matcher(param);
            if (matcher.find()) {
                targetArticleId = matcher.group(1);
            }
        }

        executeSync(forceSync, targetArticleId);
    }

    /**
     * 核心同步逻辑：将 Redis 中的浏览量批量写入 MySQL

     * 逐条处理 Redis Hash 中的浏览量数据：
     * - 同步成功：更新数据库浏览量，并删除 Redis 中对应记录（避免下次重复同步）
     * - 文章不存在：数据库更新行数为 0，说明文章已被删除，清理 Redis 脏数据
     * - 同步异常：记录错误信息，继续处理其他文章，不因单篇失败而中断整批

     * 全部处理完毕后，如果有文章同步失败，抛出异常触发 SchedulingRunnable 的告警机制，
     * 发送邮件通知管理员。
     *
     * @param forceSync       是否全量同步（目前逻辑中此参数保留，实际以 targetArticleId 为准）
     * @param targetArticleId 指定同步的文章 ID，为 null 时同步全部
     */
    private void executeSync(boolean forceSync, String targetArticleId) {
        log.info("[浏览量同步任务] 开始执行... 强制全量: {}, 指定文章ID: {}",
                forceSync, targetArticleId != null ? targetArticleId : "无");

        // 一次性读取 Redis Hash 中所有浏览量数据（articleId -> viewCount）
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

            // 指定了单篇文章时，跳过其他文章，只处理目标文章
            if (StringUtils.hasText(targetArticleId) && !targetArticleId.equals(articleId)) {
                continue;
            }

            try {
                Integer viewCount = Integer.valueOf(entry.getValue().toString());

                // 浏览量为 null 或 <= 0 时跳过，避免无意义的数据库写操作
                if (viewCount == null || viewCount <= 0) {
                    continue;
                }

                // 构建更新对象，只更新 viewCounts 字段，不影响其他字段
                Article article = new Article();
                article.setId(articleId);
                article.setViewCounts(viewCount);

                int updated = articleMapper.updateById(article);
                if (updated > 0) {
                    // 同步成功：删除 Redis 中这条记录
                    // 下次有人访问该文章才会重新写入，避免每次任务都无意义地同步
                    successCount++;
                    stringRedisTemplate.opsForHash().delete(VIEW_COUNT_KEY, articleId);
                } else {
                    // updated = 0 说明数据库中找不到这篇文章（可能已被删除）
                    // 顺手清理 Redis 中的脏数据，防止这条记录一直残留
                    log.warn("[浏览量同步任务] 数据库中未找到文章，准备清理 Redis 中的脏数据: {}", articleId);
                    stringRedisTemplate.opsForHash().delete(VIEW_COUNT_KEY, articleId);
                }

            } catch (Exception e) {
                // 单篇失败不中断整批，记录错误信息后继续处理下一篇
                errorCount++;
                errorInfo.append("ID: ").append(articleId).append("，原因: ").append(e.getMessage()).append("\n");
                log.error("[浏览量同步任务] 同步单篇文章失败, ArticleID: {}", articleId, e);
            }
        }

        log.info("[浏览量同步任务] 执行完毕。成功同步 {} 篇，清理/失败 {} 篇。", successCount, errorCount);

        // 有文章同步失败时，抛出异常触发 SchedulingRunnable 的告警机制，发送邮件通知管理员
        if (errorCount > 0) {
            throw new RuntimeException("有 " + errorCount + " 篇文章的浏览量同步异常！\n异常详情：\n" + errorInfo);
        }
    }
}