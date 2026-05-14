import { createRouter, createWebHistory } from 'vue-router'
import CollectPage from '@/views/CollectPage.vue'
import PendingPage from '@/views/PendingPage.vue'

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
    },
    {
      path: '/pending',
      name: 'pending',
      component: PendingPage
    }
  ]
})

export default router
