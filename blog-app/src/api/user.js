import request from '@/request'

export function updateUser(data) {
  return request({
    url: '/users/updateUser',
    method: 'post',
    data
  })
}
// 获取指定用户信息
export function getUserPublicInfo(id) {
  return request({
    url: `/users/info/${id}`,
    method: 'get'
  })
}
