package com.myo.blog.utils;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;

/**
 * 自定义ID生成器
 * 自动根据实体类名生成带业务前缀的 ID
 */
@Component
public class CustomIdGenerator implements IdentifierGenerator {

    @Override
    public Number nextId(Object entity) {
        return null; // 不需要生成数字ID
    }

    @Override
    public String nextUUID(Object entity) {
        // 默认前缀 (防止有漏网之鱼)
        String prefix = "MYO";

        if (entity != null) {
            String className = entity.getClass().getSimpleName();

            switch (className) {
                // --- 核心业务 ---
                case "Article":
                    prefix = "ART";      // 文章
                    break;
                case "ArticleBody":
                    prefix = "BODY";     // 文章内容
                    break;
                case "Category":
                    prefix = "CAT";      // 分类
                    break;
                case "Tag":
                    prefix = "TAG";      // 标签
                    break;
                case "ArticleTag":
                    prefix = "ART_TAG";  // 文章-标签关联
                    break;

                // --- 用户与权限 ---
                case "SysUser":
                    prefix = "USER";     // 用户
                    break;
                case "SysRole":
                    prefix = "ROLE";     // 角色
                    break;
                case "SysPermission":
                    prefix = "PERM";     // 权限
                    break;
                case "UserToken":
                    // 注意：UserToken 之前设的是 INPUT (手动输入)，可能不会走这里
                    // 但加上也没坏处，万一以后改策略了呢
                    prefix = "TOKEN";
                    break;

                // --- 互动与消息 ---
                case "Comment":
                    prefix = "CMT";      // 评论
                    break;
                case "Message":
                    prefix = "MSG";      // 留言/消息
                    break;

                // --- 其他 ---
                case "Link":
                    prefix = "LINK";     // 友链
                    break;
                case "SysLog":
                    prefix = "LOG";      // 日志 (如果日志ID也是字符串的话)
                    break;

                // 如果有未知的类，保持默认或者打印警告
                default:
                    System.out.println("未配置前缀的实体类: " + className + "，使用默认前缀 MYO");
                    break;
            }
        }

        // 调用序列号生成器
        return SerialGenerator.generate(prefix);
    }
}