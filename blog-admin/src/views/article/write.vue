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
              v-loading="coverLoading"
              element-loading-text="封面上传中..."
              action="/api/upload"
              :headers="tokenHeader"
              :show-file-list="false"
              :on-success="handleCoverSuccess"
              :on-error="handleCoverError"
              :before-upload="beforeCoverUpload"
          >
            <el-image
                v-if="form.cover"
                :src="form.cover"
                class="cover-img"
                fit="cover"
            >
              <template #placeholder>
                <div class="image-loading-slot">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span>图片加载中...</span>
                </div>
              </template>
            </el-image>
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="tips">点击上传封面 (建议尺寸 16:9)</div>
        </el-form-item>

        <el-form-item label="文章内容" prop="body.content">
          <MdEditor
              v-model="form.body.content"
              @onUploadImg="onUploadImg"
              @onChange="onChangeContent"
              style="height: 600px; width: 100%;"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select
                  v-model="form.category.id"
                  placeholder="请选择分类"
                  style="width: 100%"
                  @change="(val: string) => handleCategoryChange(val, false)"
              >
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
import { Plus, Loading } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { publishArticle, updateArticle, getArticleById } from '../../api/article'
import { getTagsByCategoryId } from '../../api/tag'

import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

const route = useRoute()
const router = useRouter()

const isEdit = ref(false)
const publishing = ref(false)
const coverLoading = ref(false)
const formRef = ref()
const categories = ref<any[]>([])
const tags = ref<any[]>([])

const form = reactive({
  id: '',
  title: '',
  summary: '',
  cover: '',
  category: { id: '' },
  tags: [] as any[],
  body: {
    content: '',
    contentHtml: ''
  }
})

const selectedTagIds = computed({
  get: () => form.tags.map(t => t.id),
  set: (val) => {
    form.tags = val.map(id => ({ id }))
  }
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  'body.content': [{ required: true, message: '请输入内容', trigger: 'blur' }],
  'category.id': [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const tokenHeader = { Authorization: localStorage.getItem('token') }

onMounted(async () => {
  fetchCategories()

  const id = route.query.id as string
  if (id) {
    isEdit.value = true
    await loadArticleData(id)

    if (form.category && form.category.id) {
      await handleCategoryChange(form.category.id, true)
    }
  }
})

const loadArticleData = async (id: string) => {
  try {
    const res = await getArticleById(id)
    if (res.data.success) {
      const data = res.data.data
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
    ElMessage.error('加载文章详情失败')
  }
}

const handleCategoryChange = async (categoryId: string, isInit = false) => {
  if (!isInit) {
    form.tags = []
  }
  if (!categoryId) {
    tags.value = []
    return
  }

  try {
    const res = await getTagsByCategoryId(categoryId)
    if (res.data.success) {
      tags.value = res.data.data
    }
  } catch (error) {
    console.error('获取该分类下的标签失败:', error)
  }
}

const onChangeContent = (_val: string, html: string) => {
  form.body.contentHtml = html
}

const onUploadImg = async (files: File[], callback: (urls: string[]) => void) => {
  const resUrls = await Promise.all(
      files.map(async (file) => {
        const formData = new FormData()
        formData.append('image', file)

        try {
          const res = await request.post('/upload', formData, {
            headers: {
              'Content-Type': 'multipart/form-data'
            }
          })
          if (res.data.success) {
            return res.data.data
          } else {
            ElMessage.error(res.data.msg || '图片上传失败')
            return ''
          }
        } catch (e) {
          ElMessage.error('图片上传异常')
          return ''
        }
      })
  )
  callback(resUrls.filter(url => url !== ''))
}

const handleSubmit = async () => {
  if (publishing.value || !formRef.value) return

  publishing.value = true

  try {
    await formRef.value.validate()

    let res;
    if (isEdit.value) {
      res = await updateArticle(form)
    } else {
      res = await publishArticle(form)
    }

    if (res.data.success) {
      ElMessage.success(isEdit.value ? '更新成功' : '发布成功')
      router.push('/article/list')
    } else {
      ElMessage.error(res.data.msg || '操作失败')
      publishing.value = false
    }
  } catch (e) {
    console.error('表单校验未通过或请求异常:', e)
    publishing.value = false
  }
}

const handleCoverSuccess = (res: any) => {
  coverLoading.value = false
  if(res.success) {
    form.cover = res.data
  } else {
    ElMessage.error('上传失败')
  }
}

const handleCoverError = () => {
  coverLoading.value = false
  ElMessage.error('网络或接口异常')
}

const beforeCoverUpload = (file: any) => {
  const isImg = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 5
  if (!isImg) ElMessage.error('只能上传图片!')
  if (!isLt2M) ElMessage.error('图片大小不能超过 5MB!')

  if (isImg && isLt2M) {
    coverLoading.value = true
    return true
  }
  return false
}

const fetchCategories = async () => {
  const res = await request.get('/categorys')
  if(res.data.success) categories.value = res.data.data
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

.image-loading-slot {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
}
.image-loading-slot .el-icon {
  font-size: 20px;
  margin-bottom: 5px;
}
</style>