import request from '../utils/request'

// 分页查询 IP 黑名单列表
export function getIpBlacklist(params: any) {
    return request({
        url: '/admin/ip/list',
        method: 'post',
        data: params
    })
}

// 封禁 IP
export function banIp(data: { ip: string, reason: string, expireTime: number | null }) {
    return request({
        url: '/admin/ip/ban',
        method: 'post',
        data
    })
}

// 解封 IP
export function unbanIp(ip: string) {
    return request({
        url: '/admin/ip/unban',
        method: 'post',
        data: ip,
        headers: { 'Content-Type': 'text/plain' }
    })
}

// 删除 IP 黑名单记录（彻底删除，同步删除 Redis）
export function deleteIpRecord(id: number) {
    return request({
        url: `/admin/ip/delete/${id}`,
        method: 'post'
    })
}