package com.myo.blog.task.jobs;

import com.myo.blog.utils.R2UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
// 数据库备份任务，用于定时备份数据库到 R2 云端
//这个任务的逻辑是：读取 MySQL 账号密码
// 利用 Java 执行 mysqldump 命令，
// 把所有数据导出成一个 SQL 文本，
// 然后直接转成 byte 数组调用 R2UploadService 传到云端。
@Slf4j
@Component
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

    public void run() {
        log.info("[数据库备份任务] 开始执行备份...");

        String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
        if (dbName.contains("?")) {
            dbName = dbName.substring(0, dbName.indexOf("?"));
        }

        try {
            String[] cmdArray = new String[]{
                    mysqlDumpPath,
                    "-u" + username,
                    "-p" + password,
                    dbName
            };
            Process process = Runtime.getRuntime().exec(cmdArray);
            InputStream inputStream = process.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("[数据库备份任务] mysqldump 执行失败，退出码: {}", exitCode);
                throw new RuntimeException("mysqldump 执行失败");
            }

            // 3. 获取导出的 SQL 字节数组
            byte[] sqlBytes = outputStream.toByteArray();

            if (sqlBytes.length == 0) {
                log.error("[数据库备份任务] 导出的 SQL 内容为空，备份中止。");
                return;
            }

            // 4. 生成 R2 云端的文件名，带上时间戳
            String dateStr = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "backup/database/blog_backup_" + dateStr + ".sql";

            // 5. 调用 R2UploadService 上传
            boolean isSuccess = r2UploadService.uploadBytes(fileName, sqlBytes);

            if (isSuccess) {
                log.info("[数据库备份任务] 备份成功！文件已上传至 R2: {}", fileName);
            } else {
                log.error("[数据库备份任务] 文件上传 R2 失败。");
                throw new RuntimeException("上传 R2 失败");
            }

        } catch (Exception e) {
            log.error("[数据库备份任务] 备份过程中发生异常", e);
            throw new RuntimeException(e);
        }
    }
}