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
          <div
              class="content"
              v-if="msg.role === 'ai'"
              v-html="renderMarkdown(msg.content)"
          ></div>
          <div class="content" v-else>{{ msg.content }}</div>
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
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { getAiHistory, saveAiHistory, clearAiHistory, sendAiChat } from '../../api/ai'

marked.setOptions({ breaks: true, gfm: true })

const renderMarkdown = (content: string): string => {
  return DOMPurify.sanitize(marked.parse(content) as string)
}

const inputText = ref('')
const loading = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const messageList = ref<Array<{role: string, content: string}>>([])

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const loadHistory = async () => {
  try {
    const res: any = await getAiHistory()
    let historyData: any[] = []
    if (Array.isArray(res)) historyData = res
    else if (res && Array.isArray(res.data)) historyData = res.data
    else if (res && res.data && Array.isArray(res.data.data)) historyData = res.data.data
    else if (typeof res === 'string') { try { historyData = JSON.parse(res) } catch(e){} }
    else if (res && res.data && typeof res.data === 'string') { try { historyData = JSON.parse(res.data) } catch(e){} }

    messageList.value = historyData.length > 0
        ? historyData
        : [{ role: 'ai', content: '管理员您好，我是基于 Spring AI Alibaba 构建的智能助手。您可以直接让我查询或封禁用户。' }]
    scrollToBottom()
  } catch (error) {
    console.error('加载历史记录失败:', error)
    messageList.value = [{ role: 'ai', content: '管理员您好，我是智能助手。' }]
  }
}

const saveHistory = async () => {
  try {
    await saveAiHistory(JSON.parse(JSON.stringify(messageList.value)))
  } catch (error) {
    console.error('保存云端记忆失败:', error)
  }
}

const clearHistory = () => {
  ElMessageBox.confirm('确定要清空与 AI 的所有聊天记录并重置其记忆吗？', '系统提示', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    loading.value = true
    try {
      await clearAiHistory()
      messageList.value = [{ role: 'ai', content: '记忆已彻底清空，我们开启全新的对话吧。' }]
      await saveHistory()
      ElMessage.success('聊天记录与AI记忆已成功清空')
    } catch (error) {
      console.error('清空失败:', error)
      ElMessage.error('清空失败，请检查网络或后端日志')
    } finally {
      loading.value = false
    }
  }).catch(() => {})
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

onMounted(() => { loadHistory() })
</script>

<style scoped>

.ai-chat-container {
  padding: 20px;
  height: calc(100vh - 120px);
  box-sizing: border-box;
  overflow: hidden;
}
.chat-card { height: 100%; display: flex; flex-direction: column; }
:deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; overflow: hidden; padding: 0; }
.card-header { font-weight: bold; font-size: 16px; display: flex; justify-content: space-between; align-items: center; }
.message-list { flex: 1; padding: 20px; overflow-y: auto; overflow-x: hidden; background-color: #f5f7fa; }
.message-item { display: flex; margin-bottom: 20px; }
.message-item.user { flex-direction: row-reverse; }
.avatar { width: 40px; height: 40px; line-height: 40px; text-align: center; border-radius: 50%; background-color: #409eff; color: white; font-size: 14px; flex-shrink: 0; }
.message-item.ai .avatar { background-color: #67c23a; margin-right: 15px; }
.message-item.user .avatar { margin-left: 15px; }
.content { max-width: 60%; padding: 10px 15px; border-radius: 8px; background-color: white; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); line-height: 1.5; word-wrap: break-word; overflow: hidden; min-width: 0; }
.message-item.user .content { background-color: #ecf5ff; }
.input-area { display: flex; padding: 15px 20px; background-color: white; border-top: 1px solid #ebeef5; }

/* Markdown 内容防止撑宽 */
:deep(.content) { overflow: hidden; min-width: 0; }

/* 表格 - 固定宽度，禁止横向撑开 */
:deep(.content table) {
  border-collapse: collapse;
  width: 100%;
  max-width: 100%;
  margin: 8px 0;
  font-size: 13px;
  table-layout: fixed;
}
:deep(.content th),
:deep(.content td) {
  border: 1px solid #ddd;
  padding: 6px 10px;
  text-align: left;
  word-break: break-all;
  overflow-wrap: break-word;
  white-space: normal;
}
:deep(.content th) {
  background-color: #f2f2f2;
  font-weight: bold;
}
:deep(.content tr:nth-child(even)) { background-color: #fafafa; }

/* 代码块 - 允许内部横向滚动，但不撑开外层 */
:deep(.content pre) {
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 5px;
  overflow-x: auto;
  max-width: 100%;
}
:deep(.content code) {
  background-color: #f5f5f5;
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 12px;
  word-break: break-all;
}
:deep(.content img) { max-width: 100%; height: auto; }
:deep(.content p) { margin: 0 0 6px; }
:deep(.content p:last-child) { margin-bottom: 0; }
</style>