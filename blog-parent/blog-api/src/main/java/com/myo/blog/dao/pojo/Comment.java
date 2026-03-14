package com.myo.blog.dao.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 评论
 */
@Data
public class Comment {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String content;

    private Long createDate;

    private String articleId;

    private String authorId;

    private String parentId;

    private String toUid;

    private Integer level;

    private Integer status;//  1=正常，2=待人工复审，0=已删除


}
