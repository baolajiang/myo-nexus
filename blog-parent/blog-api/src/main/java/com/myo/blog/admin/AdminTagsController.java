package com.myo.blog.admin;

import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.dao.pojo.Tag;
import com.myo.blog.entity.Result;
import com.myo.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/tags")
@RequiredArgsConstructor
public class AdminTagsController {
    private final TagService tagService;

    @PostMapping
    @RequirePermission("tag:add")
    public Result addTag(@RequestBody Tag tag) {
        return tagService.addTag(tag);
    }

    @PutMapping
    @RequirePermission("tag:edit")
    public Result updateTag(@RequestBody Tag tag) {

        return tagService.updateTag(tag);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("tag:delete")
    public Result deleteTag(@PathVariable("id") String id) {
        return tagService.deleteTag(id);
    }
}
