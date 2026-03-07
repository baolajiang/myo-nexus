import request from '@/request'
// 登录接口
export function login(account, password) {
  const data = {
    account,
    password
  }
  return request({
    url: '/login',
    method: 'post',
    data
  })
}

//登出
export function logout(token) {
  return request({
    headers: {'Authorization': token},
    url: '/logout',
    method: 'get'
  })
}
//获取当前登录用户的信息
export function getUserInfo(token) {
  return request({
    headers: {'Authorization': token},
    url: '/users/currentUser',
    method: 'get'
  })
}
// 注册接口
export function register(data) {
  return request({
    url: '/register',
    method: 'post',
    data // 简写为 data 原行 data: data
  })
}

// 发送验证码接口
export function sendCode(email) {
  return request({
    url: '/register/sendCode',
    method: 'post',
    data: { email }
  })
}
// 获取登录后台的一次性票据
export function getTicket(token) {
  return request({
    headers: {'Authorization': token}, // 将 Token放入请求头
    url: '/login/ticket',
    method: 'post'
  })
}

// 获取验证码接口
export function getCaptcha() {
  return request({
    url: '/yzm/captchaClass',
    method: 'post'
  })
}

// 验证验证码接口
export function verifyCaptcha(data) {
  return request({
    url: '/yzm/login',
    method: 'post',
    data
  })
}


