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
 * 用户管理 AI 工具类
 * 所有操作均需校验：
 *   1. 是否登录
 *   2. 是否拥有对应权限 code（通过 myo_sys_role_permission 三表联查）
 *   3. 是否越权（不能操作同级或更高级别的用户）
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

    // ==================== 核心防越权校验 ====================

    /**
     * 校验当前用户是否有权对目标用户执行指定操作
     * 规则：
     *   - 未登录直接拒绝
     *   - 禁止对自己执行封禁/解封/警告等高危操作
     *   - 只能操作角色等级低于自己的用户（role_level 数字越小权限越大）
     *
     * @return null 表示校验通过，非 null 表示拦截原因
     */
    private String checkLevelPermission(SysUser currentUser, SysUser targetUser, String actionName) {
        if (currentUser == null) return "系统提示：当前未登录，拒绝执行。";
        if (targetUser == null) return null;

        // 禁止操作自己（查询自己除外）
        if (currentUser.getId().equals(targetUser.getId())) {
            if ("查询信息".equals(actionName)) return null;
            return "系统拦截：高危操作！严禁对自己执行 [" + actionName + "] 操作！";
        }

        // 获取双方最高角色等级（数字越小权限越大）
        Integer currentLevel = sysUserService.getHighestRoleLevel(currentUser.getId());
        Integer targetLevel  = sysUserService.getHighestRoleLevel(targetUser.getId());

        // 当前用户等级数字 >= 目标用户，说明目标权限不低于自己，拦截
        if (currentLevel >= targetLevel) {
            return "系统拦截：越权操作！您无权对同级或更高级别的用户执行 [" + actionName + "] 操作。";
        }
        return null;
    }

    // ==================== 查询类工具 ====================

    /**
     * 根据账号查询单个用户详细信息
     * 需要权限：user:info
     */
    @Tool(description = "根据账号(account)查询单个用户的详细信息，包括状态、角色身份等")
    public String queryUser(String account) {
        log.info("查询单个用户详细信息，账号：{}", account);

        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null) return "系统提示：当前未登录，拒绝执行。";

        if (!sysUserService.hasPermission(currentUser.getId(), "user:info")) {
            return "操作失败：您没有查看用户详情的权限。";
        }

        SysUser targetUser = sysUserService.findUserByAccount(account);
        if (targetUser == null) return "未找到该用户。";

        String blockMsg = checkLevelPermission(currentUser, targetUser, "查询信息");
        if (blockMsg != null) return blockMsg;

        List<String> roleNames = sysUserService.getUserRoleNames(targetUser.getId());
        String roleStr = (roleNames != null && !roleNames.isEmpty()) ? String.join("、", roleNames) : "普通用户";

        String statusText = switch (targetUser.getStatus()) {
            case 0  -> "正常";
            case 1  -> "警告";
            case 99 -> "被封禁";
            default -> "未知状态";
        };

        targetUser.setPassword("******");
        targetUser.setSalt("******");

        return "查到该用户信息如下：\n" +
                "【账号状态：" + statusText + "】\n" +
                "【角色身份：" + roleStr + "】\n" +
                "详细数据：" + targetUser;
    }

    /**
     * 分页查询用户列表
     * 需要权限：user:list
     * 规则：只能看到比自己权限低的用户，不显示自己
     */
    @Tool(description = "分页查询系统中的用户列表。默认第1页，每页10条。")
    public String queryAllUsers(
            @ToolParam(description = "页码，不确定传 1") Integer page,
            @ToolParam(description = "每页数量，最多10条") Integer pageSize) {
        log.info("查询用户列表，页码：{}，数量：{}", page, pageSize);

        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null) return "系统提示：当前未登录，拒绝执行。";

        if (!sysUserService.hasPermission(currentUser.getId(), "user:list")) {
            return "操作失败：您没有查看用户列表的权限。";
        }

        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        if (pageSize > 10) pageSize = 10;

        Integer currentLevel = sysUserService.getHighestRoleLevel(currentUser.getId());

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(SysUser::getId, currentUser.getId());
        if (currentLevel > 1) {
            String excludeSql = "SELECT ur.user_id FROM myo_sys_user_role ur " +
                    "INNER JOIN myo_sys_role r ON ur.role_id = r.id " +
                    "WHERE r.role_level <= " + currentLevel +
                    " AND ur.user_id != '" + currentUser.getId() + "'";
            queryWrapper.notInSql(SysUser::getId, excludeSql);
        }

        Page<SysUser> pageParam = new Page<>(page, pageSize);
        Page<SysUser> userPage  = sysUserMapper.selectPage(pageParam, queryWrapper);

        List<SysUser> userList = userPage.getRecords();
        if (userList == null || userList.isEmpty()) return "当前页没有任何用户数据。";

        StringBuilder result = new StringBuilder("查询成功，共" + userPage.getTotal() + "位用户，以下是第" + page + "页数据：\n");
        for (SysUser user : userList) {
            String statusText = switch (user.getStatus()) {
                case 0  -> "正常";
                case 1  -> "警告";
                case 99 -> "被封禁";
                default -> "未知状态";
            };
            result.append("- 账号：").append(user.getAccount())
                    .append(" | 昵称：").append(user.getNickname())
                    .append(" | 状态：").append(statusText)
                    .append("\n");
        }
        return result.toString();
    }

    // ==================== 操作类工具 ====================

    /**
     * 封禁用户
     * 需要权限：user:status（只有站长和超级管理员有此权限）
     * 当管理员询问【能否封禁】【有没有封禁权限】时传入 account=__CHECK__
     * 要封禁具体用户时传入真实账号
     */
    @Tool(description = "封禁用户的唯一入口。当管理员询问【能否封禁】【有没有封禁权限】【可以封禁人吗】时必须调用此工具并传入__CHECK__；要封禁具体用户时传入真实账号。工具会自动校验权限并返回结果。")
    public String disableUser(
            @ToolParam(description = "要封禁的账号。如果只是询问能否封禁请传入 __CHECK__") String account) {
        log.info("封禁用户，账号：{}", account);

        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null) return "系统提示：当前未登录，拒绝执行。";

        // 权限校验：user:status
        if (!sysUserService.hasPermission(currentUser.getId(), "user:status")) {
            return "操作失败：您没有修改用户状态的权限，只有站长和超级管理员可执行此操作。";
        }

        // 仅询问权限，不执行封禁
        if ("__CHECK__".equals(account)) {
            return "校验通过：您有封禁用户的权限，请告诉我要封禁的账号。";
        }

        SysUser targetUser = sysUserService.findUserByAccount(account);
        if (targetUser == null) return "封禁失败：未找到该账号。";

        // 防越权校验
        String blockMsg = checkLevelPermission(currentUser, targetUser, "封禁");
        if (blockMsg != null) return blockMsg;

        if (targetUser.getStatus() == 99) return "该账号已经是封禁状态，无需重复操作。";

        targetUser.setStatus(99);
        boolean success = sysUserService.updateById(targetUser);
        return success ? "已成功封禁账号：" + account : "数据库更新异常，请稍后再试。";
    }

    /**
     * 解封用户
     * 需要权限：user:status（只有站长和超级管理员有此权限）
     * 当管理员询问【能否解封】【有没有解封权限】时传入 account=__CHECK__
     * 要解封具体用户时传入真实账号
     */
    @Tool(description = "解封用户的唯一入口。当管理员询问【能否解封】【有没有解封权限】【可以解封人吗】时必须调用此工具并传入__CHECK__；要解封具体用户时传入真实账号。工具会自动校验权限并返回结果。")
    public String enableUser(
            @ToolParam(description = "要解封的账号。如果只是询问能否解封请传入 __CHECK__") String account) {
        log.info("解封用户，账号：{}", account);

        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null) return "系统提示：当前未登录，拒绝执行。";

        // 权限校验：user:status
        if (!sysUserService.hasPermission(currentUser.getId(), "user:status")) {
            return "操作失败：您没有修改用户状态的权限，只有站长和超级管理员可执行此操作。";
        }

        // 仅询问权限，不执行解封
        if ("__CHECK__".equals(account)) {
            return "校验通过：您有解封用户的权限，请告诉我要解封的账号。";
        }

        SysUser targetUser = sysUserService.findUserByAccount(account);
        if (targetUser == null) return "解封失败：未找到该账号。";

        // 防越权校验
        String blockMsg = checkLevelPermission(currentUser, targetUser, "解封");
        if (blockMsg != null) return blockMsg;

        if (targetUser.getStatus() == 0) return "该账号已经是正常状态，无需解封。";

        targetUser.setStatus(0);
        boolean success = sysUserService.updateById(targetUser);
        return success ? "已成功解封账号：" + account : "数据库更新异常，请稍后再试。";
    }

    /**
     * 警告用户
     * 需要权限：user:status（只有站长和超级管理员有此权限）
     * 当管理员询问【能否警告】【有没有警告权限】时传入 account=__CHECK__
     * 要警告具体用户时传入真实账号
     */
    @Tool(description = "警告用户的唯一入口。当管理员询问【能否警告】【有没有警告权限】【可以警告人吗】时必须调用此工具并传入__CHECK__；要警告具体用户时传入真实账号。工具会自动校验权限并返回结果。")
    public String warnUser(
            @ToolParam(description = "要警告的账号。如果只是询问能否警告请传入 __CHECK__") String account) {
        log.info("警告用户，账号：{}", account);

        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null) return "系统提示：当前未登录，拒绝执行。";

        // 权限校验：user:status
        if (!sysUserService.hasPermission(currentUser.getId(), "user:status")) {
            return "操作失败：您没有修改用户状态的权限，只有站长和超级管理员可执行此操作。";
        }

        // 仅询问权限，不执行警告
        if ("__CHECK__".equals(account)) {
            return "校验通过：您有警告用户的权限，请告诉我要警告的账号。";
        }

        SysUser targetUser = sysUserService.findUserByAccount(account);
        if (targetUser == null) return "警告失败：未找到该账号。";

        // 防越权校验
        String blockMsg = checkLevelPermission(currentUser, targetUser, "警告");
        if (blockMsg != null) return blockMsg;

        if (targetUser.getStatus() == 1) return "该账号已经是警告状态，无需重复操作。";

        targetUser.setStatus(1);
        boolean success = sysUserService.updateById(targetUser);
        return success ? "已成功警告账号：" + account : "数据库更新异常，请稍后再试。";
    }
}