package com.myo.blog.mq;

import com.myo.blog.config.RabbitConfig;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文章缓存清理监听器 (消费者)
 * 负责监听 MQ 消息，并异步清理 Redis 缓存，保证数据一致性。
 * 具备手动 ACK、批量删除、异常重试等生产级特性。
 */
@Slf4j
@Component
public class ArticleListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 监听队列，处理缓存清理请求
     *
     * @param msg     接收到的消息内容 (例如文章ID)
     * @param channel 信道 (用于手动确认 ACK/NACK)
     * @param message 消息对象 (包含 ID 和投递标签等元数据)
     */
    @RabbitListener(queues = RabbitConfig.ARTICLE_QUEUE)
    public void handlerArticleCache(String msg, Channel channel, Message message) {
        // 获取消息的唯一投递标签 (Delivery Tag)，用于回执
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("【MQ消费者】收到消息，准备清理缓存... 文章ID: {}", msg);

            // === 1. 执行业务逻辑 (清理 Redis 缓存) ===
            // 采用 SCAN + 批量删除的方式，避免阻塞 Redis 主线程
            deleteByScan("news_article*");  // 清理最新文章
            deleteByScan("listArticle*");   // 清理文章列表
            deleteByScan("hot_article*");   // 清理最热文章
            deleteByScan("article::" + msg + "*"); // 清理该文章相关的缓存
            // === 2. 手动确认消息 (ACK) ===
            channel.basicAck(deliveryTag, false);

            log.info("【MQ消费者】缓存清理完成，消息已确认 ");

        } catch (Exception e) {
            log.error("【MQ消费者】处理消息时发生异常！文章ID: {}", msg, e);

            try {
                // === 3. 异常处理策略 ===

                // 判断是否是 Redis 连接异常 (如网络抖动、Redis宕机)
                if (e instanceof RedisConnectionFailureException || e.getCause() instanceof RedisConnectionFailureException) {
                    log.warn("【MQ消费者】Redis连接失败，尝试重试消息 (Requeue)...");
                    // 策略：网络故障，将消息放回队列 (Requeue = true)，等待下一次重试
                    channel.basicNack(deliveryTag, false, true);
                } else {
                    log.error("【MQ消费者】业务逻辑严重错误，拒绝消息 (Discard)...");
                    // 策略：代码逻辑错误或数据错误，重试无用，直接丢弃消息 (Requeue = false)
                    // 注意：如果有死信队列，这里会进入死信；否则直接丢弃
                    channel.basicNack(deliveryTag, false, false);
                }
            } catch (IOException ex) {
                // 如果在执行 NACK 时又发生了网络错误，记录日志 (通常很难发生，除非 MQ 也挂了)
                log.error("【MQ消费者】执行 NACK 失败", ex);
            }
        }
    }

    /**
     * 使用 SCAN 指令非阻塞地删除指定前缀的缓存
     * 优化点：使用批量删除 (Batch Delete) 替代逐个删除，大幅减少网络交互次数 (RTT)
     *
     * @param pattern 匹配模式，例如 "listArticle*"
     */
    private void deleteByScan(String pattern) {
        // 使用 RedisCallback以此获得更底层的连接控制权
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            // 定义 SCAN 选项：匹配模式 + 每次扫描数量 (count=1000)
            // count 越大越快，但对 Redis 瞬时压力越大；1000 是个合理的折中值
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(1000)
                    .build();

            // 使用 try-with-resources 语法，确保 Cursor 在结束后自动关闭，防止资源泄露
            try (Cursor<byte[]> cursor = connection.scan(options)) {

                // 定义一个缓冲列表，用于收集 Key 进行批量删除
                List<byte[]> batchKeys = new ArrayList<>();
                int batchSize = 100; // 批次大小 (每凑够 100 个 Key 删一次)

                while (cursor.hasNext()) {
                    batchKeys.add(cursor.next());

                    // 如果缓冲区满了，执行一次批量删除
                    if (batchKeys.size() >= batchSize) {
                        connection.del(batchKeys.toArray(new byte[0][]));
                        batchKeys.clear(); // 清空列表，准备接收下一波
                    }
                }

                // 循环结束后，处理剩余没凑够一批的 Key
                if (!batchKeys.isEmpty()) {
                    connection.del(batchKeys.toArray(new byte[0][]));
                }
            } catch (Exception e) {
                log.error("扫描删除过程中发生错误, Pattern: {}", pattern, e);
                // 关键点：将异常抛出给上层 (handlerArticleCache)
                // 这样上层才能捕获并触发 NACK 重试机制
                throw new RuntimeException(e);
            }
            return null;
        });
    }
}