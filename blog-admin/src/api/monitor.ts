import request from '../utils/request'

export function getCacheMonitorInfo() {
    return request({
        url: '/admin/monitor/cache',
        method: 'get'
    })
}