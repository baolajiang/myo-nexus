import request from '../utils/request' // 这里引入你项目中封装好的 axios 实例

// 根据分类ID获取该分类下的所有标签
export function getTagsByCategoryId(categoryId: string) {
    return request({
        url: `/tags/category/${categoryId}`,
        method: 'get'
    })
}

// 新增标签
export function addTag(data: any) {
    return request({
        url: 'admin/tags',
        method: 'post',
        data: data
    })
}

// 修改标签
export function updateTag(data: any) {
    return request({
        url: 'admin/tags',
        method: 'put',
        data: data
    })
}

// 删除标签
export function deleteTag(id: string) {
    return request({
        url: `admin/tags/${id}`,
        method: 'delete'
    })
}