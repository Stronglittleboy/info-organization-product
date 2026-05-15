import { createRouter, createWebHistory } from 'vue-router'
import CollectPage from '@/views/CollectPage.vue'
import BrowsePage from '@/views/BrowsePage.vue'
import PendingPage from '@/views/PendingPage.vue'
import SearchPage from '@/views/SearchPage.vue'
import TopicListPage from '@/views/TopicListPage.vue'
import TopicDetailPage from '@/views/TopicDetailPage.vue'
import EntryDetailPage from '@/views/EntryDetailPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/collect' },
    { path: '/collect', name: 'collect', component: CollectPage },
    { path: '/browse', name: 'browse', component: BrowsePage },
    { path: '/pending', name: 'pending', component: PendingPage },
    { path: '/search', name: 'search', component: SearchPage },
    { path: '/topics', name: 'topics', component: TopicListPage },
    { path: '/topic/:topicId', name: 'topicDetail', component: TopicDetailPage },
    { path: '/entry/:entryId', name: 'entryDetail', component: EntryDetailPage }
  ]
})

export default router
