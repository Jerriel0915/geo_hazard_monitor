import {createApp} from 'vue'
import './style.css'
import './assets/custom-icon/iconfont.css'
import App from './App.vue'
import router from './router'
// Element Plus 按需导入（组件+样式由 vite 插件自动注入，命令式 API 在各文件显式 import）
import 'element-plus/theme-chalk/index.css'

const app = createApp(App)
app.use(router)
app.mount('#app')
