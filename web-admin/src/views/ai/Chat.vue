<template>
  <div class="chat-page">
    <div class="chat-atmosphere chat-atmosphere--one"></div>
    <div class="chat-atmosphere chat-atmosphere--two"></div>

    <div class="chat-workbench">
      <ChatSidebar
        class="desktop-sidebar"
        :conversations="conversations"
        :currentId="currentConversationId"
        :collapsed="sidebarCollapsed"
        @createConversation="createNewConversation"
        @deleteConversation="handleDeleteConversation"
        @selectConversation="selectConversation"
        @toggleCollapse="sidebarCollapsed = !sidebarCollapsed"
      />

      <div v-if="mobileSidebarVisible" class="mobile-sidebar-mask" @click="mobileSidebarVisible = false">
        <ChatSidebar
          class="mobile-sidebar-panel"
          :conversations="conversations"
          :currentId="currentConversationId"
          :collapsed="false"
          @click.stop
          @createConversation="createNewConversation"
          @deleteConversation="handleDeleteConversation"
          @selectConversation="selectConversation"
          @toggleCollapse="mobileSidebarVisible = false"
        />
      </div>

      <main class="chat-main">
        <header class="chat-titlebar">
          <a-button class="mobile-menu-btn" type="text" @click="mobileSidebarVisible = true">
            <menu-unfold-outlined />
          </a-button>
          <span class="chat-title">{{ currentConversationTitle }}</span>
        </header>

        <section ref="messageListRef" class="chat-content" :class="{ empty: messages.length === 0 }">
          <WelcomeArea
            v-if="messages.length === 0"
            :model-label="currentModelMeta.label"
            @quickMessage="sendQuickMessage"
          />
          <template v-else>
            <MessageItem
              v-for="msg in messages"
              :key="msg.id"
              :message="msg"
            />
          </template>
        </section>

        <div v-if="lastFailedMessage" class="retry-panel">
          <div>
            <strong>上一条消息发送失败</strong>
            <span>问题已保留，可调整模型后重试。</span>
          </div>
          <a-button size="small" type="primary" @click="retryFailedMessage">重试</a-button>
        </div>

        <ChatInput
          v-model:inputValue="inputMessage"
          v-model:modelValue="selectedModel"
          v-model:scopeValue="selectedScope"
          :disabled="loading"
          :models="availableModels"
          :scopes="availableScopes"
          :max-length="maxMessageLength"
          @send="handleSend"
          @stop="closeSSE"
        />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { MenuUnfoldOutlined } from '@ant-design/icons-vue'
import ChatSidebar from '@/components/ai/ChatSidebar.vue'
import WelcomeArea from '@/components/ai/WelcomeArea.vue'
import MessageItem from '@/components/ai/MessageItem.vue'
import ChatInput from '@/components/ai/ChatInput.vue'
import {
  createConversation as apiCreateConversation,
  deleteConversation as apiDeleteConversation,
  getConversations,
  getMessages
} from '@/api/ai'
import {
  ErrorCodes,
  createAssistantMessage,
  createErrorMessage,
  createUserMessage,
  handleApiError,
  isNetworkError
} from '@/utils/chatUtils'

const maxMessageLength = 2000
const conversations = ref([])
const currentConversationId = ref(null)
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const sidebarCollapsed = ref(false)
const mobileSidebarVisible = ref(false)
const messageListRef = ref(null)
const currentSSE = ref(null)
const lastFailedMessage = ref('')
const scrollFrame = ref(null)

const selectedModel = ref('deepseek-v4-flash')
const selectedScope = ref('local_knowledge')
const availableModels = ref([
  { value: 'deepseek-v4-flash', label: 'DeepSeek V4 Flash', desc: '默认模型，适合日常办公问答' },
  { value: 'qwen3.5-plus', label: '通义千问', desc: '通用问答与中文表达' },
  { value: 'MiniMax-M2.7-highspeed', label: 'MiniMax', desc: '高速响应备用模型' },
  { value: 'qwen3.6:latest', label: 'qwen3.6（ollama）', desc: '本地 Ollama 模型，适合本机离线问答' }
])
const availableScopes = ref([
  { value: 'internet', label: '互联网', desc: '按原有模型能力回答' },
  { value: 'local_knowledge', label: '本地知识库', desc: '依据已发布知识库回答' }
])

const currentModelMeta = computed(() => (
  availableModels.value.find(model => model.value === selectedModel.value) || availableModels.value[0]
))

const getModelMeta = (modelValue) => (
  availableModels.value.find(model => model.value === modelValue) || availableModels.value[0]
)

const currentConversation = computed(() => (
  conversations.value.find(conv => conv.id === currentConversationId.value)
))

const currentConversationTitle = computed(() => currentConversation.value?.title || '智能问答')

/**
 * 加载会话列表，失败时交给统一错误处理。
 */
const loadConversations = async () => {
  try {
    const response = await getConversations()
    conversations.value = response?.data || response || []
  } catch (error) {
    handleApiError(error, '加载会话列表失败')
  }
}

/**
 * 创建新会话并立即选中，移动端同步关闭抽屉。
 */
const createNewConversation = async () => {
  try {
    const response = await apiCreateConversation('新对话')
    const newConversation = response?.data || response
    if (newConversation) {
      conversations.value.unshift(newConversation)
      await selectConversation(newConversation)
      mobileSidebarVisible.value = false
    }
  } catch (error) {
    handleApiError(error, '创建会话失败')
  }
}

/**
 * 切换会话并拉取历史消息。
 */
const selectConversation = async (conversation) => {
  if (!conversation?.id) return
  currentConversationId.value = conversation.id
  mobileSidebarVisible.value = false

  try {
    const response = await getMessages(conversation.id)
    messages.value = response?.data || response || []
    lastFailedMessage.value = ''
    scrollToBottom()
  } catch (error) {
    const { errorCode } = handleApiError(error, '加载消息失败')
    if (errorCode === ErrorCodes.CONVERSATION_NOT_FOUND) {
      await loadConversations()
      currentConversationId.value = null
      messages.value = []
    }
  }
}

/**
 * 删除会话前弹出确认，避免误删历史上下文。
 */
const handleDeleteConversation = (id) => {
  Modal.confirm({
    title: '删除这段对话？',
    content: '删除后会同步移除该会话下的所有消息，无法恢复。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await apiDeleteConversation(id)
        conversations.value = conversations.value.filter(item => item.id !== id)
        if (currentConversationId.value === id) {
          currentConversationId.value = null
          messages.value = []
          lastFailedMessage.value = ''
        }
        message.success('会话已删除')
      } catch (error) {
        handleApiError(error, '删除失败')
      }
    }
  })
}

/**
 * 处理输入组件发出的发送事件。
 */
const handleSend = ({ message: msg, model, scope }) => {
  sendMessage(msg, model, scope)
}

/**
 * 发送快捷问题。
 */
const sendQuickMessage = async (msg) => {
  inputMessage.value = msg
  await sendMessage(msg, selectedModel.value, selectedScope.value)
}

/**
 * 重试失败消息，用户问题保持不丢失。
 */
const retryFailedMessage = async () => {
  const retryMessage = lastFailedMessage.value
  lastFailedMessage.value = ''
  await sendMessage(retryMessage, selectedModel.value, selectedScope.value)
}

/**
 * 发送用户消息并创建AI流式占位。
 */
const sendMessage = async (userMessage, model, scope = selectedScope.value) => {
  const trimmedMessage = userMessage?.trim()
  if (!trimmedMessage || loading.value) return

  if (trimmedMessage.length > maxMessageLength) {
    message.warning(`输入内容过长，请精简后重试（最多${maxMessageLength}字符）`)
    return
  }

  inputMessage.value = ''
  loading.value = true
  lastFailedMessage.value = ''

  try {
    await ensureConversationReady()
    const tempUserMessage = createUserMessage(trimmedMessage)
    messages.value.push(tempUserMessage)
    scrollToBottom()
    await sendMessageWithSSE(trimmedMessage, model, scope)
  } catch (error) {
    loading.value = false
    handleApiError(error, '发送消息失败')
  }
}

/**
 * 确保发送前已有当前会话。
 */
const ensureConversationReady = async () => {
  if (currentConversationId.value) return
  if (conversations.value.length > 0) {
    await selectConversation(conversations.value[0])
    return
  }
  await createNewConversation()
}

/**
 * 调用后端SSE接口并持续更新AI消息内容。
 */
const sendMessageWithSSE = async (userMessage, model, scope) => {
  closeSSE()
  const controller = new AbortController()
  currentSSE.value = controller
  const startedAt = performance.now()
  const modelMeta = getModelMeta(model)
  const assistantMessage = createAssistantMessage(true, {
    model: modelMeta.value,
    modelName: modelMeta.label
  })
  messages.value.push(assistantMessage)

  try {
    const response = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        conversationId: currentConversationId.value,
        message: userMessage,
        model,
        scope
      }),
      signal: controller.signal
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    await readSSEStream(response, assistantMessage.id, startedAt)
    await loadConversations()
  } catch (error) {
    if (error.name === 'AbortError') {
      finishStreamingMessage(assistantMessage.id, startedAt)
    } else {
      handleSSEError(error, userMessage, assistantMessage.id, startedAt)
    }
  } finally {
    closeSSE()
    loading.value = false
  }
}

/**
 * 逐行解析SSE事件，保持与后端start/content/done/error合同一致。
 */
const readSSEStream = async (response, assistantMessageId, startedAt) => {
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let currentEvent = ''
  let currentData = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      buffer += decoder.decode()
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      if (line.startsWith('event:')) {
        currentEvent = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        currentData = line.slice(5).trim()
      } else if (line.trim() === '') {
        const shouldStop = handleSSEEvent(currentEvent, currentData, assistantMessageId, startedAt)
        currentEvent = ''
        currentData = ''
        if (shouldStop) {
          return
        }
      }
    }
  }

  if (buffer.trim()) {
    if (buffer.startsWith('event:')) {
      currentEvent = buffer.slice(6).trim()
    } else if (buffer.startsWith('data:')) {
      currentData = buffer.slice(5).trim()
    }
  }

  if ((currentEvent || currentData) && handleSSEEvent(currentEvent, currentData, assistantMessageId, startedAt)) {
    return
  }

  finishStreamingMessage(assistantMessageId, startedAt)
}

/**
 * 根据SSE事件类型更新消息或抛出错误。
 */
const handleSSEEvent = (eventName, eventData, assistantMessageId, startedAt) => {
  if (!eventData) return false

  const jsonData = JSON.parse(eventData)
  if (eventName === 'content' && jsonData.content) {
    updateStreamingMessage(assistantMessageId, jsonData.content)
  } else if (eventName === 'done') {
    finishStreamingMessage(assistantMessageId, startedAt)
    return true
  } else if (eventName === 'error' || jsonData.errorCode) {
    finishStreamingMessage(assistantMessageId, startedAt)
    throw new Error(jsonData.message || 'AI服务错误')
  }
  return false
}

/**
 * 替换数组项确保Vue响应式可靠触发。
 */
const updateStreamingMessage = (messageId, content) => {
  messages.value = messages.value.map(item => (
    item.id === messageId ? { ...item, content } : item
  ))
  scrollToBottom()
}

/**
 * 标记流式消息完成。
 */
const finishStreamingMessage = (messageId, startedAt) => {
  const elapsedMs = getElapsedMs(startedAt)
  messages.value = messages.value.map(item => (
    item.id === messageId ? { ...item, isStreaming: false, elapsedMs } : item
  ))
  scrollToBottom()
}

/**
 * 关闭当前SSE读取，切换会话或离开页面时释放连接。
 */
const closeSSE = () => {
  currentSSE.value?.abort()
  currentSSE.value = null
}

/**
 * 处理流式接口错误，保留用户问题并展示重试入口。
 */
const handleSSEError = (error, userMessage, assistantMessageId, startedAt) => {
  const networkHint = isNetworkError(error) ? '网络不稳定，请稍后重试。' : error.message
  lastFailedMessage.value = userMessage
  const elapsedMs = getElapsedMs(startedAt)
  messages.value = messages.value.map(item => (
    item.id === assistantMessageId
      ? {
          ...createErrorMessage(networkHint || 'AI服务暂时不可用，请稍后重试。', {
            model: item.model,
            modelName: item.modelName
          }),
          id: assistantMessageId,
          elapsedMs
        }
      : item
  ))
  message.error(networkHint || '发送消息失败')
  scrollToBottom()
}

/**
 * 计算从发起请求到当前时刻的耗时，供AI消息元信息展示。
 */
const getElapsedMs = (startedAt) => {
  if (!Number.isFinite(startedAt)) {
    return 0
  }
  return Math.max(0, Math.round(performance.now() - startedAt))
}

/**
 * 滚动到消息底部。
 */
const scrollToBottom = () => {
  nextTick(() => {
    if (scrollFrame.value) {
      cancelAnimationFrame(scrollFrame.value)
    }
    scrollFrame.value = requestAnimationFrame(() => {
      if (messageListRef.value) {
        messageListRef.value.scrollTo({
          top: messageListRef.value.scrollHeight,
          behavior: 'auto'
        })
      }
      scrollFrame.value = null
    })
  })
}

onMounted(async () => {
  await loadConversations()
  if (conversations.value.length > 0) {
    await selectConversation(conversations.value[0])
  }
})

onBeforeUnmount(() => {
  if (scrollFrame.value) {
    cancelAnimationFrame(scrollFrame.value)
  }
  closeSSE()
})
</script>

<style scoped>
.chat-page {
  --chat-ink: #10233f;
  --chat-ink-soft: #284360;
  --chat-teal: #20b8a0;
  --chat-teal-deep: #087d78;
  --chat-paper: #fffaf0;
  --chat-paper-strong: #fff4d8;
  --chat-line: rgba(16, 35, 63, 0.12);
  --chat-shadow: 0 24px 70px rgba(16, 35, 63, 0.18);
  position: relative;
  min-height: calc(100vh - 140px);
  padding: 18px;
  overflow: hidden;
  background:
    radial-gradient(circle at 12% 18%, rgba(32, 184, 160, 0.24), transparent 28%),
    radial-gradient(circle at 88% 8%, rgba(255, 189, 89, 0.24), transparent 26%),
    linear-gradient(135deg, #e8f4ef 0%, #f8edd8 48%, #eaf2f5 100%);
  border-radius: 22px;
}

.chat-atmosphere {
  position: absolute;
  pointer-events: none;
  border-radius: 999px;
}

.chat-atmosphere--one {
  width: 420px;
  height: 420px;
  right: -180px;
  bottom: -210px;
  background: rgba(32, 184, 160, 0.18);
}

.chat-atmosphere--two {
  width: 260px;
  height: 260px;
  left: 38%;
  top: -170px;
  background: rgba(16, 35, 63, 0.14);
}

.chat-workbench {
  position: relative;
  z-index: 1;
  display: flex;
  height: calc(100vh - 176px);
  min-height: 620px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 26px;
  background: rgba(255, 250, 240, 0.68);
  box-shadow: var(--chat-shadow);
}

.chat-main {
  position: relative;
  flex: 1;
  display: flex;
  min-height: 0;
  min-width: 0;
  flex-direction: column;
  background: linear-gradient(180deg, rgba(255, 250, 240, 0.92), rgba(250, 252, 247, 0.86));
}

.chat-titlebar {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 7px 18px;
  border-bottom: 1px solid rgba(16, 35, 63, 0.08);
  color: var(--chat-ink);
  background: rgba(255, 250, 240, 0.72);
}

.chat-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 700;
}

.mobile-menu-btn {
  display: none;
  color: var(--chat-ink);
}

.chat-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 28px;
}

.chat-content.empty {
  display: flex;
}

.chat-content::-webkit-scrollbar {
  width: 8px;
}

.chat-content::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(16, 35, 63, 0.18);
}

.retry-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin: 0 28px 14px;
  padding: 12px 14px;
  border: 1px solid rgba(220, 38, 38, 0.18);
  border-radius: 14px;
  color: #7f1d1d;
  background: #fff1f0;
}

.retry-panel div {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 13px;
}

.mobile-sidebar-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: none;
  background: rgba(16, 35, 63, 0.44);
}

.mobile-sidebar-panel {
  max-width: 86vw;
  height: 100%;
}

@media (max-width: 920px) {
  .chat-page {
    min-height: calc(100vh - 100px);
    padding: 10px;
    border-radius: 16px;
  }

  .chat-workbench {
    height: calc(100vh - 120px);
    min-height: 560px;
    border-radius: 18px;
  }

  .desktop-sidebar {
    display: none;
  }

  .mobile-sidebar-mask {
    display: block;
  }

  .mobile-menu-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0;
  }

  .chat-content {
    padding: 18px;
  }

  .retry-panel {
    align-items: flex-start;
    flex-direction: column;
    margin: 0 18px 12px;
  }
}
</style>
