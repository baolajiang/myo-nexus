package com.myo.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.myo.blog.config.RabbitConfig;
import com.myo.blog.dao.mapper.ArticleMapper;
import com.myo.blog.dao.mapper.CommentMapper;
import com.myo.blog.dao.pojo.Comment;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.dao.pojo.Tag;
import com.myo.blog.service.CommentsService;
import com.myo.blog.service.SysUserService;
import com.myo.blog.service.ThreadService;
import com.myo.blog.utils.UserThreadLocal;
import com.myo.blog.entity.CommentVo;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.UserVo;
import com.myo.blog.entity.params.CommentParam;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentsServiceImpl implements CommentsService {

    private final CommentMapper commentMapper;

    private final SysUserService sysUserService;


    private final RabbitTemplate rabbitTemplate;

    private final ArticleMapper articleMapper;

    private final ThreadService threadService;

    @Override
    public Result commentsByArticleId(String id) {
        // Redis
        String cacheKey = "comment::article::" + id;


        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getArticleId,id);
        queryWrapper.eq(Comment::getLevel,1);
        queryWrapper.orderByDesc(Comment::getCreateDate);
        List<Comment> comments = commentMapper.selectList(queryWrapper);

        List<CommentVo> commentVoList = copyList(comments);

        return Result.success(commentVoList);
    }

    @Override
    public Result comment(CommentParam commentParam) {
        SysUser sysUser = UserThreadLocal.get();
        // 添加非空和长度校验
        if (commentParam.getContent() == null || commentParam.getContent().trim().isEmpty()) {
            return Result.fail(400, "评论内容不能为空");
        }
        if (commentParam.getContent().trim().length() > 500) { // 限制最大长度
            return Result.fail(400, "评论内容不能超过500字");
        }
        Comment comment = new Comment();
        comment.setArticleId(commentParam.getArticleId());
        comment.setAuthorId(sysUser.getId());
        comment.setContent(commentParam.getContent());
        comment.setCreateDate(System.currentTimeMillis());
        String parent = commentParam.getParent();
        if (parent == null || parent.equals("0")) {
            comment.setLevel(1);
        }else{
            comment.setLevel(2);
        }

        comment.setParentId(parent == null ? "0" : parent);

        String toUserId = commentParam.getToUserId();

        comment.setToUid(toUserId == null ? "0" : toUserId);
        // 插入评论
        this.commentMapper.insert(comment);
        // 异步更新文章的评论数量
        threadService.updateArticleCommentCount(articleMapper, comment.getArticleId());
        return Result.success(null);
    }


    /**
     * 查询文章的评论数量
     * @param id 文章ID
     * @return 评论数量
     */
    public Result queryCommentCount(int id) {
        Integer count = commentMapper.queryCommentCount(id);
        return Result.success(count);
    }

    private List<CommentVo> copyList(List<Comment> comments) {
        List<CommentVo> commentVoList = new ArrayList<>();
        for (Comment comment : comments) {
            commentVoList.add(copy(comment));
        }
        return commentVoList;
    }

    private CommentVo copy(Comment comment) {
        CommentVo commentVo = new CommentVo();
        BeanUtils.copyProperties(comment,commentVo);
        commentVo.setId(String.valueOf(comment.getId()));
        //作者信息
        String authorId = comment.getAuthorId();
        UserVo userVo = this.sysUserService.findUserVoById(authorId);
        commentVo.setAuthor(userVo);
        //子评论
        Integer level = comment.getLevel();
        if (1 == level){
            String id = comment.getId();
            List<CommentVo> commentVoList = findCommentsByParentId(id);
            commentVo.setChildrens(commentVoList);
        }
        //to User 给谁评论
        if (level > 1){
            String toUid = comment.getToUid();

            UserVo toUserVo = this.sysUserService.findUserVoById(toUid);
            commentVo.setToUser(toUserVo);
        }
        //评论or回复 时间
        commentVo.setCreateDate(new DateTime(comment.getCreateDate()).toString("yyyy-MM-dd HH:mm"));
        return commentVo;
    }

    private List<CommentVo> findCommentsByParentId(String id) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getParentId,id);
        queryWrapper.eq(Comment::getLevel,2);
        return copyList(commentMapper.selectList(queryWrapper));
    }
}