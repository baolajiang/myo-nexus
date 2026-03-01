package com.myo.blog.ai.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
/**
 * 資料庫查詢工具類
 * 提供執行自訂 SQL 查詢的功能，適用於需要直接操作資料庫的情況
 */
@Slf4j
@Component
public class DatabaseAiTools {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Tool(description = "執行資料庫查詢的超級工具。當管理員需要統計數據、查詢複雜條件的用戶或文章時，請自行生成MySQL的SELECT語句並調用此工具。")
    public String executeQuery(@ToolParam(description = "要執行的MySQL查詢語句，必須是SELECT語句") String sql) {

        log.info("AI 正在執行自訂 SQL: {}", sql);

        // 1. 基礎安全防禦（防範 AI 產生幻覺執行危險操作）
        String upperSql = sql.trim().toUpperCase();
        if (!upperSql.startsWith("SELECT")) {
            return "系統攔截：為保證數據安全，此工具只能執行 SELECT 查詢語句！";
        }
        if (upperSql.contains("DROP") || upperSql.contains("UPDATE") || upperSql.contains("DELETE") || upperSql.contains("TRUNCATE") || upperSql.contains("INSERT")) {
            return "系統攔截：檢測到高危險的 SQL 關鍵字，拒絕執行！";
        }

        try {
            // 2. 交給 JdbcTemplate 執行查詢
            // queryForList 會把每一行資料變成一個 Map，非常適合處理未知表結構的動態查詢
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

            // 3. 判斷結果並返回給 AI
            if (result.isEmpty()) {
                return "查詢成功，但資料庫中沒有符合條件的資料。";
            }
            // 直接把 List 轉成字串，AI 大模型能輕鬆看懂這種 JSON/Map 格式的結構
            return result.toString();

        } catch (Exception e) {
            // 如果 AI 寫錯了語法，把報錯訊息打回給它，讓它在 Graph 迴圈裡自我反思重試
            return "SQL執行失敗，請檢查語法或表欄位是否正確，錯誤訊息：" + e.getMessage();
        }
    }
}