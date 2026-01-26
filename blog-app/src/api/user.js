import request from '@/request'

export function updateUser(data) {
  return request({
    url: '/users/updateUser',
    method: 'post',
    data
  })
}



export function getUserPublicInfo(id) {
  return request({
    url: `/users/user/public/${id}`, // 对应后端接口
    method: 'post'
  })
}
