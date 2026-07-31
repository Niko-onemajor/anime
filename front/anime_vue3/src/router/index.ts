import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/auth'
  },
  {
    path: '/auth',
    name: 'auth',
    component: () => import('../views/pages/Auth.vue')
  },
  {
    path: '/index',
    name: 'index',
    component: () => import('../views/pages/Index.vue')
  },
  {
    path: '/category',
    name: 'category',
    component: () => import('../views/pages/Category.vue')
  },
  {
    path: '/forum',
    name: 'forum',
    component: () => import('../views/pages/Forum.vue')
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('../views/pages/Message.vue')
  },
  {
    path: '/chat',
    name: 'chat',
    component: () => import('../views/pages/Chat.vue')
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/pages/Profile.vue')
  },
  {
    path: '/anime/:id',
    name: 'animeDetail',
    component: () => import('../views/anime/AnimeDetail.vue')
  },
  {
    
    path: '/anime/:id/play/:episode',
    name: 'animePlayer',
    component: () => import('../views/player/AnimePlayer.vue')
  },
  {
    path: '/user/:username',
    name: 'userHome',
    component: () => import('../views/pages/UserHome.vue')
  },
  {
    path: '/test',
    name: 'test',
    component: () => import('../views/test/TestPage.vue')
  },
  // 管理员路由
  {
    path: '/admin/users',
    name: 'adminUsers',
    component: () => import('../views/admin/UserManagement.vue')
  },
  {
    path: '/admin/animes',
    name: 'adminAnimes',
    component: () => import('../views/admin/AnimeManagement.vue')
  },
  {
    path: '/admin/forum',
    name: 'adminForum',
    component: () => import('../views/admin/ForumManagement.vue')
  },
  {
    path: '/admin/deleted',
    name: 'adminDeleted',
    component: () => import('../views/admin/DeletedRecords.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：管理员权限检查
router.beforeEach((to, from, next) => {
  const adminRoutes = ['/test', '/admin/users', '/admin/animes', '/admin/forum', '/admin/deleted']

  if (adminRoutes.some(route => to.path.startsWith(route))) {
    const role = localStorage.getItem('role')
    const token = localStorage.getItem('token')

    if (!token) {
      next('/auth')
      return
    }

    if (role !== 'admin' && role !== '1') {
      next('/index')
      return
    }
  }

  next()
})

export default router