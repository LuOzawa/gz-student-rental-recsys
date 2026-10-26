/**
 * 环境变量工具函数
 * 用于兼容Vue CLI和Vite的环境变量访问
 */

// 获取基础API路径
export function getBaseApi() {
  // 优先使用Vite环境变量，如果不存在则尝试使用Vue CLI环境变量，最后使用默认值
  return import.meta.env.VITE_BASE_API || '/api'
}

// 获取当前环境模式
export function getMode() {
  return import.meta.env.MODE || 'development'
}

// 判断是否为生产环境
export function isProd() {
  return import.meta.env.PROD || false
}

// 判断是否为开发环境
export function isDev() {
  return import.meta.env.DEV || true
}

// 兼容Vue CLI的process.env访问方式
export const processEnv = {
  VUE_APP_BASE_API: getBaseApi(),
  NODE_ENV: getMode()
}

// 默认导出
export default {
  getBaseApi,
  getMode,
  isProd,
  isDev,
  processEnv
} 