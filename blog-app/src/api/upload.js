import request from '@/request'
/**
 * 上传文件
 * @param {FormData} formdata - 包含文件数据的 FormData 对象
 * @returns {Promise} - 包含上传结果的 Promise 对象
 */
export function upload(formdata) {
  return request({
    headers: {'Content-Type': 'multipart/form-data'},
    url: '/upload',
    method: 'post',
    data: formdata
  })
}
