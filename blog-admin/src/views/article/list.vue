<template>
  <div class="list-container">
    <el-card class="list-card">
      <div class="header-action">
        <h2>文章列表</h2>
        <div class="search-box">
          <el-button type="primary" icon="Plus" @click="$router.push('/article/write')">写文章</el-button>
        </div>
      </div>

      <el-table
          :data="articleList"
          style="width: 100%; margin-top: 20px"
          v-loading="loading"
          border
      >
        <el-table-column label="封面" width="100" align="center">
          <template #default="scope">
            <el-image
                style="width: 60px; height: 40px; border-radius: 4px"
                :src="scope.row.cover"
                :preview-src-list="[scope.row.cover]"
                fit="cover"
                preview-teleported
            />
          </template>
        </el-table-column>

        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <span style="font-weight: bold">{{ scope.row.title }}</span>
            <el-tag v-if="scope.row.weight === 1" type="danger" size="small" style="margin-left: 5px">置顶</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="author" label="作者" width="120" align="center" />

        <el-table-column label="分类" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.category">{{ scope.row.category.categoryName }}</el-tag>
            <span v-else class="text-gray">无分类</span>
          </template>
        </el-table-column>

        <el-table-column label="数据" width="150" align="center">
          <template #default="scope">
            <div class="stats-info">
              <span>👁 {{ scope.row.viewCounts }}</span>
              <span style="margin-left: 10px">💬 {{ scope.row.commentCounts }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createDate" label="发布时间" width="180" align="center" />

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button type="primary" link icon="Edit" @click="handleEdit(scope.row.id)">编辑</el-button>
            <el-button type="danger" link icon="Delete" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
            v-model:current-page="pageParams.page"
            v-model:page-size="pageParams.pageSize"
            :page-sizes="[10, 20, 50]"
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
import { ref, onMounted, reactive } from 'vue'
import {deleteArticle, getArticleList} from '../../api/article'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const articleList = ref([])
const total = ref(0)

// 分页参数
const pageParams = reactive({
  page: 1,
  pageSize: 10
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getArticleList(pageParams)
    if (res.data.success) {
      articleList.value = res.data.data.list
      total.value = res.data.data.total
    } else {
      ElMessage.error(res.data.msg || '获取列表失败')
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 编辑文章
const handleEdit = (id: string) => {
  router.push(`/article/write?id=${id}`) // 跳转到写文章页面并带上ID
}

// 删除文章
const handleDelete = (id: string) => {
  ElMessageBox.confirm(
      '确定要删除这篇文章吗？删除后无法恢复',
      '警告',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(async () => {
    // 这里调用删除接口
     await deleteArticle(id)
    ElMessage.success('演示模式：删除成功')
    fetchData()
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.list-container {
  padding: 20px;
}
.header-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
.text-gray {
  color: #999;
  font-size: 12px;
}
</style>