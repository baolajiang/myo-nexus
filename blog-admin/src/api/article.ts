import request from '../utils/request'

// 获取文章列表
export function getArticleList(data: any) {
    return request({
        url: '/admin/article/list',
        method: 'post',
        data
    })
}

// 发布文章 (新增)
export function publishArticle(data: any) {
    return request({
        url: '/articles/publish',
        method: 'post',
        data
    })
}

// 更新文章 (修改)
export function updateArticle(data: any) {
    return request({
        url: '/articles/update',
        method: 'post',
        data
    })
}

// 获取文章详情 (用于编辑页面数据回显)
export function getArticleById(id: string) {
    return request({
        url: `/articles/view/${id}`,
        method: 'post'
    })
}

// 删除文章
export function deleteArticle(id: string) {
    return request({
        url: `/admin/article/delete/${id}`,
        method: 'post'
    })
}