import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

// 导入 Leaflet CSS
import 'leaflet/dist/leaflet.css'

// 导入全局样式（注意顺序：先变量，再使用变量的样式）
import './styles/variables.scss'
import './styles/global.scss'
import './styles/reset.scss'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.mount('#app')
