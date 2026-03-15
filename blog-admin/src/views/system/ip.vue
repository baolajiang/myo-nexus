<template>
  <div class="ip-container">
    <el-card class="ip-card">
      <template #header>
        <div class="card-header">
          <div class="left">
            <h2>IP 黑名单</h2>
            <span class="subtitle">共 {{ total }} 条封禁记录</span>
          </div>
          <el-button type="primary" icon="Plus" @click="handleAdd">手动封禁 IP</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="IP 地址">
          <el-input v-model="queryParams.ip" placeholder="搜索 IP 地址..." clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="封禁类型">
          <el-select v-model="queryParams.banType" placeholder="全部" clearable style="width: 130px">
            <el-option label="手动封禁" :value="1" />
            <el-option label="限流触发" :value="2" />
            <el-option label="AI 风控" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="封禁中" :value="1" />
            <el-option label="已解封" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="ipList" v-loading="loading" border stripe style="width: 100%" height="calc(100vh - 320px)">

        <el-table-column prop="ip" label="IP 地址" width="180" />

        <el-table-column prop="ipLocation" label="归属地" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.ipLocation">{{ scope.row.ipLocation }}</span>
            <span v-else class="text-gray">未知</span>
          </template>
        </el-table-column>

        <el-table-column label="封禁类型" width="110" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.banType === 1" type="warning" size="small">手动封禁</el-tag>
            <el-tag v-else-if="scope.row.banType === 2" type="danger" size="small">限流触发</el-tag>
            <el-tag v-else-if="scope.row.banType === 3" type="danger" size="small" effect="dark">AI 风控</el-tag>
            <el-tag v-else type="info" size="small">未知</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 1" type="danger" size="small">封禁中</el-tag>
            <el-tag v-else type="success" size="small">已解封</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="违规次数" width="90" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.violationCount > 0" type="danger" effect="plain" size="small">
              {{ scope.row.violationCount }} 次
            </el-tag>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="reason" label="封禁原因" min-width="200" show-overflow-tooltip />

        <el-table-column label="到期时间" width="160" align="center">
          <template #default="scope">
            <span v-if="!scope.row.expireTime" class="text-danger">永久封禁</span>
            <span v-else-if="isExpired(scope.row.expireTime)" class="text-gray">已过期</span>
            <span v-else>{{ formatTime(scope.row.expireTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="封禁时间" width="160" align="center">
          <template #default="scope">
            {{ formatTime(scope.row.createDate) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="scope">
            <el-button
                v-if="scope.row.status === 1"
                type="success" link size="small"
                @click="handleUnban(scope.row)"
            >解封</el-button>
            <el-button
                v-else
                type="warning" link size="small"
                @click="handleReban(scope.row)"
            >重新封禁</el-button>
            <el-popconfirm title="确定彻底删除该记录吗？" @confirm="handleDelete(scope.row.id)">
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

    <!-- 手动封禁弹窗 -->
    <el-dialog v-model="dialogVisible" title="手动封禁 IP" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="IP 地址" prop="ip">
          <el-input v-model="form.ip" placeholder="请输入要封禁的 IP 地址" />
        </el-form-item>
        <el-form-item label="封禁时长" prop="expireType">
          <el-radio-group v-model="form.expireType" @change="handleExpireTypeChange">
            <el-radio :value="0">永久封禁</el-radio>
            <el-radio :value="1">临时封禁</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.expireType === 1" label="到期时间" prop="expireTime">
          <el-date-picker
              v-model="form.expireTime"
              type="datetime"
              placeholder="选择解封时间"
              style="width: 100%"
              value-format="x"
          />
        </el-form-item>
        <el-form-item label="封禁原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入封禁原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确认封禁</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getIpBlacklist, banIp, unbanIp, deleteIpRecord } from '../../api/ip'
import dayjs from 'dayjs'

const loading = ref(false)
const ipList = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  ip: '',
  banType: undefined as number | undefined,
  status: undefined as number | undefined
})

const form = reactive({
  ip: '',
  reason: '',
  expireType: 0,   // 0=永久 1=临时
  expireTime: null as number | null
})

const rules = {
  ip: [{ required: true, message: '请输入 IP 地址', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入封禁原因', trigger: 'blur' }],
  expireTime: [{ required: true, message: '请选择到期时间', trigger: 'change' }]
}

// 获取列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getIpBlacklist(queryParams)
    if (res.data.success) {
      ipList.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { queryParams.page = 1; fetchData() }
const resetSearch = () => {
  queryParams.ip = ''
  queryParams.banType = undefined
  queryParams.status = undefined
  queryParams.page = 1
  fetchData()
}

// 临时/永久切换时清空到期时间
const handleExpireTypeChange = () => { form.expireTime = null }

// 打开封禁弹窗
const handleAdd = () => { dialogVisible.value = true }

// 提交封禁
const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const params = {
        ip: form.ip,
        reason: form.reason,
        expireTime: form.expireType === 1 ? form.expireTime : null
      }
      const res = await banIp(params)
      if (res.data.success) {
        ElMessage.success('封禁成功')
        dialogVisible.value = false
        fetchData()
      } else {
        ElMessage.error(res.data.msg || '封禁失败')
      }
    } catch (e) {
      console.error(e)
    } finally {
      submitLoading.value = false
    }
  })
}

// 解封
const handleUnban = async (row: any) => {
  try {
    const res = await unbanIp(row.ip)
    if (res.data.success) {
      ElMessage.success('解封成功')
      fetchData()
    } else {
      ElMessage.error(res.data.msg || '解封失败')
    }
  } catch (e) {
    console.error(e)
  }
}

// 重新封禁（已解封的重新封禁）
const handleReban = async (row: any) => {
  try {
    const res = await banIp({ ip: row.ip, reason: '管理员重新封禁', expireTime: null })
    if (res.data.success) {
      ElMessage.success('重新封禁成功')
      fetchData()
    } else {
      ElMessage.error(res.data.msg || '操作失败')
    }
  } catch (e) {
    console.error(e)
  }
}

// 删除记录
const handleDelete = async (id: number) => {
  try {
    const res = await deleteIpRecord(id)
    if (res.data.success) {
      ElMessage.success('删除成功')
      fetchData()
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (e) {
    console.error(e)
  }
}

// 重置表单
const resetForm = () => {
  form.ip = ''
  form.reason = ''
  form.expireType = 0
  form.expireTime = null
  formRef.value?.resetFields()
}

// 判断是否已过期
const isExpired = (expireTime: number) => expireTime < Date.now()

// 格式化时间
const formatTime = (time: number) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.ip-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.left { display: flex; align-items: center; gap: 10px; }
.left h2 { margin: 0; font-size: 18px; }
.subtitle { font-size: 13px; color: #999; }
.search-form { margin-bottom: 15px; background: #f9f9f9; padding: 15px; border-radius: 4px; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }
.text-gray { color: #c0c4cc; font-size: 12px; }
.text-danger { color: #f56c6c; font-size: 12px; font-weight: bold; }
</style>