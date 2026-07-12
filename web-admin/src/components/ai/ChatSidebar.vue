<template>
  <aside class="chat-sidebar" :class="{ 'chat-sidebar--collapsed': collapsed }">
    <div class="sidebar-header">
      <button class="brand-card" type="button" @click="$emit('createConversation')">
        <span class="brand-mark"><plus-outlined /></span>
        <span class="brand-copy">
          <strong>新建对话</strong>
          <small>开启新的办公问答</small>
        </span>
      </button>
      <button class="collapse-btn" type="button" @click="$emit('toggleCollapse')">
        <menu-fold-outlined v-if="!collapsed" />
        <menu-unfold-outlined v-else />
      </button>
    </div>

    <div class="sidebar-section" v-if="!collapsed">
      <span>历史会话</span>
      <em>{{ conversations.length }} 条</em>
    </div>

    <div class="conversation-list">
      <div v-if="conversations.length === 0" class="empty-list">
        <message-outlined />
        <span>暂无会话</span>
      </div>
      <button
        v-for="(conv, index) in conversations"
        :key="conv.id"
        type="button"
        class="conversation-item"
        :class="{ active: currentId === conv.id }"
        @click="$emit('selectConversation', conv)"
      >
        <span class="conv-icon">{{ index + 1 }}</span>
        <span class="conv-main">
          <strong>{{ conv.title || '新对话' }}</strong>
          <small>{{ formatConversationTime(conv.updateTime || conv.createTime) }}</small>
        </span>
        <delete-outlined
          class="delete-btn"
          @click.stop="$emit('deleteConversation', conv.id)"
        />
      </button>
    </div>
  </aside>
</template>

<script setup>
import {
  DeleteOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MessageOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'

/** 会话侧栏属性 */
defineProps({
  conversations: {
    type: Array,
    default: () => []
  },
  currentId: {
    type: [Number, String],
    default: null
  },
  collapsed: {
    type: Boolean,
    default: false
  }
})

defineEmits([
  'createConversation',
  'deleteConversation',
  'selectConversation',
  'toggleCollapse'
])

/**
 * 格式化会话时间，减少侧栏信息噪音。
 */
const formatConversationTime = (time) => {
  if (!time) return '刚刚'
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  if (isToday) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}
</script>

<style scoped>
.chat-sidebar {
  width: 168px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 12px;
  color: #f7fbf6;
  background:
    linear-gradient(160deg, rgba(16, 35, 63, 0.98), rgba(10, 73, 82, 0.96)),
    radial-gradient(circle at 20% 10%, rgba(32, 184, 160, 0.3), transparent 32%);
  transition: width 0.26s ease, padding 0.26s ease;
}

.chat-sidebar--collapsed {
  width: 68px;
  padding: 14px 10px;
}

.sidebar-header {
  display: flex;
  align-items: stretch;
  gap: 8px;
}

.brand-card,
.collapse-btn,
.conversation-item {
  border: none;
  cursor: pointer;
  font: inherit;
}

.brand-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 10px;
  border-radius: 16px;
  color: #f7fbf6;
  text-align: left;
  background: rgba(255, 250, 240, 0.1);
  box-shadow: inset 0 0 0 1px rgba(255, 250, 240, 0.12);
  transition: transform 0.2s ease, background 0.2s ease;
}

.brand-card:hover {
  transform: translateY(-1px);
  background: rgba(32, 184, 160, 0.16);
}

.brand-mark {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  color: #10233f;
  background: #a7f3d0;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.brand-copy small {
  display: none;
}

.brand-copy strong,
.conv-main strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-copy small,
.conv-main small,
.sidebar-section em {
  color: rgba(247, 251, 246, 0.58);
  font-size: 12px;
  font-style: normal;
}

.collapse-btn {
  width: 34px;
  border-radius: 14px;
  color: rgba(247, 251, 246, 0.76);
  background: rgba(255, 250, 240, 0.08);
}

.collapse-btn:hover {
  color: #fffaf0;
  background: rgba(255, 250, 240, 0.14);
}

.sidebar-section {
  display: flex;
  justify-content: space-between;
  margin: 22px 4px 10px;
  color: rgba(247, 251, 246, 0.9);
  font-size: 13px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding-top: 12px;
}

.conversation-list::-webkit-scrollbar {
  width: 4px;
}

.conversation-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(255, 250, 240, 0.24);
}

.conversation-item {
  width: 100%;
  display: grid;
  grid-template-columns: 28px 1fr 18px;
  align-items: center;
  gap: 7px;
  margin-bottom: 8px;
  padding: 8px;
  border-radius: 14px;
  color: rgba(247, 251, 246, 0.78);
  text-align: left;
  background: transparent;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.conversation-item:hover,
.conversation-item.active {
  color: #fffaf0;
  background: rgba(255, 250, 240, 0.12);
}

.conversation-item.active {
  background: rgba(255, 250, 240, 0.18);
  box-shadow: inset 0 0 0 1px rgba(255, 250, 240, 0.2);
}

.conv-icon {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: #10233f;
  background: rgba(167, 243, 208, 0.86);
  font-size: 12px;
  font-weight: 700;
}

.conv-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.conv-main small {
  display: none;
}

.delete-btn {
  opacity: 0;
  color: rgba(247, 251, 246, 0.54);
  transition: opacity 0.2s ease, color 0.2s ease;
}

.conversation-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: #fecaca;
}

.empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-top: 42px;
  color: rgba(247, 251, 246, 0.56);
  font-size: 13px;
}

.chat-sidebar--collapsed .brand-copy,
.chat-sidebar--collapsed .sidebar-section,
.chat-sidebar--collapsed .conv-main,
.chat-sidebar--collapsed .delete-btn {
  display: none;
}

.chat-sidebar--collapsed .sidebar-header {
  flex-direction: column;
}

.chat-sidebar--collapsed .brand-card,
.chat-sidebar--collapsed .collapse-btn {
  width: 100%;
  justify-content: center;
  padding: 10px;
}

.chat-sidebar--collapsed .conversation-item {
  grid-template-columns: 1fr;
  justify-items: center;
  padding: 9px;
}

@media (max-width: 920px) {
  .chat-sidebar {
    width: 292px;
    height: 100%;
    border-radius: 0 22px 22px 0;
  }
}
</style>
