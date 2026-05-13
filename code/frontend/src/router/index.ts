import { createRouter, createWebHistory } from 'vue-router'
import CollectPage from '@/views/CollectPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/collect'
    },
    {
      path: '/collect',
      name: 'collect',
      component: CollectPage
    }
  ]
})

export default router
