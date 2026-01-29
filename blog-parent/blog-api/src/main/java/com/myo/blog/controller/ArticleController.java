package com.myo.blog.controller;

import com.myo.blog.common.aop.LogAnnotation;
import com.myo.blog.common.aop.RateLimit;
import com.myo.blog.common.cache.Cache;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.service.ArticleService;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.ArticleParam;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.utils.UserThreadLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

//json数据进行交互
@RestController
@RequestMapping("articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;
    /**
     * 首页 文章列表
     * @param pageParams
     * @return
     */
    @PostMapping
    //加上此注解 代表要对此接口记录日志
    @LogAnnotation(module="文章",operator="获取文章列表")
    @Cache(name = "listArticle" ,expire =  5 * 60 * 1000)
    public Result listArticle(@RequestBody PageParams pageParams,@RequestHeader("Authorization") String token){
    //获取到Authorization中的token，若token为null则表示没有登录

        return articleService.listArticle(pageParams,token);
    }

    @PostMapping("space/{id}")
    public Result SpaceArticles(@RequestBody PageParams pageParams,@PathVariable("id") String id) {
        System.out.println("看看id："+id);
        return articleService.listArticlesByAuthor(pageParams,id);
    }

    //查询mac
    @PostMapping ("queryMAC")
    public Result queryMAC(){
        return articleService.queryMAC();
    }

    /**
     * 首页 最热文章
     * @return
     */
    @PostMapping("hot")
    @Cache(expire = 5 * 60 * 1000,name = "hot_article")
    public Result hotArticle(){
        int limit = 5;
        return articleService.hotArticle(limit);
    }

    /**
     * 首页 最新文章
     * @return
     */
    @PostMapping("new")
    @Cache(expire = 5 * 60 * 1000,name = "news_article")
    public Result newArticles(){
        int limit = 5;
        return articleService.newArticles(limit);
    }

    /**
     * 首页 最新文章
     * @return
     */
    @PostMapping("listarticles")
    public Result listarticles(){
        return articleService.listarticles();
    }


    @PostMapping("view/{id}")
    public Result findArticleById(@PathVariable("id") String articleId, @RequestHeader("Authorization") String token){
        // 将 token 传递给 service 层，用于权限验证
        return articleService.findArticleById(articleId,token);
    }



    //接口url：/articles/publish
    //

    /**
     * 文章发布服务
     * @param articleParam
     * @return
     */
    // 防止重复提交或恶意刷文章

    @RateLimit(time = 10, count = 1, msg = "请勿重复提交")
    //日记记录
    @LogAnnotation(module="文章",operator="发布文章")
    //请求方式：POST
    @PostMapping("publish")
    public Result publish(@RequestBody ArticleParam articleParam){

        return articleService.publish(articleParam);
    }
}

