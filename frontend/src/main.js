import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'             /* 1. Element Plus 原始样式 */
import '@/styles/tokens/primitives.css'          /* 2. 基础令牌 */
import '@/styles/tokens/semantic.css'            /* 3. 语义令牌 */
import '@/styles/tokens/typography.css'          /* 4. 字体令牌 */
import '@/styles/tokens/spacing.css'             /* 5. 间距阴影布局 */
import '@/styles/tokens/motion.css'              /* 6. 动效令牌 */
import '@/styles/tokens/components.css'          /* 7. 组件令牌 */
import '@/styles/element-override.css'           /* 8. Element Plus 覆盖 */
import '@/styles/global.css'                     /* 9. 全局样式 (最后加载) */
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
