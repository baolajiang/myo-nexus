import request from '@/request'

export function updateUser(data) {
  return request({
    url: '/users/updateUser',
    method: 'post',
    data
  })
}
