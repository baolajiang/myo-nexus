package com.myo.blog.controller;

import com.myo.blog.common.aop.RateLimit;
import com.myo.blog.common.cache.Cache;
import com.myo.blog.service.CommentsService;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.CommentParam;
import com.myo.blog.utils.IpUtils;
import com.myo.blog.utils.HttpContextUtils;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("comments")
@RequiredArgsConstructor
public class CommentsController {
    private final CommentsService commentsService;

    // 查询文章下的评论列表
    @GetMapping("article/{id}")
    public Result comments(@PathVariable("id") String id){
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IpUtils.getIpAddr(request);
        log.info("查询文章评论 - IP: {}, 文章ID: {}", ip, id);

        try {
            Result result = commentsService.commentsByArticleId(id);
            log.debug("文章评论查询成功 - IP: {}, 文章ID: {}", ip, id);
            return result;
        } catch (Exception e) {
            log.error("文章评论查询异常 - IP: {}, 文章ID: {}, 异常信息: {}", ip, id, e.getMessage(), e);
            return Result.fail(500, "系统异常");
        }
    }

    // 防止刷评论，10秒内只能发1条
    @RateLimit(time = 10, count = 1, msg = "评论发布太快了，请休息一下")
    @PostMapping("create/change")
    public Result comment(@RequestBody CommentParam commentParam){
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IpUtils.getIpAddr(request);
        String articleId = commentParam.getArticleId();
        log.info("发布评论 - IP: {}, 文章ID: {}", ip, articleId);

        try {
            Result result = commentsService.comment(commentParam);
            if (result.isSuccess()) {
                log.info("评论发布成功 - IP: {}, 文章ID: {}", ip, articleId);
            } else {
                log.warn("评论发布失败 - IP: {}, 文章ID: {}, 错误码: {}", ip, articleId, result.getCode());
            }
            return result;
        } catch (Exception e) {
            log.error("评论发布异常 - IP: {}, 文章ID: {}, 异常信息: {}", ip, articleId, e.getMessage(), e);
            return Result.fail(500, "系统异常");
        }
    }

    @GetMapping("queryCommentCount/{id}")
    public Result show(@PathVariable("id") String id){
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip = IpUtils.getIpAddr(request);
        log.debug("查询评论数量 - IP: {}, 文章ID: {}", ip, id);

        try {
            Result result = commentsService.queryCommentCount(id);
            log.debug("评论数量查询成功 - IP: {}, 文章ID: {}", ip, id);
            return result;
        } catch (Exception e) {
            log.error("评论数量查询异常 - IP: {}, 文章ID: {}, 异常信息: {}", ip, id, e.getMessage(), e);
            return Result.fail(500, "系统异常");
        }
    }

    @GetMapping("test22")
    public static String browserName(HttpServletRequest request){
        String userAgent = request.getHeader("User-Agent");
        UserAgent ua = UserAgent.parseUserAgentString(userAgent);
        Browser browser = ua.getBrowser();
        return browser.getName() + "-" + browser.getVersion(userAgent);
    }
}