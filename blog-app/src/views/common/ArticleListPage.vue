<template>
  <div class="article-list-container" style="min-height: 600px;">

    <div class="article-list">
      <article-item
        v-for="(a, index) in articles"
        :key="a.id"
        v-bind="a"
        :theme="theme"
        :index="(innerPage.pageNumber - 1) * innerPage.pageSize + index">
      </article-item>
    </div>

    <div v-if="noData" class="no-data">
      <el-empty description="这里空空如也..."></el-empty>
    </div>

    <div class="pagination-box" v-if="!loading && !noData && articles.length > 0">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page.sync="innerPage.pageNumber"
        :page-size="innerPage.pageSize"
        :total="total"
        @current-change="handlePageChange">
      </el-pagination>
    </div>

    <div v-if="loading && articles.length === 0" class="loading-box">
      <i class="el-icon-loading"></i> 加载中...
    </div>
  </div>
</template>

<script>
import ArticleItem from '@/components/article/ArticleItem'
import {getArticles} from '@/api/article'

export default {
  name: "ArticleListPage",
  components: {
    'article-item': ArticleItem
  },
  props: {
    offset: { type: Number, default: 100 },
    page: { type: Object, default() { return {} } },
    query: { type: Object, default() { return {} } },
    theme: {type: String, default: 'light'}
  },
  data() {
    return {
      loading: false,
      noData: false,
      innerPage: {
        pageSize: 5,
        pageNumber: 1,
        name: 'a.createDate',
        sort: 'desc'
      },
      total: 0,
      articles: []
    }
  },
  watch: {
    'query': {
      handler() { this.resetAndLoad() },
      deep: true
    },
    'page': {
      handler() {
        this.innerPage = { ...this.innerPage, ...this.page }
        this.resetAndLoad()
      },
      deep: true
    },
    // 2. 修改 watch $route：监听路由变化（点击浏览器前进后退时触发）
    $route(to, from) {
      if (to.params.page) {
        this.innerPage.pageNumber = parseInt(to.params.page);
      } else if (to.query.page) {
        this.innerPage.pageNumber = parseInt(to.query.page);
      } else {
        // 如果没有页码参数（比如回到了默认列表），重置为 1
        this.innerPage.pageNumber = 1;
      }
      this.getArticles();
    }
  },
  created() {
    // 优先读取 /articles/page/:page 中的 page
    const routeParamsPage = this.$route.params.page;
    // 其次读取 ?page=2 中的 page
    const routeQueryPage = this.$route.query.page;

    if (routeParamsPage) {
      this.innerPage.pageNumber = parseInt(routeParamsPage);
    } else if (routeQueryPage) {
      this.innerPage.pageNumber = parseInt(routeQueryPage);
    }

    this.getArticles();
  },
  methods: {
    resetAndLoad() {
      this.noData = false
      this.articles = []
      // 这里重置时，如果想把 URL 参数也清空，可以处理，或者保持默认
      this.innerPage.pageNumber = 1
      this.getArticles()
    },

    handlePageChange(val) {
      this.innerPage.pageNumber = val;
      window.scrollTo({ top: 0, behavior: 'auto' });

      // 获取当前路径，用于判断我们是在哪个页面
      const currentPath = this.$route.path;

      // 【核心逻辑】判断当前是不是文章归档页
      // 如果当前路径以 /articles 开头，我们就用 /articles/page/x 这种风格
      if (currentPath.startsWith('/articles')) {
        this.$router.push({ path: `/articles/page/${val}` });
      }
      // 如果是在首页或其他页面（比如 /tag/java），可能还是保持 ?page=x 比较安全，除非也给它们配了路由
      else {
        this.$router.push({
          path: this.$route.path,
          query: { ...this.$route.query, page: val }
        });
      }

      // 注意：这里不需要手动调 getArticles，因为 watch $route 会触发它
      // 但如果发现反应慢，可以解开下面这行注释，双重保险
       this.getArticles();
    },

    view(id) {
      this.$router.push({ path: `/view/${id}` })
    },

    getArticles() {
      let that = this
      that.loading = true

      getArticles(that.query, that.innerPage, this.$store.state.token).then(data => {
        let responseData = data.data;
        let newArticles = []

        if (responseData && responseData.articles) {
          newArticles = responseData.articles
          that.total = responseData.total
        } else {
          newArticles = responseData
          that.total = 0
        }

        if (newArticles && newArticles.length > 0) {
          that.articles = newArticles
          that.noData = false
        } else {
          that.articles = []
          that.noData = true
        }

      }).catch(error => {
        if (error !== 'error') {
          that.$myMessage({ type: 'error', content: '文章加载失败!', duration: 3000 })
        }
      }).finally(() => {
        that.loading = false
      })
    }
  }
}
</script>

<style scoped>
.article-list-container {
  min-height: 600px;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}



.pagination-box {
  margin: 20px 0;
  text-align: center;
  padding: 10px;
}

.loading-box {
  text-align: center;
  padding: 20px;
  color: #666;
}

.no-data {
  margin-top: 20px;
  background: #fff;
  padding: 20px;
  text-align: center;
}
</style>
