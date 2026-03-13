<template>
  <div class="task-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>定时任务监控</h2>
          <span class="subtitle">管理系统后台自动化任务</span>
        </div>
      </template>

      <el-table :data="taskList" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="taskName" label="任务名称" width="180" />
        <el-table-column prop="taskGroup" label="任务分组" width="120" align="center">
          <template #default="scope">
            <el-tag effect="light">{{ scope.row.taskGroup }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="beanName" label="调用目标 (Bean)" width="180" />

        <el-table-column prop="taskParam" label="执行参数" show-overflow-tooltip min-width="120" />

        <el-table-column label="Cron 表达式" width="220">
          <template #default="scope">
            <div>{{ scope.row.cronExpression }}</div>
            <div style="font-size: 12px; color: #909399; margin-top: 4px;">
              {{ translateCron(scope.row.cronExpression) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-switch
                v-model="scope.row.status"
                :active-value="1"
                :inactive-value="0"
                active-color="#13ce66"
                @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="200" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="CaretRight" @click="handleRunOnce(scope.row)">
              执行一次
            </el-button>
            <el-button link type="info" icon="Document" @click="showTaskLog(scope.row)">
              调度日志
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="logVisible" title="任务调度日志" width="800px">
      <el-table :data="logList" v-loading="logLoading" border height="400">
        <el-table-column label="执行时间" width="170">
          <template #default="scope">
            {{ formatTime(scope.row.createDate) }}
          </template>
        </el-table-column>
        <el-table-column label="执行状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 0 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 0 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="100" align="center" />
        <el-table-column prop="errorInfo" label="异常信息" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskList, runTaskOnce, getTaskLogList, changeTaskStatus } from '../../api/task'
import dayjs from 'dayjs'
import cronstrue from 'cronstrue/i18n'
// 针对不同任务的专属参数提示字典
const paramHints: Record<string, string>= {
  'logCleanTask': '输入示例：30 <br/>(代表清理 30 天前的日志，只能输入纯数字)',
  'contentAuditTask': '输入示例：10 <br/>(代表扫描过去 10 分钟的新评论，只能输入纯数字)',
  'linkCheckTask': '输入示例：{"timeout": 3000} <br/>(JSON格式，代表将超时时间临时设为 3 秒)',
  'databaseBackupTask': '输入示例：{"type": "schema_only", "compress": false} <br/>(JSON格式，type设为schema_only仅备份表结构，compress设为false关闭压缩)',
  'viewCountSyncTask': '输入示例：{"articleId": "123456"} <br/>(JSON格式，代表只精准同步这一篇文章的浏览量)'
}
const loading = ref(false)
const taskList = ref([])

// 翻译 Cron 表达式的函数
const translateCron = (cron: string) => {
  if (!cron) return ''
  try {
    // 强制使用简体中文翻译
    return cronstrue.toString(cron, { locale: 'zh_CN' })
  } catch (e) {
    return '表达式无法解析'
  }
}

// 日志弹窗相关
const logVisible = ref(false)
const logLoading = ref(false)
const logList = ref([])

// 时间格式化
const formatTime = (time: number) => {
  if (!time) return ''
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 初始化获取任务列表
const fetchTaskList = async () => {
  loading.value = true
  try {
    const res: any = await getTaskList({ page: 1, pageSize: 100 }) // 这里不分页直接取所有任务
    if (res.data.success) {
      taskList.value = res.data.data.records
    }
  } catch (error) {
    console.error('获取任务列表失败', error)
  } finally {
    loading.value = false
  }
}

// 启停任务开关
const handleStatusChange = async (row: any) => {
  const text = row.status === 1 ? '启动' : '暂停'
  try {
    // 调用改变状态接口
    const res: any = await changeTaskStatus({ taskId: row.id, status: row.status })
    if (res.data.success) {
      ElMessage.success(`已成功${text}任务：${row.taskName}`)
    } else {
      // 失败的话把状态回滚回去
      row.status = row.status === 1 ? 0 : 1
    }
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    console.error('更新状态失败', error)
  }
}

const handleRunOnce = (row: { beanName: string | number; remark: any; taskName: any; taskParam: any; id: any }) => {
  // 根据任务的 beanName 获取专属提示，如果没有匹配到则显示通用提示
  const specificHint = paramHints[row.beanName] || '如果该任务支持传参，请按照后端设定的格式输入。'

  // 构造带有精美提示信息的 HTML 内容
  const promptHtml = `
    <div style="font-size: 13px; color: #606266; line-height: 1.6; margin-bottom: 10px; padding: 10px; background: #f4f4f5; border-radius: 4px;">
      <div style="margin-bottom: 5px;">【任务说明】：${row.remark || '暂无说明'}</div>
      <div style="color: #e6a23c;">
        【参数提示】：<br/>
        ${specificHint}<br/>
        <span style="color: #909399; font-size: 12px;">注：留空则按无参默认逻辑执行。默认已填入数据库配置。</span>
      </div>
    </div>
    <div style="margin-bottom: 8px;">请输入本次执行的动态参数：</div>
  `;

  ElMessageBox.prompt(promptHtml, `手动执行任务：${row.taskName}`, {
    dangerouslyUseHTMLString: true, // 允许渲染 HTML，做出高级界面的关键
    confirmButtonText: '立即下发',
    cancelButtonText: '取消',
    inputValue: row.taskParam || '', // 自动填入数据库里的默认参数
    inputType: 'textarea',
    inputPlaceholder: '留空则执行无参方法'
  }).then(async ({ value }) => {
    try {
      // 构造传给后端的参数对象
      const runParams = {
        id: row.id,
        taskParam: value
      }
      const res = await runTaskOnce(runParams)
      if (res.data.success) {
        ElMessage.success(`任务 [${row.taskName}] 已触发执行`)
      }
    } catch (e) {
      console.error('执行指令下发失败', e)
    }
  }).catch(() => {
    ElMessage.info('已取消执行')
  })
}

// 查看调度日志
const showTaskLog = async (row: any) => {
  logVisible.value = true
  logLoading.value = true
  try {
    const res: any = await getTaskLogList({ page: 1, pageSize: 50, taskId: row.id })
    if (res.data.success) {
      logList.value = res.data.data.records
    }
  } catch (error) {
    console.error('获取调度日志失败', error)
  } finally {
    logLoading.value = false
  }
}

onMounted(() => {
  fetchTaskList()
})
</script>

<style scoped>
.task-container { padding: 20px; }
.card-header { display: flex; align-items: center; gap: 10px; }
.card-header h2 { margin: 0; font-size: 18px; }
.subtitle { font-size: 13px; color: #999; }
</style>