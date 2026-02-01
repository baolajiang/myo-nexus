package com.myo.blog.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.myo.blog.dao.mapper.ArticleMapper;
import com.myo.blog.dao.pojo.Article;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
/**
 * 线程池 异步执行 更新文章的阅读量
 */
@Component
public class ThreadService {
    /**
     * 异步更新文章的评论数量
     * @param articleMapper 文章映射器
     * @param articleId 文章ID
     */
    @Async("taskExecutor")
    public void updateArticleCommentCount(ArticleMapper articleMapper, String articleId) {
        // 直接让数据库执行 count = count + 1，防止并发覆盖，也无需关心旧值
        LambdaUpdateWrapper<Article> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Article::getId, articleId);
        updateWrapper.setSql("comment_counts = IFNULL(comment_counts, 0) + 1");

        articleMapper.update(null, updateWrapper);
    }
    /**
     * 异步更新文章的阅读量
     * @param articleMapper 文章映射器
     * @param articleId 文章ID
     */
    @Async("taskExecutor")
    public void updateArticleViewCount(ArticleMapper articleMapper, String articleId) {
        // 直接让数据库 view_counts + 1
        LambdaUpdateWrapper<Article> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Article::getId, articleId);
        updateWrapper.setSql("view_counts = IFNULL(view_counts, 0) + 1");

        articleMapper.update(null, updateWrapper);
    }

}
