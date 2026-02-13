<template>
  <div v-title :data-title="categoryTagTitle">
    <div class="me-allct-body">
      <el-container class="me-allct-container">
        <el-main>
          <el-tabs v-model="activeName">
            <el-tab-pane label="文章分类" name="category" class="category-title">
              <ul class="me-allct-items">
                <li v-for="c in categorys" @click="view(c.id)" :key="c.id" class="me-allct-item">
                  <div class="me-allct-content">
                    <a class="me-allct-info">
                      <img class="me-allct-img" :src="c.avatar ? c.avatar : defaultAvatar"/>
                      <h4 class="me-allct-name">{{c.categoryName}}</h4>
                      <p class="me-allct-description">{{c.description}}</p>
                    </a>
                    <div class="me-allct-meta">
                      <span>{{c.articles}} 篇文章</span>
                    </div>
                  </div>
                </li>
              </ul>
            </el-tab-pane>
            <el-tab-pane label="标签" name="tag" class="category-title">
              <ul class="me-allct-items">
                <li v-for="t in tags" @click="view(t.id)" :key="t.id" class="me-allct-item">
                  <div class="me-allct-content">
                    <a class="me-allct-info">
                      <img class="me-allct-img" :src="t.avatar ? t.avatar : defaultAvatar"/>
                      <h4 class="me-allct-name">{{t.tagName}}</h4>
                    </a>
                    <div class="me-allct-meta">
                      <span>相关文章</span>
                    </div>
                  </div>
                </li>
              </ul>
            </el-tab-pane>
          </el-tabs>
        </el-main>
      </el-container>
    </div>
  </div>
</template>

<script>
import {getAllCategorysDetail} from '@/api/category'
import {getAllTagsDetail} from '@/api/tag'

export default {
  name: 'BlogAllCategoryTag',
  created() {
    this.getCategorys()
    this.getTags()
  },
  data() {
    return {
      defaultAvatar: '/static/category/front.png', // 建议给个默认图片路径
      categorys: [],
      tags: [],
      currentActiveName: 'category'
    }
  },
  computed: {
    activeName: {
      get() {
        return (this.currentActiveName = this.$route.params.type)
      },
      set(newValue) {
        this.currentActiveName = newValue
      }
    },
    categoryTagTitle (){
      if(this.currentActiveName == 'category'){
        return '文章分类'
      }
      return '标签'
    }
  },
  methods: {
    view(id) {
      this.$router.push({path: `/${this.currentActiveName}/${id}`})
    },
    getCategorys() {
      let that = this
      getAllCategorysDetail().then(data => {
        that.categorys = data.data
      }).catch(error => {
        if (error !== 'error') {
          that.$myMessage({
            type: 'error',
            content: '文章分类加载失败',
            duration: 3000
          })
        }
      })
    },
    getTags() {
      let that = this
      getAllTagsDetail().then(data => {
        that.tags = data.data
      }).catch(error => {
        if (error !== 'error') {
          that.$myMessage({
            type: 'error',
            content: '标签加载失败',
            duration: 3000
          })
        }
      })
    }
  },
  beforeRouteEnter(to, from, next) {
    window.document.body.style.backgroundColor = '#f4f5f5'; // 改为浅灰背景，更能凸显白色卡片
    next();
  },
  beforeRouteLeave(to, from, next) {
    window.document.body.style.backgroundColor = '#f5f5f5';
    next();
  }
}
</script>

<style scoped>
.me-allct-body {
  margin: 0 auto;
  padding-top: 40px;
}

.me-allct-container {
  max-width: 1000px;
  margin: 0 auto;
}

/* 标签页标题样式优化 */
.el-tabs__item {
  font-size: 16px;
  color: #909399;
}
.el-tabs__item.is-active {
  color: #ea9c00;
  font-weight: bold;
}

/* 列表容器改为 flex 布局以更好地控制间距 */
.me-allct-items {
  padding-top: 3.5rem;
  display: flex;
  flex-wrap: wrap;
  list-style: none;
  padding-left: 0;
}

.me-allct-item {
  width: 25%;
  margin-bottom: 3.5rem;
  padding: 0 15px;
  box-sizing: border-box;
}

/* 卡片基础样式 */
.me-allct-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  text-align: center;
  padding: 0 1rem 1.5rem;
}

/* 卡片悬浮动画 */
.me-allct-content:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
}

/* 让文字在悬浮时变色 */
.me-allct-content:hover .me-allct-name {
  color: #ea9c00;
}

.me-allct-info {
  cursor: pointer;
  width: 100%;
  text-decoration: none;
}

/* 头像立体浮出效果 */
.me-allct-img {
  margin-top: -35px;
  margin-bottom: 15px;
  width: 70px;
  height: 70px;
  border-radius: 50%;
  border: 4px solid #ffffff;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  background-color: #fff;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.me-allct-content:hover .me-allct-img {
  transform: scale(1.05);
}

/* 标题与描述文字 */
.me-allct-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 10px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.3s;
}

.me-allct-description {
  min-height: 40px;
  font-size: 13px;
  line-height: 20px;
  color: #777;
  /* 多行文本省略号 */
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

/* 底部文章数量标识 */
.me-allct-meta {
  margin-top: 15px;
  font-size: 12px;
  color: #999;
  background-color: #f7f9fa;
  padding: 4px 12px;
  border-radius: 20px;
}
</style>
