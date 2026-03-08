package com.myo.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.ArticleMapper;
import com.myo.blog.dao.mapper.CategoryMapper;
import com.myo.blog.dao.mapper.TagMapper;
import com.myo.blog.dao.pojo.Article;
import com.myo.blog.dao.pojo.Category;
import com.myo.blog.dao.pojo.Tag;
import com.myo.blog.service.CategoryService;
import com.myo.blog.entity.CategoryVo;
import com.myo.blog.entity.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;


@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {


    private final CategoryMapper categoryMapper;

    private final ArticleMapper articleMapper;

    private final TagMapper tagMapper;

    @Override
    public CategoryVo findCategoryById(String categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        CategoryVo categoryVo = new CategoryVo();
        BeanUtils.copyProperties(category,categoryVo);
        categoryVo.setId(String.valueOf(category.getId()));
        return categoryVo;
    }

    @Override
    public Result findAll() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Category::getId,Category::getCategoryName);
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        //页面交互的对象
        return Result.success(copyList(categories));
    }

    @Override
    public Result findAllDetail() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        //页面交互的对象
        return Result.success(copyList(categories));
    }

    @Override
    public Result categoryDetailById(String id) {
        Category category = categoryMapper.selectById(id);
        return Result.success(copy(category));
    }

    @Override
    public List<Category> findCategoryByIds(Collection<String> ids) {
        // 使用已經注入的 categoryMapper
        return categoryMapper.selectBatchIds(ids);
    }

    public CategoryVo copy(Category category){
        CategoryVo categoryVo = new CategoryVo();
        BeanUtils.copyProperties(category,categoryVo);
        categoryVo.setId(String.valueOf(category.getId()));
        return categoryVo;
    }
    public List<CategoryVo> copyList(List<Category> categoryList){
        List<CategoryVo> categoryVoList = new ArrayList<>();
        for (Category category : categoryList) {
            categoryVoList.add(copy(category));
        }
        return categoryVoList;
    }

    /**
     * 添加分类
     */
    @Override
    public Result addCategory(Category category) {
        this.categoryMapper.insert(category);
        return Result.success(null);
    }
    /**
     * 更新分类
     */
    @Override
    public Result updateCategory(Category category) {

        this.categoryMapper.updateById(category);
        return Result.success(null);
    }
    /**
     * 删除分类
     *
     */

    @Override
    public Result deleteCategory(String id) {
        // 1. 检查是否有【文章】正在使用该分类
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.eq(Article::getCategoryId, id);
        Long articleCount = articleMapper.selectCount(articleWrapper);
        if (articleCount > 0) {
            // 返回错误提示给前端
            return Result.fail(400, "该分类下存在 " + articleCount + " 篇文章，请先转移或删除文章！");
        }

        // 2. 检查是否有【标签】归属于该分类
        LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(Tag::getCategoryId, id);
        Long tagCount = tagMapper.selectCount(tagWrapper);
        if (tagCount > 0) {
            return Result.fail(400, "该分类下还存在 " + tagCount + " 个标签，请先删除下属标签！");
        }

        // 3. 经过上面两道关卡，说明这个分类是个“空壳”，可以安全删除
        this.categoryMapper.deleteById(id);

        return Result.success(null);
    }


}
