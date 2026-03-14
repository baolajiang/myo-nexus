import request from '../utils/request'

// 获取分类列表
export function getCategoryList() {
    return request({
        url: '/categorys/detail',
        method: 'get'
    })
}

// 新增分类
export function addCategory(data: any) {
    return request({
        url: 'admin/category',
        method: 'post',
        data
    })
}

// 修改分类
export function updateCategory(data: any) {
    return request({
        url: 'admin/category',
        method: 'put',
        data
    })
}

// 删除分类
export function deleteCategory(id: string) {
    return request({
        url: `admin/category/${id}`,
        method: 'delete'
    })
}