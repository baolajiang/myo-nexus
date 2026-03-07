package com.myo.blog.service;

import com.myo.blog.entity.CategoryVo;
import com.myo.blog.entity.Result;
import com.myo.blog.dao.pojo.Category;
import java.util.Collection;
import java.util.List;

public interface CategoryService {

    CategoryVo findCategoryById(String categoryId); 

    Result findAll();

    Result findAllDetail();

    Result categoryDetailById(String id); 

    /**
     * 批量查询分类
     */
    List<Category> findCategoryByIds(Collection<String> ids);
    /**
     * 添加分类
     */
    Result addCategory(Category category);
    /**
     * 更新分类
     */
    Result updateCategory(Category category);
    /**
     * 删除分类
     */
    Result deleteCategory(String id);
}