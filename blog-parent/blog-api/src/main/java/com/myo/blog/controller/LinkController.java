package com.myo.blog.controller;

import com.myo.blog.common.cache.Cache;
import com.myo.blog.entity.Result;
import com.myo.blog.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : myo
 * @create 2023/7/29 15:40
 */
@RestController
@RequestMapping("link")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;
    @GetMapping
    @Cache(expire = 5 * 60 * 1000,name = "link_List")
    public Result categories(){

        return linkService.findAll();
    }

}
