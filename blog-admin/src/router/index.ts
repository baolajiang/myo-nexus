import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import Layout from '../layout/index.vue'
import { exchangeToken } from '../api/login'
import { ElMessage } from 'element-plus'
import { getConfig } from '../utils/config'

const { frontendLoginUrl } = getConfig()
const routes: Array<RouteRecordRaw> = [
    {
        path: '/',
        component: Layout,
        redirect: '/dashboard',
        children: [
            {
                path: 'dashboard',
                component: () => import('../views/dashboard/index.vue'),
                meta: { title: '仪表盘' }
            },
            // === 博客管理 ===
            {
                path: 'article/write',
                component: () => import('../views/article/write.vue'),
                meta: { title: '写文章' }
            },
            {
                path: 'article/list',
                component: () => import('../views/article/list.vue'),
                meta: { title: '文章列表' }
            },
            {
                path: 'category/index',
                component: () => import('../views/category/index.vue'),
                meta: { title: '分类管理' }
            },
            {
                path: 'tag/index',
                component: () => import('../views/tag/index.vue'),
                meta: { title: '标签管理' }
            },
            // === 运营管理 ===
            {
                path: 'comment/index',
                component: () => import('../views/comment/index.vue'),
                meta: { title: '评论管理' }
            },
            {
                path: 'link/index',
                component: () => import('../views/link/index.vue'),
                meta: { title: '友链管理' }
            },
            // === 用户管理 ===
            {
                path: 'user/index',
                component: () => import('../views/user/index.vue'),
                meta: { title: '用户列表', requireAuth: true }
            },
            // === 系统管理 ===
            {
                path: 'system/ip',
                component: () => import('../views/system/ip.vue'),
                meta: { title: 'IP黑名单' }
            },
            {
                path: 'system/log',
                component: () => import('../views/system/log.vue'),
                meta: { title: '操作日志' }
            },
            // === 智能助手 ===
            {
                path: 'ai/chat',
                component: () => import('../views/ai/chat.vue'),
                meta: { title: '智能助手' }
            },
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

//  全局路由守卫
router.beforeEach(async (to, _from, next) => {
    // 1. 获取 URL 里的 ticket 参数 (从前台跳过来的)
    const ticket = to.query.ticket as string

    // 2. 获取本地存储的 token (已登录过的)
    const token = localStorage.getItem('token')

    // === 情况 A: 带有 Ticket (正在进行单点登录) ===
    if (ticket) {

        try {
            // 调用后端接口，用票据换 Token
            const res = await exchangeToken(ticket)
            if (res.data.success) {
                // 换取成功：存 Token，提示成功
                localStorage.setItem('token', res.data.data)
                ElMessage.success('登录成功')

                // 这里的 replace: true 是为了把 url 里的 ticket 参数洗掉，不让用户看见
                next({ path: to.path, query: {}, replace: true })
            } else {
                ElMessage.error('票据已失效，请重新登录')
                // 验证失败，踢回前台登录页
                window.location.href = frontendLoginUrl
            }
        } catch (error) {
            console.error(error)
            window.location.href = frontendLoginUrl
        }
        return
    }

    // === 情况 B: 已有 Token (正常访问) ===
    if (token) {
        // 放行，让他进去

        next()
        return
    }

    // === 情况 C: 既没 Ticket 也没 Token (非法闯入) ===
    // 拦截！直接踢回前台博客的登录页
    ElMessage.warning('请先登录')
    //注意：请确保这个地址是前台博客的真实地址
    window.location.href = frontendLoginUrl
})

export default router