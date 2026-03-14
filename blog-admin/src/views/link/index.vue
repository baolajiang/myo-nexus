<template>
  <div class="index-container">
    <el-card class="index-card">
      <template #header>
        <div class="card-header" style="display: flex; justify-content: space-between; align-items: center;">
          <h2>我的收藏夹管理</h2>
          <el-button type="primary" @click="handleAdd">新增收藏网站</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="网站名称">
          <el-input v-model="queryParams.keyword" placeholder="搜索名称..." clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="linkList" v-loading="loading" style="width: 100%" border>
        <el-table-column label="图标" width="80" align="center">
          <template #default="scope">
            <el-avatar :size="40" :src="scope.row.imgicon" shape="square" />
          </template>
        </el-table-column>

        <el-table-column prop="name" label="网站名称" width="180" />
        <el-table-column prop="content" label="网站描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="url" label="链接网址" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <el-link type="primary" :href="scope.row.url" target="_blank" underline="never">{{ scope.row.url }}</el-link>
          </template>
        </el-table-column>

        <el-table-column label="排序权重" width="100" align="center">
          <template #default="scope">
            <el-tag effect="plain" type="info">{{ scope.row.sort }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="前台展示状态" width="120" align="center">
          <template #default="scope">
            <el-switch
                v-model="scope.row.status"
                :active-value="1"
                :inactive-value="0"
                active-color="#13ce66"
                inactive-color="#ff4949"
                @change="(val: number) => handleStatusChange(scope.row.id, val)"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button type="primary" size="small" link @click="handleEdit(scope.row)">编辑</el-button>
            <el-popconfirm title="确定删除这个网站吗？" @confirm="handleDelete(scope.row.id)">
              <template #reference>
                <el-button type="danger" size="small" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" style="margin-top: 20px; display: flex; justify-content: flex-end;">
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="网站名称" required>
          <el-input v-model="form.name" placeholder="请输入网站名称" />
        </el-form-item>
        <el-form-item label="网站网址" required>
          <el-input v-model="form.url" placeholder="需包含 http:// 或 https://" />
        </el-form-item>
        <el-form-item label="网站描述">
          <el-input v-model="form.content" type="textarea" placeholder="简单介绍这个网站" />
        </el-form-item>
        <el-form-item label="图标地址 (URL)">
          <el-input v-model="form.imgicon" placeholder="贴上 Logo 的图片链接" />
        </el-form-item>
        <el-form-item label="排序权重">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
          <span style="margin-left: 10px; color: #999; font-size: 12px;">数值越大越靠前</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确认保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getLinkList, addLink, updateLink, deleteLink, changeLinkStatus } from '../../api/link'

const loading = ref(false)
const linkList = ref<any[]>([])
const total = ref(0)

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  keyword: ''
})

// 弹窗相关
const dialogVisible = ref(false)
const dialogTitle = ref('新增收藏网站')
const form = reactive({
  id: '',
  name: '',
  url: '',
  content: '',
  imgicon: '',
  sort: 0,
  status: 1
})

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLinkList(queryParams)
    if (res.data.success) {
      linkList.value = res.data.data.list || []
      total.value = res.data.data.total || 0
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

// 状态切换
const handleStatusChange = async (id: string, status: number) => {
  try {
    const res = await changeLinkStatus({ id, status })
    if (res.data.success) {
      ElMessage.success('状态切换成功')
    } else {
      ElMessage.error(res.data.msg || '状态切换失败')
      fetchData() // 失败时重新拉取恢复原状
    }
  } catch (error) {
    fetchData()
  }
}

// 删除
const handleDelete = async (id: string) => {
  try {
    const res = await deleteLink(id)
    if (res.data.success) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch (error) {}
}

// 打开新增窗口
const handleAdd = () => {
  dialogTitle.value = '新增收藏网站'
  Object.assign(form, { id: '', name: '', url: '', content: '', imgicon: '', sort: 0, status: 1 })
  dialogVisible.value = true
}

// 打开编辑窗口
const handleEdit = (row: any) => {
  dialogTitle.value = '编辑收藏网站'
  Object.assign(form, row)
  dialogVisible.value = true
}

// 提交表单
const submitForm = async () => {
  if (!form.name || !form.url) {
    ElMessage.warning('网站名称和网址必填！')
    return
  }
  try {
    const api = form.id ? updateLink : addLink
    const res = await api(form)
    if (res.data.success) {
      ElMessage.success(form.id ? '修改成功' : '新增成功')
      dialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {}
}
</script>