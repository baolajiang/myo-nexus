package com.myo.blog.controller;

import com.myo.blog.common.aop.RateLimit;
import com.myo.blog.dao.mapper.ArticleMapper;

import com.myo.blog.entity.Result;
import com.myo.blog.utils.R2UploadService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("upload")
public class UploadController {
    @Autowired
    private R2UploadService r2UploadService;

    @Value("${r2.domain}")
    private String r2Domain;

    @PostMapping
    public Result upload(@RequestParam("image") MultipartFile file) {
        try {
            // 直接调用 Service 上传
            String url = r2UploadService.uploadAvatar(file);

            // 返回成功结果，data 就是图片的 URL
            return Result.success(url);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(20001, "上传失败: " + e.getMessage());
        }
    }
    @Autowired
    private ArticleMapper articleMapper;
    // 通用上传限制 1分钟10次
    @RateLimit(time = 60, count = 10, msg = "上传过于频繁")
    @PostMapping("file")
    public Result upcover(@RequestParam("image")  MultipartFile file){
        //原始文件名称 比如 aa.png
        String originalFilename = file.getOriginalFilename();
        //文件名称
        /*LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Article::getId);
        queryWrapper.last("limit 1");
        queryWrapper.orderByDesc(Article::getCreateDate);
        List<Article> articles = articleMapper.selectList(queryWrapper);
        int id=Integer.parseInt(String.valueOf(articles.get(0).getId()));
        String cover="cover/cover"+id;
        System.out.println("cover值："+id);*/
        //UUID.randomUUID().toString()文件名称变成cover.jpg
        String fileName ="cover/"+ UUID.randomUUID().toString() + "." + StringUtils.substringAfterLast(originalFilename, ".");
        //上传文件
        // 降低自身应用服务器的带宽消耗




        return Result.fail(20001,"上传失败");
    }
    @PostMapping("file2")
    public Result upcover2( MultipartFile file){
        String originalFilename = file.getOriginalFilename();

        String fileName ="cover/"+ UUID.randomUUID().toString() + "." + StringUtils.substringAfterLast(originalFilename, ".");
        //上传文件
        // 降低自身应用服务器的带宽消耗



        return Result.fail(20001,"上传失败");
    }
    // 通用上传限制 1分钟10次
    @RateLimit(time = 60, count = 10, msg = "上传过于频繁")
    @PostMapping("qrcode")
    public Result qrcode(@RequestParam("image") MultipartFile file){
        String originalFilename = file.getOriginalFilename();

        String fileName ="QR_code/"+ UUID.randomUUID().toString() + "." + StringUtils.substringAfterLast(originalFilename, ".");
        //上传文件
        // 降低自身应用服务器的带宽消耗


        return Result.fail(20001,"上传失败");
    }


}
