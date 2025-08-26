import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './assets/styles/global.scss'

import App from './App.vue'
import router from './router'

// Create Vue app
const app = createApp(App)

// Register Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// Install plugins
app.use(createPinia())
app.use(router)
app.use(ElementPlus, {
  size: 'default',
  zIndex: 3000
})

// Global error handler
app.config.errorHandler = (err, vm, info) => {
  console.error('Admin global error:', err)
  console.error('Component:', vm)
  console.error('Error info:', info)
}

// Global warning handler for development
if (import.meta.env.DEV) {
  app.config.warnHandler = (msg, vm, trace) => {
    console.warn('Admin global warning:', msg)
    console.warn('Component:', vm)
    console.warn('Trace:', trace)
  }
}

// Mount app
app.mount('#app')