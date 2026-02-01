<template>
<div  v-title :data-title="title">
	<div class="me-view-body">
	<div class="view-body">
    <el-container class="me-view-container">
      <!--<el-aside class="me-area">-->
        <!--<ul class="me-operation-list">-->
          <!--<li class="me-operation-item">-->
            <!--<el-button type="primary" icon="el-icon-edit"></el-button>-->
          <!--</li>-->
        <!--</ul>-->
      <!--</el-aside>-->
      <el-main>
		<a @click="sss">返回</a>
        <div class="me-view-card">
          <h1 class="me-view-title">{{article.title}}</h1>
          <div class="me-view-author">
            <a class="">
              <img class="me-view-picture" src="../../assets/img/tx.gif"></img>
            </a>
            <div class="me-view-info">
              <span>{{article.author}}</span>
              <div class="me-view-meta">
                <span>{{article.createDate | format}}</span>
                <span>阅读  {{article.viewCounts}}</span>
                <span>评论   {{article.count}}</span>
              </div>

            </div>
            <el-button
              v-if="this.article.author.id == this.$store.state.id"
              @click="editArticle()"
              style="position: absolute;left: 60%;"
              size="mini"
              round
              icon="el-icon-edit">编辑</el-button>
          </div>
      <div class="article-alert" v-if="daysDiff > 180">
        <el-alert
          :title="alertMessage"
          type="warning"
          show-icon
          :closable="false">
        </el-alert>
      </div>
       <div class="me-view-content">
            <markdown-editor :editor="article.editor"/>
        </div>

          <div class="me-view-end">
            <el-alert
              title="文章End..."
              type="success"
              center
              :closable="false">
            </el-alert>
          </div>

          <div class="me-view-tag">
            标签：
            <!--<el-tag v-for="t in article.tags" :key="t.id" class="me-view-tag-item" size="mini" type="success">{{t.tagName}}</el-tag>-->
            <el-button @click="tagOrCategory('tag', t.id)" size="mini" type="primary" v-for="t in article.tags" :key="t.id" round plain>{{t.tagName}}</el-button>
          </div>

          <div class="me-view-tag">
            文章分类：
            <!--<span style="font-weight: 600">{{article.category.categoryName}}</span>-->
            <el-button @click="tagOrCategory('category', article.category.id)" size="mini" type="primary" round plain>{{article.category.categoryName}}</el-button>
          </div>

          <div class="me-view-comment">
            <div class="me-view-comment-write">
              <el-row :gutter="20">
                <el-col :span="2">
                  <a class="">
                    <img class="me-view-picture" :src="avatar"></img>
                  </a>
                </el-col>
                <el-col :span="22">
                  <el-input
                    type="textarea"
                    :autosize="{ minRows: 2}"
                    placeholder="你的评论..."
                    class="me-view-comment-text"
                    v-model="comment.content"
                    resize="none">
                  </el-input>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="2" :offset="22">
                  <el-button type="text" @click="publishComment()">评论</el-button>
                </el-col>
              </el-row>
            </div>

            <div class="me-view-comment-title">
              <span>{{article.count}} 条评论</span>
            </div>

            <commment-item v-for="(c,index) in comments"
              :comment="c"
              :articleId="article.id"
              :index="index"
              :rootCommentCounts="comments.length"
              @commentCountsIncrement="commentCountsIncrement"
              :key="c.id">
            </commment-item>

          </div>

        </div>
      </el-main>

    </el-container>
	</div>
	</div>
</div>
</template>

<script>
  import MarkdownEditor from '@/components/markdown/MarkdownEditor'
  import CommmentItem from '@/components/comment/CommentItem'
  import {viewArticle} from '@/api/article'
  import {getCommentsByArticle, publishComment,getCount} from '@/api/comment'

  import default_avatar from '@/assets/img/tx.gif'

  export default {
    name: 'BlogView',
    created() {
      this.getArticle()

    },
    watch: {
      '$route': 'getArticle'
    },
    data() {
      return {
        article: {
		count:0,
          id: '',
          title: '',
          commentCounts: 0,
          viewCounts: 0,
          summary: '',
          author: {},
          tags: [],
          category:{},
          createDate: '',
          editor: {
            value: '',
            toolbarsFlag: false,
            subfield: false,
            defaultOpen: 'preview'
          }
        },
        comments: [],
        comment: {
          article: {},
          content: ''
        }
      }
    },
    computed: {
      // 1. 计算文章发布至今的天数
      daysDiff() {
        if (!this.article.createDate) return 0;

        // 获取当前时间戳
        const now = new Date().getTime();
        // 获取文章发布时间戳 (注意：后端传来的可能是 'yyyy-MM-dd' 字符串，Date对象能自动解析)
        const create = new Date(this.article.createDate).getTime();

        // 计算差值 (毫秒 -> 天)
        const diff = now - create;
        return Math.floor(diff / (1000 * 3600 * 24));
      },

      // 2. 生成提示文字
      alertMessage() {
        return `本文发布于 ${this.daysDiff} 天前，可能其部分内容已经发生变化，如有疑问可询问作者。`;
      },

      avatar() {
        let avatar = this.$store.state.avatar

        if (avatar.length > 0) {
          return avatar
        }
        return default_avatar
      },
      title() {
        return `${this.article.title} - 文章 - Blog`
      }
    },
    methods: {
		sss(){
			this.$router.back()
		},
      // 接收三个参数：类型、ID、名称
      tagOrCategory(type, id) {
        // 核心判断逻辑：
        // 如果文章是加密状态 (viewKeys == 1) 并且 用户未登录 (!token)
        // 后端传过来的 viewKeys 可能是字符串 "1" 也可能是数字 1，使用 == 比较保险
        if (this.article.viewKeys == 1 && !this.$store.state.token) {

          this.$myMessage({
            type: 'warning',
            content: '该信息仅对登录用户开放，请先登录!',
            duration: 3000
          })

          // 跳转登录页
          this.$router.push({path: '/login'})
          return;
        }

        // 其他情况（公开文章或已登录用户），允许跳转
        this.$router.push({path: `/${type}/${id}`})
      },
      editArticle() {
        this.$router.push({path: `/write/${this.article.id}`})
      },
      getArticle() {
        let that = this
        viewArticle(that.$route.params.id,that.$store.state.token).then(data => {

          Object.assign(that.article, data.data)
		  const param = {'name' : that.article.id}


          that.article.editor.value = data.data.body.content

          that.getCommentsByArticle();
		  that.queryCount();
        }).catch(error => {
          if (error !== 'error') {
            that.$myMessage({
              type: 'error',
              content: '文章加载失败',
              duration: 3000
            })
          }
        })
      },
	  open5() {
			this.$notify.info({
			  title: '博客提示',
			  message: '评论暂时关闭',
			  type: 'warning'
			});
	  },
      publishComment() {
        let that = this

        // 添加非空校验
        if (!that.comment.content || !that.comment.content.trim()) {
          that.$myMessage({type: 'warning', content: '评论内容不能为空'})
          return
        }

        that.comment.article.id = that.article.id
        let parms = {articleId:that.article.id,content:that.comment.content}
        publishComment(parms,this.$store.state.token).then(data => {
          if(data.success){
            that.$myMessage({
              type: 'success', content: '评论成功', showClose: true
            })
            that.comment.content = ''
			      that. getCommentsByArticle()//刷新评论区
          }else{
               that.$myMessage({type: 'error', content: data.msg, showClose: true})
          }

        }).catch(error => {
          if (error !== 'error') {
            that.$myMessage({type: 'error', content: error, showClose: true})
          }
        })
      },
	  //评论区
      getCommentsByArticle() {
        let that = this
        getCommentsByArticle(that.article.id).then(data => {
          if(data.success){
               that.comments = data.data
          }else{
             that.$message({type: 'error', message: '评论加载失败', showClose: true})
          }
        }).catch(error => {
          if (error !== 'error') {
            that.$message({type: 'error', message: '评论加载失败', showClose: true})
          }
        })
      },
      commentCountsIncrement() {
        this.getCommentsByArticle();
      },
	  queryCount(){
		let that = this;
		getCount(that.article.id).then(data =>{
			that.article.count=data.data.length;
		}).catch(error=>{
			console.debug(error)
		})

	  }
    },
    components: {
      'markdown-editor': MarkdownEditor,
      CommmentItem
    },
    //组件内的守卫 调整body的背景色
    beforeRouteEnter(to, from, next) {
      window.document.body.style.backgroundColor = '#fff';
      next();
    },
    beforeRouteLeave(to, from, next) {
      window.document.body.style.backgroundColor = '#f5f5f5';
      next();
    }
  }
</script>

<style>
  .me-view-body {

  }
  .view-body{
	  padding-top: 80px;
  }

  .me-view-container {
    max-width: 800px;
	margin: 0 auto;
  }

  .el-main {
    overflow: hidden;
  }

  .me-view-title {
    font-size: 34px;
    font-weight: 800;
    line-height: 1.3;
  }

  .me-view-author {
    /*margin: 30px 0;*/
    margin-top: 30px;
    vertical-align: middle;
  }

  .me-view-picture {
    width: 40px;
    height: 40px;
    border: 1px solid #ddd;
    border-radius: 50%;
    vertical-align: middle;
    background-color: #409EFF;
  }

  .me-view-info {
    display: inline-block;
    vertical-align: middle;
    margin-left: 8px;
  }

  .me-view-meta {
    font-size: 12px;
    color: #969696;
  }

  .me-view-end {
    margin-top: 20px;
  }

  .me-view-tag {
    margin-top: 20px;
    padding-left: 6px;
    border-left: 4px solid #38e598;
  }

  .me-view-tag-item {
    margin: 0 4px;
  }

  .me-view-comment {
    margin-top: 60px;
  }

  .me-view-comment-title {
    font-weight: 600;
    border-bottom: 1px solid #f0f0f0;
    padding-bottom: 20px;
  }

  .me-view-comment-write {
    margin-top: 20px;
  }

  .me-view-comment-text {
    font-size: 16px;
  }

  .v-show-content {
    padding: 8px 25px 15px 30px !important;
  }

  .v-note-wrapper .v-note-panel {
    box-shadow: none !important;
  }

  .v-note-wrapper .v-note-panel .v-note-show .v-show-content, .v-note-wrapper .v-note-panel .v-note-show .v-show-content-html {
    background: #fff !important;
  }
  /* 在 style 标签最后添加 */
  .article-alert {
    margin: 20px 0;
    border-radius: 4px;
    overflow: hidden;
  }


</style>
