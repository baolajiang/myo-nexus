<template>

<div v-title :data-title="title" >
	<!-- 首页布局 -->
	<div class="container">
    <header-top class="header-top"></header-top>
		<div class="row">

			  <div class="content-card">
			  <div class="card-list">
<!--          <card-tag :tags="hotTags"></card-tag>

          <card-article cardHeader="最热" :articles="hotArticles"></card-article>

          <card-article cardHeader="最新" :articles="newArticles"></card-article>-->

          <card-technology cardHeader="暂时？" :articles="articles2"></card-technology>
			  </div>
			</div>
		</div>

	</div>
</div>

</template>

<script>

  import HeaderTop from '@/components/page'

  import CardTechnology from '@/components/card/CardTechnology'
  import cardTag from '@/components/card/cardTag'
  import ArticleListPage from '@/views/common/ArticleListPage'
  import CardArticle from '@/components/card/CardArticle'



  import {getArticles, getHotArtices, getNewArtices} from '@/api/article'
  import {getWebinfo} from '@/api/utils'
  import {getHotTags} from '@/api/tag'
  import {listarticles} from '@/api/article'

  export default {
    name: 'Index',
    created() {
      this.getHotArtices()
      this.getNewArtices()
      this.getHotTags()
      this.listarticles()
    },
    data() {
      return {
		webinfo:[],
        hotTags: [],
        hotArticles: [],
        newArticles: [],
        article: {
          query: {
            month: this.$route.params.month,
            year: this.$route.params.year
          }
        },
        articles: [],
		articles2:[]
      }
    },
		computed: {
		  title() {
		    return `${this.$myName}`
		  }
		},
    methods: {
      getHotArtices() {
        let that = this
        getHotArtices().then(data => {
          that.hotArticles = data.data
        }).catch(error => {
          console.log(error)
          if (error !== 'error') {
            that.$myMessage({type: 'error', content: '最热文章加载失败!', duration: 3000})
          }

        })

      },
      getNewArtices() {
        let that = this
        getNewArtices().then(data => {
          that.newArticles = data.data
        }).catch(error => {
          if (error !== 'error') {
            that.$myMessage({type: 'error', content: '最新文章加载失败!', duration: 3000})
          }

        })

      },
      getHotTags() {
        let that = this
        getHotTags().then(data => {
          that.hotTags = data.data
        }).catch(error => {
          if (error !== 'error') {
            that.$myMessage({type: 'error', content: '最热标签加载失败!', duration: 3000})
          }

        })
      },

      listarticles() {
        let that = this
        listarticles().then((data => {
          this.articles = data.data
		      this.articles2 = data.data.reverse()

        })).catch(error => {
          if (error !== 'error') {
            that.$myMessage({type: 'error', content: '文章加载失败!', duration: 3000})
          }
        })
      }
    },
    components: {

      'card-tag': cardTag,
      ArticleListPage,
      CardTechnology,
      'card-article': CardArticle,
	    HeaderTop,


    }
  }
</script>

<style>
/* 页面容器：居中，最大宽度限制 */
/*.container {
  max-width: 1200px; !* 稍微调宽一点，视觉更舒服 *!
  margin: 0 auto;
  padding: 20px 15px;
  box-sizing: border-box; !* 确保 padding 不会撑大宽度 *!
}*/

/* Flex 布局容器 */
.row {
  height: 100vh;
  display: flex;
  justify-content: space-between; /* 让左右两边分开对齐 */
  gap: 20px; /* 这一行很关键：设置列与列之间的间距 */
  align-items: flex-start; /* 顶部对齐 */
}

/* 1. 左侧空占位 (如果以后要用，可以给它宽度) */
.content-left {
  display: none; /* 暂时隐藏，因为它是空的，占位置会影响布局 */
  /* 如果要三栏布局，可以把上面这行删掉，改成 width: 200px; */
}

/* 2. 中间主要内容区 (文章列表) */
.content-main {
  flex: 1; /* 关键：让它占据剩余的所有空间 */
  min-width: 0; /* 关键：防止文章内部长图或代码块撑爆布局 */
  /* 这里的 padding 可以去掉，由 gap 控制间距更现代 */
}

/* 3. 右侧侧边栏 (个人卡片等) */
.content-card {
  width: 300px; /* 固定宽度 */
  flex-shrink: 0; /* 关键：防止屏幕变窄时侧边栏被挤扁 */
}

/* 侧边栏内部的卡片列表 */
.card-list {
  width: 100%;
}

/* 卡片样式微调 */
.el-card {
  border-radius: 8px;
  border: none; /* 可选：去掉边框，看起来更干净 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); /* 加一点现代阴影 */
  margin-bottom: 20px; /* 统一卡片间距 */
}

/* 覆盖 el-card 的 margin-top，改用 margin-bottom 统一控制 */
.el-card:not(:first-child) {
  margin-top: 0;
}

/* --- 响应式适配 --- */

/* 当屏幕小于 960px 时 (平板/手机) */
@media only screen and (max-width: 960px) {




}
</style>
