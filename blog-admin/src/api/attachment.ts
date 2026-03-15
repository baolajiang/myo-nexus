import request from '../utils/request'

// 分页查询附件列表
export function getAttachmentList(data: any, fileType?: string) {
    return request({
        url: '/admin/attachment/list' + (fileType ? `?fileType=${fileType}` : ''),
        method: 'post',
        data
    })
}

// 删除附件（同步删除 R2 文件和数据库记录）
export function deleteAttachment(id: string) {
    return request({
        url: `/admin/attachment/delete/${id}`,
        method: 'post'
    })
}