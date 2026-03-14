package com.myo.blog.controller;

import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.dao.pojo.Tag;
import com.myo.blog.service.TagService;
import com.myo.blog.entity.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("tags")
public class TagsController {

    private final TagService tagService;

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


}
