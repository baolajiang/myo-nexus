package com.myo.blog.task.jobs;

import com.myo.blog.utils.R2UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Component("databaseBackupTask")
@RequiredArgsConstructor
public class DatabaseBackupTask {

    private final R2UploadService r2UploadService;

    // 注入数据库配置信息
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${backup.mysqldump.path:mysqldump}")
    private String mysqlDumpPath;

    // 1. 无参方法：供定时任务默认调度使用
    public void run() {
        // 默认执行全量备份，并开启压缩
        executeBackup(false, true);
    }

    // 2. 有参方法：供前端“执行一次”时动态传参使用
    public void run(String param) {
        log.info("[数据库备份任务] 手动触发，接收到动态参数：{}", param);

        // 解析参数（使用简单的字符串匹配，避免引入复杂的 JSON 解析库）
        // 如果参数里带有 schema_only，就只备份表结构
        boolean schemaOnly = StringUtils.hasText(param) && param.contains("schema_only");
        // 除非明确指定 compress: false，否则默认全部开启压缩
        boolean compress = !StringUtils.hasText(param) || !param.contains("\"compress\":false");

        executeBackup(schemaOnly, compress);
    }

    // 3. 核心备份逻辑 ,schemaOnly 是否只备份表结构, compress 是否压缩
    private void executeBackup(boolean schemaOnly, boolean compress) {
        log.info("[数据库备份任务] 准备执行... 仅表结构: {}, 是否压缩: {}", schemaOnly, compress);

        // 解析数据库名
        String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
        if (dbName.contains("?")) {
            dbName = dbName.substring(0, dbName.indexOf("?"));
        }

        try {
            // 组装 mysqldump 命令
            List<String> command = new ArrayList<>();
            command.add(mysqlDumpPath);
            command.add("-u" + username);
            command.add("-p" + password);
            if (schemaOnly) {
                command.add("-d"); // -d 参数表示只导出结构，不导出数据
            }
            command.add(dbName);

            // 使用 ProcessBuilder 替代 Runtime.exec，并合并错误流防止死锁
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            InputStream inputStream = process.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // 在内存中直接进行 GZIP 压缩，不用落盘写文件
            if (compress) {
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    gzipOutputStream.write(buffer, 0, length);
                }
                gzipOutputStream.finish();
                gzipOutputStream.close();
            } else {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
            }

            // 等待命令执行结束
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String errorMsg = outputStream.toString("UTF-8");
                log.error("[数据库备份任务] mysqldump 执行失败，退出码: {}，输出详情: {}", exitCode, errorMsg);
                throw new RuntimeException("mysqldump 执行失败: " + exitCode);
            }

            // 获取最终要上传的字节数组
            byte[] finalBytes = outputStream.toByteArray();

            if (finalBytes.length == 0) {
                log.error("[数据库备份任务] 导出的 SQL 内容为空，备份中止。");
                return;
            }

            // 动态生成 R2 云端的文件名，带上时间戳和状态标识
            String dateStr = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String ext = compress ? ".sql.gz" : ".sql";
            String prefix = schemaOnly ? "schema_" : "full_";
            String fileName = "backup/database/blog_" + prefix + dateStr + ext;

            // 调用 R2UploadService 上传
            boolean isSuccess = r2UploadService.uploadBytes(fileName, finalBytes);

            if (isSuccess) {
                log.info("[数据库备份任务] 成功！文件已上传至 R2: {}，最终文件大小: {} KB", fileName, finalBytes.length / 1024);
            } else {
                log.error("[数据库备份任务] 文件上传 R2 失败。");
                throw new RuntimeException("上传 R2 失败");
            }

        } catch (Exception e) {
            log.error("[数据库备份任务] 备份过程中发生致命异常", e);
            throw new RuntimeException(e);
        }
    }
}