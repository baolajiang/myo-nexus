<template>
  <div class="log-container">
    <el-card class="log-card">
      <template #header>
        <div class="card-header">
          <h2>操作日志</h2>
          <span class="subtitle">系统管理员操作审计</span>
        </div>
      </template>

      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="操作模块">
          <el-input v-model="queryParams.module" placeholder="如：用户管理" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="queryParams.nickname" placeholder="昵称" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="成功" :value="0" />
            <el-option label="失败" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="TraceID">
          <el-input v-model="queryParams.traceId" placeholder="追踪ID" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logList" v-loading="loading" style="width: 100%" border stripe>
        <el-table-column prop="traceId" label="TraceID" width="140" show-overflow-tooltip />
        <el-table-column prop="module" label="模块" width="120" align="center" />
        <el-table-column prop="operation" label="操作描述" width="150" />

        <el-table-column label="操作人" width="120">
          <template #default="scope">
            <el-tag size="small" effect="plain">{{ scope.row.nickname }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="ip" label="IP地址" width="130" />

        <el-table-column label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 0 ? 'success' : 'danger'">
              {{ scope.row.status === 0 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="耗时" width="100" align="center">
          <template #default="scope">
            <span :class="{'slow-query': scope.row.time > 1000}">{{ scope.row.time }}ms</span>
          </template>
        </el-table-column>

        <el-table-column label="详情" min-width="120" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="showDetail(scope.row)">查看报文</el-button>
          </template>
        </el-table-column>

        <el-table-column prop="createDate" label="操作时间" width="170">
          <template #default="scope">
            {{ formatTime(scope.row.createDate) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="fetchData"
            @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="日志详情" width="750px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="请求方法">{{ currentLog.method }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="json-box">{{ formatJson(currentLog.params) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="响应结果" v-if="currentLog.status === 0">
          <pre class="json-box">{{ formatJson(currentLog.result) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" v-else>
          <span class="error-text">{{ currentLog.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getLogList } from '../../api/log'
import dayjs from 'dayjs'

const loading = ref(false)
const logList = ref([])
const total = ref(0)
const detailVisible = ref(false)
const currentLog = ref<any>({})

// 重点：这里的 key 必须和后端 PageParams 实体类中的属性名完全一致
const queryParams = reactive({
  page: 1,
  pageSize: 20,
  module: '',
  nickname: '',
  status: undefined,
  traceId: ''
})

const formatTime = (time: number) => dayjs(time).format('YYYY-MM-DD HH:mm:ss')

const formatJson = (str: string) => {
  if (!str) return '无数据'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch (e) {
    return str
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    // 将整个 queryParams 对象作为参数传递给 API 接口
    const res: any = await getLogList(queryParams)
    if (res.data.success) {
      logList.value = res.data.data.records
      total.value = res.data.data.total
    }
  } catch (error) {
    console.error('获取日志失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

const resetSearch = () => {
  // 重置所有搜索字段
  queryParams.page = 1
  queryParams.module = ''
  queryParams.nickname = ''
  queryParams.status = undefined
  queryParams.traceId = ''
  fetchData()
}

const showDetail = (row: any) => {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.log-container { padding: 20px; }
.card-header { display: flex; align-items: center; gap: 10px; }
.card-header h2 { margin: 0; font-size: 18px; }
.subtitle { font-size: 13px; color: #999; font-weight: normal; }
.search-form { margin-bottom: 15px; background: #f9f9f9; padding: 15px; border-radius: 4px; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }
.slow-query { color: #e6a23c; font-weight: bold; }
.json-box {
  background: #2d2d2d;
  color: #abb2bf;
  padding: 15px;
  border-radius: 4px;
  border-radius: 4px;
  max-height: 350px;
  overflow-y: auto;
  font-family: 'Courier New', Courier, monospace;
  white-space: pre-wrap;
  word-break: break-all;
}
.error-text { color: #f56c6c; font-family: monospace; }
</style>