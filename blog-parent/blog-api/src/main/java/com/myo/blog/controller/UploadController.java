package com.myo.blog.controller;

import com.myo.blog.common.aop.RateLimit;
import com.myo.blog.dao.mapper.ArticleMapper;

import com.myo.blog.dao.mapper.SysUserMapper;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.entity.Result;
import com.myo.blog.service.AttachmentService;
import com.myo.blog.service.SysUserService;
import com.myo.blog.utils.R2UploadService;
import com.myo.blog.utils.UserThreadLocal;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RequestMapping("upload")
public class UploadController {

    private final R2UploadService r2UploadService;


    private final AttachmentService attachmentService;


    @Value("${r2.domain}")
    private String r2Domain;
    /**
     * 上传文件到 R2 存储桶, 并保存文件元数据到数据库myo_attachment表中
     * 支持自定义路径（默认 common），文件分类（image/log/backup/other）
     *
     * @param file 上传的文件（MultipartFile）
     * @param path 自定义路径（默认 common），用于分类存储
     * @return 上传成功后的完整 URL
     */
    @PostMapping
    public Result upload(@RequestParam("image") MultipartFile file,
                         @RequestParam(value = "path", defaultValue = "common") String path) {
        try {
            // 上传到 R2
            String url = r2UploadService.uploadAvatar(file, path);

            // 上传成功后，将文件元数据写入 myo_attachment 表
            String fileKey = path + "/" + url.substring(url.lastIndexOf("/") + 1);
            SysUser currentUser = UserThreadLocal.get();
            String uploaderId = (currentUser != null) ? currentUser.getId() : "SYSTEM";

            attachmentService.save(
                    file.getOriginalFilename(),   // 原始文件名
                    fileKey,                       // R2 存储路径
                    url,                           // 完整访问 URL
                    file.getSize(),                // 文件大小（字节）
                    "image",                       // 文件分类
                    file.getContentType(),         // MIME 类型
                    uploaderId,                    // 上传者
                    "前台上传 - " + path           // 备注
            );

            return Result.success(url);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(20001, "上传失败: " + e.getMessage());
        }
    }

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
        */
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
