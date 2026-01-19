package com.myo.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.config.RabbitConfig;
import com.myo.blog.dao.dos.articles;
import com.myo.blog.dao.mapper.ArticleBodyMapper;
import com.myo.blog.dao.mapper.ArticleMapper;
import com.myo.blog.dao.mapper.ArticleTagMapper;
import com.myo.blog.dao.pojo.Article;
import com.myo.blog.dao.pojo.ArticleBody;
import com.myo.blog.dao.pojo.ArticleTag;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.service.*;
import com.myo.blog.entity.ArticleBodyVo;
import com.myo.blog.entity.ArticleVo;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.TagVo;
import com.myo.blog.entity.params.ArticleBodyParam;
import com.myo.blog.entity.params.ArticleParam;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.utils.ArticleUtils;
import com.myo.blog.utils.UserThreadLocal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
@Slf4j
@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private TagService tagService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private ArticleBodyMapper articleBodyMapper;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ThreadService threadService;
    @Autowired
    private ArticleTagMapper articleTagMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate; // 注入 RabbitTemplate

    @Override
    public Result listArticle(PageParams pageParams,String token) {
        Page<Article> page = new Page<>(pageParams.getPage(),pageParams.getPageSize());
        IPage<Article> articleIPage = this.articleMapper.listArticle(page,
                pageParams.getCategoryId(),
                pageParams.getTagId(),
                pageParams.getYear(),
                pageParams.getMonth(),
                token != null);
        return Result.success(copyList(articleIPage.getRecords(),true,true));
    }

    @Override
    public Result listArticleCount(String token) {
        return null;
    }


    @Override
    public Result queryMAC() {
        return Result.success(getMacAddress());
    }

    @Override
    public Result hotArticle(int limit) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Article::getViewCounts);
        queryWrapper.select(Article::getId,Article::getTitle);
        queryWrapper.last("limit "+limit);
        List<Article> articles = articleMapper.selectList(queryWrapper);
        return Result.success(copyList(articles,false,false));
    }

    @Override
    public Result newArticles(int limit) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Article::getCreateDate);
        queryWrapper.select(Article::getId,Article::getTitle);
        queryWrapper.last("limit "+limit);
        List<Article> articles = articleMapper.selectList(queryWrapper);
        return Result.success(copyList(articles,false,false));
    }

    @Override
    public Result listarticles() {
        List<articles> articles = articleMapper.listarticles();
        return Result.success(articles);
    }

    @Override
    public Result findArticleById(String articleId, String token) { // Long -> String
        Article article = this.articleMapper.selectById(articleId);
        if (article == null) {
            return Result.fail(404, "文章不存在");
        }
        // 增加阅读数
        threadService.updateArticleViewCount(articleMapper, article);
        return Result.success(copy(article, true, true, true, true));
    }

    @Override
    @Transactional
    public Result publish(ArticleParam articleParam) {
        SysUser sysUser = UserThreadLocal.get();

        Article article = new Article();
        article.setAuthorId(sysUser.getId()); // 直接获取 String ID
        article.setCategoryId(articleParam.getCategory().getId()); // 已经是 String
        article.setCreateDate(System.currentTimeMillis());
        article.setCommentCounts(0);
        article.setSummary(articleParam.getSummary());
        article.setTitle(articleParam.getTitle());
        article.setViewCounts(0);
        article.setWeight(Article.Article_Common);
        article.setBodyId("-1");
        article.setCover(articleParam.getCover());

        this.articleMapper.insert(article);

        // Tags
        List<TagVo> tags = articleParam.getTags();
        if (tags != null) {
            for (TagVo tag : tags) {
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(article.getId());
                articleTag.setTagId(tag.getId()); // 已经是 String
                this.articleTagMapper.insert(articleTag);
            }
        }

        // Body
        ArticleBody articleBody = new ArticleBody();
        articleBody.setContent(articleParam.getBody().getContent());
        articleBody.setContentHtml(articleParam.getBody().getContentHtml());
        articleBody.setArticleId(article.getId());

        articleBodyMapper.insert(articleBody);

        article.setBodyId(articleBody.getId());

        articleMapper.updateById(article);
        // ================== 发送 MQ 消息 (生产级写法) ==================
        try {
            // 发送消息
            rabbitTemplate.convertAndSend(RabbitConfig.BLOG_EXCHANGE, "article.publish", article.getId());
            log.info("【MQ消息发送成功】文章ID: {}", article.getId());

        } catch (AmqpException e) {
            // 错误处理：如果 MQ 发送失败，不能让发布文章这个动作回滚！
            // 只要数据库存进去了，文章就算发成功了。缓存没删掉顶多是延迟更新，不能影响主业务。
            log.error("【MQ消息发送失败】MQ服务可能异常，请检查！文章ID: {}", article.getId(), e);
            // 降级策略 (可选)：如果 MQ 挂了，这里可以手动删一次 Redis 作为兜底
            // redisTemplate.delete("listArticle*");
        }

        Map<String, String> map = new HashMap<>();
        map.put("id", article.getId()); // 已经是 String


        return Result.success(map);
    }

    private List<ArticleVo> copyList(List<Article> records, boolean isTag, boolean isAuthor) {
        List<ArticleVo> articleVoList = new ArrayList<>();
        for (Article record : records) {
            articleVoList.add(copy(record,isTag,isAuthor,false,false));
        }
        return articleVoList;
    }

    private List<ArticleVo> copyList(List<Article> records, boolean isTag, boolean isAuthor,boolean isBody) {
        List<ArticleVo> articleVoList = new ArrayList<>();
        for (Article record : records) {
            articleVoList.add(copy(record,isTag,isAuthor,isBody,false));
        }
        return articleVoList;
    }

    private ArticleVo copy(Article article, boolean isTag, boolean isAuthor, boolean isBody,boolean isCategory){
        ArticleVo articleVo = new ArticleVo();
        BeanUtils.copyProperties(article,articleVo);

        articleVo.setId(article.getId()); // 直接赋值
        articleVo.setCreateDate(ArticleUtils.time(article.getCreateDate()));

        if (isTag){
            String articleId = article.getId();
            articleVo.setTags(tagService.findTagsByArticleId(articleId));
        }
        if (isAuthor){
            String authorId = article.getAuthorId();
            articleVo.setAuthor(sysUserService.findUserById(authorId).getNickname());
        }
        if (isBody){
            String bodyId = article.getBodyId();
            articleVo.setBody(findArticleBodyById(bodyId));
        }
        if (isCategory){
            String categoryId = article.getCategoryId();
            articleVo.setCategory(categoryService.findCategoryById(categoryId));
        }
        return articleVo;
    }

    private ArticleBodyVo findArticleBodyById(String bodyId) { // Long -> String
        ArticleBody articleBody = articleBodyMapper.selectById(bodyId);
        ArticleBodyVo articleBodyVo = new ArticleBodyVo();
        articleBodyVo.setContent(articleBody.getContent());
        return articleBodyVo;
    }

    // ... getMacAddress 方法保持不变 ...
    private static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            byte[] mac = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
                    continue;
                } else {
                    mac = netInterface.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                        }
                        if (sb.length() > 0) {
                            return sb.toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}