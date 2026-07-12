<template>
  <article
    class="message-item"
    :class="{
      'message-user': message.role === 'user',
      'message-assistant': message.role === 'assistant',
      'message-error': message.isError
    }"
  >
    <div class="message-card">
      <div class="message-meta">
        <strong>
          {{ senderName }}
          <span v-if="durationText" class="model-duration">耗时 {{ durationText }}</span>
        </strong>
        <span>{{ formattedTime }}</span>
      </div>
      <div
        v-if="message.role === 'assistant'"
        class="message-text markdown-body"
        :class="{ streaming: message.isStreaming }"
        v-html="displayContent"
      ></div>
      <div v-else class="message-text plain-text">{{ message.content }}</div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { formatTime, renderMarkdown } from '@/utils/chatUtils'

const props = defineProps({
  /** 消息对象 */
  message: {
    type: Object,
    required: true
  },
  /** 当前模型名称 */
  modelLabel: {
    type: String,
    default: 'AI助手'
  }
})

const isUser = computed(() => props.message.role === 'user')
const senderName = computed(() => (
  isUser.value ? '我' : props.message.modelName || props.modelLabel
))
const durationText = computed(() => {
  if (isUser.value || !Number.isFinite(props.message.elapsedMs)) {
    return ''
  }
  const seconds = Math.max(0, props.message.elapsedMs) / 1000
  if (seconds < 10) {
    return `${seconds.toFixed(1)}秒`
  }
  return `${Math.round(seconds)}秒`
})

/**
 * AI回复按Markdown渲染；用户消息使用文本插值避免注入风险。
 */
const displayContent = computed(() => renderMarkdown(props.message.content))

const formattedTime = computed(() => formatTime(props.message.createTime))
</script>

<style scoped>
.message-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
}

.message-user {
  flex-direction: row-reverse;
}

.message-card {
  max-width: min(760px, 78%);
  padding: 14px 16px;
  border: 1px solid rgba(16, 35, 63, 0.1);
  border-radius: 20px;
  color: #10233f;
  background: rgba(255, 250, 240, 0.84);
  box-shadow: 0 14px 34px rgba(16, 35, 63, 0.08);
}

.message-user .message-card {
  color: #fffaf0;
  border-color: rgba(16, 35, 63, 0.22);
  background: linear-gradient(145deg, #10233f, #1c4d55);
}

.message-error .message-card {
  color: #7f1d1d;
  border-color: rgba(220, 38, 38, 0.22);
  background: #fff1f0;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 8px;
  color: rgba(16, 35, 63, 0.58);
  font-size: 12px;
}

.message-user .message-meta {
  color: rgba(255, 250, 240, 0.68);
}

.message-meta strong {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: inherit;
  font-weight: 700;
}

.model-duration {
  padding: 1px 7px;
  border-radius: 999px;
  color: #087d78;
  background: rgba(32, 184, 160, 0.12);
  font-size: 11px;
  font-weight: 700;
}

.message-error .model-duration {
  color: #b42318;
  background: rgba(220, 38, 38, 0.1);
}

.message-text {
  font-size: 14px;
  line-height: 1.85;
}

.plain-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.streaming::after {
  content: '▋';
  display: inline-block;
  margin-left: 3px;
  color: #20b8a0;
  animation: cursorBlink 0.9s infinite;
}

.markdown-body :deep(p) {
  margin: 0 0 0.82em;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 0.9em 0 0.42em;
  color: #10233f;
  line-height: 1.35;
}

.markdown-body :deep(code) {
  padding: 0.12em 0.38em;
  border-radius: 6px;
  color: #0f766e;
  background: rgba(32, 184, 160, 0.12);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 0.9em;
}

.markdown-body :deep(pre) {
  margin: 0.6em 0;
  padding: 14px;
  overflow-x: auto;
  border-radius: 14px;
  background: #10233f;
}

.markdown-body :deep(pre code) {
  padding: 0;
  color: #e8fff7;
  background: transparent;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.4em;
}

.markdown-body :deep(blockquote) {
  margin: 0.7em 0;
  padding: 10px 12px;
  border-left: 3px solid #20b8a0;
  border-radius: 0 12px 12px 0;
  background: rgba(32, 184, 160, 0.08);
}

.markdown-body :deep(table) {
  width: 100%;
  margin: 0.6em 0;
  border-collapse: collapse;
  font-size: 0.92em;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 8px 10px;
  border: 1px solid rgba(16, 35, 63, 0.12);
}

.markdown-body :deep(a) {
  color: #087d78;
}

@keyframes cursorBlink {
  0%, 45% { opacity: 1; }
  46%, 100% { opacity: 0; }
}

@media (max-width: 720px) {
  .message-card {
    max-width: calc(100vw - 72px);
  }
}
</style>
