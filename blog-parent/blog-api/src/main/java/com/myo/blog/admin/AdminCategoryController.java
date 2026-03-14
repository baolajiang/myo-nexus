package com.myo.blog.admin;

import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.dao.pojo.Category;
import com.myo.blog.entity.Result;
import com.myo.blog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryService categoryService;
    /**
     * 添加分类
     */
    @PostMapping
    @RequirePermission("category:add")
    public Result addCategory(@RequestBody Category category) {
        return categoryService.addCategory(category);
    }
    /**
     * 更新分类
     */
    @PutMapping
    @RequirePermission("category:edit")
    public Result updateCategory(@RequestBody Category category) {

        return categoryService.updateCategory(category);
    }
    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    @RequirePermission("category:delete")
    public Result deleteCategory(@PathVariable("id") String id) {
        return categoryService.deleteCategory(id);
    }
}
