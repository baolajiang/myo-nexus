package com.myo.blog.service;

import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.ArticleParam;
import com.myo.blog.entity.params.PageParams;

public interface ArticleService {
    /**
     * 分页查询 文章列表
     * @param pageParams
     * @return
     */
    Result listArticle(PageParams pageParams,String token);
    /**
     * 根据作者ID查询文章列表
     * @param pageParams 分页参数
     * @param authorId 作者ID
     * @return
     */
    Result listArticlesByAuthor(PageParams pageParams, String authorId);
    /**
     * 查询一共多少数据
     */
    Result listArticleCount(String token);
    /*
    * 查询mac
    * */
    Result queryMAC();
    /**
     * 最热文章
     * @param limit
     * @return
     */
    Result hotArticle(int limit);

    /**
     * 最新文章
     * @param limit
     * @return
     */
    Result newArticles(int limit);

    /**
     * 文章列表
     * @return
     */
    Result listarticles();

    /**
     * 查看文章详情
     * @param articleId
     * @return
     */
    Result findArticleById(String articleId,String token);

    /**
     * 文章发布服务
     * @param articleParam
     * @return
     */
    Result publish(ArticleParam articleParam);

    /**
     * 更新文章
     * @param articleParam
     * @return
     */
    Result updateArticle(ArticleParam articleParam);


    /**
     * 后台管理：获取文章列表
     * @param pageParams 分页参数（可能包含 query 搜索关键词）
     * @return Result
     */
    Result listArticleForAdmin(PageParams pageParams);

     /**
     * 删除文章
     * @param articleId
     * @return
     */
    Result deleteArticle(String articleId);
}
