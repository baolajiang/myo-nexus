<template>
  <div class="category-tag-container">
    <el-row :gutter="20" class="full-height">

      <el-col :span="8" class="full-height">
        <el-card class="box-card left-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>分类管理</span>
              <el-button type="primary" link icon="Plus" @click="handleAddCategory">新增分类</el-button>
            </div>
          </template>

          <div class="category-list-wrapper" v-loading="categoryLoading">
            <ul class="category-list">
              <li
                  v-for="item in categoryList"
                  :key="item.id"
                  :class="{ active: currentCategory?.id === item.id }"
                  @click="handleSelectCategory(item)"
              >
                <div class="cat-info">
                  <el-image
                      v-if="item.avatar"
                      :src="item.avatar"
                      class="cat-avatar"
                  >
                    <template #error>
                      <div class="image-slot">
                        <el-icon><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <span class="cat-name">{{ item.categoryName }}</span>
                </div>

                <div class="cat-action" @click.stop>
                  <el-button type="primary" link icon="Edit" @click="handleEditCategory(item)">编辑</el-button>
                  <el-button type="danger" link icon="Delete" @click="handleDeleteCategory(item.id)">删除</el-button>
                </div>
              </li>
            </ul>
            <el-empty v-if="categoryList.length === 0" description="暂无分类数据" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="16" class="full-height">
        <el-card class="box-card right-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>
                <span v-if="currentCategory" class="highlight-text">【{{ currentCategory.categoryName }}】</span>
                下属标签管理
              </span>
              <el-button
                  type="success"
                  icon="Plus"
                  :disabled="!currentCategory"
                  @click="handleAddTag"
              >新增标签</el-button>
            </div>
          </template>

          <el-table
              :data="tagList"
              style="width: 100%"
              height="100%"
              v-loading="tagLoading"
              border
          >
            <el-table-column label="标签图标" width="100" align="center">
              <template #default="scope">
                <el-image
                    style="width: 30px; height: 30px; border-radius: 4px"
                    :src="scope.row.avatar"
                    fit="cover"
                    preview-teleported
                >
                  <template #error>
                    <div class="image-slot">无图</div>
                  </template>
                </el-image>
              </template>
            </el-table-column>

            <el-table-column prop="tagName" label="标签名称" min-width="150" />

            <el-table-column label="操作" width="160" fixed="right" align="center">
              <template #default="scope">
                <el-button type="primary" link icon="Edit" @click="handleEditTag(scope.row)">编辑</el-button>
                <el-button type="danger" link icon="Delete" @click="handleDeleteTag(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty :description="currentCategory ? '该分类下暂无标签' : '请先在左侧选择一个分类'" />
            </template>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="catDialogVisible" :title="isEditCat ? '编辑分类' : '新增分类'" width="500px" @close="resetCatForm">
      <el-form ref="catFormRef" :model="catForm" :rules="catRules" label-width="80px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="catForm.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类图标" prop="avatar">
          <el-upload
              class="avatar-uploader"
              action=""
              :auto-upload="false"
              :show-file-list="false"
              :on-change="(file: any) => handleFileSelect(file, 'category')"
          >
            <img v-if="catForm.avatar" :src="catForm.avatar" class="uploaded-avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="分类描述" prop="description">
          <el-input type="textarea" v-model="catForm.description" placeholder="请输入描述" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="catDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitCatForm">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="tagDialogVisible" :title="isEditTag ? '编辑标签' : '新增标签'" width="400px" @close="resetTagForm">
      <el-form ref="tagFormRef" :model="tagForm" :rules="tagRules" label-width="80px">
        <el-form-item label="归属分类">
          <el-input :value="currentCategory?.categoryName" disabled />
        </el-form-item>
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="tagForm.tagName" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="标签图标" prop="avatar">
          <el-upload
              class="avatar-uploader"
              action=""
              :auto-upload="false"
              :show-file-list="false"
              :on-change="(file: any) => handleFileSelect(file, 'tag')"
          >
            <img v-if="tagForm.avatar" :src="tagForm.avatar" class="uploaded-avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="tagDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitTagForm">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="cropperVisible" title="裁剪图标" width="800px" append-to-body :close-on-click-modal="false" custom-class="bili-cropper-dialog">
      <div class="bili-cropper-layout">
        <div class="cropper-left">
          <div class="cropper-box-wrap">
            <VueCropper
                ref="cropperRef"
                :img="cropperImg"
                :outputSize="1"
                outputType="png"
                :info="true"
                :full="false"
                :canMove="true"
                :canMoveBox="true"
                :autoCrop="true"
                :autoCropWidth="200"
                :autoCropHeight="200"
                :fixedBox="true"
                :centerBox="true"
                :high="true"
                @realTime="realTime">
            </VueCropper>
          </div>
          <p style="margin-top:12px;font-size:12px;color:#999">支持 JPG、PNG，建议图片小于 2MB</p>
        </div>
        <div class="cropper-right">
          <p class="preview-title">预览</p>
          <div class="preview-item">
            <div class="preview-circle-box" style="width:100px;height:100px">
              <div :style="ps(100)">
                <div :style="previews.div">
                  <img :src="previews.url" :style="previews.img">
                </div>
              </div>
            </div>
            <p class="size-text">100px</p>
          </div>
          <div class="preview-item">
            <div class="preview-circle-box" style="width:50px;height:50px">
              <div :style="ps(50)">
                <div :style="previews.div">
                  <img :src="previews.url" :style="previews.img">
                </div>
              </div>
            </div>
            <p class="size-text">50px</p>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cropperVisible = false">取消</el-button>
          <el-button type="primary" @click="finishCrop" :loading="cropLoading">确认裁剪</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Plus } from '@element-plus/icons-vue'
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '../../api/category'
// 完善点1：引入了标签的新增、修改、删除接口
import { getTagsByCategoryId, addTag, updateTag, deleteTag } from '../../api/tag'
import { upload } from '../../api/upload'

import 'vue-cropper/dist/index.css'
import { VueCropper } from 'vue-cropper'

// 列表数据
const categoryList = ref<any[]>([])
const tagList = ref<any[]>([])
const currentCategory = ref<any>(null)

// 加载状态
const categoryLoading = ref(false)
const tagLoading = ref(false)

// ======== 裁剪逻辑相关的状态 ========
const cropperVisible = ref(false)
const cropperImg = ref('')
const cropLoading = ref(false)
const cropperRef = ref()
const previews = ref<any>({})
const currentUploadTarget = ref('')

// 选择图片后不直接上传，而是打开裁剪弹窗
const handleFileSelect = (file: any, target: string) => {
  const rawFile = file.raw
  if (rawFile.type.indexOf('image/') === -1) {
    ElMessage.warning('请选择图片文件')
    return false
  }
  currentUploadTarget.value = target

  // 读取本地文件转换为 Base64 给裁剪组件
  const reader = new FileReader()
  reader.onload = (e: any) => {
    cropperImg.value = e.target.result
    cropperVisible.value = true
  }
  reader.readAsDataURL(rawFile)
}

// 实时预览事件
const realTime = (data: any) => {
  previews.value = data
}

// 计算预览缩放比例
const ps = (size: number) => {
  if (!previews.value.w) return {}
  const sc = size / previews.value.w
  return {
    width: previews.value.w + 'px',
    height: previews.value.h + 'px',
    transform: `scale(${sc})`,
    transformOrigin: 'top left',
    position: 'relative' as const
  }
}

// 确认裁剪并调用接口上传
const finishCrop = () => {
  if (!cropperRef.value) return
  cropLoading.value = true
  cropperRef.value.getCropBlob(async (blob: Blob) => {
    try {
      const formData = new FormData()
      formData.append('image', blob, 'avatar.png')

      const res = await upload(formData)
      if (res.data.success) {
        if (currentUploadTarget.value === 'category') {
          catForm.avatar = res.data.data
        } else {
          tagForm.avatar = res.data.data
        }
        ElMessage.success('图片上传成功')
        cropperVisible.value = false
      } else {
        ElMessage.error(res.data.msg || '上传失败')
      }
    } catch (e) {
      console.error(e)
      ElMessage.error('网络请求失败')
    } finally {
      cropLoading.value = false
    }
  })
}


// 加载左侧分类
const fetchCategoryData = async () => {
  categoryLoading.value = true
  try {
    const res = await getCategoryList()
    if (res.data.success) {
      categoryList.value = res.data.data
      if (categoryList.value.length > 0) {
        handleSelectCategory(categoryList.value[0])
      }
    }
  } catch (error) {
    console.error(error)
  } finally {
    categoryLoading.value = false
  }
}

// 选中左侧分类，触发右侧标签刷新
const handleSelectCategory = async (cat: any) => {
  currentCategory.value = cat
  tagLoading.value = true
  tagList.value = []
  try {
    const res = await getTagsByCategoryId(cat.id)
    if (res.data.success) {
      tagList.value = res.data.data
    }
  } catch (error) {
    console.error(error)
  } finally {
    tagLoading.value = false
  }
}

// ================= 分类表单逻辑 =================
const catDialogVisible = ref(false)
const isEditCat = ref(false)
const catFormRef = ref()
const catForm = reactive({ id: '', categoryName: '', avatar: '', description: '' })
const catRules = { categoryName: [{ required: true, message: '请输入名称', trigger: 'blur' }] }


const handleAddCategory = () => {
  isEditCat.value = false
  catDialogVisible.value = true
}

const handleEditCategory = (row: any) => {
  isEditCat.value = true
  catForm.id = row.id
  catForm.categoryName = row.categoryName
  catForm.avatar = row.avatar
  catForm.description = row.description
  catDialogVisible.value = true
}

const submitCatForm = async () => {
  if (!catFormRef.value) return
  await catFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (isEditCat.value) {
          const res = await updateCategory(catForm)
          if (res.data.success) {
            ElMessage.success('修改分类成功')
            catDialogVisible.value = false
            fetchCategoryData()
          } else {
            ElMessage.error(res.data.msg || '修改失败')
          }
        } else {
          const res = await addCategory(catForm)
          if (res.data.success) {
            ElMessage.success('新增分类成功')
            catDialogVisible.value = false
            fetchCategoryData()
          } else {
            ElMessage.error(res.data.msg || '新增失败')
          }
        }
      } catch (error) {
        console.error(error)
      }
    }
  })
}

const handleDeleteCategory = (id: string) => {
  ElMessageBox.confirm('确定要删除该分类吗？如果分类下有文章或标签可能会受影响。', '警告', { type: 'warning' })
      .then(async () => {
        try {
          const res = await deleteCategory(id)
          if (res.data.success) {
            ElMessage.success('删除分类成功')
            fetchCategoryData()
          } else {
            ElMessage.error(res.data.msg || '删除失败')
          }
        } catch (error) {
          console.error(error)
        }
      }).catch(() => {})
}

const resetCatForm = () => {
  if (catFormRef.value) catFormRef.value.resetFields()
  catForm.id = ''
  catForm.avatar = ''
}

// ================= 标签表单逻辑 =================
const tagDialogVisible = ref(false)
const isEditTag = ref(false)
const tagFormRef = ref()
const tagForm = reactive({ id: '', tagName: '', avatar: '', categoryId: '' })
const tagRules = { tagName: [{ required: true, message: '请输入名称', trigger: 'blur' }] }

const handleAddTag = () => {
  isEditTag.value = false
  tagForm.categoryId = currentCategory.value.id
  tagDialogVisible.value = true
}

const handleEditTag = (row: any) => {
  isEditTag.value = true
  tagForm.id = row.id
  tagForm.tagName = row.tagName
  tagForm.avatar = row.avatar
  tagForm.categoryId = row.categoryId
  tagDialogVisible.value = true
}

// 完善点2：替换了标签提交的演示代码，对接了真实接口
const submitTagForm = async () => {
  if (!tagFormRef.value) return
  await tagFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (isEditTag.value) {
          const res = await updateTag(tagForm)
          if (res.data.success) {
            ElMessage.success('修改标签成功')
            tagDialogVisible.value = false
            if (currentCategory.value) {
              handleSelectCategory(currentCategory.value)
            }
          } else {
            ElMessage.error(res.data.msg || '修改失败')
          }
        } else {
          const res = await addTag(tagForm)
          if (res.data.success) {
            ElMessage.success('新增标签成功')
            tagDialogVisible.value = false
            if (currentCategory.value) {
              handleSelectCategory(currentCategory.value)
            }
          } else {
            ElMessage.error(res.data.msg || '新增失败')
          }
        }
      } catch (error) {
        console.error(error)
      }
    }
  })
}

// 完善点3：替换了标签删除的演示代码，对接了真实接口
const handleDeleteTag = (id: string) => {
  ElMessageBox.confirm('确定要删除该标签吗？', '提示', { type: 'warning' })
      .then(async () => {
        try {
          const res = await deleteTag(id)
          if (res.data.success) {
            ElMessage.success('删除标签成功')
            if (currentCategory.value) {
              handleSelectCategory(currentCategory.value)
            }
          } else {
            ElMessage.error(res.data.msg || '删除失败')
          }
        } catch (error) {
          console.error(error)
        }
      }).catch(() => {})
}

const resetTagForm = () => {
  if (tagFormRef.value) tagFormRef.value.resetFields()
  tagForm.id = ''
  tagForm.categoryId = ''
  tagForm.avatar = ''
}

onMounted(() => {
  fetchCategoryData()
})
</script>

<style scoped>
.category-tag-container {
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
}
.full-height {
  height: 100%;
}
.box-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}
:deep(.el-card__body) {
  flex: 1;
  overflow: hidden;
  padding: 0;
}
.left-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
}
.right-card :deep(.el-card__body) {
  padding: 15px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
.highlight-text {
  color: #409EFF;
}

/* 左侧分类列表样式 */
.category-list-wrapper {
  flex: 1;
  overflow-y: auto;
}
.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.category-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  cursor: pointer;
  border-bottom: 1px solid #ebeef5;
  transition: all 0.3s;
}
.category-list li:hover {
  background-color: #f5f7fa;
}
.category-list li.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409EFF;
}
.category-list li.active .cat-name {
  color: #409EFF;
  font-weight: bold;
}
.cat-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cat-avatar {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f9fafc;
}

.cat-action {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 无图时的占位样式 */
.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  color: #909399;
  font-size: 14px;
}

/* ============ 上传与裁剪组件相关样式 ============ */
.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px;
}

.uploaded-avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
}

/* 裁剪弹窗布局 */
.bili-cropper-layout { display: flex; height: 360px; gap: 30px; }
.cropper-left  { flex: 1; display: flex; flex-direction: column; }
.cropper-box-wrap { flex: 1; background: #f0f0f0; border: 1px solid #e7e7e7; border-radius: 4px; overflow: hidden; height: 300px; }
.cropper-right { width: 160px; background: #f9f9f9; border-radius: 4px; padding: 20px;
  display: flex; flex-direction: column; align-items: center; border: 1px solid #eee; }
.preview-title { font-size: 14px; font-weight: 700; color: #333; margin: 0 0 16px; }
.preview-item  { display: flex; flex-direction: column; align-items: center; margin-bottom: 16px; }
.preview-circle-box { border-radius: 50%; overflow: hidden; border: 2px solid #fff; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
.size-text { margin-top: 6px; font-size: 12px; color: #666; }
</style>

<style>
/* 针对弹窗的全局微调 */
.bili-cropper-dialog .el-dialog__body { padding: 20px 30px !important; }
</style>