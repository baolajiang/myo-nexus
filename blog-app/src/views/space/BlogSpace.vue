<template>
  <div v-loading="loading" class="space-container">

    <div class="space-banner">
    </div>

    <div class="space-info-wrapper">
      <div class="space-info-content me-area">

        <div class="avatar-box">
          <el-avatar :size="100" :src="userInfo.avatar" class="space-avatar"></el-avatar>
        </div>

        <div class="info-box">
          <div class="name-row">
            <span class="nickname">{{ userInfo.nickname }}</span>
            <el-tag size="mini" v-if="isMe" type="danger" effect="plain" class="role-tag">我自己</el-tag>
            <i class="el-icon-male gender-icon" v-if="userInfo.sex === 1" style="color: #409EFF"></i>
            <i class="el-icon-female gender-icon" v-else-if="userInfo.sex === 0" style="color: #F56C6C"></i>
          </div>
          <p class="signature">{{ userInfo.summary || '这个人很懒，什么都没有写~' }}</p>
        </div>

        <div class="action-box">
          <template v-if="isMe">
            <el-button type="primary" plain round size="medium" @click="$router.push('/write')">去创作</el-button>
            <el-button plain round size="medium">编辑资料</el-button>
          </template>

          <template v-else>
            <el-button type="primary" round size="medium" icon="el-icon-plus">关注</el-button>
            <el-button plain round size="medium">私信</el-button>
          </template>
        </div>

      </div>
    </div>

    <div class="me-area space-body">
      <div class="space-nav">
        <div class="nav-item active">
          <i class="el-icon-document"></i> 文章 <span class="count">{{ total }}</span>
        </div>
        <div class="nav-item">
          <i class="el-icon-star-off"></i> 收藏
        </div>
      </div>

      <div class="article-list-box">
        <el-empty v-if="articles.length === 0" :description="isMe ? '你还没有发布过文章' : 'Ta还没有发布过文章'"></el-empty>

        <article-item
          v-for="(item, index) in articles"
          :key="item.id"
          v-bind="item"
          :index="index"
          class="space-article-item"
        ></article-item>

        <div class="pagination-box" v-if="total > 0">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            @current-change="handlePageChange"
          ></el-pagination>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
// 引入 API
import { getArticlesByAuthor } from '@/api/article' // 需确保你有这个查某人文章的接口
import { getUserPublicInfo } from '@/api/user'      // 获取用户信息的接口
import ArticleItem from '@/components/article/ArticleItem'

export default {
  name: 'BlogSpace',
  components: { ArticleItem },
  data() {
    return {
      uid: null,           // URL 里的 ID
      userInfo: {          // 页面展示的用户信息
        nickname: '加载中...',
        avatar: '',
        summary: ''
      },
      articles: [],
      loading: false,
      total: 0,
      pageSize: 10,
      currentPage: 1
    }
  },
  computed: {
    // 【核心】判断是不是我本人
    isMe() {
      // 比较 URL 里的 uid 和 Vuex 里的当前登录用户 id
      return this.uid === this.$store.state.id;
    }
  },
  watch: {
    // 监听路由变化 (比如从“我的空间”点到了“别人的空间”)，必须重新加载
    '$route.params.uid': {
      immediate: true,
      handler(val) {
        if (val) {
          this.uid = val;
          this.initData();
        }
      }
    }
  },
  methods: {
    initData() {
      this.fetchUserInfo();
      this.fetchArticles();
    },
    // 1. 获取用户信息 (头像、昵称)
    fetchUserInfo() {
      // 如果是本人，直接用 store 里的数据，省一次请求 (优化体验)
      if (this.isMe) {
        this.userInfo = {
          nickname: this.$store.state.nickname,
          avatar: this.$store.state.avatar,
          summary: '这是我自己的地盘' // store 里如果有 summary 更好
        };
        return;
      }

      // 如果是别人，调用 API 获取
      getUserPublicInfo(this.uid).then(res => {
        if (res.success) {
          this.userInfo = res.data;
        }
      });
    },
    // 2. 获取该用户的文章
    fetchArticles() {
      this.loading = true;
      let params = {
        page: this.currentPage,
        pageSize: this.pageSize,
        authorId: this.uid // 【关键】传给后端，只查这个人的
      };

      // 调用通用的查询接口
      getArticlesByAuthor(params).then(res => {
        if (res.success) {
          this.articles = res.data.articles;
          this.total = res.data.total;
        }
      }).finally(() => {
        this.loading = false;
      });
    },
    handlePageChange(val) {
      this.currentPage = val;
      this.fetchArticles();
    }
  }
}
</script>

<style scoped>
.space-container {
  background-color: #f4f5f7; /* B站灰 */
  min-height: 100vh;
  padding-bottom: 50px;
}

/* 1. Banner */
.space-banner {
  height: 200px;
  background-image: url('https://cos.myo.pub/cover/www.l2.webp'); /* 默认背景 */
  background-size: cover;
  background-position: center;
}

/* 2. 信息条 */
.space-info-wrapper {
  background: #fff;
  box-shadow: 0 0 0 1px #eee;
  margin-bottom: 20px;
}
.space-info-content {
  position: relative;
  display: flex;
  padding-bottom: 20px;
  padding-left: 20px;
  padding-right: 20px;
}
/* 头像上移，压住 Banner */
.avatar-box {
  margin-top: -25px;
  margin-right: 20px;
  z-index: 2;
}
.space-avatar {
  border: 4px solid rgba(255,255,255,0.8);
  background: #fff;
}

.info-box {
  padding-top: 10px;
  flex: 1;
}
.name-row {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.nickname {
  font-size: 22px;
  font-weight: 700;
  color: #222;
  margin-right: 10px;
}
.gender-icon { font-size: 16px; margin-left: 10px; }

.signature {
  font-size: 14px;
  color: #6d757a;
  margin: 0;
}

.action-box {
  padding-top: 15px;
  display: flex;
  gap: 10px;
}

/* 3. 导航栏 */
.space-nav {
  background: #fff;
  border-radius: 4px;
  margin-bottom: 10px;
  display: flex;
  border: 1px solid #eee;
}
.nav-item {
  padding: 15px 25px;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #222;
  border-bottom: 3px solid transparent;
  transition: all 0.3s;
}
.nav-item:hover, .nav-item.active {
  color: #00a1d6; /* B站蓝 */
  border-bottom-color: #00a1d6;
}
.nav-item i { margin-right: 5px; }
.count { font-size: 12px; color: #99a2aa; margin-left: 3px; }

/* 4. 列表区 */
.space-body {
  width: 1000px;
  margin: 0 auto;
}
.article-list-box {
  /* 可以不加背景，让卡片自己浮起来 */
}
.space-article-item {
  margin-bottom: 15px;
}

.pagination-box {
  text-align: center;
  margin-top: 30px;
}
</style>
