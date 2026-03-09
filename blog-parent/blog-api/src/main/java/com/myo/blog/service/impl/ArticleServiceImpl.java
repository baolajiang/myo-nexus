package com.myo.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.config.RabbitConfig;
import com.myo.blog.dao.dos.articles;
import com.myo.blog.dao.mapper.*;
import com.myo.blog.dao.pojo.*;
import com.myo.blog.entity.*;
import com.myo.blog.service.*;
import com.myo.blog.entity.params.ArticleBodyParam;
import com.myo.blog.entity.params.ArticleParam;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.utils.ArticleUtils;
import com.myo.blog.utils.JWTUtils;
import com.myo.blog.utils.UserThreadLocal;
import io.lettuce.core.ScriptOutputType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {


    private final ArticleMapper articleMapper;
    private final TagService tagService;
    private final SysUserService sysUserService;
    private final ArticleBodyMapper articleBodyMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final CategoryService categoryService;
    private final ThreadService threadService;
    private final ArticleTagMapper articleTagMapper;
    private final CommentMapper commentMapper;
    private final RabbitTemplate rabbitTemplate; // 注入 RabbitTemplate
    // 直接注入 Mapper 以便進行批量查詢 (Batch Query)
    // 這是為了在 copyList 方法中解決 N+1 問題，直接用 ID 列表查出資料

    private final SysUserMapper sysUserMapper;

    private final CategoryMapper categoryMapper;

    @Override
    public Result listArticle(PageParams pageParams, String token) {

        // 1. 建立分頁物件
        Page<Article> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());

        // 2. 判斷是否有 Token (是否登入)
        boolean isToken = false;
        if (StringUtils.isNotBlank(token) && !"undefined".equals(token)) {
            // 务必验证 Token 是否合法
            isToken = JWTUtils.checkToken(token) != null;
        }

        // 3. 呼叫 Mapper 進行自定義的分頁查詢 (支持歸檔、標籤、分類篩選)
        IPage<Article> articleIPage = articleMapper.listArticle(
                page,
                pageParams.getCategoryId(),
                pageParams.getTagId(),
                pageParams.getYear(),
                pageParams.getMonth(),
                isToken
        );

        List<Article> records = articleIPage.getRecords();
        System.out.println("看看数据："+records);
        // 4. 【核心優化點】 將資料庫實體轉換為前端視圖對象 (VO)

        List<ArticleVo> articleVoList = copyList(records, true, true, false, false, isToken);

        // 5. 獲取總記錄數
        long total = articleIPage.getTotal();

        // 6. 封裝結果返回
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("articles", articleVoList);
        resultData.put("total", total);

        return Result.success(resultData);
    }

    @Override
    public Result listArticlesByAuthor(PageParams pageParams, String authorId) {
        // 1. 分页对象
        Page<Article> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());

        // 2. 查询条件
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(Article::getAuthorId, authorId);
        // 按时间倒序
        queryWrapper.orderByDesc(Article::getCreateDate);

        // 3. 执行查询
        articleMapper.selectPage(page, queryWrapper);

        // 4. 转换 VO
        List<Article> records = page.getRecords();

        // isAuthor 传 false！
        // 既然查的是“我的文章”，作者肯定是自己，不需要再去数据库查一遍 User 表（避免了 N+1 问题和密码泄露风险）
        List<ArticleVo> articleVoList = copyList(records, true, false);

        // 手动填充作者信息
        // 从 ThreadLocal 获取当前登录用户信息（比查库快）
        SysUser sysUser = UserThreadLocal.get();
        String nickname = (sysUser != null) ? sysUser.getNickname() : "Unknown";

        for (ArticleVo articleVo : articleVoList) {
            articleVo.setAuthor(nickname);
        }

        // 返回结构要和前端分页匹配 (带上 total)
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("articles", articleVoList);
        resultData.put("total", page.getTotal());

        return Result.success(resultData);
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
    public Result findArticleById(String articleId, String token) { 
        Article article = this.articleMapper.selectById(articleId);
        if (article == null) {
            return Result.fail(404, "文章不存在");
        }
        // 增加阅读数
        threadService.updateArticleViewCount(articleMapper, article.getId());
        return Result.success(copy(article, true, true, true, true));
    }

    @Override
    @Transactional
    public Result publish(ArticleParam articleParam) {
        // ==================== 1. 幂等性防护：Redis 分布式锁 ====================
        SysUser sysUser = UserThreadLocal.get();
        // Key 加上用户 ID，保证不同用户之间互不影响
        String lockKey = "LOCK:ARTICLE:PUBLISH:" + sysUser.getId();

        // SETNX + 过期时间（原子操作），防止网络抖动导致前端重复提交
        // 过期时间兜底：即使 finally 没执行（如 JVM 崩溃），5 秒后锁自动释放，不会死锁
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            return Result.fail(400, "操作太快啦，正在为您发布中，请勿重复点击！");
        }

        try {
            // ==================== 2. 构建并插入文章基本信息 ====================
            Article article = new Article();
            article.setAuthorId(sysUser.getId());
            article.setCategoryId(articleParam.getCategory().getId());
            article.setCreateDate(System.currentTimeMillis());
            article.setCommentCounts(0);
            article.setSummary(articleParam.getSummary());
            article.setTitle(articleParam.getTitle());
            article.setViewCounts(0);
            article.setWeight(Article.Article_Common);
            article.setBodyId("-1"); // 内容 ID 占位，下面插入 body 后再回填
            article.setCover(articleParam.getCover());
            this.articleMapper.insert(article);

            // ==================== 3. 处理标签关联 ====================
            List<TagVo> tags = articleParam.getTags();
            if (tags != null) {
                for (TagVo tag : tags) {
                    ArticleTag articleTag = new ArticleTag();
                    articleTag.setArticleId(article.getId());
                    articleTag.setTagId(tag.getId());
                    this.articleTagMapper.insert(articleTag);
                }
            }

            // ==================== 4. 插入文章内容，并回填 bodyId ====================
            ArticleBody articleBody = new ArticleBody();
            articleBody.setContent(articleParam.getBody().getContent());
            articleBody.setContentHtml(articleParam.getBody().getContentHtml());
            articleBody.setArticleId(article.getId());
            articleBodyMapper.insert(articleBody);

            // 回填：将刚生成的 bodyId 更新到文章表
            article.setBodyId(articleBody.getId());
            articleMapper.updateById(article);

            // ==================== 5. 发送 MQ 消息，异步清理文章列表缓存 ====================
            // 注意：MQ 发送失败不能影响主业务（文章已入库即为成功）
            // 失败后果：文章列表缓存未及时清理，最终一致性保证，不影响数据正确性
            try {
                rabbitTemplate.convertAndSend(RabbitConfig.BLOG_EXCHANGE, "article.publish", article.getId());
                log.info("【MQ】缓存清理消息发送成功，文章ID: {}", article.getId());
            } catch (AmqpException e) {
                log.error("【MQ】消息发送失败，MQ 服务可能异常，文章ID: {}，请人工排查！", article.getId(), e);
                // 降级兜底：MQ 挂了可在此处直接删 Redis（按需开启）
                // stringRedisTemplate.delete("listArticle*");
            }

            // ==================== 6. 返回结果 ====================
            Map<String, String> map = new HashMap<>();
            map.put("id", article.getId());
            return Result.success(map);

        } finally {
            // ==================== 7. 释放分布式锁 ====================
            // 放在 finally 确保业务异常时锁也能被释放，避免用户在 5 秒内无法重试
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional
    public Result updateArticle(ArticleParam articleParam) {
        String articleId = articleParam.getId();
        if (StringUtils.isBlank(articleId)) {
            return Result.fail(400, "文章ID不能为空");
        }

        // 1. 获取当前登录用户
        SysUser sysUser = UserThreadLocal.get();

        // 2. 构造唯一的 Redis Key，防止频繁点击更新导致数据混乱
        String lockKey = "LOCK:ARTICLE:UPDATE:" + sysUser.getId() + ":" + articleId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(locked)) {
            return Result.fail(400, "操作太快啦，正在为您更新中，请勿频繁点击！");
        }

        try {
            // 3. 更新文章主表基本信息
            Article article = new Article();
            article.setId(articleId);
            article.setTitle(articleParam.getTitle());
            article.setSummary(articleParam.getSummary());
            article.setCategoryId(articleParam.getCategory().getId());
            article.setCover(articleParam.getCover());
            this.articleMapper.updateById(article);

            // 4. 处理标签关联 (先删后插)
            // 先清理旧标签
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArticleTag> tagDeleteWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            tagDeleteWrapper.eq(ArticleTag::getArticleId, articleId);
            this.articleTagMapper.delete(tagDeleteWrapper);

            // 再插入新标签
            List<TagVo> tags = articleParam.getTags();
            if (tags != null && !tags.isEmpty()) {
                for (TagVo tag : tags) {
                    ArticleTag articleTag = new ArticleTag();
                    articleTag.setArticleId(articleId);
                    articleTag.setTagId(tag.getId());
                    this.articleTagMapper.insert(articleTag);
                }
            }

            // 5. 更新文章内容表
            ArticleBody articleBody = new ArticleBody();
            articleBody.setContent(articleParam.getBody().getContent());
            articleBody.setContentHtml(articleParam.getBody().getContentHtml());

            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ArticleBody> updateBodyWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            updateBodyWrapper.eq(ArticleBody::getArticleId, articleId);
            this.articleBodyMapper.update(articleBody, updateBodyWrapper);

            // 6. 发送 MQ 消息清理缓存
            // 文章更新了，必须把列表和详情的缓存清掉。路由键写 article.update 同样可以被你之前配置的 article.# 规则监听到
            try {
                rabbitTemplate.convertAndSend(RabbitConfig.BLOG_EXCHANGE, "article.update", articleId);
                log.info("【MQ消息发送成功】文章更新，ID: {}", articleId);
            } catch (org.springframework.amqp.AmqpException e) {
                log.error("【MQ消息发送失败】MQ服务可能异常，但不影响文章更新。文章ID: {}", articleId, e);
            }

            // 7. 返回结果
            Map<String, String> map = new HashMap<>();
            map.put("id", articleId);
            return Result.success(map);

        } finally {
            // 解除分布式锁
            stringRedisTemplate.delete(lockKey);
        }
    }

    private List<ArticleVo> copyList(List<Article> records, boolean isTag, boolean isAuthor) {
        List<ArticleVo> articleVoList = new ArrayList<>();
        for (Article record : records) {
            articleVoList.add(copy(record,isTag,isAuthor,false,false));
        }
        return articleVoList;
    }



    private List<ArticleVo> copyList(List<Article> records, boolean isTag, boolean isAuthor, boolean isBody, boolean isCategory, boolean isToken) {
        List<ArticleVo> articleVoList = new ArrayList<>();
        // === 1. 收集 ID (準備階段) ===
        // 遍歷所有文章，把需要的 AuthorId 和 CategoryId 收集起來，準備一次查完
        List<String> authorIds = new ArrayList<>();
        List<String> categoryIds = new ArrayList<>();

        for (Article record : records) {
            // 【修復 NPE】 如果是隱藏文章且未登入，跳過，不需要收集它的 ID
            if (!isToken && record.getViewKeys() != null && record.getViewKeys() == 2) {
                continue;
            }
            if (isAuthor) {
                authorIds.add(record.getAuthorId());
            }
            if (isCategory) {
                categoryIds.add(record.getCategoryId());
            }
        }

        // === 2. 批量查詢 (資料庫交互階段) ===

        // A. 查詢作者信息，並轉為 Map<String, SysUser> 方便查找
        Map<String, SysUser> authorMap = new HashMap<>();
        if (isAuthor && !authorIds.isEmpty()) {
            // 使用 MyBatis Plus 的 selectBatchIds (SELECT * FROM sys_user WHERE id IN (...))
            List<SysUser> users = sysUserMapper.selectBatchIds(authorIds);
            for (SysUser u : users) {
                authorMap.put(u.getId(), u);
            }
        }

        // B. 查詢分類信息，並轉為 Map<String, CategoryVo> 方便查找
        // 注意：Map 的 Value 是 CategoryVo，因為 ArticleVo 需要的是這個類型
        Map<String, CategoryVo> categoryMap = new HashMap<>();
        if (isCategory && !categoryIds.isEmpty()) {
            List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
            for (Category c : categories) {
                // 將 Category 實體轉換為 VO 對象
                CategoryVo vo = new CategoryVo();
                BeanUtils.copyProperties(c, vo);
                vo.setId(String.valueOf(c.getId())); // ID 類型轉換 (Long -> String)
                categoryMap.put(c.getId(), vo);
            }
        }

        // === 3. 組裝數據 (記憶體處理階段) ===
        for (Article record : records) {
            // 【修復 NPE】 再次過濾隱藏文章
            if (!isToken && record.getViewKeys() != null && record.getViewKeys() == 2) {
                continue;
            }

            // 【重點】調用 copy 時，isAuthor 和 isCategory 傳入 false
            // 這是為了防止 copy 方法內部再去查詢資料庫，稍後用 Map 手動賦值
            ArticleVo articleVo = copy(record, isTag, false, isBody, false);

            // 手動填入作者信息 (從 Map 獲取，不查庫)
            if (isAuthor && authorMap.containsKey(record.getAuthorId())) {
                articleVo.setAuthor(authorMap.get(record.getAuthorId()).getNickname());
            }

            // 手動填入分類信息 (從 Map 獲取，不查庫)
            if (isCategory && categoryMap.containsKey(record.getCategoryId())) {
                articleVo.setCategory(categoryMap.get(record.getCategoryId()));
            }

            // 加密/脫敏處理 (viewKeys=1)
            // 【修復 NPE】 增加 null 判斷
            if (!isToken && record.getViewKeys() != null && record.getViewKeys() == 1) {
                int titleLen = articleVo.getTitle() != null ? articleVo.getTitle().length() : 5;
                articleVo.setTitle(ArticleUtils.keys(titleLen));

                int summaryLen = articleVo.getSummary() != null ? articleVo.getSummary().length() : 10;
                articleVo.setSummary(ArticleUtils.keys(summaryLen));

                List<TagVo> maskTagList = new ArrayList<>();
                TagVo maskTag = new TagVo();
                maskTag.setTagName("******");
                maskTagList.add(maskTag);
                articleVo.setTags(maskTagList);

                if (articleVo.getCategory() != null) {
                    articleVo.getCategory().setCategoryName("******");
                }
            }

            articleVoList.add(articleVo);
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

    private ArticleBodyVo findArticleBodyById(String bodyId) { 
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
    @Override
    public Result listArticleForAdmin(PageParams pageParams) {
        // 1. 创建分页对象
        Page<Article> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.orderByDesc(Article::getWeight, Article::getCreateDate);

        // 3. 执行查询
        articleMapper.selectPage(page, queryWrapper);

        // 4. 转换 VO (复用 copyList，填充作者和分类信息)
        // 【注意】：后台列表通常不需要显示 Tag (太占位置)，但需要 Author 和 Category
        List<ArticleVo> articleVoList = copyList(page.getRecords(), false, true, false, true, true);

        // 5. 封装结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", articleVoList);
        result.put("total", page.getTotal());

        return Result.success(result);
    }

    @Override
    @Transactional
    public Result deleteArticle(String id) {
        // 1. 获取当前登录用户（管理员）
        SysUser sysUser = UserThreadLocal.get();

        // 2. 构造防重 Redis Key，精确到用户ID和文章ID
        String lockKey = "LOCK:ARTICLE:DELETE:" + sysUser.getId() + ":" + id;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(locked)) {
            return Result.fail(400, "操作太快啦，正在处理中，请勿频繁点击！");
        }

        try {
            // 3. 删除文章主表
            this.articleMapper.deleteById(id);

            // 4. 删除文章内容表
            LambdaQueryWrapper<ArticleBody> bodyQuery = new LambdaQueryWrapper<>();
            bodyQuery.eq(ArticleBody::getArticleId, id);
            this.articleBodyMapper.delete(bodyQuery);

            // 5. 删除文章标签关联表
            LambdaQueryWrapper<ArticleTag> tagQuery = new LambdaQueryWrapper<>();
            tagQuery.eq(ArticleTag::getArticleId, id);
            this.articleTagMapper.delete(tagQuery);

            // 6. 删除文章相关的评论
            LambdaQueryWrapper<Comment> commentQuery = new LambdaQueryWrapper<>();
            commentQuery.eq(Comment::getArticleId, id);
            this.commentMapper.delete(commentQuery);

            // 7. 发送 MQ 消息清理缓存
            try {
                rabbitTemplate.convertAndSend(RabbitConfig.BLOG_EXCHANGE, "article.delete", id);
                log.info("【MQ】缓存清理消息发送成功，文章及相关数据已删除，ID: {}", id);
            } catch (AmqpException e) {
                log.error("【MQ】消息发送失败，MQ 服务可能异常，但不影响删除。文章ID: {}", id, e);
            }

            return Result.success("删除成功");

        } finally {
            // 8. 解除分布式锁
            stringRedisTemplate.delete(lockKey);
        }
    }
}