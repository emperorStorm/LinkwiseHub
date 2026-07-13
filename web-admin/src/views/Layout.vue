<template>
  <a-layout class="layout">
    <a-layout-sider 
      v-model:collapsed="collapsed" 
      :trigger="null" 
      collapsible
      width="240"
      :collapsed-width="80"
      class="layout-sider"
    >
      <div class="logo" :class="{ 'logo-collapsed': collapsed }">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <transition name="fade">
          <span v-if="!collapsed" class="logo-text">智链中枢</span>
        </transition>
      </div>
      
      <a-menu
        mode="inline"
        :selected-keys="[currentKey]"
        :open-keys="openKeys"
        @select="handleMenuSelect"
        @openChange="handleOpenChange"
        class="side-menu"
      >
        <a-sub-menu key="base">
          <template #icon><setting-outlined /></template>
          <template #title>基础管理</template>
          <a-menu-item key="organization">
            <template #icon><home-outlined /></template>
            组织机构管理
          </a-menu-item>
          <a-menu-item key="user">
            <template #icon><user-outlined /></template>
            用户管理
          </a-menu-item>
          <a-menu-item key="role">
            <template #icon><team-outlined /></template>
            角色管理
          </a-menu-item>
        </a-sub-menu>
        <a-sub-menu key="ai">
          <template #icon><robot-outlined /></template>
          <template #title>AI管理</template>
          <a-menu-item key="ai-chat">
            <template #icon><message-outlined /></template>
            智能问答
          </a-menu-item>
          <a-menu-item key="ai-documents">
            <template #icon><file-text-outlined /></template>
            文档分片
          </a-menu-item>
          <a-menu-item key="ai-knowledge">
            <template #icon><folder-open-outlined /></template>
            知识库
          </a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>
    
    <a-layout class="layout-main">
      <a-layout-header class="layout-header">
        <div class="header-left">
          <div class="trigger" @click="toggleCollapsed">
            <menu-unfold-outlined v-if="collapsed" />
            <menu-fold-outlined v-else />
          </div>
          <a-breadcrumb class="breadcrumb">
            <a-breadcrumb-item>
              <home-outlined />
            </a-breadcrumb-item>
            <a-breadcrumb-item v-if="currentMenuParent">{{ currentMenuParent }}</a-breadcrumb-item>
            <a-breadcrumb-item>{{ currentTitle }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        
        <div class="header-right">
          <a-dropdown>
            <div class="user-info">
              <a-avatar class="user-avatar" :style="{ backgroundColor: '#3b82f6' }">
                <template #icon><user-outlined /></template>
              </a-avatar>
              <span class="user-name">管理员</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile">
                  <user-outlined /> 个人中心
                </a-menu-item>
                <a-menu-item key="settings">
                  <setting-outlined /> 系统设置
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="handleLogout">
                  <logout-outlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      
      <a-layout-content class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  HomeOutlined, UserOutlined, TeamOutlined, SettingOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined, LogoutOutlined,
  RobotOutlined, MessageOutlined, FileTextOutlined, FolderOpenOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()

const collapsed = ref(false)
const currentKey = ref(route.name)
const openKeys = ref(['base'])

const menuMap = {
  organization: { title: '组织机构管理', parent: '基础管理' },
  user: { title: '用户管理', parent: '基础管理' },
  role: { title: '角色管理', parent: '基础管理' },
  'ai-chat': { title: '智能问答', parent: 'AI管理' },
  'ai-documents': { title: '文档分片', parent: 'AI管理' },
  'ai-knowledge': { title: '知识库', parent: 'AI管理' }
}

const currentTitle = computed(() => {
  return menuMap[currentKey.value]?.title || '智链中枢'
})

const currentMenuParent = computed(() => {
  return menuMap[currentKey.value]?.parent || ''
})

watch(
  () => route.name,
  (newName) => {
    currentKey.value = newName
    if (['organization', 'user', 'role'].includes(newName)) {
      openKeys.value = ['base']
    } else if (['ai-chat', 'ai-documents', 'ai-knowledge'].includes(newName)) {
      openKeys.value = ['ai']
    }
  },
  { immediate: true }
)

const toggleCollapsed = () => {
  collapsed.value = !collapsed.value
}

const handleMenuSelect = ({ key }) => {
  const routeMap = {
    organization: '/base/organization',
    user: '/base/user',
    role: '/base/role',
    'ai-chat': '/ai/chat',
    'ai-documents': '/ai/documents',
    'ai-knowledge': '/ai/knowledge'
  }
  if (routeMap[key]) {
    router.push(routeMap[key])
  }
}

const handleOpenChange = (keys) => {
  openKeys.value = keys
}

const handleLogout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background: #f1f5f9;
}

.layout-sider {
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.logo-collapsed {
  padding: 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  color: #3b82f6;
  flex-shrink: 0;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.logo-text {
  margin-left: 12px;
  font-size: 18px;
  font-weight: 700;
  color: white;
  white-space: nowrap;
}

.side-menu {
  background: transparent;
  border: none;
}

.side-menu :deep(.ant-menu-submenu-title),
.side-menu :deep(.ant-menu-item) {
  color: rgba(255, 255, 255, 0.75);
  margin: 4px 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.side-menu :deep(.ant-menu-submenu-title:hover),
.side-menu :deep(.ant-menu-item:hover) {
  color: white;
  background: rgba(255, 255, 255, 0.1);
}

.side-menu :deep(.ant-menu-item-selected) {
  color: white;
  background: linear-gradient(135deg, #3b82f6 0%, #1e40af 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.side-menu :deep(.ant-menu-sub) {
  background: transparent;
}

.layout-main {
  background: #f1f5f9;
}

.layout-header {
  background: white;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.trigger {
  font-size: 18px;
  color: #64748b;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.trigger:hover {
  color: #3b82f6;
  background: #f1f5f9;
}

.breadcrumb {
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.user-info:hover {
  background: #f1f5f9;
}

.user-avatar {
  width: 32px;
  height: 32px;
}

.user-name {
  font-size: 14px;
  color: #334155;
  font-weight: 500;
}

.layout-content {
  margin: 24px;
  padding: 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  min-height: calc(100vh - 112px);
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.page-enter-active,
.page-leave-active {
  transition: all 0.3s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 响应式 */
@media (max-width: 768px) {
  .layout-content {
    margin: 16px;
    padding: 16px;
  }
  
  .user-name {
    display: none;
  }
}
</style>
