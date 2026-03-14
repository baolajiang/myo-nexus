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
          <el-input v-model="queryParams.keyword" placeholder="搜索评论内容" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待审核" name="2"></el-tab-pane>
        <el-tab-pane label="正常可见" name="1"></el-tab-pane>
        <el-tab-pane label="回收站" name="0"></el-tab-pane>
        <el-tab-pane label="全部" name="all"></el-tab-pane>
      </el-tabs>

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

        <el-table-column label="所属文章" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <el-link type="primary" underline="never">
              {{ scope.row.articleTitle || '文章(ID:' + scope.row.articleId + ')' }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 1" type="success">正常</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="warning">待审核</el-tag>
            <el-tag v-else-if="scope.row.status === 0" type="danger">已删除</el-tag>
            <el-tag v-else type="info">未知</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createDate" label="发布时间" width="160" align="center" />

        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="scope">

            <template v-if="scope.row.status == 2">
              <el-button type="success" size="small" @click="updateStatus(scope.row.id, 1)">恢复</el-button>
              <el-button type="danger" size="small" @click="updateStatus(scope.row.id, 0)">移到回收站</el-button>
            </template>

            <template v-if="scope.row.status == 1">
              <el-button type="warning" size="small" @click="updateStatus(scope.row.id, 0)">封禁</el-button>
            </template>

            <template v-if="scope.row.status == 0">
              <el-button type="primary" size="small" @click="updateStatus(scope.row.id, 1)">解除</el-button>

              <el-popconfirm title="确定彻底物理删除吗？" @confirm="handleDelete(scope.row.id)">
                <template #reference>
                  <el-button type="danger" size="small" style="margin-left: 10px;">彻底删除</el-button>
                </template>
              </el-popconfirm>
            </template>

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
import { getCommentList, deleteComment, changeCommentStatus } from '../../api/comment'

const loading = ref(false)
const commentList = ref<any[]>([])
const total = ref(0)

// 默认停留在“待审核”标签页，方便你进来就处理积压工单
const activeTab = ref('2')

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: 2 as number | undefined // 初始携带状态2去查
})

onMounted(() => {
  fetchData()
})

// 获取数据
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

// 切换标签页
const handleTabChange = (tabName: string | number) => {
  if (tabName === 'all') {
    queryParams.status = undefined
  } else {
    queryParams.status = Number(tabName)
  }
  queryParams.page = 1 // 切换状态时重置为第一页
  fetchData()
}

// 搜索和重置
const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

const resetSearch = () => {
  queryParams.keyword = ''
  handleSearch()
}

// 修改状态（软删除、审核通过、恢复）
const updateStatus = async (id: string, status: number) => {
  try {
    const res = await changeCommentStatus({ id, status })
    if (res.data.success) {
      ElMessage.success('状态更新成功')
      fetchData() // 更新成功后重新拉取列表
    } else {
      ElMessage.error(res.data.msg || '状态更新失败')
    }
  } catch (error) {
    console.error(error)
  }
}

// 物理强制删除
const handleDelete = async (id: string) => {
  try {
    const res = await deleteComment(id)
    if (res.data.success) {
      ElMessage.success('彻底删除成功')
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