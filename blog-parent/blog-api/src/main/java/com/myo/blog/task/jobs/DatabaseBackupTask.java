package com.myo.blog.task.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myo.blog.service.AttachmentService;
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

/**
 * 数据库备份任务
 *
 * 调用系统的 mysqldump 命令导出数据库，将备份文件上传到 Cloudflare R2 对象存储。
 * 支持全量备份和仅备份表结构两种模式，支持 GZIP 压缩以减小文件体积。
 * 上传成功后将文件元数据写入 myo_attachment 表，方便后台统一管理。
 *
 * 安全说明：数据库密码通过环境变量 MYSQL_PWD 传递给 mysqldump，
 * 不拼接到命令行参数中，防止密码在系统进程列表（ps aux）中泄漏。
 */
@Slf4j
@Component("databaseBackupTask")
@RequiredArgsConstructor
public class DatabaseBackupTask {

    // 引用 R2 上传服务，用于将备份文件上传到 Cloudflare R2 对象存储
    private final R2UploadService r2UploadService;

    // 引用附件管理 Service，备份文件上传成功后将元数据写入 myo_attachment 表
    private final AttachmentService attachmentService;

    // Jackson JSON 解析器，用于解析前端传入的 JSON 参数
    // 提升为类成员变量，避免每次任务执行都重新创建，节省资源
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 数据库连接 URL，从配置文件读取，用于截取数据库名称
    // 例如 jdbc:mysql://localhost:3306/blog?xxx → 截取出 blog
    @Value("${spring.datasource.url}")
    private String dbUrl;

    // 数据库用户名，作为 mysqldump 的 -u 参数
    @Value("${spring.datasource.username}")
    private String username;

    // 数据库密码，只保留在内存中，通过环境变量 MYSQL_PWD 传递给 mysqldump
    // 不拼接到命令行，防止密码在 ps aux 进程列表中明文暴露
    @Value("${spring.datasource.password}")
    private String password;

    // mysqldump 可执行文件路径，默认为系统 PATH 中的 mysqldump
    // 如果服务器上 mysqldump 不在默认路径，可在配置文件中指定绝对路径
    // 例如：backup.mysqldump.path=/usr/bin/mysqldump
    @Value("${backup.mysqldump.path:mysqldump}")
    private String mysqlDumpPath;

    // R2 访问域名，用于拼接文件的完整访问 URL
    // 例如：https://cos.myo.pub
    @Value("${r2.domain}")
    private String r2Domain;

    /**
     * 无参方法：供定时任务自动调度使用
     * 默认执行全量备份（包含表结构和数据），并开启 GZIP 压缩
     */
    public void run() {
        executeBackup(false, true);
    }

    /**
     * 有参方法：数据库中配置了 taskParam 时调用此方法（自动调度和手动执行都可能走这里）
     *
     * 支持两个 JSON 参数：
     * - schemaOnly：是否只备份表结构（不含数据），默认 false
     * - compress：是否开启 GZIP 压缩，默认 true
     *
     * 示例参数：{"schemaOnly": true, "compress": false}
     *
     * @param param JSON 格式的参数字符串
     */
    public void run(String param) {
        log.info("[数据库备份任务] 接收到动态参数：{}", param);

        boolean schemaOnly = false;
        boolean compress = true;

        if (StringUtils.hasText(param)) {
            String trimmedParam = param.trim();
            if (!trimmedParam.startsWith("{") || !trimmedParam.endsWith("}")) {
                throw new IllegalArgumentException("参数格式错误：必须是标准 JSON 格式！");
            }
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

    /**
     * 核心备份逻辑：执行 mysqldump → 读取输出 → 可选压缩 → 上传 R2 → 入库
     *
     * 执行流程：
     * 1. 从 JDBC URL 截取数据库名
     * 2. 组装 mysqldump 命令（密码通过环境变量传递，不拼入命令行）
     * 3. 启动独立守护线程消费 stderr，防止缓冲区满导致进程死锁
     * 4. 读取 stdout 中的 SQL 内容，可选 GZIP 压缩后存入内存
     * 5. 等待 stderr 线程结束，再获取进程退出码
     * 6. 上传到 R2，成功后将文件元数据写入 myo_attachment 表
     *
     * @param schemaOnly 是否只备份表结构，true = 只导出 DDL，false = 导出完整数据
     * @param compress   是否对备份文件进行 GZIP 压缩，压缩后体积通常减少 80% 以上
     */
    private void executeBackup(boolean schemaOnly, boolean compress) {
        log.info("[数据库备份任务] 准备执行... 仅表结构: {}, 是否压缩: {}", schemaOnly, compress);

        // 从 JDBC URL 中截取数据库名，例如 jdbc:mysql://host:3306/blog?xxx → blog
        String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
        if (dbName.contains("?")) {
            dbName = dbName.substring(0, dbName.indexOf("?"));
        }

        try {
            // 组装 mysqldump 命令行参数列表
            List<String> command = new ArrayList<>();
            command.add(mysqlDumpPath);
            command.add("-u" + username);
            // 密码不加入命令行，通过下方的环境变量 MYSQL_PWD 传递，防止 ps aux 泄漏
            if (schemaOnly) {
                // --no-data：只导出表结构（DDL），不导出数据，等同于 -d 参数
                command.add("--no-data");
            }
            command.add(dbName);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 通过环境变量传递密码，mysqldump 会自动读取 MYSQL_PWD，无需 -p 参数
            processBuilder.environment().put("MYSQL_PWD", password);
            // 不合并错误流，保持 stdout（SQL 内容）和 stderr（错误信息）分离
            // 若合并，mysqldump 的警告/错误信息会混入 SQL 文件，导致备份文件损坏无法还原
            processBuilder.redirectErrorStream(false);
            Process process = processBuilder.start();

            // 用独立守护线程异步消费 stderr 缓冲区，防止缓冲区写满后进程阻塞（死锁）
            // 守护线程特性：主线程结束时自动销毁，不会阻止 JVM 退出
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

            InputStream inputStream = process.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            if (compress) {
                // 开启压缩：通过 GZIPOutputStream 边读边压缩，在内存中完成，不落盘
                // 压缩后体积通常减少 80% 以上，大幅降低 R2 存储成本
                try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
                    byte[] buffer = new byte[8192]; // 8KB 缓冲区，减少 IO 次数
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        gzipOutputStream.write(buffer, 0, length);
                    }
                }
            } else {
                // 不压缩：直接读取原始 SQL 内容到内存
                byte[] buffer = new byte[8192];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
            }

            // 等待 stderr 读取线程结束（最多 5 秒），再调用 waitFor 获取进程退出码
            // 顺序不能颠倒：必须先消费完 stderr，再 waitFor
            // 原因：若先 waitFor，stderr 缓冲区可能未读完，导致进程阻塞，waitFor 永远不返回（死锁）
            stderrThread.join(5000);
            int exitCode = process.waitFor();

            // 退出码非 0 表示 mysqldump 执行失败，打印 stderr 中的错误信息便于排查
            if (exitCode != 0) {
                String errorMsg = stderrBuilder.toString();
                log.error("[数据库备份任务] mysqldump 执行失败，退出码: {}，错误信息: {}", exitCode, errorMsg);
                throw new RuntimeException("mysqldump 执行失败: " + errorMsg);
            }

            byte[] finalBytes = outputStream.toByteArray();
            if (finalBytes.length == 0) {
                // 导出内容为空，可能是数据库名错误或 mysqldump 权限不足，放弃上传
                log.error("[数据库备份任务] 导出的 SQL 内容为空，备份中止。");
                return;
            }

            // 生成带时间戳的文件名，格式：backup/database/blog_full_20260315_120000.sql.gz
            // prefix 区分全量备份（full_）和仅表结构备份（schema_）
            // ext 区分压缩（.sql.gz）和不压缩（.sql）
            String dateStr = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String ext = compress ? ".sql.gz" : ".sql";
            String prefix = schemaOnly ? "schema_" : "full_";
            String fileName = "backup/database/blog_" + prefix + dateStr + ext;
            // fileDisplayName 是不含路径的纯文件名，存入数据库 file_name 字段，便于展示
            String fileDisplayName = "blog_" + prefix + dateStr + ext;

            // 上传到 Cloudflare R2 对象存储
            boolean isSuccess = r2UploadService.uploadBytes(fileName, finalBytes);
            if (isSuccess) {
                log.info("[数据库备份任务] 成功！文件已上传至 R2: {}，大小: {} KB",
                        fileName, finalBytes.length / 1024);

                // 上传成功后将文件元数据写入 myo_attachment 表
                // 便于在后台附件管理页面统一查看、管理和删除备份文件
                String fileUrl = (r2Domain.endsWith("/") ? r2Domain : r2Domain + "/") + fileName;
                attachmentService.save(
                        fileDisplayName,                                    // 原始文件名（不含路径）
                        fileName,                                           // R2 存储路径（fileKey），删除时用
                        fileUrl,                                            // 完整访问 URL，前端展示用
                        (long) finalBytes.length,                           // 文件大小（字节）
                        "backup",                                           // 文件分类
                        compress ? "application/gzip" : "application/sql", // MIME 类型
                        "SYSTEM",                                           // 上传者，定时任务统一填 SYSTEM
                        "数据库备份任务 - " + (schemaOnly ? "仅表结构" : "全量备份") // 备注，说明本次备份类型
                );
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