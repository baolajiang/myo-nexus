import request from '../utils/request'

// 获取 AI 聊天历史记录
export function getAiHistory() {
    return request({
        url: '/admin/ai/history',
        method: 'get'
    })
}

// 保存 AI 聊天历史记录
export function saveAiHistory(data: any) {
    return request({
        url: '/admin/ai/history',
        method: 'post',
        data
    })
}

// 清空 AI 聊天历史记录与记忆
export function clearAiHistory() {
    return request({
        url: '/admin/ai/clear',
        method: 'get'
    })
}

// 发送指令给 AI
export function sendAiChat(data: string) {
    return request({
        url: '/admin/ai/chat',
        method: 'post',
        data,
        headers: {
            'Content-Type': 'text/plain'
        },
        timeout: 60000 // 专门为大模型留足 60 秒超时时间
    })
}