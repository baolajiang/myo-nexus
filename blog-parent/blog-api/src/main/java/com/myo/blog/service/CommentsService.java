package com.myo.blog.service;

import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.CommentParam;
import com.myo.blog.entity.params.PageParams;

public interface CommentsService {
    /**
     * 根据文章id 查询所有的评论列表
     * @param id
     * @return
     */
    Result commentsByArticleId(String id);
     /**
     * 新增评论
     * @param commentParam
     * @return
     */
    Result comment(CommentParam commentParam);

    /**
     * 查询文章的评论总数
     * @param id 文章id
     * @return
     */
    Result queryCommentCount(String id);

     /**
     * 查询评论列表
     * @param pageParams
     * @return
     */
    Result listComment(PageParams pageParams);
     /**
     * 删除评论
     * @param id
     * @return
     */
    Result deleteComment(String id);
     /**
     * 改变评论状态
     * @param id 评论id
     * @param status 状态值
     * @return
     */
    Result changeCommentStatus(String id, Integer status);
}
