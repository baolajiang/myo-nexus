package com.myo.blog.config;

import com.myo.blog.entity.Result;
import com.myo.blog.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class ArticleAiTools {

    // 注入 ArticleService
     private final ArticleService articleService;
    // ... 构造方法
    public ArticleAiTools(ArticleService articleService) {
        this.articleService = articleService;
    }


    @Tool(description = "根据文章ID(articleId)查询文章详细内容")
    public String queryArticleById(Long articleId) {
        log.info("根据文章ID查询文章详细内容，文章ID：{}", articleId);
        // 调用 articleService 查询文章
        Result result = articleService.findArticleById(articleId.toString(), "");
        if (result.getCode() != 200) return "查询文章失败：" + result.getMsg();


        // 1. 正确拆包：getData() 里面装的是 ArticleVo 对象
        com.myo.blog.entity.ArticleVo article = (com.myo.blog.entity.ArticleVo) result.getData();

        if (article == null) return "未找到该文章的详细内容。";

        // 2. 提取属性并拼装成大模型能看懂的纯文本
        StringBuilder aiReport = new StringBuilder("文章查询成功，信息如下：\n");
        aiReport.append("文章ID：").append(article.getId()).append("\n");
        // 注意：下面这些 get 方法请根据你 ArticleVo 真实的属性名进行调整
        // 如果 ArticleVo 没有 getTitle()，可能是叫 getArticleName()，请自行替换
        aiReport.append("标题：").append(article.getTitle() != null ? article.getTitle() : "无标题").append("\n");
        aiReport.append("作者：").append(article.getAuthor() != null ? article.getAuthor() : "未知作者").append("\n");
        aiReport.append("简介：").append(article.getSummary() != null ? article.getSummary() : "无简介").append("\n");

        // 如果需要让大模型看到文章的具体内容，把下面这行加上，并且注意限制内容长度防止撑爆
         if (article.getBody() != null && article.getBody().getContent() != null) {
             String content = article.getBody().getContent();
             aiReport.append("正文内容：\n").append(content.length() > 500 ? content.substring(0, 500) + "..." : content);
         }

        return aiReport.toString();
    }

    // 还可以加删除文章、发布文章的方法...
}