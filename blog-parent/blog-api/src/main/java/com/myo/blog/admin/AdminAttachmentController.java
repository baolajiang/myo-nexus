package com.myo.blog.admin;

import com.myo.blog.common.aop.LogAnnotation;
import com.myo.blog.common.aop.RequirePermission;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 附件管理后台接口
 * 提供附件列表查询和删除功能
 * 所有接口均需要管理员权限
 */
@RestController
@RequestMapping("/admin/attachment")
@RequiredArgsConstructor
public class AdminAttachmentController {

    // 引用附件管理 Service
    private final AttachmentService attachmentService;

    /**
     * 分页查询附件列表
     * 支持按文件分类（image/log/backup/other）筛选
     * 支持按文件名关键词搜索
     *
     * @param pageParams 分页参数（page、pageSize、keyword）
     * @param fileType   文件分类筛选，不传则查全部
     */
    @PostMapping("/list")
    @RequirePermission("attachment:list")
    public Result list(@RequestBody PageParams pageParams,
                       @RequestParam(value = "fileType", required = false) String fileType) {
        return attachmentService.listAttachment(pageParams, fileType);
    }

    /**
     * 删除附件（同步删除 R2 文件和数据库记录，先删 R2 再删数据库）
     *
     * @param id 附件ID
     */
    @PostMapping("/delete/{id}")
    @RequirePermission("attachment:delete")
    @LogAnnotation(module = "附件管理", operator = "删除附件")
    public Result delete(@PathVariable("id") String id) {
        return attachmentService.deleteAttachment(id);
    }

    /**
     * 一键同步 R2 历史文件到数据库，仅需调用一次
     * 扫描 R2 存储桶中的所有文件，将未入库的历史文件批量写入 myo_attachment 表
     * 已存在的文件（根据 fileKey 判断）自动跳过，可重复调用，不会产生重复数据
     * 首次部署后执行一次即可，后续新上传的文件由上传逻辑自动入库
     * 使用方法：http://localhost:48882/admin/attachment/sync
     *
     */
    @PostMapping("/sync")
    @RequirePermission("attachment:delete")
    @LogAnnotation(module = "附件管理", operator = "同步R2历史文件")
    public Result sync() {
        return attachmentService.syncFromR2();
    }
}