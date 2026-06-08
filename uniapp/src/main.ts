console.log('[main.ts] Start importing modules')
import { createSSRApp } from 'vue'
console.log('[main.ts] Vue imported successfully')

import { createPinia } from 'pinia'
console.log('[main.ts] Pinia imported successfully')

import App from './App.vue'
console.log('[main.ts] App.vue imported successfully')

export function createApp() {
  console.log('[main.ts] createApp() called')
  const app = createSSRApp(App)
  console.log('[main.ts] SSR app created')

  const pinia = createPinia()
  console.log('[main.ts] Pinia created')

  app.use(pinia)
  console.log('[main.ts] Pinia installed')

  const result = {
    app,
    Pinia: pinia,
  }
  console.log('[main.ts] createApp() returning, app:', !!result.app, 'pinia:', !!result.Pinia)
  return result
}
