package com.myo.blog.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.dao.mapper.SysUserMapper;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.service.SysUserService;
import com.myo.blog.utils.UserThreadLocal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用戶相關的 AI 工具類
 */
@Slf4j
@Component
public class UserAiTools {

    private final SysUserService sysUserService;
    private final SysUserMapper sysUserMapper;

    public UserAiTools(SysUserService sysUserService, SysUserMapper sysUserMapper) {
        this.sysUserService = sysUserService;
        this.sysUserMapper = sysUserMapper;
    }

    // ================== 核心防越权校验方法 ==================
    // 判断当前用户是否有权操作目标用户
    private String checkLevelPermission(SysUser currentUser, SysUser targetUser, String actionName) {
        if (currentUser == null) return "系统提示：当前未登录，拒绝执行。";
        if (targetUser == null) return null; // 目标不存在时不在这里拦截，交给业务逻辑处理

        // 如果是操作自己，永远允许
        if (currentUser.getId().equals(targetUser.getId())) {
            if ("查询信息".equals(actionName)) {
                return null; // 允许自己查自己的详细信息
            }
            // 只要是查信息以外的操作（封禁、解封、警告），一律拦截！
            return "系统拦截：高危操作！为了系统安全，严禁管理员对自己执行 [" + actionName + "] 操作！";
        }
        // 查询当前用户的最高角色等级
        Integer currentLevel = sysUserService.getHighestRoleLevel(currentUser.getId());
        // 查询目标用户的最高角色等级
        Integer targetLevel = sysUserService.getHighestRoleLevel(targetUser.getId());

        // 如果当前操作者的 level 数字 >= 目标的 level 数字（官阶小于等于目标），拦截！
        if (currentLevel >= targetLevel) {
            return "系统拦截：越权操作！您当前的角色等级无权对级别更高或同级的用户执行 [" + actionName + "] 操作。";
        }
        return null; // 校验通过
    }
    // =======================================================


    // 第 1 個方法：查詢單人 (已修复越权漏洞)
    @Tool(description = "根據賬號(account)查詢詳細用戶資訊")
    public String queryUser(String account) {
        log.info("根據賬號查詢用戶詳細資訊，賬號：{}", account);

        SysUser currentUser = UserThreadLocal.get();
        SysUser targetUser = sysUserService.findUserByAccount(account);
        if (targetUser == null) return "未找到該用戶。";

        // 执行防越权拦截
        String blockMsg = checkLevelPermission(currentUser, targetUser, "查询信息");
        if (blockMsg != null) return blockMsg;
        // 查询该用户的真实角色名（例如：管理员、站长）
        List<String> roleNames = sysUserService.getUserRoleNames(targetUser.getId());
        String roleStr = (roleNames != null && !roleNames.isEmpty()) ? String.join("、", roleNames) : "普通用户";
        // 狀態翻譯邏輯
        String statusText;
        if (targetUser.getStatus() == 0) statusText="正常";
        else if (targetUser.getStatus() == 1) statusText="警告";
        else if (targetUser.getStatus() == 99) statusText="被禁用";
        else statusText="未知狀態";

        targetUser.setPassword("******");
        targetUser.setSalt("******");

        return "查到該用戶資訊如下：\n" +
                "【系統判定該賬號當前狀態為：" + statusText + "】\n" +
                "【该用户的实际角色身份为：" + roleStr + "】\n" +
                "底層詳細數據：" + targetUser.toString();
    }


    // 第 2 個方法：分頁查詢用戶列表（保持之前的修复不变）
    @Tool(description = "查詢系統中的用戶列表資訊。如果用戶未指定，默認查詢第1頁，每頁10條。")
    public String queryAllUsers(
            @ToolParam(description = "你想查詢的頁碼，如果不確定請傳 1") Integer page,
            @ToolParam(description = "每頁展示的數量，最多傳 10") Integer pageSize) {
        log.info("查詢系統中的用戶列表資訊，頁碼：{}，數量：{}", page, pageSize);

        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null) return "系統提示：當前未登錄，拒絕執行。";

        Integer currentLevel = sysUserService.getHighestRoleLevel(currentUser.getId());
        if (currentLevel >= 99) return "系統提示：您當前的角色等級無權查詢後台數據列表。";

        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        if (pageSize > 10) pageSize = 10;

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        //无论什么级别，都在列表里彻底过滤掉自己（不想在管理列表里看到自己
        queryWrapper.ne(SysUser::getId, currentUser.getId());
        if (currentLevel > 1) {
            String excludeSql = "SELECT ur.user_id FROM myo_sys_user_role ur " +
                    "INNER JOIN myo_sys_role r ON ur.role_id = r.id " +
                    "WHERE r.role_level <= " + currentLevel +
                    " AND ur.user_id != '" + currentUser.getId() + "'";
            queryWrapper.notInSql(SysUser::getId, excludeSql);
        }

        Page<SysUser> pageParam = new Page<>(page, pageSize);
        Page<SysUser> userPage = sysUserMapper.selectPage(pageParam, queryWrapper);

        List<SysUser> userList = userPage.getRecords();
        if (userList == null || userList.isEmpty()) return "當前頁沒有任何用戶數據。";

        StringBuilder aiReport = new StringBuilder("查詢成功，以下是系統中的用戶列表（第" + page + "頁數據）：\n");
        for (SysUser user : userList) {
            aiReport.append("- 賬號: ").append(user.getAccount())
                    .append(" | 狀態: ").append(user.getStatus() == 0 ? "正常" : user.getStatus() == 1 ? "警告" : user.getStatus() == 99 ? "被禁用" : "未知狀態")
                    .append("\n");
        }
        return aiReport.toString();
    }

    // 第 3 個方法：封禁單人
    @Tool(description = "根據賬號(account)對目標用戶執行封禁操作")
    public String disableUser(String account) {
        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null || !sysUserService.hasPermission(currentUser.getId(), "user:status")) {
            return "操作失敗：系統拒絕執行。原因：當前操作者沒有 [修改用戶狀態] 的權限，請委婉地告知用戶。";
        }

        SysUser targetUser = sysUserService.findUserByAccount(account);
        if (targetUser == null) return "封禁失敗：未找到該賬號";

        // 执行防越权拦截（防止下级封禁上级）
        String blockMsg = checkLevelPermission(currentUser, targetUser, "封禁");
        if (blockMsg != null) return blockMsg;

        targetUser.setStatus(99);
        boolean success = sysUserService.updateById(targetUser);
        return success ? "已成功在底層資料庫將該賬號封禁。" : "資料庫更新異常。";
    }

    // 第 4 個方法：解封單人
    @Tool(description = "根據賬號(account)對目標用戶執行解封操作")
    public String enableUser(String account) {
        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null || !sysUserService.hasPermission(currentUser.getId(), "user:status")) {
            return "操作失敗：系統拒絕執行。";
        }
        // 获取目标用户
        SysUser targetUser = sysUserService.findUserByAccount(account);
        if (targetUser == null) return "解封失敗：未找到該賬號";
        // 执行防越权拦截（防止下级解封上级）
        String blockMsg = checkLevelPermission(currentUser, targetUser, "解封");
        if (blockMsg != null) return blockMsg;

        targetUser.setStatus(0);
        boolean success = sysUserService.updateById(targetUser);
        return success ? "已成功在底層資料庫將該賬號解封。" : "資料庫更新異常。";
    }

    // 第 5個方法：警告單人 (已修复越权漏洞)
    @Tool(description = "根據賬號(account)對目標用戶執行警告操作")
    public String warnUser(String account) {
        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null || !sysUserService.hasPermission(currentUser.getId(), "user:status")) {
            return "操作失敗：系統拒絕執行。";
        }

        SysUser targetUser = sysUserService.findUserByAccount(account);
        if (targetUser == null) return "警告失敗：未找到該賬號";

        // 执行防越权拦截
        String blockMsg = checkLevelPermission(currentUser, targetUser, "警告");
        if (blockMsg != null) return blockMsg;

        targetUser.setStatus(1);
        boolean success = sysUserService.updateById(targetUser);
        return success ? "已成功在底層資料庫將該賬號警告。" : "資料庫更新異常。";
    }
}