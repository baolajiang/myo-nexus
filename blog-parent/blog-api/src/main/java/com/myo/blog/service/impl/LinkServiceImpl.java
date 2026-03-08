package com.myo.blog.service.impl;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.LinkMapper;
import com.myo.blog.dao.pojo.Article;
import com.myo.blog.dao.pojo.Category;
import com.myo.blog.dao.pojo.Link;
import com.myo.blog.entity.ArticleVo;
import com.myo.blog.entity.CategoryVo;
import com.myo.blog.entity.LinkVo;
import com.myo.blog.entity.Result;
import com.myo.blog.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : myo
 * @create 2023/7/29 15:08
 */
@Service
@RequiredArgsConstructor
public class LinkServiceImpl implements LinkService {

    private final LinkMapper LinkMapper;

    @Override
    public Result findAll() {
        LambdaQueryWrapper<Link> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Link::getId,Link::getName,Link::getContent,Link::getUrl,Link::getImgicon);
        List<Link> link = LinkMapper.selectList(queryWrapper);
        //页面交互的对象
        return Result.success(copyList(link));
    }

    public LinkVo copy(Link link){
        LinkVo linkVo = new LinkVo();
        BeanUtils.copyProperties(link,linkVo);
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





    //LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
    //        queryWrapper.orderByDesc(Article::getCreateDate);
    //        queryWrapper.select(Article::getId,Article::getTitle);
}
