package com.myo.blog.ai.tools;

import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.utils.UserThreadLocal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseAiTools {


    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, String> TABLE_PERMISSION_MAP = new HashMap<>();

    static {
        // 文章列表
        TABLE_PERMISSION_MAP.put("myo_article",             "article:list");
        TABLE_PERMISSION_MAP.put("myo_article_tag",         "article:list");
        TABLE_PERMISSION_MAP.put("myo_category",            "article:list");
        TABLE_PERMISSION_MAP.put("myo_tag",                 "article:list");
        // 文章正文（需要 article:content 权限）
        TABLE_PERMISSION_MAP.put("myo_article_body",        "article:content");
        // 评论
        TABLE_PERMISSION_MAP.put("myo_comment",             "comment:list");
        // 用户列表
        TABLE_PERMISSION_MAP.put("myo_sys_user",            "user:list");
        TABLE_PERMISSION_MAP.put("myo_sys_user_role",       "user:list");
        // 用户详情（角色权限信息）
        TABLE_PERMISSION_MAP.put("myo_sys_role",            "user:info");
        TABLE_PERMISSION_MAP.put("myo_sys_permission",      "user:info");
        TABLE_PERMISSION_MAP.put("myo_sys_role_permission", "user:info");
        // 基础运营
        TABLE_PERMISSION_MAP.put("myo_link",                "base:list");
    }

    @Tool(description = "执行数据库查询的超级工具。当管理员需要统计数据、查询复杂条件的用户或文章时，请自行生成MySQL的SELECT语句并调用此工具。")
    public String executeQuery(@ToolParam(description = "要执行的MySQL查询语句，必须是SELECT语句") String sql) {

        log.info("AI 执行 SQL: {}", sql);

        // ===== 第一层：语法解析，只允许 SELECT =====
        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                return "系统拦截：只允许执行 SELECT 查询语句！";
            }
        } catch (Exception e) {
            return "SQL 语法解析失败，请检查语句是否正确：" + e.getMessage();
        }

        // ===== 第二层：表名解析 -> 权限校验 =====
        try {
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableNames = tablesNamesFinder.getTableList(statement);

            Set<String> requiredCodes = new HashSet<>();
            for (String tableName : tableNames) {
                String code = TABLE_PERMISSION_MAP.get(tableName.toLowerCase());
                if (code != null) {
                    requiredCodes.add(code);
                }
            }

            if (!requiredCodes.isEmpty()) {
                SysUser currentUser = UserThreadLocal.get();
                if (currentUser == null) {
                    return "无法获取当前登录用户信息，请重新登录后再试。";
                }
                String userId = currentUser.getId();

                for (String code : requiredCodes) {
                    if (!hasPermission(userId, code)) {
                        log.warn("用户 [{}] 缺少权限：{}", userId, code);
                        return "您当前身份没有该查询权限哦，如有需要请联系站长开通～";
                    }
                }
            }
        } catch (Exception e) {
            log.error("权限校验异常", e);
            return "权限校验异常，请稍后再试。";
        }

        // ===== 第三层：LIMIT 强制上限 =====
        String safeSql = appendLimitIfAbsent(sql, 50);

        // ===== 执行查询 =====
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(safeSql);
            if (result.isEmpty()) {
                return "查询成功，但没有符合条件的数据。";
            }
            return result.toString();
        } catch (Exception e) {
            return "SQL 执行失败，错误信息：" + e.getMessage();
        }
    }

    private boolean hasPermission(String userId, String permissionCode) {
        String sql = "SELECT COUNT(1) " +
                "FROM myo_sys_user_role ur " +
                "JOIN myo_sys_role_permission rp ON ur.role_id = rp.role_id " +
                "JOIN myo_sys_permission p ON rp.permission_id = p.id " +
                "WHERE ur.user_id = ? AND p.code = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, permissionCode);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("查询权限失败，userId={}, code={}", userId, permissionCode, e);
            return false;
        }
    }

    private String appendLimitIfAbsent(String sql, int maxRows) {
        try {
            Select select = (Select) CCJSqlParserUtil.parse(sql);
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
            Limit limit = plainSelect.getLimit();
            if (limit == null) {
                limit = new Limit();
                limit.setRowCount(new LongValue(maxRows));
                plainSelect.setLimit(limit);
            } else {
                long rowCount = ((LongValue) limit.getRowCount()).getValue();
                if (rowCount > maxRows) {
                    limit.setRowCount(new LongValue(maxRows));
                }
            }
            return select.toString();
        } catch (Exception e) {
            return sql.trim() + " LIMIT " + maxRows;
        }
    }
}