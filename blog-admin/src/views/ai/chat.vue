<template>
  <div class="ai-chat-container">
    <el-card class="chat-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>AI 超级管理员助手</span>
          <el-button type="danger" size="small" plain @click="clearHistory" :loading="loading">
            清空对话记忆
          </el-button>
        </div>
      </template>

      <div class="message-list" ref="messageListRef">
        <div v-for="(msg, index) in messageList" :key="index" :class="['message-item', msg.role]">
          <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
          <div class="content">{{ msg.content }}</div>
        </div>
      </div>

      <div class="input-area">
        <el-input
            v-model="inputText"
            placeholder="请输入指令，例如：帮我查询名叫 miyu 的账号"
            @keyup.enter="sendMessage"
            :disabled="loading"
        />
        <el-button type="primary" :loading="loading" @click="sendMessage" style="margin-left: 10px;">
          发送指令
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAiHistory, saveAiHistory, clearAiHistory, sendAiChat } from '../../api/ai'

const inputText = ref('')
const loading = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const messageList = ref<Array<{role: string, content: string}>>([])

// 滚动到底部的平滑函数
const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 1. 初始化时从 Redis 拉取历史记录
const loadHistory = async () => {
  try {
    const res: any = await getAiHistory()

    let historyData = [];
    if (Array.isArray(res)) {
      historyData = res;
    } else if (res && Array.isArray(res.data)) {
      historyData = res.data;
    } else if (res && res.data && Array.isArray(res.data.data)) {
      historyData = res.data.data;
    } else if (typeof res === 'string') {
      try { historyData = JSON.parse(res) } catch(e){}
    } else if (res && res.data && typeof res.data === 'string') {
      try { historyData = JSON.parse(res.data) } catch(e){}
    }

    if (Array.isArray(historyData) && historyData.length > 0) {
      messageList.value = historyData;
    } else {
      messageList.value = [
        { role: 'ai', content: '管理员您好，我是基于 Spring AI Alibaba 构建的智能助手。您可以直接让我查询或封禁用户。' }
      ]
    }
    scrollToBottom()
  } catch (error) {
    console.error('加载历史记录失败:', error)
    messageList.value = [
      { role: 'ai', content: '管理员您好，我是智能助手。' }
    ]
  }
}
// 2. 每次对话后，将最新数组推送到 Redis
const saveHistory = async () => {
  try {
    const rawData = JSON.parse(JSON.stringify(messageList.value))
    await saveAiHistory(rawData)
  } catch (error) {
    console.error('保存云端记忆失败:', error)
  }
}

// 3. 清空记忆动作
const clearHistory = () => {
  ElMessageBox.confirm('确定要清空与 AI 的所有聊天记录并重置其记忆吗？', '系统提示', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    loading.value = true
    try {
      await clearAiHistory()

      messageList.value = [
        { role: 'ai', content: '记忆已彻底清空，我们开启全新的对话吧。' }
      ]
      await saveHistory()
      ElMessage.success('聊天记录与AI记忆已成功清空')
    } catch (error) {
      console.error('清空失败:', error)
      ElMessage.error('清空失败，请检查网络或后端日志')
    } finally {
      loading.value = false
    }
  }).catch(() => {
    // 用户取消操作
  })
}

// 发送消息的主逻辑
const sendMessage = async () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入指令内容')
    return
  }

  const userMsg = inputText.value
  messageList.value.push({ role: 'user', content: userMsg })
  inputText.value = ''
  loading.value = true

  await saveHistory()
  scrollToBottom()

  try {
    const res: any = await sendAiChat(userMsg)
    const reply = res.data || res || '已执行完成'
    messageList.value.push({ role: 'ai', content: reply })

    await saveHistory()
  } catch (error) {
    messageList.value.push({ role: 'ai', content: '请求超时或后端报错，请检查控制台。' })
    console.error('AI接口调用失败:', error)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
/* 你的原有样式保持不变，我这边省略展示节省篇幅，你保留原本的 style 即可 */
.ai-chat-container { padding: 20px; height: calc(100vh - 120px); }
.chat-card { height: 100%; display: flex; flex-direction: column; }
:deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; overflow: hidden; padding: 0; }
.card-header { font-weight: bold; font-size: 16px; display: flex; justify-content: space-between; align-items: center; }
.message-list { flex: 1; padding: 20px; overflow-y: auto; background-color: #f5f7fa; }
.message-item { display: flex; margin-bottom: 20px; }
.message-item.user { flex-direction: row-reverse; }
.avatar { width: 40px; height: 40px; line-height: 40px; text-align: center; border-radius: 50%; background-color: #409eff; color: white; font-size: 14px; flex-shrink: 0; }
.message-item.ai .avatar { background-color: #67c23a; margin-right: 15px; }
.message-item.user .avatar { margin-left: 15px; }
.content { max-width: 60%; padding: 10px 15px; border-radius: 8px; background-color: white; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); line-height: 1.5; word-wrap: break-word; }
.message-item.user .content { background-color: #ecf5ff; }
.input-area { display: flex; padding: 15px 20px; background-color: white; border-top: 1px solid #ebeef5; }
</style>