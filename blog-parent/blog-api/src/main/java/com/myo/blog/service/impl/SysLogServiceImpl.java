package com.myo.blog.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.dao.mapper.SysLogMapper;
import com.myo.blog.dao.pojo.SysLog;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.service.AttachmentService;
import com.myo.blog.service.SysLogService;
import com.myo.blog.utils.R2UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SysLogServiceImpl implements SysLogService {

    private final SysLogMapper sysLogMapper;
    private final R2UploadService r2UploadService;
    private final AttachmentService attachmentService;

    //  注入独立的 DbDeleter Bean，解决 @Transactional 自调用失效问题
    private final SysLogDbDeleter sysLogDbDeleter;

    private static final int BATCH_SIZE = 1000;

    @Value("${r2.domain}")
    private String domain;

    @Override
    public Result listLog(PageParams pageParams) {
        Page<SysLog> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());
        LambdaQueryWrapper<SysLog> queryWrapper = new LambdaQueryWrapper<>();

        if (pageParams.getStatus() != null) {
            queryWrapper.eq(SysLog::getStatus, pageParams.getStatus());
        }
        if (StringUtils.isNotBlank(pageParams.getModule())) {
            queryWrapper.like(SysLog::getModule, pageParams.getModule());
        }
        if (StringUtils.isNotBlank(pageParams.getNickname())) {
            queryWrapper.like(SysLog::getNickname, pageParams.getNickname());
        }
        if (StringUtils.isNotBlank(pageParams.getTraceId())) {
            queryWrapper.eq(SysLog::getTraceId, pageParams.getTraceId());
        }
        if (StringUtils.isNotBlank(pageParams.getKeyword())) {
            String kw = pageParams.getKeyword();
            queryWrapper.and(q -> q.like(SysLog::getModule, kw)
                    .or().like(SysLog::getNickname, kw)
                    .or().like(SysLog::getTraceId, kw)
                    .or().like(SysLog::getOperation, kw));
        }

        queryWrapper.orderByDesc(SysLog::getCreateDate);
        Page<SysLog> logPage = sysLogMapper.selectPage(page, queryWrapper);
        return Result.success(logPage);
    }


    /**
     备份清理主流程（backupAndCleanLogs）
     用游标分页驱动整个任务：
     lastId = 0
     while(true):
     查询 id > lastId 且 createDate < 30天前 的数据，取 1000 条
     如果没数据 → 结束
     调用 processSafeBackup 处理这批
     成功 → lastId 推进到这批最后一条的 id，继续下一批
     失败 → 记录日志，终止任务（等明天凌晨重试）
     为什么用游标而不是普通分页（page=1,2,3...）？因为删掉第一批之后，第二页的数据会往前移，用页码会漏数据。
     用 id > lastId 就不会有这个问题。
     */
    @Override
    public void backupAndCleanLogs(Long expireTime) {
        // 改用游标分页，用 lastId 代替 totalCount 计算页数
        // 好处：① 不受任务执行期间新增数据影响；② 跳过失败批次后不会重复处理
        long lastId = 0L;
        int batchIndex = 1;

        while (true) {
            LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.lt(SysLog::getCreateDate, expireTime)
                    .gt(SysLog::getId, lastId)           // 游标：只取上次处理之后的数据
                    .orderByAsc(SysLog::getId)            // 必须按 id 升序，保证游标稳定
                    .last("LIMIT " + BATCH_SIZE);

            List<SysLog> records = sysLogMapper.selectList(wrapper);
            if (records.isEmpty()) break;

            log.info("[归档任务] 开始处理批次 {}，本批数量：{}", batchIndex, records.size());

            boolean success = processSafeBackup(records, batchIndex);

            if (success) {
                // 只有成功才推进游标，失败则停止（避免无限重试同一批次）
                lastId = records.get(records.size() - 1).getId();
            } else {
                log.error("[归档任务] 批次 {} 处理失败，任务终止，下次从 id={} 继续", batchIndex, lastId);
                break;
            }

            batchIndex++;
        }

        log.info("[归档任务] 全部完成，共处理 {} 批次", batchIndex - 1);
    }

    /**
     单批处理（processSafeBackup）
     这是最核心的安全逻辑，顺序非常重要：
     ① 把这 1000 条序列化成 JSON bytes
     ② 上传到 R2（文件名：backup/yyyy-MM/logs_batch_N_随机码.json）
     ③ 上传成功？
     是 → 走事务删除数据库记录
     否 → 直接返回 false，数据库记录保留不动
     先备份、后删除，确保数据不会丢。如果上传失败，数据库里的数据完整保留，等下次任务重新处理。
     */
    private boolean processSafeBackup(List<SysLog> records, int index) {
        try {
            byte[] data = JSON.toJSONString(records).getBytes(StandardCharsets.UTF_8);
            String fileName = String.format("backup/%s/logs_batch_%d_%s.json",
                    new SimpleDateFormat("yyyy-MM").format(new Date()),
                    index,
                    UUID.randomUUID().toString().substring(0, 8));

            // 1. 先上传（在事务外，避免长事务占用连接）
            boolean isUploaded = r2UploadService.uploadBytes(fileName, data);

            if (isUploaded) {
                // 2. 上传成功后，通过代理 Bean 调用事务方法，确保 @Transactional 真正生效
                sysLogDbDeleter.deleteByIds(records);
                log.info("[归档任务] 批次 {} 备份并清理成功", index);

                // 3. 写入附件管理表
                String fileDisplayName = fileName.substring(fileName.lastIndexOf("/") + 1);
                String fileUrl = (domain.endsWith("/") ? domain : domain + "/") + fileName;
                attachmentService.save(
                        fileDisplayName,      // 原始文件名
                        fileName,             // R2 存储路径（fileKey）
                        fileUrl,              // 完整访问 URL
                        (long) data.length,   // 文件大小（字节）
                        "log",                // 文件分类
                        "application/json",   // MIME 类型
                        "SYSTEM",             // 上传者
                        "日志归档清理任务"      // 备注
                );
                return true;
            } else {
                log.error("[归档任务] 批次 {} 上传至 R2 失败，已跳过本地删除", index);
                return false;
            }
        } catch (Exception e) {
            log.error("[归档任务] 处理批次 {} 时发生异常", index, e);
            return false;
        }
    }


    // =========================================================
    //  独立的 Bean，专门负责带事务的数据库删除
    //    放在同一文件内（静态内部类），保持代码内聚，不用新建文件
    // =========================================================
    @Service
    @RequiredArgsConstructor
    public static class SysLogDbDeleter {

        private final SysLogMapper sysLogMapper;

        /**
         * 带事务的批量删除，必须通过 Spring 代理调用才能生效
         */
        @Transactional(rollbackFor = Exception.class)
        public void deleteByIds(List<SysLog> records) {
            List<Long> ids = records.stream()
                    .map(SysLog::getId)
                    .collect(Collectors.toList());

            sysLogMapper.delete(new LambdaQueryWrapper<SysLog>().in(SysLog::getId, ids));
        }
    }

    @Override
    public Result exportLogToR2(PageParams pageParams) {
        // 1. 构建查询条件
        LambdaQueryWrapper<SysLog> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(pageParams.getModule())) {
            queryWrapper.eq(SysLog::getModule, pageParams.getModule());
        }
        if (StringUtils.isNotBlank(pageParams.getNickname())) {
            queryWrapper.eq(SysLog::getNickname, pageParams.getNickname());
        }
        if (pageParams.getStatus() != null) {
            queryWrapper.eq(SysLog::getStatus, pageParams.getStatus());
        }
        if (StringUtils.isNotBlank(pageParams.getTraceId())) {
            queryWrapper.eq(SysLog::getTraceId, pageParams.getTraceId());
        }

        // 按时间倒序，并限制最大导出条数为 5000 条，防止内存溢出
        queryWrapper.orderByDesc(SysLog::getCreateDate);
        queryWrapper.last("limit 5000");

        // 2. 从数据库查出日志列表
        List<SysLog> logList = sysLogMapper.selectList(queryWrapper);

        if (logList.isEmpty()) {
            return Result.fail(20001, "当前条件下没有可导出的日志");
        }

        // 3. 将对象转为 JSON 字符串，然后直接转为 byte 数组
        String jsonString = JSON.toJSONString(logList);
        byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);

        // 4. 生成 R2 云端的文件名，带上时间戳防止覆盖
        String dateStr = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "backup/syslog/log_export_" + dateStr + ".json";

        // 5. 调用你已有的 uploadBytes 方法进行上传
        boolean isSuccess = r2UploadService.uploadBytes(fileName, bytes);

        if (isSuccess) {
            // 6. 拼接完整的访问 URL 返回给前端
            String fileUrl = domain.endsWith("/") ? domain + fileName : domain + "/" + fileName;

            // 7. 写入附件管理表
            String fileDisplayName = fileName.substring(fileName.lastIndexOf("/") + 1);
            attachmentService.save(
                    fileDisplayName,      // 原始文件名
                    fileName,             // R2 存储路径（fileKey）
                    fileUrl,              // 完整访问 URL
                    (long) bytes.length,  // 文件大小（字节）
                    "log",                // 文件分类
                    "application/json",   // MIME 类型
                    "SYSTEM",             // 上传者
                    "手动导出日志"          // 备注
            );
            return Result.success(fileUrl);
        } else {
            return Result.fail(50000, "日志导出到R2失败，请查看后台运行日志");
        }
    }
}