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
    // 【新增注入】直接注入 Mapper 以便進行批量查詢 (Batch Query)
    // 這是為了在 copyList 方法中解決 N+1 問題，直接用 ID 列表查出資料
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public Result listArticle(PageParams pageParams, String token) {
        System.out.println("来了");
        System.out.println("看看参数："+pageParams);
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
            // 這是為了防止 copy 方法內部再去查詢資料庫，我們稍後用 Map 手動賦值
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