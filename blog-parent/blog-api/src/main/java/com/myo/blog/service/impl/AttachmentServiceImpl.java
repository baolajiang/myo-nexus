package com.myo.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.dao.mapper.AttachmentMapper;
import com.myo.blog.dao.pojo.Attachment;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.service.AttachmentService;
import com.myo.blog.utils.R2UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 附件管理 Service 实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    // 引用附件 Mapper，用于对 myo_attachment 表进行增删改查
    private final AttachmentMapper attachmentMapper;

    // 引用 R2 上传服务，删除附件时需要同步删除 R2 中的文件，同步时需要列举 R2 文件
    private final R2UploadService r2UploadService;

    // R2 访问域名，用于拼接文件完整访问 URL
    @Value("${r2.domain}")
    private String r2Domain;

    /**
     * 保存附件记录
     * 上传成功后调用，将文件元数据写入 myo_attachment 表
     */
    @Override
    public void save(String fileName, String fileKey, String fileUrl,
                     Long fileSize, String fileType, String mimeType,
                     String uploaderId, String remark) {
        Attachment attachment = new Attachment();
        attachment.setFileName(fileName);
        attachment.setFileKey(fileKey);
        attachment.setFileUrl(fileUrl);
        attachment.setFileSize(fileSize);
        attachment.setFileType(fileType);
        attachment.setMimeType(mimeType);
        attachment.setUploaderId(uploaderId);
        attachment.setRemark(remark);
        attachment.setCreateDate(System.currentTimeMillis());
        attachmentMapper.insert(attachment);
        log.info("[附件管理] 记录入库成功，文件名：{}，路径：{}", fileName, fileKey);
    }

    /**
     * 分页查询附件列表
     * 按创建时间倒序，最新上传的排在最前面
     */
    @Override
    public Result listAttachment(PageParams pageParams, String fileType) {
        Page<Attachment> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());
        LambdaQueryWrapper<Attachment> queryWrapper = new LambdaQueryWrapper<>();

        // 按文件分类筛选，不传或传空则查全部
        if (StringUtils.isNotBlank(fileType)) {
            queryWrapper.eq(Attachment::getFileType, fileType);
        }

        // 按文件名关键词模糊搜索
        if (StringUtils.isNotBlank(pageParams.getKeyword())) {
            queryWrapper.like(Attachment::getFileName, pageParams.getKeyword());
        }

        // 按上传时间倒序，最新的排在最前
        queryWrapper.orderByDesc(Attachment::getCreateDate);
        attachmentMapper.selectPage(page, queryWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return Result.success(result);
    }

    /**
     * 删除附件
     * 先删 R2 文件，成功后再删数据库记录
     * 顺序不能颠倒：若先删数据库再删 R2 失败，fileKey 就找不回来了
     */
    @Override
    public Result deleteAttachment(String id) {
        // 1. 查询附件是否存在
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            return Result.fail(404, "附件不存在");
        }

        // 2. 先删除 R2 中的真实文件
        boolean r2Deleted = r2UploadService.deleteFile(attachment.getFileKey());
        if (!r2Deleted) {
            log.error("[附件管理] R2 文件删除失败，已中止数据库删除，fileKey：{}", attachment.getFileKey());
            return Result.fail(500, "R2 文件删除失败，操作已中止");
        }

        // 3. R2 删除成功，再删除数据库记录
        attachmentMapper.deleteById(id);
        log.info("[附件管理] 附件删除成功，文件名：{}，路径：{}", attachment.getFileName(), attachment.getFileKey());
        return Result.success("删除成功");
    }

    /**
     * 将 R2 存储桶中的历史文件全量同步到 myo_attachment 表
     *
     * 执行流程：
     * 1. 查出数据库中已有的所有 fileKey，存入 Set，用于快速判断是否已存在
     * 2. 调用 R2 listAllFiles() 游标分页扫出全部文件
     * 3. 逐个判断：已在数据库中的跳过，不存在的根据路径自动推断分类后入库
     * 4. 返回同步结果统计
     */
    @Override
    public Result syncFromR2() {
        log.info("[R2同步] 开始扫描 R2 存储桶，准备同步历史文件...");

        // 第一步：查出数据库中已有的所有 fileKey，放入 Set 便于 O(1) 查重
        List<Attachment> existList = attachmentMapper.selectList(
                new LambdaQueryWrapper<Attachment>().select(Attachment::getFileKey)
        );
        Set<String> existKeys = new java.util.HashSet<>();
        for (Attachment a : existList) {
            existKeys.add(a.getFileKey());
        }
        log.info("[R2同步] 数据库中已有 {} 条附件记录", existKeys.size());

        // 第二步：调用 R2 游标分页，扫出桶里全部文件
        List<S3Object> allFiles = r2UploadService.listAllFiles();
        log.info("[R2同步] R2 中共扫描到 {} 个文件", allFiles.size());

        int newCount = 0;   // 本次新增入库的数量
        int skipCount = 0;  // 已存在跳过的数量

        String domain = r2Domain.endsWith("/") ? r2Domain : r2Domain + "/";

        for (S3Object s3Object : allFiles) {
            String fileKey = s3Object.key();

            // 跳过"目录占位符"（R2 中以 / 结尾的空对象）
            if (fileKey.endsWith("/")) {
                skipCount++;
                continue;
            }

            // 已在数据库中，跳过不重复插入
            if (existKeys.contains(fileKey)) {
                skipCount++;
                continue;
            }

            // 根据路径前缀自动推断文件分类
            String fileType = inferFileType(fileKey);

            // 根据路径推断 MIME 类型
            String mimeType = inferMimeType(fileKey);

            // 截取纯文件名（不含路径），例如 cover/xxx.jpg → xxx.jpg
            String fileName = fileKey.contains("/")
                    ? fileKey.substring(fileKey.lastIndexOf("/") + 1)
                    : fileKey;

            // 拼接完整访问 URL
            String fileUrl = domain + fileKey;

            // 构建附件对象并入库
            Attachment attachment = new Attachment();
            attachment.setFileName(fileName);
            attachment.setFileKey(fileKey);
            attachment.setFileUrl(fileUrl);
            attachment.setFileSize(s3Object.size());
            attachment.setFileType(fileType);
            attachment.setMimeType(mimeType);
            attachment.setUploaderId("SYSTEM");
            attachment.setRemark("历史数据同步");
            attachment.setCreateDate(
                    s3Object.lastModified() != null
                            ? s3Object.lastModified().toEpochMilli()
                            : System.currentTimeMillis()
            );
            attachmentMapper.insert(attachment);
            newCount++;
        }

        log.info("[R2同步] 同步完成。共扫描 {} 个，新增 {} 条，跳过 {} 条",
                allFiles.size(), newCount, skipCount);

        // 返回同步统计结果给前端展示
        Map<String, Object> result = new HashMap<>();
        result.put("total", allFiles.size());
        result.put("newCount", newCount);
        result.put("skipCount", skipCount);
        return Result.success(result);
    }

    /**
     * 根据 R2 文件路径前缀自动推断文件分类
     * 路径规律：
     *   backup/database/ → backup（数据库备份）
     *   backup/syslog/   → log（日志备份）
     *   backup/          → log（其他日志）
     *   cover/ covers/ common/ avatar/ article_body/ → image（图片）
     *   其他             → other
     */
    private String inferFileType(String fileKey) {
        if (fileKey.startsWith("backup/database/")) return "backup";
        if (fileKey.startsWith("backup/")) return "log";
        if (fileKey.startsWith("cover/")
                || fileKey.startsWith("covers/")
                || fileKey.startsWith("common/")
                || fileKey.startsWith("avatar/")
                || fileKey.startsWith("articles/")
                || fileKey.startsWith("article_body/")) return "image";
        return "other";
    }

    /**
     * 根据文件扩展名推断 MIME 类型
     */
    private String inferMimeType(String fileKey) {
        String lower = fileKey.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".sql.gz") || lower.endsWith(".gz")) return "application/gzip";
        if (lower.endsWith(".sql"))  return "application/sql";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".txt"))  return "text/plain";
        return "application/octet-stream";
    }
}