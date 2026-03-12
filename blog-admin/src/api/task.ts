import request from '../utils/request'

// 获取任务列表
export function getTaskList(params: any) {
    return request({
        url: '/admin/task/list',
        method: 'post',
        data: params
    })
}

// 手动执行一次任务
export function runTaskOnce(taskId: string) {
    return request({
        url: `/admin/task/run/${taskId}`,
        method: 'post'
    })
}

// 获取任务的执行日志
export function getTaskLogList(params: any) {
    return request({
        url: '/admin/task/log/list',
        method: 'post',
        data: params
    })
}
// 启停任务
export function changeTaskStatus(params: any) {
    return request({
        url: '/admin/task/status',
        method: 'post',
        data: params
    })
}