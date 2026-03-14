package com.myo.blog.task.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String username;

    // 【修复1】密码只保留在内存字段中，不再拼接到命令行，彻底避免 ps aux 泄漏
    @Value("${spring.datasource.password}")
    private String password;

    @Value("${backup.mysqldump.path:mysqldump}")
    private String mysqlDumpPath;

    // 1. 无参方法：供定时任务默认调度使用
    public void run() {
        executeBackup(false, true);
    }

    // 2. 有参方法：供前端"执行一次"时动态传参使用
    public void run(String param) {
        log.info("[数据库备份任务] 手动触发，接收到动态参数：{}", param);

        boolean schemaOnly = false;
        boolean compress = true;

        if (StringUtils.hasText(param)) {
            String trimmedParam = param.trim();
            // 格式校验
            if (!trimmedParam.startsWith("{") || !trimmedParam.endsWith("}")) {
                throw new IllegalArgumentException("参数格式错误：必须是标准 JSON 格式！");
            }

            // 【修复2】使用 Jackson 正规解析 JSON，支持各种空格写法，不再用脆弱的 contains
            try {
                JsonNode node = objectMapper.readTree(trimmedParam);
                if (node.has("schemaOnly")) {
                    schemaOnly = node.get("schemaOnly").asBoolean(false);
                }
                if (node.has("compress")) {
                    compress = node.get("compress").asBoolean(true);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("JSON 参数解析失败：" + e.getMessage());
            }
        }

        executeBackup(schemaOnly, compress);
    }

    // 3. 核心备份逻辑
    private void executeBackup(boolean schemaOnly, boolean compress) {
        log.info("[数据库备份任务] 准备执行... 仅表结构: {}, 是否压缩: {}", schemaOnly, compress);

        // 解析数据库名
        String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
        if (dbName.contains("?")) {
            dbName = dbName.substring(0, dbName.indexOf("?"));
        }

        try {
            // 【修复1-核心】命令行中去掉 -p 参数，改用环境变量 MYSQL_PWD 传递密码
            // 这样在 ps aux / 系统进程列表中完全看不到数据库密码
            List<String> command = new ArrayList<>();
            command.add(mysqlDumpPath);
            command.add("-u" + username);
            // 不再 command.add("-p" + password) ！
            if (schemaOnly) {
                command.add("--no-data"); // 语义更清晰，等同于 -d
            }
            command.add(dbName);

            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // 【修复1-关键】通过环境变量传递密码，mysqldump 会自动读取 MYSQL_PWD
            processBuilder.environment().put("MYSQL_PWD", password);

            // 【修复3】不再合并错误流！分离 stdout(SQL内容) 和 stderr(错误信息)
            // 原来 redirectErrorStream(true) 会把 mysqldump 的错误信息混入 SQL 文件，
            // 导致备份文件损坏且无法还原！
            processBuilder.redirectErrorStream(false);
            Process process = processBuilder.start();

            // 【修复3-关键】用独立线程异步读取 stderr，防止缓冲区满导致进程死锁
            StringBuilder stderrBuilder = new StringBuilder();
            Thread stderrThread = new Thread(() -> {
                try (InputStream errStream = process.getErrorStream()) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = errStream.read(buf)) != -1) {
                        stderrBuilder.append(new String(buf, 0, len));
                    }
                } catch (Exception ignored) {}
            });
            stderrThread.setDaemon(true);
            stderrThread.start();

            // 读取 stdout（纯净的 SQL 内容）
            InputStream inputStream = process.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // 【修复4】buffer 从 1024 提升到 8192，减少 IO 次数，提升大库备份性能
            if (compress) {
                try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        gzipOutputStream.write(buffer, 0, length);
                    }
                }
            } else {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
            }

            // 等待 stderr 读取线程结束，再 waitFor，避免死锁
            stderrThread.join(5000);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String errorMsg = stderrBuilder.toString();
                log.error("[数据库备份任务] mysqldump 执行失败，退出码: {}，错误信息: {}", exitCode, errorMsg);
                throw new RuntimeException("mysqldump 执行失败: " + errorMsg);
            }

            byte[] finalBytes = outputStream.toByteArray();
            if (finalBytes.length == 0) {
                log.error("[数据库备份任务] 导出的 SQL 内容为空，备份中止。");
                return;
            }

            String dateStr = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String ext = compress ? ".sql.gz" : ".sql";
            String prefix = schemaOnly ? "schema_" : "full_";
            String fileName = "backup/database/blog_" + prefix + dateStr + ext;

            boolean isSuccess = r2UploadService.uploadBytes(fileName, finalBytes);
            if (isSuccess) {
                log.info("[数据库备份任务] 成功！文件已上传至 R2: {}，大小: {} KB", fileName, finalBytes.length / 1024);
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