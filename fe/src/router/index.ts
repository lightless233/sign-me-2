import {createRouter, createWebHistory} from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'index',
      component: () => import("@/views/IndexView.vue")
    },
    {
      path: "/rule-manager",
      name: "rule-manager",
      component: () => import("@/views/RuleManager.vue")
    },
    {
      path: "/rule-sniff",
      name: "rule-sniff",
      component: () => import("@/views/RuleSniff.vue")
    }
  ],
})

export default router
