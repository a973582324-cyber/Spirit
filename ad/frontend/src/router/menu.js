/**
 * 侧边栏菜单配置（按角色显示）
 */
export const menuByRole = {
  CS_MANAGER: [
    { path: '/dashboard', title: '工作台', icon: 'HomeFilled' },
    { path: '/orders', title: '订单列表', icon: 'Document' },
    { path: '/orders/create', title: '新建订单', icon: 'Plus' },
    { path: '/tasks', title: '任务列表', icon: 'Tickets' }
  ],
  BIZ_LEADER: [
    { path: '/dashboard', title: '工作台', icon: 'HomeFilled' },
    { path: '/orders', title: '订单列表', icon: 'Document' },
    { path: '/tasks', title: '任务列表', icon: 'Tickets' }
  ],
  DESIGNER: [
    { path: '/dashboard', title: '工作台', icon: 'HomeFilled' },
    { path: '/tasks', title: '我的任务', icon: 'Tickets' }
  ]
}
