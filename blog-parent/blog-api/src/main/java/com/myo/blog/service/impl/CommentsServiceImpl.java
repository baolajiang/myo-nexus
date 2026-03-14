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
import com.myo.blog.entity.params.PageParams;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentsServiceImpl implements CommentsService {

    private final CommentMapper commentMapper;

    private final SysUserService sysUserService;


    private final RabbitTemplate rabbitTemplate;

    private final ArticleMapper articleMapper;

    private final ThreadService threadService;
    /**
     * 根据文章id 查询所有的评论列表
     * @param id 文章id
     * @return  评论列表
     */
    @Override
    public Result commentsByArticleId(String id) {
        // Redis
        String cacheKey = "comment::article::" + id;


        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getArticleId,id);
        queryWrapper.eq(Comment::getLevel,1);
        queryWrapper.eq(Comment::getStatus, 1);
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
        comment.setStatus(1);
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
    @Override
    public Result queryCommentCount(String id) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getArticleId, id);
        queryWrapper.eq(Comment::getStatus, 1);
        // 直接执行 select count(*) from myo_comment where article_id = ? and status = 1  // 统计正常状态的评论数量
        Long count = commentMapper.selectCount(queryWrapper);
        return Result.success(count);
    }
    /**
     * 查询评论列表
     * @param pageParams 分页参数
     * @return 评论列表
     */
    @Override
    public Result listComment(PageParams pageParams) {
        // 1. 分页参数
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Comment> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParams.getPage(), pageParams.getPageSize());
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        // 对 status 的条件判断
        if (pageParams.getStatus() != null) {
            queryWrapper.eq(Comment::getStatus, pageParams.getStatus());
        }
        // 2. 关键词模糊搜索
        if (StringUtils.isNotBlank(pageParams.getKeyword())) {
            queryWrapper.like(Comment::getContent, pageParams.getKeyword());
        }

        // 3. 按时间倒序
        queryWrapper.orderByDesc(Comment::getCreateDate);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Comment> commentPage =
                commentMapper.selectPage(page, queryWrapper);

        // 4. 转换为后台专用的 VO
        List<CommentVo> commentVoList = new ArrayList<>();
        for (Comment comment : commentPage.getRecords()) {
            commentVoList.add(copyAdmin(comment));
        }

        // 5. 组装前端需要的 list 和 total
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("list", commentVoList);
        map.put("total", commentPage.getTotal());

        return Result.success(map);
    }

    @Override
    @Transactional
    public Result deleteComment(String id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            return Result.fail(400, "评论不存在");
        }
        String articleId = comment.getArticleId();

        // 1. 删除当前评论
        commentMapper.deleteById(id);

        // 2. 删除它底下的所有子评论 (通过 parentId)
        LambdaQueryWrapper<Comment> childQuery = new LambdaQueryWrapper<>();
        childQuery.eq(Comment::getParentId, id);
        commentMapper.delete(childQuery);

        // 3. 异步更新文章的评论总数
        threadService.updateArticleCommentCount(articleMapper, articleId);

        return Result.success("删除成功");
    }

        /**
         * 改变评论状态
         * @param id 评论id
         * @param status 状态值
         * @return
         */
    @Override
    @Transactional
    public Result changeCommentStatus(String id, Integer status) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            return Result.fail(400, "评论不存在");
        }
        comment.setStatus(status);
        commentMapper.updateById(comment);
        return Result.success("状态更新成功");
    }



    // 这是专门给后台用的转换方法，只查作者、被回复人、文章标题，不查树状子节点
    // 前端显示 :评论者 评论内容 所属文章 发布时间
    private CommentVo copyAdmin(Comment comment) {
        CommentVo commentVo = new CommentVo();
        BeanUtils.copyProperties(comment, commentVo);
        commentVo.setId(String.valueOf(comment.getId()));

        // 查询评论作者信息
        UserVo userVo = this.sysUserService.findUserVoById(comment.getAuthorId());
        commentVo.setAuthor(userVo);

        // 如果是回复，查询被回复人信息
        if (comment.getLevel() > 1) {
            UserVo toUserVo = this.sysUserService.findUserVoById(comment.getToUid());
            commentVo.setToUser(toUserVo);
        }

        // 格式化时间
        commentVo.setCreateDate(new DateTime(comment.getCreateDate()).toString("yyyy-MM-dd HH:mm"));

        // 查询所属文章标题
        com.myo.blog.dao.pojo.Article article = articleMapper.selectById(comment.getArticleId());
        if (article != null) {
            commentVo.setArticleTitle(article.getTitle());
        }

        return commentVo;
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
        queryWrapper.eq(Comment::getStatus, 1);
        return copyList(commentMapper.selectList(queryWrapper));
    }
}