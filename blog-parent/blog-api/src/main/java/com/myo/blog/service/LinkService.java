package com.myo.blog.service;

import com.myo.blog.dao.pojo.Link;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : myo
 * @create 2023/7/29 15:05
 */
public interface LinkService {
    // 前台获取所有正常状态的连接
    Result findAll();

    // 以下为后台管理专用方法
    Result listLink(PageParams pageParams);

    Result addLink(Link link);

    Result updateLink(Link link);

    Result deleteLink(String id);

    Result changeLinkStatus(String id, Integer status);


}
