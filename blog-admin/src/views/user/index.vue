<template>
  <div class="index-container">
    <el-card class="index-card">
      <template #header>
        <div class="card-header">
          <div class="left">
            <h3>用户管理</h3>
            <span class="subtitle">共 {{ total }} 位用户</span>
          </div>
          <el-button type="primary" :loading="loading" @click="fetchData">刷新列表</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" border stripe>
        <el-table-column label="头像" width="80" align="center">
          <template #default="scope">
            <el-avatar :src="scope.row.avatar" shape="square" size="small" />
          </template>
        </el-table-column>

        <el-table-column prop="account" label="账号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />

        <el-table-column label="账号状态" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 99" type="danger">已封禁</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="warning">警告</el-tag>
            <el-tag v-else-if="scope.row.status === 0" type="success">正常</el-tag>
            <el-tag v-else type="info">未知</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="在线状态" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.online" type="success" effect="plain" round size="small">在线</el-tag>
            <el-tag v-else type="info" effect="plain" round size="small">离线</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="最后活跃" width="160">
          <template #default="scope">
            <div v-if="scope.row.lastLogin">
              <div class="time-text">{{ formatTime(scope.row.lastLogin) }}</div>
              <div class="time-ago">{{ formatRelativeTime(scope.row.lastLogin) }}</div>
            </div>
            <span v-else class="no-data">从未登录</span>
          </template>
        </el-table-column>

        <el-table-column label="注册时间" width="160">
          <template #default="scope">
            {{ formatTime(scope.row.createDate) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="handleDetail(scope.row)">详情</el-button>

          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.pageSize"
            :page-sizes="[5, 10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
        v-model="dialogVisible"
        :title="isEditMode ? '编辑用户资料' : '用户详细信息'"
        width="500px"
        :close-on-click-modal="false"
        @close="handleDialogClose"
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="头像">
          <el-avatar :src="editForm.avatar" shape="square" />
        </el-form-item>

        <el-form-item label="账号">
          <el-input v-model="editForm.account" disabled />
        </el-form-item>

        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" :disabled="!isEditMode" />
        </el-form-item>

        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="未绑定" :disabled="!isEditMode" />
        </el-form-item>

        <el-form-item label="手机号">
          <el-input v-model="editForm.mobilePhoneNumber" placeholder="未绑定" :disabled="!isEditMode" />
        </el-form-item>

        <el-form-item label="最近IP">
          <el-input v-model="editForm.lastIpaddr" disabled />
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status" :disabled="!isEditMode">
            <el-radio  value="0">正常</el-radio>
            <el-radio  value="1">警告</el-radio>
            <el-radio  value="99">封禁</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="处理理由" v-if="editForm.status === '1' || editForm.status === '99'">
          <el-input
              v-model="editForm.remark"
              type="textarea"
              :rows="2"
              placeholder="请输入警告或封禁的理由"
              :disabled="!isEditMode"
          />
        </el-form-item>

      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <template v-if="!isEditMode">
            <el-button @click="dialogVisible = false">关闭</el-button>
            <el-button type="primary" @click="enableEditMode">修改信息</el-button>
          </template>

          <template v-else>
            <el-button @click="cancelEdit">取消编辑</el-button>
            <el-button
                type="primary"
                :loading="submitLoading"
                :disabled="!isFormChanged"
                @click="submitEdit"
            >
              保存修改
            </el-button>
          </template>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed} from 'vue'
import { getUserList, updateUser } from '../../api/user'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const queryParams = reactive({
  page: 1,
  pageSize: 10
})

// 弹窗相关状态
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEditMode = ref(false) // 是否处于编辑模式

// 表单数据
const editForm = reactive({
  id: '',
  account: '',// 账号
  nickname: '',// 昵称
  email: '',// 邮箱
  mobilePhoneNumber: '',// 手机号
  avatar: '',// 头像
  lastIpaddr: '', // 最近登录IP
  status: '0',// 状态
  remark: '',// 备注
})

// 备份数据（用于取消编辑时恢复）
const backupForm = reactive({...editForm})
//判断表单内容是否发生了实质性变化
const isFormChanged = computed(() => {
  return editForm.nickname !== backupForm.nickname ||
      editForm.email !== backupForm.email ||
      editForm.mobilePhoneNumber !== backupForm.mobilePhoneNumber ||
      editForm.status !== backupForm.status ||
      editForm.remark !== backupForm.remark
})

const formatTime = (timestamp: number) => {
  if (!timestamp) return ''
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm')
}

const formatRelativeTime = (timestamp: number) => {
  if (!timestamp) return ''
  return dayjs(timestamp).fromNow()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getUserList(queryParams)

    if (res.data.success && res.data.data) {

      tableData.value = res.data.data.records
      total.value = res.data.data.total

    } else if(res.records) {

      tableData.value = res.data.srecords
      total.value = res.data.stotal
    }
  } catch (error) {
    console.error('获取用户列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  fetchData()
}
const handleCurrentChange = (val: number) => {
  queryParams.page = val
  fetchData()
}



// ===/修改后的详情弹窗逻辑 ===

// 1. 点击“详情”按钮
const handleDetail = (row: any) => {
  // 填充数据
  editForm.id = row.id
  editForm.account = row.account
  editForm.nickname = row.nickname
  editForm.email = row.email
  editForm.mobilePhoneNumber = row.mobilePhoneNumber
  editForm.avatar = row.avatar
  editForm.lastIpaddr = row.lastIpaddr || row.ipaddr
  editForm.status = String(row.status) || '0'
  editForm.remark = row.remark || ''

  // 备份原始数据
  Object.assign(backupForm, editForm)

  // 默认进入只读模式
  isEditMode.value = false
  dialogVisible.value = true
}

// 2. 点击“修改信息”，进入编辑模式
const enableEditMode = () => {
  isEditMode.value = true
}

// 3. 点击“取消编辑”，恢复数据并回到只读模式
const cancelEdit = () => {
  Object.assign(editForm, backupForm) // 恢复数据
  isEditMode.value = false
}

// 4. 提交保存
const submitEdit = async () => {
  submitLoading.value = true
  try {
    const res: any = await updateUser(editForm)
    if (res.data.success) {
      ElMessage.success('保存成功')
      dialogVisible.value = false // 保存成功后关闭弹窗
      fetchData()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('系统异常')
  } finally {
    submitLoading.value = false
  }
}

// 弹窗关闭时重置状态
const handleDialogClose = () => {
  isEditMode.value = false
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.index-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.subtitle { margin-left: 10px; font-size: 13px; color: #999; }
.left { display: flex; align-items: center; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
.time-text { font-size: 13px; line-height: 1.2; }
.time-ago { font-size: 12px; color: #999; margin-top: 2px; }
.no-data { color: #ccc; font-size: 12px; }
</style>