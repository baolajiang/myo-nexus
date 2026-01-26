import request from '@/request'

export function getMAC() {
  return request({
    url: `/articles/queryMAC`,
    method: 'post',
  })
}
// 获取文章接口
export function getArticles(query, page,token) {
  return request({
	headers: {'Authorization': token},
    url: '/articles',
    method: 'post',
    data: {
      page: page.pageNumber,
      pageSize: page.pageSize,
      name: page.name,
      sort: page.sort,
      year: page.year,
      month: page.month,
      tagId: query.tagId,
      categoryId: query.categoryId
    }
  })
}
// 获取热门文章接口
export function getHotArtices() {
  return request({
    url: '/articles/hot',
    method: 'post'
  })
}
// 获取最新文章接口
export function getNewArtices() {
  return request({
    url: '/articles/new',
    method: 'post'
  })
}
// 查看文章接口
export function viewArticle(id,token) {
  return request({
	headers: {'Authorization': token},
    url: `/articles/view/${id}`,
    method: 'post'
  })
}
// 获取分类下的文章接口
export function getArticlesByCategory(id) {
  return request({
    url: `/articles/category/${id}`,
    method: 'post'
  })
}

export function getArticlesByTag(id) {
  return request({
    url: `/articles/tag/${id}`,
    method: 'post'
  })
}

// 发布文章接口
export function publishArticle(article,token) {
  return request({
    headers: {'Authorization': token},
    url: '/articles/publish',
    method: 'post',
    data: article
  })
}
// 获取文章列表接口
export function listarticles() {
  return request({
    url: '/articles/listarticles',
    method: 'post'
  })
}
// 获取文章详情接口
export function getArticleById(id) {
  return request({
    url: `/articles/${id}`,
    method: 'post'
  })
}


// 获取文章列表数量接口
export function getListArticleCount(token) {
  return request({
	headers: {'Authorization': token},
    url: `/articles/getListArticleCount`,
    method: 'post'
  })
}

// 获取我的文章
export function getMyArticles(page) {
  return request({
    url: '/articles/my', // 对应后端的接口地址
    method: 'post',
    data: page
  })
}

// src/api/article.js
export function getArticlesByAuthor(query) {
  return request({
    url: '/articles/author', // 假设你后端把 my 改成了通用的 author 或者你复用之前的
    method: 'post',
    data: query
  })
}
