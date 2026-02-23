<template>
  <div class="ai-chat-container">
    <el-card class="chat-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>AI 超级管理员助手</span>
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
import { ElMessage } from 'element-plus'
import request from '../../utils/request' // 确保你的 request.ts 路径是正确的

const inputText = ref('')
const loading = ref(false)
const messageListRef = ref<HTMLElement | null>(null)

// 从本地存储加载历史记录，实现刷新不丢失
const loadHistory = () => {
  const saved = localStorage.getItem('blog_ai_chat_history')
  if (saved) {
    return JSON.parse(saved)
  }
  return [
    { role: 'ai', content: '管理员您好，我是基于 Spring AI Alibaba 构建的智能助手。您可以直接让我查询或封禁用户。' }
  ]
}

const messageList = ref(loadHistory())

const saveHistory = () => {
  localStorage.setItem('blog_ai_chat_history', JSON.stringify(messageList.value))
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const sendMessage = async () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入指令内容')
    return
  }

  const userMsg = inputText.value
  messageList.value.push({ role: 'user', content: userMsg })
  inputText.value = ''
  loading.value = true
  saveHistory()
  scrollToBottom()

  try {
    const res = await request({
      url: '/admin/ai/chat',
      method: 'post',
      data: userMsg,
      headers: {
        'Content-Type': 'text/plain'
      },
      timeout: 60000 // 强行设置 60 秒超时，专门给大模型留足思考时间
    })

    const reply = res.data || res || '已执行完成'
    messageList.value.push({ role: 'ai', content: reply })
    saveHistory()

  } catch (error) {
    messageList.value.push({ role: 'ai', content: '请求超时或后端报错，请检查控制台。' })
    console.error('AI接口调用失败:', error)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.ai-chat-container {
  padding: 20px;
  height: calc(100vh - 120px);
}
.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}
:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}
.card-header {
  font-weight: bold;
  font-size: 16px;
}
.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f5f7fa;
}
.message-item {
  display: flex;
  margin-bottom: 20px;
}
.message-item.user {
  flex-direction: row-reverse;
}
.avatar {
  width: 40px;
  height: 40px;
  line-height: 40px;
  text-align: center;
  border-radius: 50%;
  background-color: #409eff;
  color: white;
  font-size: 14px;
  flex-shrink: 0;
}
.message-item.ai .avatar {
  background-color: #67c23a;
  margin-right: 15px;
}
.message-item.user .avatar {
  margin-left: 15px;
}
.content {
  max-width: 60%;
  padding: 10px 15px;
  border-radius: 8px;
  background-color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  line-height: 1.5;
  word-wrap: break-word;
}
.message-item.user .content {
  background-color: #ecf5ff;
}
.input-area {
  display: flex;
  padding: 15px 20px;
  background-color: white;
  border-top: 1px solid #ebeef5;
}
</style>