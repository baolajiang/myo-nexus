package com.myo.blog.utils;

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
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
/**
 *
 */
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
        // R2 的 Endpoint 格式固定为: https://<account_id>.r2.cloudflarestorage.com
        String endpoint = "https://" + accountId + ".r2.cloudflarestorage.com";

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1) // R2 不区分 Region，但 SDK 需要填一个，通常填 US_EAST_1 或 auto
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    /**
     * 上传头像到 R2 的 avatar 目录
     * @param file 前端传来的文件
     * @return 文件的完整访问 URL
     */
    public String uploadAvatar(MultipartFile file) throws IOException {
        // 1. 获取原始文件名后缀 (如 .png, .jpg)
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        // 2. 生成唯一文件名 (防止覆盖)，格式: avatar/uuid.png
        String key = "avatar/" + UUID.randomUUID().toString() + suffix;

        // 3. 构建上传请求
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType()) // 显式设置 Content-Type，否则浏览器访问可能是下载而不是预览
                .build();

        // 4. 执行上传
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // 5. 返回完整的访问 URL
        // 如果 domain 末尾没有 /，这里加上
        return domain.endsWith("/") ? domain + key : domain + "/" + key;
    }
}