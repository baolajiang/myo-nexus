package com.myo.blog.controller;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.utils.UserThreadLocal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 博客后台 AI 管理控制器

 * 基于阿里云百炼 DashScope 大模型 + Spring AI Graph 的 ReactAgent 实现。
 * 对外暴露 /admin/ai/chat 接口，接收自然语言指令，由 AI 智能体自动调用
 * 用户模块（UserAiTools）和文章模块（ArticleAiTools）中注册的工具方法完成操作。

 * Agent 的初始化逻辑已抽离至 {@link com.myo.blog.config.AiAgentConfig}，
 * 本类只负责接收请求、安全校验和调用 Agent，职责单一。

 * 请求链路说明：
 * 每个请求在到达本 Controller 之前，已经过两道拦截器处理：
 * LoginInterceptor（验证登录，将 SysUser 存入 UserThreadLocal）→
 * AdminInterceptor（验证管理员权限）→ AdminAiController
 * 因此本类可以直接从 UserThreadLocal 获取当前管理员信息，无需重复校验。

 * 安全机制（三层防御）：
 *  拦截器层：LoginInterceptor + AdminInterceptor 确保只有合法管理员能访问
 *  关键词拦截层：Java 层对高危词汇做前置过滤
 *  系统提示词层：Agent 初始化时注入安全准则，防止模型被越权指令操控
 * 
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai")
public class AdminAiController {

    /**
     * AI 智能体，由 AiAgentConfig 构建并注册为 Spring Bean，此处直接注入使用
     */
    private final ReactAgent blogAdminAgent;

    public AdminAiController(ReactAgent blogAdminAgent) {
        this.blogAdminAgent = blogAdminAgent;
    }

    /**
     * 接收前端 AI 控制台发送的自然语言指令并执行

     * 请求处理流程：

     *  从 UserThreadLocal 获取当前管理员（由 LoginInterceptor 预先注入）
     *  日志记录原始指令（带管理员 ID，便于审计溯源）
     *  空指令校验
     *  高危关键词拦截（Java 层防御）
     *  以管理员 ID 为 threadId 调用 ReactAgent，保证每个管理员拥有独立的对话上下文
     *  返回 AI 回复并记录日志

     * 会话隔离说明：threadId 格式为 {@code admin-session-{userId}}，
     * 每个管理员拥有独立的对话历史，互不干扰。
     * userId 直接来自经过认证的 UserThreadLocal，安全可靠，无需解析 token。
     *
     * @param message 前端传入的自然语言指令（Content-Type: text/plain）
     * @return AI 智能体的回复文本，或安全拦截提示信息
     */
    @PostMapping(value = "/chat", consumes = "text/plain")
    public String chat(@RequestBody String message) {

        // 从 UserThreadLocal 获取当前登录的管理员（由 LoginInterceptor 在请求进入时注入）
        SysUser currentUser = UserThreadLocal.get();
        String threadId = "admin-session-" + currentUser.getId();

        // 审计日志：带上管理员 ID，出了问题能快速定位是谁发的指令
        log.info("管理员 [{}] 发送AI指令: [{}]", currentUser.getId(), message);

        // 空指令校验
        if (message == null || message.trim().isEmpty()) {
            log.warn("管理员 [{}] 发送了空指令，已拒绝", currentUser.getId());
            return "指令不能为空";
        }

        // Java 层关键词拦截（第二道防线，配合系统提示词形成双重保险）
        // 针对提示词注入、权限绕过等攻击场景做前置过滤
        List<String> blockList = Arrays.asList("密码", "提权", "破解", "删库", "忽略设定", "我是管理员");
        for (String badWord : blockList) {
            if (message.contains(badWord)) {
                // 安全审计日志：记录是哪个管理员、命中了什么词、完整指令是什么
                log.warn("触发高危安全拦截！管理员: [{}], 命中违规词: [{}], 完整指令: [{}]",
                        currentUser.getId(), badWord, message);
                return "系统安全警告：检测到违规或越权操作指令，请求已被拒绝执行。";
            }
        }

        // 以当前管理员 ID 构建专属会话上下文，实现多管理员对话严格隔离
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        try {
            AssistantMessage response = blogAdminAgent.call(message, runnableConfig);
            log.info("管理员 [{}] AI回复: [{}]", currentUser.getId(), response.getText());
            return response.getText();
        } catch (GraphRunnerException e) {
            log.error("AI智能体执行异常, 管理员: [{}], 指令: [{}]", currentUser.getId(), message, e);
            return "AI服务暂时不可用，请稍后重试";
        }
    }
}