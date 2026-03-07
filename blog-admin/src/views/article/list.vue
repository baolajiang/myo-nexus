<template>
  <div class="list-container">
    <el-card class="list-card">
      <div class="header-action">
        <h2>文章列表</h2>
        <div>
          <el-button type="primary" icon="Search" @click="dialogVisible = true">查询文章</el-button>
        </div>
      </div>

      <el-dialog
          v-model="dialogVisible"
          title="查询文章"
          width="500px"
      >
        <el-form :model="pageParams" label-width="80px">
          <el-form-item label="关键字">
            <el-input
                v-model="pageParams.keyword"
                placeholder="搜索文章标题或简介..."
                clearable
                @keyup.enter="handleSearch"
            />
          </el-form-item>

          <el-form-item label="分类">
            <el-select
                v-model="pageParams.categoryId"
                placeholder="请选择分类"
                clearable
                @change="handleCategoryChange"
                style="width: 100%"
            >
              <el-option
                  v-for="c in categoryList"
                  :key="c.id"
                  :label="c.categoryName"
                  :value="c.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="标签">
            <el-select v-model="pageParams.tagId" placeholder="请选择标签" clearable style="width: 100%">
              <el-option
                  v-for="t in tagList"
                  :key="t.id"
                  :label="t.tagName"
                  :value="t.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="发布时间">
            <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                @change="handleDateChange"
                clearable
                style="width: 100%"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="resetSearch">重 置</el-button>
            <el-button type="primary" @click="handleSearch">查 询</el-button>
          </span>
        </template>
      </el-dialog>

      <el-table
          :data="articleList"
          style="width: 100%; margin-top: 10px"
          height="calc(100vh - 250px)"
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
            >
              <template #error>
                <div class="image-slot">无图</div>
              </template>
            </el-image>
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

        <el-table-column label="操作" width="160" fixed="right" align="center">
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
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { deleteArticle, getArticleList } from '../../api/article'
// 引入写好的真实 API
import { getCategoryList } from '../../api/category'
import { getTagsByCategoryId } from '../../api/tag'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const articleList = ref([])
const total = ref(0)

const dialogVisible = ref(false)
const dateRange = ref([])

const categoryList = ref<any[]>([])
const tagList = ref<any[]>([])

const pageParams = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  categoryId: '',
  tagId: '',
  startDate: '',
  endDate: ''
})

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

// 页面初始化时，只请求分类数据
const loadOptions = async () => {
  try {
    const catRes = await getCategoryList()
    if(catRes.data.success) {
      categoryList.value = catRes.data.data
    }
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

// 核心联动逻辑：当分类发生改变时，请求对应的标签
const handleCategoryChange = async (categoryId: string) => {
  // 分类变了，先把选中的标签重置为空
  pageParams.tagId = ''

  // 如果分类被清空了，把标签列表也清空，不再发请求
  if (!categoryId) {
    tagList.value = []
    return
  }

  // 拿着新的 categoryId 去请求标签
  try {
    const res = await getTagsByCategoryId(categoryId)
    if (res.data.success) {
      tagList.value = res.data.data
    } else {
      ElMessage.error(res.data.msg || '获取标签失败')
    }
  } catch (e) {
    console.error('获取标签失败', e)
  }
}

const handleDateChange = (val: string[]) => {
  if (val && val.length === 2) {
    pageParams.startDate = val[0]
    pageParams.endDate = val[1]
  } else {
    pageParams.startDate = ''
    pageParams.endDate = ''
  }
}

const handleSearch = () => {
  pageParams.page = 1
  fetchData()
  dialogVisible.value = false
}

const resetSearch = () => {
  pageParams.keyword = ''
  pageParams.categoryId = ''
  pageParams.tagId = ''
  pageParams.startDate = ''
  pageParams.endDate = ''
  dateRange.value = []
  tagList.value = [] // 重置时顺便清空联动的标签列表
  pageParams.page = 1
  fetchData()
  dialogVisible.value = false
}

const handleSizeChange = (size: number) => {
  pageParams.pageSize = size
  pageParams.page = 1
  fetchData()
}

const handleCurrentChange = (page: number) => {
  pageParams.page = page
  fetchData()
}

const handleEdit = (id: string) => {
  router.push(`/article/write?id=${id}`)
}

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
    await deleteArticle(id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

onMounted(() => {
  loadOptions()
  fetchData()
})
</script>

<style scoped>
.list-container {
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
}
.list-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}
:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-bottom: 10px;
}
.header-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.pagination-container {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
}
.text-gray {
  color: #999;
  font-size: 12px;
}
.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
}
</style>