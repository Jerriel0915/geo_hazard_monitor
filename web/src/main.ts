import { createApp } from 'vue'
import axios from 'axios'
import './style.css'
import './assets/custom-icon/iconfont.css'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { handleAuthFailure } from './utils/auth'

axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token && !config.headers?.Authorization) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

axios.interceptors.response.use(
  (response) => {
    if (handleAuthFailure(response.data, response.status)) {
      return Promise.reject(new Error('登录状态已失效'))
    }
    return response
  },
  (error) => {
    if (handleAuthFailure(error.response?.data, error.response?.status)) {
      return Promise.reject(error)
    }
    return Promise.reject(error)
  }
)

const app = createApp(App)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
