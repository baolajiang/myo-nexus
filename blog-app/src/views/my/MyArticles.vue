<template>
  <div v-loading="loading" class="my-column-container">

    <div class="my-column-header">
      <div class="header-content">
        <el-avatar
          :size="80"
          :src="$store.state.avatar || '/static/img/default_avatar.png'"
          class="user-avatar"
        ></el-avatar>

        <div class="user-info">
          <div class="name-row">
            <h1 class="nickname">{{ $store.state.nickname || '我的专栏' }}</h1>
            <el-tag size="mini" effect="dark" type="warning" class="role-tag">作者</el-tag>
          </div>
          <p class="motto">在这里，记录思想的轨迹。已创作 {{ total }} 篇文章。</p>
        </div>
      </div>
    </div>

    <div class="me-area">
      <el-empty v-if="articles.length === 0" description="暂无文章，快去点亮第一颗星吧！"></el-empty>

      <article-item
        v-for="(item, index) in articles"
        :key="item.id"
        v-bind="item"
        :index="index"
        class="my-article-item"
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
</template>

<script>
import { getMyArticles } from '@/api/article'
import ArticleItem from '@/components/article/ArticleItem'

export default {
  name: 'MyArticles',
  components: { ArticleItem },
  data() {
    return {
      articles: [],
      loading: false,
      total: 0,
      pageSize: 5,
      currentPage: 1
    }
  },
  mounted() {
    this.fetchData();
  },
  methods: {
    fetchData() {
      this.loading = true;
      let params = {
        page: this.currentPage,
        pageSize: this.pageSize
      };
      getMyArticles(params).then(res => {
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
      this.fetchData();
    }
  }
}
</script>

<style scoped>
.my-column-container {
  min-height: 100vh;
  background-color: #f9f9f9;
  /* 【核心修改】留出顶部距离，防止被 Header 遮挡 */
  padding-top: 80px;
}

/* 头部样式优化 */
.my-column-header {
  background: #fff;
  /* 给个淡雅的渐变背景，不像纯白那么单调 */
  background: linear-gradient(to bottom, #fff, #fdfdfd);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  padding: 30px 0;
  margin-bottom: 30px;
  border-bottom: 1px solid #eee;
}

.header-content {
  width: 960px;
  margin: 0 auto;
  display: flex;
  align-items: center;
}

.user-avatar {
  border: 3px solid #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-right: 20px;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.nickname {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin: 0 10px 0 0;
}

.role-tag {
  border-radius: 10px;
  padding: 0 8px;
}

.motto {
  font-size: 14px;
  color: #888;
  margin: 0;
  font-family: 'Georgia', serif; /* 换个衬线体显得文艺点 */
  font-style: italic;
}

/* 列表区域宽度限制 */
.me-area {
  width: 960px;
  margin: 0 auto;
  padding-bottom: 50px;
}

.my-article-item {
  margin-bottom: 20px;
}

.pagination-box {
  text-align: center;
  margin-top: 40px;
  padding-bottom: 20px;
}
</style>
