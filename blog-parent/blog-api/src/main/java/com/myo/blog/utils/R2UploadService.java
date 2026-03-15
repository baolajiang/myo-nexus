package com.myo.blog.utils;

import cn.hutool.core.img.Img;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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

    // ：新增 @PreDestroy，应用关闭时释放 S3Client 连接池资源
    @PreDestroy
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    /**
     * 上传图片到 R2，并自动生成 _thumb.jpg 缩略图
     *
     * @param file 前端传来的文件
     * @param path 路径文件夹，例如 "common" 或 "avatar"
     * @return 原图的完整访问 URL
     */
    public String uploadAvatar(MultipartFile file, String path) throws IOException {
        String originalFilename = file.getOriginalFilename();

        // 1. 获取后缀
        String suffix = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 2. 生成文件名
        String fileId = SerialGenerator.generate(path);
        String key = path + "/" + fileId + suffix;

        // ：一次性读取字节，避免 InputStream 被消费两次
        byte[] fileBytes = file.getBytes();

        // 3. 上传原图
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

        // 4. 生成并上传缩略图
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            try {
                String thumbKey = path + "/" + fileId + "_thumb.jpg";
                ByteArrayOutputStream thumbOut = new ByteArrayOutputStream();

                // ：用 ByteArrayInputStream 包装已读取的字节，而不是重新调用 getInputStream()
                Img.from(new ByteArrayInputStream(fileBytes))
                        .setTargetImageType("jpg")
                        .setQuality(0.2)
                        .scale(300, -1)   // ：50px 太小，改为 300px
                        .write(thumbOut);

                byte[] thumbBytes = thumbOut.toByteArray();

                PutObjectRequest thumbRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(thumbKey)
                        .contentType("image/jpeg")
                        .build();

                s3Client.putObject(thumbRequest, RequestBody.fromBytes(thumbBytes));

            } catch (Exception e) {
                // 缩略图失败不影响主流程，但必须记录日志
                // ：替换 e.printStackTrace()，改用 log.error 进入日志系统
                log.error("[R2] 缩略图生成或上传失败, key={}", key, e);
            }
        }

        // 5. 返回访问 URL
        return domain.endsWith("/") ? domain + key : domain + "/" + key;
    }

    /**
     * 通用的字节流上传方法（用于日志备份等非图片文件）
     *
     * @param key   存储路径和文件名，例如 "backup/logs_2026_03.json"
     * @param bytes 文件的字节数组
     * @return 是否上传成功
     */
    public boolean uploadBytes(String key, byte[] bytes) {
        try {
            String contentType = "application/octet-stream";
            if (key.endsWith(".json")) {
                contentType = "application/json";
            } else if (key.endsWith(".txt")) {
                contentType = "text/plain";
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
            return true;
        } catch (Exception e) {
            // ：替换 e.printStackTrace()，改用 log.error 进入日志系统
            log.error("[R2] 字节流上传失败, key={}", key, e);
            return false;
        }
    }

    /**
     * 删除 R2 中的文件
     * 附件管理删除时调用，传入 fileKey（存储路径，不含域名）
     *
     * @param key R2 存储路径，例如 cover/xxx.jpg
     * @return true = 删除成功，false = 删除失败
     */
    public boolean deleteFile(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
            log.info("[R2] 文件删除成功, key={}", key);
            return true;
        } catch (Exception e) {
            log.error("[R2] 文件删除失败, key={}", key, e);
            return false;
        }
    }
    /**
     * 列举 R2 存储桶中的所有文件
     * 使用游标分页循环请求，每次最多返回 1000 个对象，直到取完所有文件
     * 不管桶里有多少文件，都能全部扫出来
     *
     * @return 所有文件的 S3Object 列表，每个对象包含 key（路径）和 size（字节大小）
     */
    public List<S3Object> listAllFiles() {
        List<S3Object> allFiles = new ArrayList<>();
        String continuationToken = null;

        do {
            // 构建列举请求，每次最多取 1000 个
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(1000);

            // 非第一次请求时带上上次返回的游标，继续从上次结束的位置取
            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            // 将本次返回的文件追加到总列表
            allFiles.addAll(response.contents());

            // 如果还有下一页，取出游标供下次请求使用；否则置为 null 结束循环
            continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;

        } while (continuationToken != null);

        log.info("[R2] 文件列举完毕，共 {} 个文件", allFiles.size());
        return allFiles;
    }

}