import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 路由配置
const routes = [
  { path: '/login', component: () => import('@/views/Login.vue') },

  // 403
  { path: '/forbidden', component: () => import('@/views/Forbidden.vue') },

  // 主布局（所有业务页面的父路由）
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: '工作台',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', roles: ['CS_MANAGER', 'BIZ_LEADER', 'DESIGNER'] }
      },
      {
        path: 'orders',
        name: '订单列表',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单列表', roles: ['CS_MANAGER', 'BIZ_LEADER'] }
      },
      {
        path: 'orders/create',
        name: '创建订单',
        component: () => import('@/views/order/OrderCreate.vue'),
        meta: { title: '创建订单', roles: ['CS_MANAGER'] }
      },
      {
        path: 'orders/:id',
        name: '订单详情',
        component: () => import('@/views/order/OrderDetail.vue'),
        meta: { title: '订单详情', roles: ['CS_MANAGER', 'BIZ_LEADER'] }
      },
      {
        path: 'tasks',
        name: '任务列表',
        component: () => import('@/views/task/TaskList.vue'),
        meta: { title: '任务列表', roles: ['CS_MANAGER', 'BIZ_LEADER', 'DESIGNER'] }
      },
      {
        path: 'tasks/:id',
        name: '任务详情',
        component: () => import('@/views/task/TaskDetail.vue'),
        meta: { title: '任务详情', roles: ['CS_MANAGER', 'BIZ_LEADER', 'DESIGNER'] }
      }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 刷新时恢复
  if (!userStore.isLoggedIn) userStore.restore()

  // 没登录 → 去登录页（除了 /login 自己）
  if (!userStore.isLoggedIn && to.path !== '/login') {
    return next('/login')
  }
  if (userStore.isLoggedIn && to.path === '/login') {
    return next('/dashboard')
  }

  // 角色权限校验
  if (to.meta.roles && userStore.role) {
    if (!to.meta.roles.includes(userStore.role)) {
      return next('/forbidden')
    }
  }

  next()
})

export default router
