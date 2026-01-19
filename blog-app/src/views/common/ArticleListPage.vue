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
    }
  },
  created() {
    this.getArticles()
  },
  methods: {
    resetAndLoad() {
      this.noData = false
      this.articles = []
      this.innerPage.pageNumber = 1
      this.getArticles()
    },

    //  修改点 2：移除所有延迟逻辑，直接请求
    handlePageChange(val) {
      this.innerPage.pageNumber = val
      // 立即回到顶部
      window.scrollTo({ top: 0, behavior: 'auto' }) // behavior: 'auto' 瞬间跳回，比 smooth 更快
      this.getArticles()
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

        //  修改点 3：数据回来直接赋值，不等待
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
