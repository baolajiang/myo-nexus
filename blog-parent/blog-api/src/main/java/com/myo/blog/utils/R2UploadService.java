package com.myo.blog.utils;

import cn.hutool.core.img.Img;
import cn.hutool.core.io.FileTypeUtil; // 如果 hutool 版本较新，可能不需要这个，直接用 contentType 判断
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

@Service
public class R2UploadService {

    @Value("${r2.access-key}")
    private String accessKey;

    @Value("${r2.secret-key}")
    private String secretKey;

    @Value("${r2.account-id}")
    private String accountId;

    @Value("${r2.bucket}")
    private String bucketName;

    @Value("${r2.domain}")
    private String domain;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        String endpoint = "https://" + accountId + ".r2.cloudflarestorage.com";
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    /**
     * 上传图片到 R2，并自动生成 _thumb.jpg 缩略图
     * @param file 前端传来的文件
     * @param path 路径文件夹，例如 "common" 或 "avatar"
     * @return 原图的完整访问 URL
     */
    public String uploadAvatar(MultipartFile file, String path) throws IOException {
        String originalFilename = file.getOriginalFilename();

        // 1. 获取后缀
        String suffix = ".jpg"; // 默认兜底
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 2. 生成文件名 (优化：变量名改为 fileId，使用 path 作为序列号的 key)
        // 这样 avatar 就用 avatar 的序列，cover 就用 cover 的序列
        String fileId = SerialGenerator.generate(path);
        String key = path + "/" + fileId + suffix;

        // 3. 上传【原图】
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // ================== 生成并上传缩略图 ==================
        try {
            String contentType = file.getContentType();
            // 简单判断是否为图片
            if (contentType != null && contentType.startsWith("image/")) {

                // 缩略图文件名：例如 common/1001_thumb.jpg
                String thumbKey = path + "/" + fileId + "_thumb.jpg";

                ByteArrayOutputStream thumbOut = new ByteArrayOutputStream();

                // ★★★ 优化点：强制转为 JPEG 格式 ★★★
                // 这样无论原图是 PNG 还是 WebP，缩略图统一变成体积最小的 JPG
                Img.from(file.getInputStream())
                        .setTargetImageType("jpg") // 强制输出 JPG
                        .setQuality(0.2)           // 20% 质量
                        .scale(50, -1)             // 宽 50px
                        .write(thumbOut);

                byte[] thumbBytes = thumbOut.toByteArray();

                // 上传缩略图
                PutObjectRequest thumbRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(thumbKey)
                        .contentType("image/jpeg")
                        .build();

                s3Client.putObject(thumbRequest, RequestBody.fromBytes(thumbBytes));

                 //System.out.println("缩略图生成完毕: " + thumbKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 缩略图失败不影响主流程
        }

        // 5. 返回访问 URL
        return domain.endsWith("/") ? domain + key : domain + "/" + key;
    }
}