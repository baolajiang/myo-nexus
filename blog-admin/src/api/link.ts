import request from '../utils/request'

export function getLinkList(data: any) {
    return request({
        url: '/admin/link/list',
        method: 'post', data
    })
}
export function addLink(data: any) {
    return request({
        url: '/admin/link/add',
        method: 'post', data
    })
}
export function updateLink(data: any) {
    return request({
        url: '/admin/link/update',
        method: 'post', data
    })
}
export function deleteLink(id: string) {
    return request({
        url: `/admin/link/delete/${id}`,
        method: 'post'
    })
}
export function changeLinkStatus(data: { id: string, status: number }) {
    return request({
        url: '/admin/link/changeStatus',
        method: 'post', data
    })
}