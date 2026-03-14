package com.myo.blog.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.dao.pojo.Category;
import com.myo.blog.exception.BusinessException;
import com.myo.blog.exception.ParamException;
import com.myo.blog.service.CategoryService;
import com.myo.blog.entity.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


// ... existing code ...
@RestController
@RequestMapping("categorys")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // /categorys
    @GetMapping
    public Result categories() {
        try {
            return categoryService.findAll();
        } catch (Exception e) {
            throw new BusinessException(500, "获取分类列表失败");
        }
    }

    @GetMapping("detail")
    public Result categoriesDetail() {
        try {
            return categoryService.findAllDetail();
        } catch (Exception e) {
            throw new BusinessException(500, "获取分类详情失败");
        }
    }


    @GetMapping("detail/{id}")
    public Result categoryDetailById(@PathVariable("id") String id) {
        // 判空逻辑
        if (StringUtils.isBlank(id)) {
            throw new ParamException("分类ID不能为空");
        }
        try {
            return categoryService.categoryDetailById(id);
        } catch (Exception e) {
            throw new BusinessException(500, "获取分类详情失败");
        }
    }

}
