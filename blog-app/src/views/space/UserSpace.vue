<template>
  <div v-loading="loading" class="space-container">
    <div class="h-banner">
      <img src="/static/img/anime-sunset-art-wallpaper-2560x1080_14.jpg" class="banner-img">
    </div>

    <div class="h-wrapper">
      <div class="h-inner">
        <div class="h-avatar">
          <el-avatar
            :size="80"
            :src="userInfo.avatar || '/static/img/default_avatar.png'"
            class="avatar-img"
          ></el-avatar>
          <span v-if="isMe" class="me-tag">UP</span>
        </div>

        <div class="h-info">
          <div class="h-basic">
            <span class="nickname">{{ userInfo.nickname || '神秘旅人' }}</span>
            <span class="uid">UID: {{ userId }}</span>
            <el-tag size="mini" type="info" class="level-tag">Lv.{{ userInfo.level || 1 }}</el-tag>
          </div>
          <div class="h-sign">{{ userInfo.motto || '这个人很懒，什么都没有写~' }}</div>
        </div>

        <div class="h-action">
          <template v-if="isMe">
            <el-button plain size="medium" @click="$router.push('/settings')">编辑资料</el-button>
          </template>
          <template v-else>
            <el-button type="primary" icon="el-icon-plus" size="medium">关注</el-button>
            <el-button plain icon="el-icon-chat-dot-round" size="medium">私信</el-button>
          </template>
        </div>
      </div>
    </div>

    <div class="n-wrapper">
      <div class="n-inner">
        <div class="n-tab-item active">
          <i class="el-icon-document"></i>
          <span class="n-text">文章</span>
          <span class="n-num">{{ total }}</span>
        </div>
        <div class="n-tab-item">
          <i class="el-icon-star-off"></i>
          <span class="n-text">收藏</span>
          <span class="n-num">0</span>
        </div>
        <div class="n-tab-item">
          <i class="el-icon-picture-outline"></i>
          <span class="n-text">相册</span>
        </div>
      </div>
    </div>

    <div class="s-main">
      <div class="col-left">
        <div class="section-title">
          <span class="t-name">文章</span>
          <div class="t-filter">
            <span class="active">最新发布</span>
            <span>最多点击</span>
          </div>
        </div>

        <div class="article-list-wrapper">
          <el-empty v-if="articles.length === 0" description="暂无文章，快去点亮第一颗星吧！"></el-empty>

          <article-item
            v-for="(item, index) in articles"
            :key="item.id"
            v-bind="item"
            :index="index"
            class="space-article-item"
          ></article-item>
        </div>

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

      <div class="col-right">
        <div class="side-card">
          <div class="card-title">个人公告</div>
          <div class="card-content notice-content">
            {{ userInfo.notice || '暂时没有公告内容哦~' }}
          </div>
        </div>

        <div class="side-card">
          <div class="card-title">创作统计</div>
          <div class="card-content stat-grid">
            <div class="stat-item">
              <div class="num">{{ total }}</div>
              <div class="label">文章</div>
            </div>
            <div class="stat-item">
              <div class="num">123</div>
              <div class="label">获赞</div>
            </div>
            <div class="stat-item">
              <div class="num">456</div>
              <div class="label">访问</div>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import { getMyArticles } from '@/api/article'
import { getUserPublicInfo } from '@/api/user'
import ArticleItem from '@/components/article/ArticleItem'

export default {
  name: 'UserSpace',
  components: { ArticleItem },
  data() {
    return {
      articles: [],
      userInfo: {}, // 存储当前页面所属用户的信息
      loading: false,
      total: 0,
      pageSize: 5,
      currentPage: 1,
      userId: '',
    }
  },
  computed: {
    // 判断是否是自己 (显示编辑按钮 vs 关注按钮)
    isMe() {
      // 这里的 store.state.id 是登录人的ID，userId 是 URL 里的 ID
      return String(this.$store.state.id) === String(this.userId);
    }
  },
  watch: {
    // 监听路由变化，如果从一个用户跳到另一个用户，需要刷新
    '$route.params.id': function(val) {
      this.userId = val;
      this.initAllData();
    }
  },
  mounted() {
    // 获取路由参数中的ID
    this.userId = this.$route.params.id;
    this.initAllData();
  },
  methods: {
    initAllData() {
      this.fetchUserInfo();
      this.fetchArticles();
    },
    // 1. 获取用户信息 (头像、昵称等)
    fetchUserInfo() {
      // 这里的逻辑是：如果是看自己，优先用store里的最新数据；如果是看别人，走接口
      if (this.isMe) {
        this.userInfo = {
          nickname: this.$store.state.account,
          avatar: this.$store.state.avatar,
          motto: '在这里，记录思想的轨迹。', // Store里可能没存签名，暂时写死或从接口取
          level: 6

        };
      } else {
        getUserPublicInfo(this.userId).then(res => {
          if (res.success) {
            this.userInfo = {
              id: res.data.id,
              // 同理，后端返回的数据里如果有 nickname 就用，没有就用 account
              nickname: res.data.nickname || res.data.account,
              avatar: res.data.avatar,
              motto: '这个家伙很懒，什么都没写'
            };
          } else {
            this.userInfo.nickname = '用户不存在';
          }
        }).catch(err => {
          this.userInfo.nickname = '加载失败';
        })
      }
    },
    // 2. 获取文章列表
    fetchArticles() {
      this.loading = true;
      let params = {
        page: this.currentPage,
        pageSize: this.pageSize,
      };

      getMyArticles(params, this.userId).then(res => {
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
/* 容器背景，稍微带点灰，突出内容白底 */
.space-container {
  min-height: 100vh;
  background-color: #f4f5f7;
  padding-top: 60px; /* 避开顶部导航遮挡 */
}

/* 1. Banner */
.h-banner {
  height: 200px;
  width: 100%;
  overflow: hidden;
  background-color: #e3e5e7; /* 加载前的占位色 */
}
.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 保证图片铺满不拉伸 */
  object-position: center;
}

/* 2. 用户信息栏 */
.h-wrapper {
  background: #fff;
  box-shadow: 0 0 0 1px #eee;
  position: relative;
}
.h-inner {
  width: 1100px; /* B站标准宽度 */
  margin: 0 auto;
  height: 84px;
  display: flex;
  align-items: center;
  position: relative;
}

/* 头像：往上浮动，压住 Banner */
.h-avatar {
  position: relative;
  top: -20px;
  margin-right: 20px;
}
.avatar-img {
  border: 4px solid rgba(255,255,255,0.8); /* 白边框 */
  background-color: #fff;
}
.me-tag {
  position: absolute;
  right: 0;
  top: 0;
  background: #fb7299;
  color: #fff;
  font-size: 12px;
  padding: 1px 4px;
  border-radius: 4px;
}

/* 文本信息 */
.h-info {
  flex: 1;
  padding-bottom: 10px; /* 修正对齐 */
}
.h-basic {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
.nickname {
  font-size: 22px;
  font-weight: 700;
  color: #222;
  margin-right: 10px;
}
.uid {
  font-size: 12px;
  color: #999;
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
  margin-right: 10px;
}
.h-sign {
  font-size: 14px;
  color: #6d757a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 600px;
}

/* 3. 导航栏 */
.n-wrapper {
  background: #fff;
  border-top: 1px solid #e7e7e7;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  margin-bottom: 20px;
}
.n-inner {
  width: 1100px;
  margin: 0 auto;
  display: flex;
  height: 66px; /* 稍微高一点 */
  align-items: center;
}
.n-tab-item {
  display: flex;
  align-items: center;
  margin-right: 40px;
  font-size: 16px;
  color: #222;
  cursor: pointer;
  height: 100%;
  border-bottom: 3px solid transparent;
  transition: all 0.2s;
}
.n-tab-item:hover, .n-tab-item.active {
  color: #00a1d6; /* B站蓝 */
}
.n-tab-item.active {
  border-bottom-color: #00a1d6;
}
.n-tab-item i {
  margin-right: 6px;
  font-size: 20px;
}
.n-num {
  font-size: 12px;
  color: #99a2aa;
  margin-left: 4px;
}

/* 4. 主体双栏布局 */
.s-main {
  width: 1100px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  padding-bottom: 50px;
}

/* 左侧文章列表 */
.col-left {
  width: 780px; /* 约 70% 宽度 */
  background: #fff;
  border-radius: 4px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.section-title {
  border-bottom: 1px solid #eee;
  padding-bottom: 15px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.t-name {
  font-size: 20px;
  color: #222;
}
.t-filter span {
  font-size: 12px;
  cursor: pointer;
  margin-left: 20px;
  color: #222;
}
.t-filter span.active {
  color: #00a1d6;
}

/* 右侧侧边栏 */
.col-right {
  width: 300px;
}
.side-card {
  background: #fff;
  border-radius: 4px;
  margin-bottom: 20px;
  padding: 15px 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.card-title {
  font-size: 14px;
  color: #333;
  border-bottom: 1px solid #e7e7e7;
  padding-bottom: 10px;
  margin-bottom: 10px;
}
.notice-content {
  font-size: 12px;
  color: #666;
  line-height: 1.6;
}
.stat-grid {
  display: flex;
  justify-content: space-around;
  text-align: center;
}
.stat-item .num {
  font-size: 16px;
  font-weight: bold;
  color: #222;
  margin-bottom: 4px;
}
.stat-item .label {
  font-size: 12px;
  color: #99a2aa;
}

.pagination-box {
  text-align: center;
  margin-top: 30px;
}
</style>
