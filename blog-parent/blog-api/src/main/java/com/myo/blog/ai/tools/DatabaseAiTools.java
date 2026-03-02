package com.myo.blog.ai.tools;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 数据库查询 AI 工具
 * 暴露给 Spring AI 大模型调用，AI 自动生成 SQL 并通过此工具查询数据库
 */
@Slf4j
@Component
public class DatabaseAiTools {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * AI 可调用的数据库查询工具
     * @Tool 的 description 是给大模型看的，AI 根据它判断何时调用此方法
     */
    @Tool(description = "执行数据库查询的超级工具。当管理员需要统计数据、查询复杂条件的用户或文章时，请自行生成MySQL的SELECT语句并调用此工具。")
    public String executeQuery(@ToolParam(description = "要执行的MySQL查询语句，必须是SELECT语句") String sql) {

        log.info("AI 执行 SQL: {}", sql);

        // 【安全校验】使用 JSqlParser 从语法层面解析 SQL
        // 与 contains() 字符串匹配相比，能有效防止多语句注入和关键字混淆绕过
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                return "系统拦截：只允许执行 SELECT 查询语句！";
            }
        } catch (Exception e) {
            // 语法解析失败，说明 SQL 本身有误，返回错误让 AI 自行修正后重试
            return "SQL 语法解析失败，请检查语句是否正确：" + e.getMessage();
        }

        // 【限流保护】强制限制查询结果行数，防止 AI 全表扫描返回海量数据
        String safeSql = appendLimitIfAbsent(sql, 50);

        try {
            // queryForList 将每行映射为 Map<列名, 值>，适合处理表结构未知的动态查询
            List<Map<String, Object>> result = jdbcTemplate.queryForList(safeSql);

            if (result.isEmpty()) {
                return "查询成功，但没有符合条件的数据。";
            }

            // 大模型可直接理解 Map 格式的结构化数据，无需额外转换
            return result.toString();

        } catch (Exception e) {
            // 执行失败时将错误信息返回给 AI，使其在 Agent 循环中自我反思并重新生成 SQL
            return "SQL 执行失败，错误信息：" + e.getMessage();
        }
    }

    /**
     * 强制保证 SQL 的 LIMIT 不超过 maxRows
     * - 没有 LIMIT：直接设置为 maxRows
     * - 有 LIMIT 但超过上限：强制覆盖为 maxRows
     * - 解析失败兜底：直接字符串拼接
     */
    private String appendLimitIfAbsent(String sql, int maxRows) {
        try {
            Select select = (Select) CCJSqlParserUtil.parse(sql);
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

            Limit limit = plainSelect.getLimit();
            if (limit == null) {
                // 没有 LIMIT，新建并设置上限
                limit = new Limit();
                limit.setRowCount(new LongValue(maxRows));
                plainSelect.setLimit(limit);
            } else {
                // 有 LIMIT，若超过上限则强制覆盖，防止 AI 绕过限制
                long rowCount = ((LongValue) limit.getRowCount()).getValue();
                if (rowCount > maxRows) {
                    limit.setRowCount(new LongValue(maxRows));
                }
            }

            return select.toString();
        } catch (Exception e) {
            // 极端情况下解析失败，兜底直接拼接
            return sql.trim() + " LIMIT " + maxRows;
        }
    }
}