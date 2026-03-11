<template>
  <div class="index-container">
    <el-card class="index-card">
      <template #header>
        <div class="card-header">
          <h2>评论管理</h2>
        </div>
      </template>

      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="搜索评论内容" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="commentList" v-loading="loading" style="width: 100%" border>
        <el-table-column label="评论者" width="180">
          <template #default="scope">
            <div class="user-info">
              <el-avatar :size="30" :src="scope.row.author?.avatar" />
              <span class="nickname">{{ scope.row.author?.nickname || '未知用户' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="content" label="评论内容" min-width="250" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.level > 1" class="reply-text">
              回复 @{{ scope.row.toUser?.nickname }}:
            </span>
            <span>{{ scope.row.content }}</span>
          </template>
        </el-table-column>

        <el-table-column label="所属文章" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <el-link type="primary"  underline="never">
              {{ scope.row.articleTitle || '文章(ID:' + scope.row.articleId + ')' }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column prop="createDate" label="发布时间" width="160" />

        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="scope">
            <el-popconfirm title="确定要删除这条评论及其子评论吗？" @confirm="handleDelete(scope.row.id)">
              <template #reference>
                <el-button type="danger" size="small" link>强制删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

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
import { getCommentList, deleteComment } from '../../api/comment'

const loading = ref(false)
const commentList = ref<any[]>([])
const total = ref(0)

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  keyword: ''
})

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCommentList(queryParams)
    if (res.data.success) {
      commentList.value = res.data.data.list || []
      total.value = res.data.data.total || 0
    } else {
      ElMessage.error(res.data.msg || '获取评论列表失败')
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

const resetSearch = () => {
  queryParams.keyword = ''
  handleSearch()
}

const handleDelete = async (id: string) => {
  try {
    const res = await deleteComment(id)
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
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.nickname {
  font-weight: bold;
  font-size: 14px;
}
.reply-text {
  color: #409EFF;
  margin-right: 5px;
}
.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>