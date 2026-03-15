<template>
  <div class="media-container">
    <el-card class="media-card">
      <template #header>
        <div class="card-header">
          <h2>附件管理</h2>
          <span class="subtitle">共 {{ total }} 个文件</span>
        </div>
      </template>

      <!-- 分类 Tab -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name=""></el-tab-pane>
        <el-tab-pane label="图片" name="image"></el-tab-pane>
        <el-tab-pane label="日志" name="log"></el-tab-pane>
        <el-tab-pane label="数据库备份" name="backup"></el-tab-pane>
        <el-tab-pane label="其他" name="other"></el-tab-pane>
      </el-tabs>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
            v-model="queryParams.keyword"
            placeholder="搜索文件名..."
            clearable
            style="width: 300px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <!-- 文件列表 -->
      <el-table
          :data="attachmentList"
          v-loading="loading"
          style="width: 100%"
          border
          stripe
      >
        <!-- 预览列：图片类型直接展示缩略图，其他类型展示图标 -->
        <el-table-column label="预览" width="90" align="center">
          <template #default="scope">
            <el-image
                v-if="scope.row.fileType === 'image'"
                style="width: 50px; height: 50px; border-radius: 4px"
                :src="scope.row.fileUrl"
                :preview-src-list="[scope.row.fileUrl]"
                fit="cover"
                preview-teleported
            >
              <template #error>
                <div class="img-error-slot">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <el-icon v-else-if="scope.row.fileType === 'log'" :size="30" color="#409EFF">
              <Document />
            </el-icon>
            <el-icon v-else-if="scope.row.fileType === 'backup'" :size="30" color="#67C23A">
              <FolderOpened />
            </el-icon>
            <el-icon v-else :size="30" color="#909399">
              <Files />
            </el-icon>
          </template>
        </el-table-column>

        <!-- 文件名 -->
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />

        <!-- 文件分类 -->
        <el-table-column label="分类" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getTagType(scope.row.fileType)" size="small">
              {{ getFileTypeLabel(scope.row.fileType) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 文件大小 -->
        <el-table-column label="大小" width="100" align="center">
          <template #default="scope">
            {{ formatSize(scope.row.fileSize) }}
          </template>
        </el-table-column>

        <!-- 上传者 -->
        <el-table-column label="上传者" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.uploaderId === 'SYSTEM'" type="warning" size="small">系统任务</el-tag>
            <span v-else class="uploader-text">{{ scope.row.uploaderId }}</span>
          </template>
        </el-table-column>

        <!-- 备注 -->
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />

        <!-- 上传时间 -->
        <el-table-column label="上传时间" width="170" align="center">
          <template #default="scope">
            {{ formatTime(scope.row.createDate) }}
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="scope">
            <el-button
                type="primary"
                link
                size="small"
                @click="handleCopyUrl(scope.row.fileUrl)"
            >复制链接</el-button>
            <el-popconfirm
                title="确定删除吗？将同步删除 R2 中的文件，不可恢复！"
                @confirm="handleDelete(scope.row.id)"
            >
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.pageSize"
            :page-sizes="[10, 20, 50]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="fetchData"
            @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Picture, Document, FolderOpened, Files } from '@element-plus/icons-vue'
import { getAttachmentList, deleteAttachment } from '../../api/attachment'
import dayjs from 'dayjs'

const loading = ref(false)
const attachmentList = ref<any[]>([])
const total = ref(0)
const activeTab = ref('')

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  keyword: ''
})

// 获取附件列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAttachmentList(queryParams, activeTab.value)
    if (res.data.success) {
      attachmentList.value = res.data.data.list || []
      total.value = res.data.data.total || 0
    } else {
      ElMessage.error(res.data.msg || '获取列表失败')
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 切换 Tab
const handleTabChange = () => {
  queryParams.page = 1
  fetchData()
}

// 搜索
const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

// 重置
const resetSearch = () => {
  queryParams.keyword = ''
  queryParams.page = 1
  fetchData()
}

// 删除附件
const handleDelete = async (id: string) => {
  try {
    const res = await deleteAttachment(id)
    if (res.data.success) {
      ElMessage.success('删除成功')
      fetchData()
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (error) {
    console.error(error)
  }
}

// 复制文件链接
const handleCopyUrl = (url: string) => {
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制')
  })
}

// 格式化文件大小
const formatSize = (size: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

// 格式化时间
const formatTime = (time: number) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

// 获取文件分类标签文字
const getFileTypeLabel = (fileType: string) => {
  const map: Record<string, string> = {
    image: '图片',
    log: '日志',
    backup: '备份',
    other: '其他'
  }
  return map[fileType] || fileType
}

// 获取 Tag 类型
const getTagType = (fileType: string) => {
  const map: Record<string, string> = {
    image: 'success',
    log: 'primary',
    backup: 'warning',
    other: 'info'
  }
  return map[fileType] || 'info'
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.media-container {
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
}

.media-card {
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
}

.subtitle {
  font-size: 13px;
  color: #999;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.img-error-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
}

.uploader-text {
  font-size: 12px;
  color: #606266;
}
</style>