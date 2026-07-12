import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/ai/onlyoffice-editor',
    name: 'onlyoffice-editor',
    component: () => import('../views/ai/OnlyOfficeEditor.vue'),
    meta: { requiresAuth: true, title: '文件编辑' }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../views/Layout.vue'),
    redirect: '/base/organization',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'base/organization',
        name: 'organization',
        component: () => import('../views/base/Organization.vue'),
        meta: { title: '组织机构管理' }
      },
      {
        path: 'base/user',
        name: 'user',
        component: () => import('../views/base/User.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'base/role',
        name: 'role',
        component: () => import('../views/base/Role.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'base/file',
        name: 'file',
        component: () => import('../views/base/FileManage.vue'),
        meta: { title: '文件管理' }
      },
      {
        path: 'ai/chat',
        name: 'ai-chat',
        component: () => import('../views/ai/Chat.vue'),
        meta: { title: '智能问答' }
      },
      {
        path: 'ai/documents',
        name: 'ai-documents',
        component: () => import('../views/ai/DocumentProcess.vue'),
        meta: { title: '文档分片' }
      },
      {
        path: 'ai/knowledge',
        name: 'ai-knowledge',
        component: () => import('../views/ai/KnowledgeBase.vue'),
        meta: { title: '知识库' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      next({ name: 'Login' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
