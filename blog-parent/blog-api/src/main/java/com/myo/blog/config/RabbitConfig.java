package com.myo.blog.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 配置类，定义交换机、队列、路由键
@Configuration
public class RabbitConfig {

    // 定义交换机名称
    public static final String BLOG_EXCHANGE = "blog_topic_exchange";
    // 定义队列名称
    public static final String ARTICLE_QUEUE = "article_cache_queue";
    // 定义路由键 (Routing Key)
    public static final String ROUTING_KEY = "article.#";

    // 1. 声明交换机 (Topic类型，灵活)
    @Bean("blogExchange")
    public Exchange blogExchange() {
        return ExchangeBuilder.topicExchange(BLOG_EXCHANGE).durable(true).build();
    }

    // 2. 声明队列
    @Bean("articleQueue")
    public Queue articleQueue() {
        return QueueBuilder.durable(ARTICLE_QUEUE).build();
    }

    // 3. 绑定队列到交换机
    @Bean
    public Binding bindArticleQueue(Exchange blogExchange, Queue articleQueue) {
        return BindingBuilder.bind(articleQueue).to(blogExchange).with(ROUTING_KEY).noargs();
    }
}