package com.myo.blog.admin;

import com.myo.blog.common.aop.LogAnnotation;
import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/article")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;

    /**
     * 文章列表
     */
    @PostMapping("/list")
    public Result listArticle(@RequestBody PageParams pageParams) {
        return articleService.listArticleForAdmin(pageParams);
    }


    /**
     * 删除文章
     */
    @PostMapping("/delete/{id}")
    @RequirePermission("article:delete")
    @LogAnnotation(module = "文章管理", operator = "刪除文章")
    public Result deleteArticle(@PathVariable("id") String id) {
        return articleService.deleteArticle(id);
    }
}
