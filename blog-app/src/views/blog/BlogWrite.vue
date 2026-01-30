<template>
  <div id="write" v-title :data-title="title">
    <el-container class="write-container">

      <el-header class="write-header" height="60px">
        <div class="header-left">
          <div class="back-btn" @click="cancel">
            <i class="el-icon-arrow-left"></i>
            <span>返回</span>
          </div>
          <span class="header-title-text">写文章</span>
        </div>

        <div class="header-right">
          <el-avatar :size="32" :src="userAvatar" class="header-avatar"></el-avatar>
          <el-button type="primary" size="medium" round @click="publishShow">发布文章</el-button>
        </div>
      </el-header>

      <el-main class="write-main">
        <div class="write-content">
          <div class="title-input-wrapper">
            <el-input
              type="textarea"
              autosize
              placeholder="请输入标题..."
              v-model="articleForm.title"
              maxlength="100"
              class="title-input"
            ></el-input>
          </div>

          <div class="editor-wrapper">
            <mavon-editor
              ref="md"
              v-model="articleForm.content"
              class="me-editor"
              :ishljs="true"
              placeholder="开始你的创作..."
              @imgAdd="imgAdd"
              style="min-height: 600px; z-index: 1;"
            />
          </div>
        </div>
      </el-main>

      <el-dialog
        title="发布文章"
        :visible.sync="publishVisible"
        :close-on-click-modal="false"
        width="600px"
      >
        <el-form :model="articleForm" ref="articleForm" :rules="rules" label-width="80px" label-position="top">
          <el-form-item label="文章摘要" prop="summary">
            <el-input type="textarea" v-model="articleForm.summary" :rows="4" placeholder="好的摘要能吸引更多读者..."></el-input>
          </el-form-item>
          <el-form-item label="选择分类" prop="category">
            <el-select v-model="articleForm.category" value-key="id" placeholder="请选择文章分类" style="width:100%">
              <el-option v-for="c in categorys" :key="c.id" :label="c.categoryName" :value="c"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="添加标签" prop="tags">
            <el-checkbox-group v-model="articleForm.tags">
              <el-checkbox v-for="t in tags" :key="t.id" :label="t.id" border size="small" class="tag-checkbox">{{t.tagName}}</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button @click="publishVisible = false">取消</el-button>
          <el-button type="primary" @click="publish('articleForm')">确定发布</el-button>
        </div>
      </el-dialog>

    </el-container>
  </div>
</template>

<script>
// 【关键】直接引入 mavon-editor 和样式
import { mavonEditor } from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'

import { publishArticle, getArticleById } from '@/api/article'
import { getAllCategorys } from '@/api/category'
import { getAllTags } from '@/api/tag'
import { upload } from '@/api/upload'

export default {
  name: 'BlogWrite',
  components: { mavonEditor }, // 注册组件
  data() {
    return {
      publishVisible: false,
      categorys: [],
      tags: [],
      articleForm: {
        id: '',
        title: '',
        summary: '',
        category: '',
        tags: [],
        content: '' // 【关键】直接绑定字符串，简单直接
      },
      rules: {
        summary: [{required: true, message: '请输入摘要', trigger: 'blur'}],
        category: [{required: true, message: '请选择分类', trigger: 'change'}],
        tags: [{type: 'array', required: true, message: '请选择标签', trigger: 'change'}]
      }
    }
  },
  computed: {
    title() {
      return '写文章 - ' + (this.articleForm.title || '无标题')
    },
    userAvatar() {
      return this.$store.state.avatar || '/static/img/default_avatar.png'
    }
  },
  mounted() {
    if (this.$route.params.id) {
      this.getArticle()
    }
    this.getCategorysAndTags()
  },
  methods: {
    // 获取文章详情（编辑模式）
    getArticle() {
      getArticleById(this.$route.params.id).then(res => {
        if(res.success){
          let article = res.data
          this.articleForm.id = article.id
          this.articleForm.title = article.title
          this.articleForm.summary = article.summary
          this.articleForm.category = article.category
          this.articleForm.tags = article.tags.map(t => t.id)
          // 【关键】回显内容
          this.articleForm.content = article.body.content
        }
      })
    },
    getCategorysAndTags() {
      getAllCategorys().then(res => { this.categorys = res.data })
      getAllTags().then(res => { this.tags = res.data })
    },

    // 【关键】图片上传功能
    imgAdd(pos, $file) {
      var formdata = new FormData();
      formdata.append('image', $file);
      // 后端根据这个字段，把图片存到 /articles/2026/01/xxx.png
      formdata.append('path', 'articles');
      upload(formdata).then(res => {
        if(res.success) {
          this.$refs.md.$img2Url(pos, res.data);
        } else {
          this.$message.error(res.msg);
        }
      }).catch(err => {
        console.error(err)
        this.$message.error('图片上传失败');
      })
    },

    publishShow() {
      if (!this.articleForm.title) {
        this.$message({message: '标题不能为空哦', type: 'warning'})
        return
      }
      if (!this.articleForm.content) {
        this.$message({message: '内容不能为空哦', type: 'warning'})
        return
      }
      this.publishVisible = true
    },

    publish(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          let tags = this.articleForm.tags.map(id => { return {id: id} });

          let article = {
            id: this.articleForm.id,
            title: this.articleForm.title,
            summary: this.articleForm.summary,
            category: this.articleForm.category,
            tags: tags,
            body: {
              content: this.articleForm.content,
              // 【关键】获取渲染后的 HTML
              contentHtml: this.$refs.md.d_render
            }
          }

          this.publishVisible = false
          publishArticle(article).then(res => {
            if (res.success) {
              this.$message({message: '发布成功啦！', type: 'success'})
              this.$router.push({path: `/view/${res.data.id}`})
            } else {
              this.$message({message: res.msg, type: 'error'})
            }
          })
        }
      });
    },
    cancel() {
      this.$confirm('文章将不会保存, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$router.push('/')
      }).catch(() => {});
    }
  },
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

<style scoped>
.write-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部 Header */
.write-header {
  background: #fff;
  border-bottom: 1px solid #ddd;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-btn {
  font-size: 15px;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: color 0.3s;
}
.back-btn:hover { color: #409EFF; }
.back-btn i { margin-right: 4px; font-weight: bold; }

.header-title-text {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}
.header-avatar { border: 1px solid #eee; }

/* 主体区域 */
.write-main {
  margin-top: 60px; /* 避开 Header */
  padding: 40px 0;
  overflow-y: auto;
}

.write-content {
  width: 900px;
  margin: 0 auto;
}

/* 标题输入框 */
.title-input-wrapper { margin-bottom: 30px; }

.title-input >>> .el-textarea__inner {
  border: none;
  resize: none;
  font-size: 32px;
  font-weight: bold;
  color: #000;
  padding: 0;
  background: transparent;
  line-height: 1.5;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
}
.title-input >>> .el-textarea__inner::placeholder { color: #ccc; font-weight: 400; }

/* 编辑器样式修复 */
.me-editor {
  z-index: 1 !important; /* 防止被 header 遮挡 */
  box-shadow: none !important; /* 去掉默认阴影，更清爽 */
  border: 1px solid #eee;
  border-radius: 4px;
}

/* Dialog 样式 */
.tag-checkbox {
  margin-bottom: 10px;
  margin-left: 0 !important;
  margin-right: 10px;
}
</style>
