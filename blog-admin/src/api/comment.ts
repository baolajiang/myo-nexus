import request from '../utils/request'

// 获取评论列表 (分页、可带关键词搜索)
export function getCommentList(data: any) {
    return request({
        url: '/admin/comment/list',
        method: 'post',
        data
    })
}

// 强制删除评论
export function deleteComment(id: string) {
    return request({
        url: `/admin/comment/delete/${id}`,
        method: 'post'
    })
}