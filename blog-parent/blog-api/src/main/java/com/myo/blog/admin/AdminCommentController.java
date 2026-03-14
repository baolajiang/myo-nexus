package com.myo.blog.admin;

import com.myo.blog.common.aop.LogAnnotation;
import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.dao.pojo.Comment;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.service.CommentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/comment")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentsService commentsService;

    // 获取评论列表
    @PostMapping("list")
    @RequirePermission("comment:list")
    public Result listComment(@RequestBody PageParams pageParams) {
        return commentsService.listComment(pageParams);
    }

    // 彻底物理删除评论
    @PostMapping("delete/{id}")
    @RequirePermission("comment:delete")
    @LogAnnotation(module = "评论管理", operator = "彻底删除评论")
    public Result deleteComment(@PathVariable("id") String id) {
        return commentsService.deleteComment(id);
    }

    // 改变评论状态（通过/驳回/下架/恢复）
    @PostMapping("changeStatus")
    public Result changeStatus(@RequestBody Comment comment) {
        if (comment.getId() == null || comment.getStatus() == null) {
            return Result.fail(400, "参数缺失");
        }
        return commentsService.changeCommentStatus(comment.getId(), comment.getStatus());
    }
}