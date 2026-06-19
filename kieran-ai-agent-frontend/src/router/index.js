import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoveAppView from '../views/LoveAppView.vue'
import ManusView from '../views/ManusView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { title: 'AI 应用中心' },
    },
    {
      path: '/love-app',
      name: 'love-app',
      component: LoveAppView,
      meta: { title: 'AI 嘴替教练' },
    },
    {
      path: '/manus',
      name: 'manus',
      component: ManusView,
      meta: { title: 'AI 超级智能体' },
    },
  ],
})

router.afterEach((to) => {
  document.title = `${to.meta.title || 'AI 应用'} - Kieran AI Agent`
})

export default router
