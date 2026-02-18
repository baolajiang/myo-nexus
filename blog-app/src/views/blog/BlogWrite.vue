<template>
  <div id="write" v-title :data-title="title">
    <el-container class="write-container">

      <el-header class="write-header" height="64px">
        <div class="header-left">
          <div class="back-home-btn" @click="cancel">
            <i class="el-icon-house"></i>
            <span>首頁</span>
          </div>
          <div class="divider"></div>
          <span class="header-status">稿件管理 / 寫文章</span>
        </div>

        <div class="header-right">
          <div class="save-status">已自動保存</div>
          <el-button type="primary" class="b-publish-btn" size="medium" round @click="publishShow">發佈文章</el-button>
          <el-avatar :size="36" :src="userAvatar" class="header-avatar"></el-avatar>
        </div>
      </el-header>

      <el-main class="write-main">
        <div class="write-card">
          <div class="title-section">
            <el-input
              type="textarea"
              autosize
              placeholder="請輸入一個吸引人的標題吧~"
              v-model="articleForm.title"
              maxlength="100"
              class="b-title-input"
            ></el-input>
            <div class="title-underline"></div>
          </div>

          <div class="editor-section">
            <mavon-editor
              ref="md"
              v-model="articleForm.content"
              class="b-editor"
              :ishljs="true"
              placeholder="請開始你的表演..."
              @imgAdd="imgAdd"
              style="min-height: 70vh; z-index: 1;"
            />
          </div>
        </div>
      </el-main>

      <el-dialog
        title="發佈設置"
        :visible.sync="publishVisible"
        :close-on-click-modal="false"
        width="600px"
        custom-class="b-dialog"
      >
        <el-form :model="articleForm" ref="articleForm" :rules="rules" label-position="top">

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="文章分類" prop="category">
                <el-select v-model="articleForm.category" value-key="id" placeholder="選擇分類" style="width:100%">
                  <el-option v-for="c in categorys" :key="c.id" :label="c.categoryName" :value="c"></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="誰可以看" prop="viewKeys">
                <el-select v-model="articleForm.viewKeys" placeholder="選擇權限" style="width:100%">
                  <el-option label="全員可見" :value="1">
                    <span style="float: left">全員可見</span>
                    <span style="float: right; color: #8492a6; font-size: 13px"><i class="el-icon-view"></i></span>
                  </el-option>
                  <el-option label="僅自己可見" :value="2">
                    <span style="float: left">僅自己可見</span>
                    <span style="float: right; color: #8492a6; font-size: 13px"><i class="el-icon-lock"></i></span>
                  </el-option>
                  <el-option label="登錄可見" :value="3">
                    <span style="float: left">登錄可見</span>
                    <span style="float: right; color: #8492a6; font-size: 13px"><i class="el-icon-user"></i></span>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="文章封面" prop="cover">
            <div
              class="cover-uploader"
              @click="triggerCoverUpload"
              v-loading="coverLoading"
              element-loading-text="上傳中..."
              element-loading-spinner="el-icon-loading"
              element-loading-background="rgba(255, 255, 255, 0.8)"
            >
              <img v-if="articleForm.cover" :src="articleForm.cover" class="cover-image">
              <div v-else class="upload-placeholder">
                <i class="el-icon-plus"></i>
                <span>點擊上傳封面</span>
              </div>
            </div>
            <input type="file" ref="coverInput" accept="image/*" style="display: none" @change="handleCoverUpload">
          </el-form-item>

          <el-form-item label="添加標籤" prop="tags">
            <div class="b-tag-wrapper">
              <el-checkbox-group v-model="articleForm.tags">
                <el-checkbox v-for="t in tags" :key="t.id" :label="t.id" size="small" class="b-tag-checkbox">
                  # {{t.tagName}}
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </el-form-item>

          <el-form-item label="文章摘要" prop="summary">
            <el-input
              type="textarea"
              v-model="articleForm.summary"
              :rows="3"
              placeholder="給你的文章寫一段簡短的介紹吧..."
              maxlength="200"
              show-word-limit
            ></el-input>
          </el-form-item>
        </el-form>
        <div slot="footer" class="b-dialog-footer">
          <el-button @click="publishVisible = false">再改改</el-button>
          <el-button type="primary" class="b-final-btn" @click="publish('articleForm')">確認提交</el-button>
        </div>
      </el-dialog>

    </el-container>
  </div>
</template>

<script>
import { mavonEditor } from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'

import { publishArticle, getArticleById } from '@/api/article'
import { getAllCategorys } from '@/api/category'
import { getAllTags } from '@/api/tag'
import { upload } from '@/api/upload'

export default {
  name: 'BlogWrite',
  components: { mavonEditor },
  data() {
    return {
      publishVisible: false,
      coverLoading: false, //  控制封面局部的 loading 狀態
      categorys: [],
      tags: [],
      articleForm: {
        id: '',
        title: '',
        summary: '',
        category: '',
        tags: [],
        content: '',
        cover: '',
        viewKeys: 1
      },
      rules: {
        summary: [{required: true, message: '請輸入摘要', trigger: 'blur'}],
        category: [{required: true, message: '請選擇分類', trigger: 'change'}],
        tags: [{type: 'array', required: true, message: '請選擇標籤', trigger: 'change'}],
        viewKeys: [{required: true, message: '請選擇可視範圍', trigger: 'change'}]
      }
    }
  },
  computed: {
    title() {
      return '寫文章 - ' + (this.articleForm.title || '無標題')
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
    getArticle() {
      getArticleById(this.$route.params.id).then(res => {
        if(res.success){
          let article = res.data
          this.articleForm.id = article.id
          this.articleForm.title = article.title
          this.articleForm.summary = article.summary
          this.articleForm.category = article.category
          this.articleForm.tags = article.tags.map(t => t.id)
          this.articleForm.content = article.body.content
          this.articleForm.cover = article.cover
          this.articleForm.viewKeys = article.viewKeys || 1
        }
      })
    },
    getCategorysAndTags() {
      getAllCategorys().then(res => { this.categorys = res.data })
      getAllTags().then(res => { this.tags = res.data })
    },
    imgAdd(pos, $file) {
      var formdata = new FormData();
      formdata.append('image', $file);
      formdata.append('path', 'articles');
      upload(formdata).then(res => {
        if(res.success) {
          this.$refs.md.$img2Url(pos, res.data);
        } else {
          this.$message.error(res.msg);
        }
      })
    },
    triggerCoverUpload() {
      this.$refs.coverInput.click()
    },
    handleCoverUpload(event) {
      const file = event.target.files[0]
      if (!file) return

      if (file.size > 2 * 1024 * 1024) {
        this.$message.warning('封面圖片建議小於 2MB 哦');
        return;
      }

      //  開始上傳：只開啟局部 Loading
      this.coverLoading = true;

      var formdata = new FormData();
      formdata.append('image', file);
      formdata.append('path', 'covers');

      upload(formdata).then(res => {
        this.coverLoading = false; //  結束上傳
        if(res.success) {
          this.articleForm.cover = res.data;
          this.$message.success('封面設置成功！');
        } else {
          this.$message.error(res.msg);
        }
      }).catch(() => {
        this.coverLoading = false; //  出錯也要關閉
        this.$message.error('上傳失敗');
      })
      event.target.value = '';
    },
    publishShow() {
      if (!this.articleForm.title) {
        this.$message({message: '標題不能為空哦', type: 'warning'})
        return
      }
      if (!this.articleForm.content) {
        this.$message({message: '內容不能為空哦', type: 'warning'})
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
              contentHtml: this.$refs.md.d_render
            },
            cover: this.articleForm.cover,
            viewKeys: this.articleForm.viewKeys
          }
          this.publishVisible = false
          publishArticle(article).then(res => {
            if (res.success) {
              this.$message({message: '發佈成功啦！', type: 'success'})
              this.$router.push({path: `/view/${res.data.id}`})
            } else {
              this.$message({message: res.msg, type: 'error'})
            }
          })
        }
      });
    },
    cancel() {
      this.$confirm('文章將不會保存, 是否繼續?', '提示', {
        confirmButtonText: '確定',
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
  min-height: 100vh;
  /* 莫蘭迪灰藍 */
  background: #eef2f5;
  display: flex;
  flex-direction: column;
}

.write-header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1000;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03);
}

.header-left { display: flex; align-items: center; }
.back-home-btn { font-size: 14px; color: #61666d; cursor: pointer; display: flex; align-items: center; gap: 5px; }
.back-home-btn:hover { color: #fb7299; }
.header-right { display: flex; align-items: center; gap: 24px; }
.save-status { font-size: 12px; color: #9499a0; }
.b-publish-btn { background-color: #fb7299; border: none; padding: 10px 24px; font-weight: bold; }
.b-publish-btn:hover { transform: scale(1.05); background-color: #ff85ad; }
.write-main { margin-top: 84px; padding: 0 20px 40px; }
.write-card { max-width: 1100px; margin: 0 auto; background: rgba(255, 255, 255, 0.95); border-radius: 12px; padding: 40px; box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05); }
.title-section { margin-bottom: 30px; position: relative; }
.b-title-input >>> .el-textarea__inner { border: none; resize: none; font-size: 30px; font-weight: 600; color: #18191c; padding: 0; background: transparent; line-height: 1.4; }
.title-underline { height: 2px; background: #e3e5e7; width: 100%; margin-top: 10px; }
.b-editor { border: 1px solid #f1f2f3 !important; border-radius: 8px !important; box-shadow: none !important; }

/* 彈窗樣式 */
.b-dialog >>> .el-dialog { border-radius: 16px; overflow: hidden; }
.b-dialog >>> .el-dialog__header { text-align: center; font-weight: bold; padding-top: 30px; }
.b-tag-wrapper { background: #f6f7f8; padding: 15px; border-radius: 8px; }
.b-tag-checkbox { margin-right: 15px !important; margin-bottom: 5px; }
.b-final-btn { background: #00aeec; border: none; width: 140px; }


.cover-uploader {
  width: 320px;
  height: 180px;
  margin: 0 auto;
  border: 2px dashed #e3e5e7;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: border-color 0.3s;
  overflow: hidden;
  background-color: #fbfbfb;
  position: relative; /* 為了讓 v-loading 定位正確 */
}
.cover-uploader:hover { border-color: #409EFF; }
.upload-placeholder { display: flex; flex-direction: column; align-items: center; color: #8c939d; }
.upload-placeholder i { font-size: 28px; margin-bottom: 8px; }
.cover-image { width: 100%; height: 100%; object-fit: cover; }
</style>
