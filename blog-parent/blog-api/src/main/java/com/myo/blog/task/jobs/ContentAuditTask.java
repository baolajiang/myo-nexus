package com.myo.blog.task.jobs;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.CommentMapper;
import com.myo.blog.dao.pojo.Comment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
// 内容审核任务，用于定时审核评论内容是否违规
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentAuditTask {

    private final CommentMapper commentMapper;

    // 直接读取你配置文件里的 API Key
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    public void run() {
        log.info("[智能风控任务] 开始执行，扫描近期新增评论...");
        executeAudit(10);// 默认查过去 10 分钟
    }

    // 2. 有参方法（供动态调度使用）
    public void run(String param) {
        log.info("[智能风控任务] 接收到动态参数：{}", param);
        int minutes = 10;
        try {
            if (org.apache.commons.lang3.StringUtils.isNotBlank(param)) {
                minutes = Integer.parseInt(param.trim());
            }
        } catch (Exception e) {
            log.warn("参数解析失败，默认扫描过去10分钟");
        }
        executeAudit(minutes);
    }
    // 3. 核心逻辑：执行审核
    private void executeAudit(int minutes) {
        log.info("[智能风控任务] 开始扫描过去 {} 分钟的新增评论...", minutes);
        // 1. 查询过去 minutes 分钟内新增的评论
        long tenMinutesAgo = System.currentTimeMillis() - ((long) minutes * 60 * 1000);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(Comment::getCreateDate, tenMinutesAgo);
        List<Comment> recentComments = commentMapper.selectList(queryWrapper);

        if (recentComments.isEmpty()) {
            log.info("[智能风控任务] 过去{}分钟无新评论，巡检结束。", minutes);
            return;
        }

        // 2. 初始化一个专门用于审核的底层大模型（绕过你原本的 Agent）
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        // 温度设为 0.1，剥夺 AI 的创造力，让它变成一个严谨的机器人
                        .temperature(0.1)
                        .build())
                .build();

        int violationCount = 0;

        // 3. 遍历评论并交给大模型审核
        for (Comment comment : recentComments) {
            String content = comment.getContent();

            // 构建严格的提示词，限制其输出
            String prompt = "你是一个严格的平台内容审核员。请检查以下文本是否包含色情、暴力、辱骂、引战、政治敏感或违规广告。如果是，请仅回复“违规”二字；如果正常，请仅回复“正常”二字。不要有任何多余的解释。文本内容：" + content;

            try {
                // 直接调用底层模型获取字符串回复
                String aiResponse = chatModel.call(prompt);

                if (aiResponse != null && aiResponse.contains("违规")) {
                    log.warn("[智能风控任务] 发现违规评论！内容: {}，执行清理操作。", content);

                    // 物理删除违规评论
                    commentMapper.deleteById(comment.getId());
                    violationCount++;
                }
            } catch (Exception e) {
                log.error("[智能风控任务] 调用 AI 模块检测失败，评论ID: {}", comment.getId(), e);
            }
        }

        log.info("[智能风控任务] 巡检完毕。共扫描 {} 条评论，清理违规评论 {} 条。", recentComments.size(), violationCount);
    }
}