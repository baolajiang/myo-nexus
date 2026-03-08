package com.myo.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.ArticleMapper;
import com.myo.blog.dao.mapper.ArticleTagMapper;
import com.myo.blog.dao.mapper.TagMapper;
import com.myo.blog.dao.pojo.ArticleTag;
import com.myo.blog.dao.pojo.Tag;
import com.myo.blog.service.TagService;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.TagVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    private final ArticleTagMapper articleTagMapper;

    public TagVo copy(Tag tag){
        TagVo tagVo = new TagVo();
        BeanUtils.copyProperties(tag,tagVo);
        tagVo.setId(String.valueOf(tag.getId()));
        return tagVo;
    }
    public List<TagVo> copyList(List<Tag> tagList){
        List<TagVo> tagVoList = new ArrayList<>();
        for (Tag tag : tagList) {
            tagVoList.add(copy(tag));
        }
        return tagVoList;
    }
    @Override
    public List<TagVo> findTagsByArticleId(String articleId) {
        //mybatisplus 无法进行多表查询
        List<Tag> tags = tagMapper.findTagsByArticleId(articleId);
        return copyList(tags);
    }


    @Override
    public Result hots(int limit) {
        /**
         * 1. 标签所拥有的文章数量最多 最热标签
         * 2. 查询 根据tag_id 分组 计数，从大到小 排列 取前 limit个
         */
        List<String> tagIds = tagMapper.findHotsTagIds(limit);
        if (CollectionUtils.isEmpty(tagIds)){
            return Result.success(Collections.emptyList());
        }
        //需求的是 tagId 和 tagName  Tag对象
        //select * from tag where id in (1,2,3,4)
        List<Tag> tagList = tagMapper.findTagsByTagIds(tagIds);
        return Result.success(tagList);
    }

    @Override
    public Result findAll() {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Tag::getId,Tag::getTagName);
        List<Tag> tags = this.tagMapper.selectList(queryWrapper);
        return Result.success(copyList(tags));
    }

    @Override
    public Result findAllDetail() {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        List<Tag> tags = this.tagMapper.selectList(queryWrapper);
        return Result.success(copyList(tags));
    }

    @Override
    public Result findDetailById(String id) {
        Tag tag = tagMapper.selectById(id);
        return Result.success(copy(tag));
    }

    @Override

    public Result findTagsByCategoryId(String categoryId) {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        // 匹配对应的 category_id
        queryWrapper.eq(Tag::getCategoryId, categoryId);
        List<Tag> tags = this.tagMapper.selectList(queryWrapper);
        // 使用自带的 copyList 方法转换为 TagVo 列表并返回
        return Result.success(copyList(tags));
    }
    /**
     * 添加标签
     * @param tag
     * @return
     */
    @Override
    public Result addTag(Tag tag) {
        this.tagMapper.insert(tag);
        return Result.success(null);
    }
    /**
     * 更新标签
     * @param tag
     * @return
     */
    @Override
    public Result updateTag(Tag tag) {
        this.tagMapper.updateById(tag);
        return Result.success(null);
    }
    /**
     * 删除标签
     * @param id
     * @return
     */
    @Override
    public Result deleteTag(String id) {
        // 去文章标签关联表查一下 count
        LambdaQueryWrapper<ArticleTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleTag::getTagId, id);
        Long count = articleTagMapper.selectCount(wrapper);

        if (count > 0) {
            return Result.fail(400, "有文章正在使用该标签，无法删除！");
        }

        this.tagMapper.deleteById(id);
        return Result.success(null);
    }
}
