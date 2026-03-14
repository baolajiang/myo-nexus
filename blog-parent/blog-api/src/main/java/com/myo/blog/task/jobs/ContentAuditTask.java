package com.myo.blog.task.jobs;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.CommentMapper;
import com.myo.blog.dao.pojo.Comment;
import jakarta.annotation.PostConstruct;
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

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    // 【修复3】提升为类成员变量，只初始化一次，避免每次任务执行都重复构建
    private DashScopeChatModel chatModel;

    @PostConstruct
    public void init() {
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
        this.chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        .temperature(0.1)
                        .build())
                .build();
    }

    // 1. 无参方法：默认扫描过去 10 分钟
    public void run() {
        log.info("[智能风控任务] 开始执行，扫描近期新增评论...");
        executeAudit(10);
    }

    // 2. 有参方法：供动态调度使用
    public void run(String param) {
        log.info("[智能风控任务] 接收到动态参数：{}", param);

        // 【修复4】加防御，传入非数字时给出明确提示，而不是让 parseInt 直接抛 NumberFormatException
        int minutes = 10;
        if (org.springframework.util.StringUtils.hasText(param)) {
            try {
                minutes = Integer.parseInt(param.trim());
                if (minutes <= 0) {
                    throw new IllegalArgumentException("扫描分钟数必须大于 0，当前值：" + minutes);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("参数格式错误：请传入一个正整数（分钟数），当前值：" + param);
            }
        }

        executeAudit(minutes);
    }

    // 3. 核心逻辑：执行审核
    private void executeAudit(int minutes) {
        log.info("[智能风控任务] 开始扫描过去 {} 分钟的新增评论...", minutes);

        long timeAgo = System.currentTimeMillis() - ((long) minutes * 60 * 1000);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(Comment::getCreateDate, timeAgo);
        // 【修复1-前置】只查询状态正常的评论，避免重复审核已经被标记的评论
        queryWrapper.eq(Comment::getStatus, 1);
        List<Comment> recentComments = commentMapper.selectList(queryWrapper);

        if (recentComments.isEmpty()) {
            log.info("[智能风控任务] 过去 {} 分钟无新评论，巡检结束。", minutes);
            return;
        }

        int pendingCount = 0;

        for (Comment comment : recentComments) {
            String content = comment.getContent();

            // 【修复5】优化提示词：明确要求只输出单个词，不能有任何标点、空格或解释
            String prompt = "你是一个平台内容审核机器人，只能输出一个词作为结果，不能有任何标点、空格或解释文字。" +
                    "请判断以下评论是否包含色情、暴力、辱骂、引战、政治敏感或违规广告内容。" +
                    "如果包含，输出：违规\n如果不包含，输出：正常\n" +
                    "评论内容：" + content;

            try {
                String aiResponse = chatModel.call(prompt);

                // 【修复2】改用 trim().equals() 精确匹配，彻底杜绝 AI 回复"违规，因为……"时的误判
                // 原来的 contains("违规") 只要回复里出现"违规"二字就会触发，极易误删正常评论
                if (aiResponse != null && aiResponse.trim().equals("违规")) {
                    log.warn("[智能风控任务] AI 判定违规评论，ID: {}，内容: {}，已标记为待人工复审。", comment.getId(), content);

                    // 【修复1-核心】软删除：将评论状态改为"待人工复审"（status = 2），而不是物理删除
                    // 物理删除一旦误判，数据永久丢失无法恢复；软删除可以让管理员在后台审核并一键恢复
                    Comment updateComment = new Comment();
                    updateComment.setId(comment.getId());
                    updateComment.setStatus(2); // 2 = 待人工复审
                    commentMapper.updateById(updateComment);
                    pendingCount++;
                }
            } catch (Exception e) {
                log.error("[智能风控任务] 调用 AI 模块检测失败，评论ID: {}，已跳过", comment.getId(), e);
            }

        }

        log.info("[智能风控任务] 巡检完毕。共扫描 {} 条评论，标记待复审 {} 条。", recentComments.size(), pendingCount);
    }
}