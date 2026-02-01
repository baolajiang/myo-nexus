import request from '../utils/request'

// 获取文章列表
export function getArticleList(data: any) {
    return request({
        url: '/admin/article/list',
        method: 'post',
        data
    })
}


// 删除文章 (预留)
export function deleteArticle(id: string) {
    return request({
        url: `/admin/article/delete/${id}`,
        method: 'post'
    })
}