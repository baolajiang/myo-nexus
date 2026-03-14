package com.myo.blog.admin;

import com.myo.blog.common.aop.LogAnnotation;
import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.dao.pojo.Link;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/link")
@RequiredArgsConstructor
public class AdminLinkController {

    private final LinkService linkService;

    @PostMapping("list")
    @RequirePermission("link:list")
    public Result list(@RequestBody PageParams pageParams) {
        return linkService.listLink(pageParams);
    }

    @PostMapping("add")
    @RequirePermission("link:add")
    @LogAnnotation(module = "收藏夹", operator = "新增链接")
    public Result add(@RequestBody Link link) {
        return linkService.addLink(link);
    }

    @PostMapping("update")
    @RequirePermission("link:update")
    @LogAnnotation(module = "收藏夹", operator = "修改链接")
    public Result update(@RequestBody Link link) {
        return linkService.updateLink(link);
    }

    @PostMapping("delete/{id}")
    @RequirePermission("link:delete")
    @LogAnnotation(module = "收藏夹", operator = "删除链接")
    public Result delete(@PathVariable String id) {
        return linkService.deleteLink(id);
    }

    @PostMapping("changeStatus")
    @RequirePermission("link:update")
    public Result changeStatus(@RequestBody Link link) {
        return linkService.changeLinkStatus(link.getId(), link.getStatus());
    }
}