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

/**
 * AI 智能内容审核任务
 *
 * 定时扫描近期新增评论，调用阿里云 DashScope 大模型判断评论是否违规。
 * 发现违规评论后不直接删除，而是将状态标记为"待人工复审"，
 * 由管理员在后台确认后再决定是否删除，避免 AI 误判导致正常评论丢失。
 *
 * 评论状态约定：1 = 正常，2 = 待人工复审 0 = 已删除
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentAuditTask {

    // 引用评论 Mapper，用于查询待审核评论和更新评论状态
    private final CommentMapper commentMapper;

    // 从配置文件读取阿里云 DashScope API Key
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    // 大模型客户端，提升为类成员变量，Bean 初始化时构建一次，所有任务执行共用同一个实例
    // 避免每次任务执行都重新构建，节省资源
    private DashScopeChatModel chatModel;

    /**
     * Bean 初始化完成后自动执行，构建大模型客户端
     * 此时 @Value 注入的 apiKey 已经有值，可以安全使用
     */
    @PostConstruct
    public void init() {
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
        this.chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        // 温度设为 0.1，降低随机性，让模型输出更稳定可预期
                        .temperature(0.1)
                        .build())
                .build();
    }

    /**
     * 无参方法：供定时任务自动调度使用
     * 默认扫描过去 10 分钟内新增的评论
     */
    public void run() {
        log.info("[智能风控任务] 开始执行，扫描近期新增评论...");
        executeAudit(10);
    }

    /**
     * 有参方法：数据库中配置了 taskParam 时调用此方法（自动调度和手动执行都可能走这里）
     * 参数为扫描的分钟数，如传 "30" 则扫描过去 30 分钟内的评论
     *
     * @param param 扫描时间范围（分钟数），必须是正整数字符串
     */
    public void run(String param) {
        log.info("[智能风控任务] 接收到动态参数：{}", param);

        int minutes = 10; // 默认 10 分钟
        if (org.springframework.util.StringUtils.hasText(param)) {
            try {
                minutes = Integer.parseInt(param.trim());
                if (minutes <= 0) {
                    throw new IllegalArgumentException("扫描分钟数必须大于 0，当前值：" + minutes);
                }
            } catch (NumberFormatException e) {
                // 传入非数字时抛出 IllegalArgumentException，
                // SchedulingRunnable 识别到后会阻断重试并静默处理，不发送告警邮件
                throw new IllegalArgumentException("参数格式错误：请传入一个正整数（分钟数），当前值：" + param);
            }
        }

        executeAudit(minutes);
    }

    /**
     * 核心审核逻辑：扫描指定时间范围内的正常评论，逐条交给 AI 判断是否违规
     *
     * @param minutes 扫描过去多少分钟内新增的评论
     */
    private void executeAudit(int minutes) {
        log.info("[智能风控任务] 开始扫描过去 {} 分钟的新增评论...", minutes);

        // 计算时间起点，只查询该时间之后新增的评论
        long timeAgo = System.currentTimeMillis() - ((long) minutes * 60 * 1000);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(Comment::getCreateDate, timeAgo);
        // 只查询状态为"正常"的评论（status = 1），已标记待复审的不重复处理
        queryWrapper.eq(Comment::getStatus, 1);
        List<Comment> recentComments = commentMapper.selectList(queryWrapper);

        if (recentComments.isEmpty()) {
            log.info("[智能风控任务] 过去 {} 分钟无新评论，巡检结束。", minutes);
            return;
        }

        int pendingCount = 0;

        for (Comment comment : recentComments) {
            String content = comment.getContent();

            // 构建提示词，严格限制模型只输出"违规"或"正常"两个词，不能有任何多余内容
            // 限制输出格式是为了后续用 equals() 精确匹配，防止模型回复"这条评论违规，因为……"时误判
            String prompt = "你是一个平台内容审核机器人，只能输出一个词作为结果，不能有任何标点、空格或解释文字。" +
                    "请判断以下评论是否包含色情、暴力、辱骂、引战、政治敏感或违规广告内容。" +
                    "如果包含，输出：违规\n如果不包含，输出：正常\n" +
                    "评论内容：" + content;

            try {
                String aiResponse = chatModel.call(prompt);

                // 使用 trim().equals() 精确匹配，必须严格等于"违规"才触发，防止误判
                if (aiResponse != null && aiResponse.trim().equals("违规")) {
                    log.warn("[智能风控任务] AI 判定违规评论，ID: {}，内容: {}，已标记为待人工复审。",
                            comment.getId(), content);

                    // 软删除：将评论状态改为"待人工复审"（status = 2），不直接物理删除
                    // 管理员可在后台查看并决定是否真正删除，防止 AI 误判导致正常评论丢失
                    Comment updateComment = new Comment();
                    updateComment.setId(comment.getId());
                    updateComment.setStatus(2);
                    commentMapper.updateById(updateComment);
                    pendingCount++;
                }
            } catch (Exception e) {
                // 单条评论检测失败不中断整批，记录日志后继续处理下一条
                log.error("[智能风控任务] 调用 AI 模块检测失败，评论ID: {}，已跳过", comment.getId(), e);
            }
        }

        log.info("[智能风控任务] 巡检完毕。共扫描 {} 条评论，标记待复审 {} 条。",
                recentComments.size(), pendingCount);
    }
}