package com.myo.blog.config;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.service.SysUserService;
import com.myo.blog.utils.UserThreadLocal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
@Slf4j
// 加上 @Component 让 Spring 管理它，这就相当于一个 AI 专属的 Controller
@Component
public class UserAiTools {

    private final SysUserService sysUserService;

    public UserAiTools(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    // 第 1 个方法：查询单人
    @Tool(description = "根据账号(account)查询详细用户信息")
    public String queryUser(String account) {
        log.info("根据账号查询用户详细信息，账号：{}", account);
        SysUser targetUser = sysUserService.findUserByAccount(account);
        if (targetUser == null) return "未找到该用户。";
        // 状态翻译逻辑
        String statusText;
        if (targetUser.getStatus() == 0)statusText="正常";
        else if (targetUser.getStatus() == 1)statusText="警告";
        else if (targetUser.getStatus() == 99)statusText="被禁用";
        else statusText="未知状态";
        System.out.println("targetUser.getStatus():" + targetUser.getStatus());
        // 物理切断：把密码变成掩码，大模型根本接收不到真实密码
        targetUser.setPassword("******");
        targetUser.setSalt("******"); // 如果有盐值字段也一并抹除
        return "查到该用户信息如下：\n" +
                "【系统判定该账号当前状态为：" + statusText + "】\n" +
                "底层详细数据：" + targetUser.toString();
    }



    // 第 2 个方法：分页查询用户列表
    @Tool(description = "查询系统中的用户列表信息。如果用户未指定，默认查询第1页，每页10条。")
    public String queryAllUsers(
            @ToolParam(description = "你想查询的页码，如果不确定请传 1") Integer page,
            @ToolParam(description = "每页展示的数量，最多传 10") Integer pageSize) {
        log.info("查询系统中的用户列表信息，页码：{}，数量：{}", page, pageSize);
        System.out.println("执行分页查询，页码：" + page + "，数量：" + pageSize);
        // 1. 安全兜底拦截（极其重要！）
        // 防止 AI 传 null，或者乱传负数
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        // 强制截断！不管 AI 想要查多少条，坚决不允许超过 10 条，保护系统内存！
        if (pageSize > 10) pageSize = 10;

        // 2. 在本地组装复杂对象
        PageParams pageParams = new PageParams();
        pageParams.setPage(page);
        pageParams.setPageSize(pageSize);

        // 3. 执行业务逻辑
        Result result = sysUserService.UserList(pageParams);

        if (!result.isSuccess()) {
            return "查询用户列表失败，后端报错信息：" + result.getMsg();
        }


        //删除下面这行错误的强转代码：
        //List<SysUser> userList = (List<SysUser>) result.getData();

        //替换为下面这两行正确地拆包代码：
        // 第一步：先将 data 强转为 MyBatis-Plus 的 Page 对象
        Page<SysUser> pageData = (Page<SysUser>) result.getData();
        // 第二步：从 Page 对象中提取用户列表
        List<SysUser> userList = pageData.getRecords();
        if (userList == null || userList.isEmpty()) {
            return "当前页没有任何用户数据。";
        }

        // 4. 动态拼接返回结果，注意这里的页码变成了动态的
        StringBuilder aiReport = new StringBuilder("查询成功，以下是系统中的用户列表（第" + page + "页数据）：\n");
        for (SysUser user : userList) {
            aiReport.append("- 账号: ").append(user.getAccount())
                    .append(" | 状态: ").append(user.getStatus() == 0 ? "正常" : user.getStatus() == 1 ? "警告" : user.getStatus() == 99 ?"被禁用":"未知状态")
                    .append("\n");
        }
        return aiReport.toString();
    }
    // 第 3 个方法：封禁单人
    @Tool(description = "根据账号(account)对目标用户执行封禁操作")
    public String disableUser(String account) {
        // 绝对防御：只认权限不认人！
        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null || !sysUserService.hasPermission(currentUser.getId(), "user:status")) {
            return "操作失败：系统拒绝执行。原因：当前操作者没有 [修改用户状态] 的权限，请委婉地告知用户。";
        }
        log.info("根据账号封禁用户，账号：{}", account);
        SysUser targetUser = sysUserService.findUserByAccount(account);
        if (targetUser == null) return "封禁失败：未找到该账号";


        targetUser.setStatus(99); //
        boolean success = sysUserService.updateById(targetUser);
        return success ? "已成功在底层数据库将该账号封禁。" : "数据库更新异常。";
    }
    // 第 4 个方法：解封单人
    @Tool(description = "根据账号(account)对目标用户执行解封操作")
    public String enableUser(String account) {
        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null || !sysUserService.hasPermission(currentUser.getId(), "user:status")) {
            return "操作失败：系统拒绝执行。原因：当前操作者没有 [修改用户状态] 的权限，请委婉地告知用户。";
        }
        log.info("根据账号解封用户，账号：{}", account);
        SysUser targetUser = sysUserService.findUserByAccount(account);
        if (targetUser == null) return "解封失败：未找到该账号";
        // 解封操作，将状态设置为 0（正常）
        targetUser.setStatus(0);
        boolean success = sysUserService.updateById(targetUser);
        return success ? "已成功在底层数据库将该账号解封。" : "数据库更新异常。";
    }
    // 第 5个方法：警告单人
    @Tool(description = "根据账号(account)对目标用户执行警告操作")
    public String warnUser(String account) {
        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null || !sysUserService.hasPermission(currentUser.getId(), "user:status")) {
            return "操作失败：系统拒绝执行。原因：当前操作者没有 [修改用户状态] 的权限，请委婉地告知用户。";
        }
        log.info("根据账号警告用户，账号：{}", account);
        SysUser targetUser = sysUserService.findUserByAccount(account);
        if (targetUser == null) return "警告失败：未找到该账号";
        // 警告操作，将状态设置为 1（警告）
        targetUser.setStatus(1);
        boolean success = sysUserService.updateById(targetUser);
        return success ? "已成功在底层数据库将该账号警告。" : "数据库更新异常。";
    }

}