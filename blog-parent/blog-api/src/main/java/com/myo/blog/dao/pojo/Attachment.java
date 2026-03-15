package com.myo.blog.dao.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 附件实体类，对应 myo_attachment 表
 * 记录所有上传到 Cloudflare R2 的文件元数据，
 * 作为 R2 的管理索引，支持分页查询、搜索、删除联动
 */
@Data
@TableName("myo_attachment")
public class Attachment {

    // 附件唯一ID，使用自定义序列号生成器
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    // 原始文件名，例如 avatar.jpg、log_export_20260314.json
    private String fileName;

    // R2 存储路径（不含域名），例如 cover/xxx.jpg
    // 删除 R2 文件时使用此字段作为 key
    private String fileKey;

    // 完整访问 URL，例如 https://cos.myo.pub/cover/xxx.jpg
    // 前端展示和预览时使用此字段
    private String fileUrl;

    // 文件大小（字节）
    private Long fileSize;

    // 文件分类：image / log / backup / other
    // 对应前端列表页的 Tab 分类筛选
    private String fileType;

    // MIME 类型，例如 image/jpeg、application/json、application/gzip
    private String mimeType;

    // 上传者用户ID
    // 人工上传：填当前登录用户的 ID
    // 定时任务自动上传（日志备份、数据库备份）：填 "SYSTEM"
    private String uploaderId;

    // 上传时间戳（毫秒）
    private Long createDate;

    // 备注，用于说明文件来源
    // 例如：数据库备份任务、日志导出任务、文章封面上传
    private String remark;
}