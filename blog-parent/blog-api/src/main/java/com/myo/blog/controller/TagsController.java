package com.myo.blog.controller;

import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.dao.pojo.Tag;
import com.myo.blog.service.TagService;
import com.myo.blog.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("tags")
public class TagsController {
    @Autowired
    private TagService tagService;

    //   /tags/hot
    @GetMapping("hot")
    public Result hot(){
        int limit = 6;
        return tagService.hots(limit);
    }
    @GetMapping
    public Result findAll(){
        return tagService.findAll();
    }

    @GetMapping("detail")
    public Result findAllDetail(){
        return tagService.findAllDetail();
    }

    @GetMapping("detail/{id}")
    public Result findDetailById(@PathVariable("id") String id){
        return tagService.findDetailById(id);
    }


    //根据分类ID查询标签
    @GetMapping("category/{categoryId}")
    public Result findTagsByCategoryId(@PathVariable("categoryId") String categoryId){
        return tagService.findTagsByCategoryId(categoryId);
    }

    @PostMapping
    @RequirePermission("tag:add")
    public Result addTag(@RequestBody Tag tag) {
        return tagService.addTag(tag);
    }

    @PutMapping
    @RequirePermission("tag:edit")
    public Result updateTag(@RequestBody Tag tag) {
        System.out.println("更新标签控制器" );
        return tagService.updateTag(tag);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("tag:delete")
    public Result deleteTag(@PathVariable("id") String id) {
        return tagService.deleteTag(id);
    }
}
