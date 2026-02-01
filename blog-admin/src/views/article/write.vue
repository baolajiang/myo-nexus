<template>
  <div class="write-container">
    <el-card class="write-card">
      <template #header>
        <div class="card-header">
          <h2>{{ isEdit ? '编辑文章' : '写文章' }}</h2>
          <div class="header-actions">
            <el-button @click="$router.back()">返回</el-button>
            <el-button type="primary" :loading="publishing" @click="handleSubmit">
              {{ isEdit ? '更新' : '发布' }}
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="form" ref="formRef" :rules="rules" label-width="80px" label-position="top">

        <el-form-item label="文章标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文章标题" size="large" />
        </el-form-item>

        <el-form-item label="文章封面" prop="cover">
          <el-upload
              class="cover-uploader"
              action="/api/upload"
              :headers="tokenHeader"
              :show-file-list="false"
              :on-success="handleCoverSuccess"
              :before-upload="beforeCoverUpload"
          >
            <img v-if="form.cover" :src="form.cover" class="cover-img" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="tips">点击上传封面 (建议尺寸 16:9)</div>
        </el-form-item>

        <el-form-item label="文章内容" prop="body.content">
          <el-input
              v-model="form.body.content"
              type="textarea"
              :rows="15"
              placeholder="请输入 Markdown 内容..."
              class="markdown-input"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category.id" placeholder="请选择分类" style="width: 100%">
                <el-option
                    v-for="item in categories"
                    :key="item.id"
                    :label="item.categoryName"
                    :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="标签" prop="tags">
              <el-select
                  v-model="selectedTagIds"
                  multiple
                  placeholder="请选择标签"
                  style="width: 100%"
              >
                <el-option
                    v-for="item in tags"
                    :key="item.id"
                    :label="item.tagName"
                    :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="文章摘要" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="如果不填，将自动截取正文前 100 字" />
        </el-form-item>

      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '../../utils/request' // 假设你在这个路径
import { getArticleDetail } from '../../api/article'

// 路由
const route = useRoute()
const router = useRouter()

// 状态
const isEdit = ref(false)
const publishing = ref(false)
const formRef = ref()
const categories = ref([]) // 需调用API获取
const tags = ref([])       // 需调用API获取

// 表单数据
const form = reactive({
  id: '',
  title: '',
  summary: '',
  cover: '',
  category: { id: '' },
  tags: [] as any[],
  body: {
    content: '',
    contentHtml: '' // 简单处理，如果用 MD编辑器通常会生成 HTML
  }
})

// 辅助变量：用于 el-select 多选标签，因为 form.tags 结构是对象数组 [{id:1}, {id:2}]
// 我们需要一个纯 ID 数组 [1, 2] 来绑定 select
const selectedTagIds = computed({
  get: () => form.tags.map(t => t.id),
  set: (val) => {
    // 当 select 变化时，把 ID 数组转回对象数组
    form.tags = val.map(id => ({ id }))
  }
})

// 校验规则
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  'body.content': [{ required: true, message: '请输入内容', trigger: 'blur' }],
  'category.id': [{ required: true, message: '请选择分类', trigger: 'change' }]
}

// 上传 Header
const tokenHeader = { Authorization: localStorage.getItem('token') }

// --- 生命周期 ---
onMounted(async () => {
  // 1. 获取所有分类和标签 (你需要实现这两个接口)
  fetchCategories()
  fetchTags()

  // 2. 检查是否有 ID 参数 (编辑模式)
  const id = route.query.id as string
  if (id) {
    isEdit.value = true
    await loadArticleData(id)
  }
})

// --- 方法 ---

// 加载文章详情
const loadArticleData = async (id: string) => {
  try {
    // 复用你已有的 findArticleById 接口
    // 注意：后台通常用 /admin/article/{id}，如果你复用前台接口也可以，只要权限够
    // 这里假设你在 api/article.ts 里封装了 getArticleDetail(id)
    const res = await request.post(`/articles/view/${id}`)
    if (res.data.success) {
      const data = res.data.data

      // 数据回显
      form.id = data.id
      form.title = data.title
      form.summary = data.summary
      form.cover = data.cover
      form.category = data.category || { id: '' }
      form.tags = data.tags || []
      form.body.content = data.body.content
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载文章失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      publishing.value = true
      try {
        // 简单处理 contentHtml (实际应由 MD 编辑器生成)
        form.body.contentHtml = form.body.content

        // 调用发布接口 (后端已修改为支持更新)
        const res = await request.post('/articles/publish', form)

        if (res.data.success) {
          ElMessage.success(isEdit.value ? '更新成功' : '发布成功')
          router.push('/article/list')
        } else {
          ElMessage.error(res.data.msg || '操作失败')
        }
      } catch (e) {
        console.error(e)
      } finally {
        publishing.value = false
      }
    }
  })
}

// 上传相关
const handleCoverSuccess = (res: any) => {
  if(res.success) {
    form.cover = res.data
  } else {
    ElMessage.error('上传失败')
  }
}
const beforeCoverUpload = (file: any) => {
  const isImg = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 5
  if (!isImg) ElMessage.error('只能上传图片!')
  if (!isLt2M) ElMessage.error('图片大小不能超过 5MB!')
  return isImg && isLt2M
}

// 模拟获取分类标签 (请替换为真实接口)
const fetchCategories = async () => {
  const res = await request.get('/categorys') // 假设
  if(res.data.success) categories.value = res.data.data
}
const fetchTags = async () => {
  const res = await request.get('/tags') // 假设
  if(res.data.success) tags.value = res.data.data
}

</script>

<style scoped>
.write-card {
  min-height: 80vh;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.cover-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 178px;
  height: 100px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.cover-uploader:hover {
  border-color: #409EFF;
}
.cover-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.tips {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}
.markdown-input :deep(.el-textarea__inner) {
  font-family: Consolas, "Courier New", monospace;
  line-height: 1.5;
}
</style>