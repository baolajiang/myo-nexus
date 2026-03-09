<template>
  <div class="log-container">
    <el-card class="log-card">
      <template #header>
        <div class="card-header">
          <h2>操作日誌</h2>
          <span class="subtitle">系統管理員操作審計</span>
        </div>
      </template>

      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="操作模塊">
          <el-input v-model="queryParams.module" placeholder="如：用戶管理" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="queryParams.nickname" placeholder="暱稱" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="狀態">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="成功" :value="0" />
            <el-option label="失敗" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="TraceID">
          <el-input v-model="queryParams.traceId" placeholder="全鏈路追踪ID" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logList" v-loading="loading" style="width: 100%" border stripe>
        <el-table-column prop="traceId" label="TraceID" width="140" show-overflow-tooltip />
        <el-table-column prop="module" label="模塊" width="100" align="center" />
        <el-table-column prop="operation" label="操作描述" width="150" />

        <el-table-column label="操作人" width="120">
          <template #default="scope">
            <el-tag size="small" effect="plain">{{ scope.row.nickname }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="ip" label="IP地址" width="130" />

        <el-table-column label="狀態" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 0 ? 'success' : 'danger'">
              {{ scope.row.status === 0 ? '成功' : '失敗' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="耗時" width="100" align="center">
          <template #default="scope">
            <span :class="{'slow-query': scope.row.time > 1000}">{{ scope.row.time }}ms</span>
          </template>
        </el-table-column>

        <el-table-column label="詳情" min-width="120" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="showDetail(scope.row)">查看參數/結果</el-button>
          </template>
        </el-table-column>

        <el-table-column prop="createDate" label="操作時間" width="170">
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

    <el-dialog v-model="detailVisible" title="日誌詳情" width="700px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="請求方法">{{ currentLog.method }}</el-descriptions-item>
        <el-descriptions-item label="請求參數">
          <pre class="json-box">{{ formatJson(currentLog.params) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="響應結果" v-if="currentLog.status === 0">
          <pre class="json-box">{{ formatJson(currentLog.result) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="錯誤信息" v-else>
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
  if (!str) return '无'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch (e) {
    return str
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLogList(queryParams)
    if (res.data.success) {
      logList.value = res.data.data.records
      total.value = res.data.data.total
    }
  } catch (error) {
    console.error('獲取日誌失敗', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

const resetSearch = () => {
  Object.assign(queryParams, {
    page: 1,
    pageSize: 20,
    module: '',
    nickname: '',
    status: undefined,
    traceId: ''
  })
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
.subtitle { font-size: 13px; color: #999; font-weight: normal; }
.search-form { margin-bottom: 15px; background: #f9f9f9; padding: 15px; border-radius: 4px; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }
.slow-query { color: #e6a23c; font-weight: bold; }
.json-box {
  background: #2d2d2d;
  color: #ccc;
  padding: 10px;
  border-radius: 4px;
  max-height: 300px;
  overflow-y: auto;
  font-family: monospace;
  white-space: pre-wrap;
  word-break: break-all;
}
.error-text { color: #f56c6c; }
</style>