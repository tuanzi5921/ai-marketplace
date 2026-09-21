import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
// Element Plus 全量样式（保证 ElMessage/ElMessageBox 等命令式组件在 .ts 中可用）
import 'element-plus/dist/index.css'
import './styles/main.css'

// 企业 AI 应用市场运营后台 — 应用入口
const app = createApp(App)

// 注册 Element Plus 图标为全局组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
