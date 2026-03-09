import request from '../utils/request'

export function getLogList(params: any) {
    return request({
        url: '/admin/log/list',
        method: 'post',
        data: params // PageParams 包含 page, pageSize, keyword
    })
}