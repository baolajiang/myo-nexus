import request from '../utils/request'

export function getLogList(params: any) {
    return request({
        url: '/admin/sysLog/list',
        method: 'post',
        data: params // PageParams 包含 page, pageSize, keyword
    })
}




export function uploadLog(params: any) {
    return request({
        url: '/admin/sysLog/upload',
        method: 'post',
        data: params
    })
}