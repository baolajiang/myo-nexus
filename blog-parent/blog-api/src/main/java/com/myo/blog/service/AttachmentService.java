package com.myo.blog.service;

import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;

/**
 * 附件管理 Service 接口
 */
public interface AttachmentService {

    /**
     * 保存附件记录到数据库
     * 每次文件上传到 R2 成功后调用此方法入库
     *
     * @param fileName   原始文件名
     * @param fileKey    R2 存储路径（用于后续删除）
     * @param fileUrl    完整访问 URL（用于前端展示）
     * @param fileSize   文件大小（字节）
     * @param fileType   文件分类：image / log / backup / other
     * @param mimeType   MIME 类型
     * @param uploaderId 上传者用户ID，定时任务传 "SYSTEM"
     * @param remark     备注，说明文件来源
     */
    void save(String fileName, String fileKey, String fileUrl,
              Long fileSize, String fileType, String mimeType,
              String uploaderId, String remark);

    /**
     * 分页查询附件列表
     * 支持按文件分类（fileType）筛选和文件名关键词搜索
     *
     * @param pageParams 分页参数，keyword 用于文件名模糊搜索
     * @param fileType   文件分类筛选，传 null 或空字符串则查全部
     */
    Result listAttachment(PageParams pageParams, String fileType);

    /**
     * 删除附件
     * 先删除 R2 中的文件，成功后再删除数据库记录，保证数据一致性
     *
     * @param id 附件ID
     */
    Result deleteAttachment(String id);

    /**
     * 将 R2 存储桶中的历史文件全量同步到 myo_attachment 表
     * 已存在的文件（根据 file_key 判断）自动跳过，不重复插入
     * 适合首次部署时一次性执行，后续新文件由上传逻辑自动入库
     *
     * @return 同步结果，包含总数、新增数、跳过数
     */
    Result syncFromR2();
}