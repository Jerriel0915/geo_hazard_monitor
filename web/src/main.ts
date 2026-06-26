import {createApp} from 'vue'
import './style.css'
import './assets/custom-icon/iconfont.css'
import App from './App.vue'
import router from './router'
// Element Plus 按需导入（组件+样式由 vite 插件自动注入，命令式 API 在各文件显式 import）
import 'element-plus/theme-chalk/index.css'

const app = createApp(App)

// 全局 Vue 错误边界 — 组件渲染异常不白屏，降级展示错误信息
app.config.errorHandler = (err, _instance, info) => {
  console.error('[Vue Error]', err, `\n  component: ${info}`)
  // 生产环境可上报到监控平台（Sentry / 自定义日志）
  // if (import.meta.env.PROD) reportError(err, info)
}

// 未捕获的 Promise  rejection（非 axios 拦截器已处理的）
window.addEventListener('unhandledrejection', (event) => {
  console.error('[Unhandled Promise]', event.reason)
  event.preventDefault()
})

app.use(router)
app.mount('#app')
