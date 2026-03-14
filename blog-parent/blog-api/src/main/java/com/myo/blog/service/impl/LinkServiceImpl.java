package com.myo.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.dao.mapper.LinkMapper;
import com.myo.blog.dao.pojo.Link;
import com.myo.blog.entity.LinkVo;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LinkServiceImpl implements LinkService {

    private final LinkMapper linkMapper;

    // 前台：只查询状态为 1 (正常) 的链接，并按 sort 排序
    @Override
    public Result findAll() {
        LambdaQueryWrapper<Link> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Link::getStatus, 1);
        queryWrapper.orderByDesc(Link::getSort);
        // 新增 select status 和 sort
        queryWrapper.select(Link::getId, Link::getName, Link::getContent, Link::getUrl, Link::getImgicon, Link::getStatus, Link::getSort);
        List<Link> link = linkMapper.selectList(queryWrapper);
        return Result.success(copyList(link));
    }

    // 后台：分页查询所有链接 (可选关键字搜索)
    @Override
    public Result listLink(PageParams pageParams) {
        Page<Link> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());
        LambdaQueryWrapper<Link> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(pageParams.getKeyword())) {
            queryWrapper.like(Link::getName, pageParams.getKeyword());
        }
        queryWrapper.orderByDesc(Link::getSort).orderByDesc(Link::getId);
        Page<Link> linkPage = linkMapper.selectPage(page, queryWrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("list", linkPage.getRecords());
        map.put("total", linkPage.getTotal());
        return Result.success(map);
    }

    // 后台：新增链接
    @Override
    public Result addLink(Link link) {
        if (link.getStatus() == null) link.setStatus(1);
        if (link.getSort() == null) link.setSort(0);
        linkMapper.insert(link);
        return Result.success("新增成功");
    }

    // 后台：修改链接
    @Override
    public Result updateLink(Link link) {
        linkMapper.updateById(link);
        return Result.success("修改成功");
    }

    // 后台：删除链接
    @Override
    public Result deleteLink(String id) {
        linkMapper.deleteById(id);
        return Result.success("删除成功");
    }

    // 后台：快速切换状态
    @Override
    public Result changeLinkStatus(String id, Integer status) {
        Link link = new Link();
        link.setId(id);
        link.setStatus(status);
        linkMapper.updateById(link);
        return Result.success("状态更新成功");
    }

    public LinkVo copy(Link link){
        LinkVo linkVo = new LinkVo();
        BeanUtils.copyProperties(link, linkVo);
        linkVo.setId(link.getId());
        return linkVo;
    }

    public List<LinkVo> copyList(List<Link> ListList){
        List<LinkVo> LinkVOList = new ArrayList<>();
        for (Link link : ListList) {
            LinkVOList.add(copy(link));
        }
        return LinkVOList;
    }
}