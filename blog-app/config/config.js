/**
 * 应用配置管理
 * 统一管理所有硬编码的URL和配置项
 */
import process from "shelljs";

// 环境变量配置
const ENV_CONFIG = {
  // 开发环境
  development: {
    backendBaseUrl: '/api',
    requestTimeout: 5000,
    tokenErrorCodes: [10003, 401, 403],
    frontendUrl: 'http://localhost:48082',
    adminUrl: 'http://localhost:48182'
  },
  // 生产环境
  production: {
    backendBaseUrl: '/api',
    requestTimeout: 10000,
    tokenErrorCodes: [10003, 401, 403],
    frontendUrl: 'https://myo.pub',
    adminUrl: 'https://admin.myo.pub'
  }
}

// 获取当前环境配置
const getConfig = () => {
  const env = process.env.NODE_ENV || 'development'
  return ENV_CONFIG[env] || ENV_CONFIG.development
}

// 导出配置
export const APP_CONFIG = getConfig()
