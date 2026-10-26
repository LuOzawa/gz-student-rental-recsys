import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
// 导入 Element Plus
import ElementPlus from 'element-plus'
// 导入自定义主题色配置
import './styles/element-variables.scss'
// 导入设计系统
import './styles/design-system.scss'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// 导入初始化样式
import './assets/global.css'
// 中文
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

// 添加全局警告过滤器，抑制第三方库的无关警告
const filterWarnings = (args) => {
  if (args[0] && typeof args[0] === 'string') {
    const msg = args[0]
    // 抑制无关的第三方库警告
    const filters = [
      'ResizeObserver loop',
      '[el-radio]',
      '[el-pagination]',
      'ElementPlusError'
    ]
    return filters.some(f => msg.includes(f))
  }
  // 也支持对象形式的错误消息
  if (args[0] && args[0].message && typeof args[0].message === 'string') {
    const msg = args[0].message
    const filters = [
      'ResizeObserver loop',
      '[el-radio]',
      '[el-pagination]',
      'ElementPlusError'
    ]
    return filters.some(f => msg.includes(f))
  }
  return false
}

const originalConsoleError = console.error
const originalConsoleWarn = console.warn

console.error = (...args) => {
  if (!filterWarnings(args)) {
    originalConsoleError(...args)
  }
}

console.warn = (...args) => {
  if (!filterWarnings(args)) {
    originalConsoleWarn(...args)
  }
}

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus, {
  locale: zhCn
})
// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.mount('#app')
