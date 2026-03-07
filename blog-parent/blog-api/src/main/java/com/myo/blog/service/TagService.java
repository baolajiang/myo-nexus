package com.myo.blog.service;

import com.myo.blog.dao.pojo.Tag;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.TagVo;

import java.util.List;

public interface TagService {

    List<TagVo> findTagsByArticleId(String articleId); 

    Result hots(int limit);

    /**
     * 查询所有的文章标签
     * @return
     */
    Result findAll();

    Result findAllDetail();

    Result findDetailById(String id); 

    //根据分类ID查询标签
    Result findTagsByCategoryId(String categoryId);
    /**
     * 添加标签
     * @param tag
     * @return
     */
    Result addTag(Tag tag);
    /**
     * 更新标签
     * @param tag
     * @return
     */
    Result updateTag(Tag tag);
    /**
     * 删除标签
     * @param id
     * @return
     */
    Result deleteTag(String id);

}