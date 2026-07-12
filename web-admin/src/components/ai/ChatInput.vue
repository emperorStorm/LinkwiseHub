<template>
  <footer class="composer">
    <div class="composer-toolbar">
      <div class="model-picker">
        <span class="toolbar-label">模型</span>
        <a-select
          v-model:value="selectedModelValue"
          class="model-select"
          :dropdownMatchSelectWidth="false"
          :disabled="disabled"
        >
          <a-select-option
            v-for="model in models"
            :key="model.value"
            :value="model.value"
          >
            <div class="model-option">
              <strong>{{ model.label }}</strong>
              <span>{{ model.desc }}</span>
            </div>
          </a-select-option>
        </a-select>
      </div>
      <div class="scope-picker">
        <span class="toolbar-label">范围</span>
        <a-select
          v-model:value="selectedScopeValue"
          class="scope-select"
          :dropdownMatchSelectWidth="false"
          :disabled="disabled"
        >
          <a-select-option
            v-for="scope in scopes"
            :key="scope.value"
            :value="scope.value"
          >
            <div class="model-option">
              <strong>{{ scope.label }}</strong>
              <span>{{ scope.desc }}</span>
            </div>
          </a-select-option>
        </a-select>
      </div>
    </div>

    <div class="composer-box" :class="{ disabled }">
      <a-textarea
        v-model:value="localTextValue"
        :maxlength="maxLength"
        :auto-size="{ minRows: 2, maxRows: 6 }"
        placeholder="写下你的问题，例如：帮我整理今天会议纪要的重点..."
        :disabled="disabled"
        @keydown="handleKeydown"
      />
      <div class="composer-actions">
        <span class="count" :class="{ warning: localTextValue.length > maxLength * 0.9 }">
          {{ localTextValue.length }}/{{ maxLength }}
        </span>
        <a-button v-if="disabled" class="stop-btn" @click="$emit('stop')">停止</a-button>
        <a-button
          v-else
          type="primary"
          class="send-btn"
          :disabled="!localTextValue.trim()"
          @click="handleSend"
        >
          <send-outlined />
          发送
        </a-button>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { SendOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  /** 选中的模型值 */
  modelValue: {
    type: String,
    default: 'deepseek-v4-flash'
  },
  /** 选中的问答范围 */
  scopeValue: {
    type: String,
    default: 'local_knowledge'
  },
  /** 输入框内容 */
  inputValue: {
    type: String,
    default: ''
  },
  /** 是否禁用输入 */
  disabled: {
    type: Boolean,
    default: false
  },
  /** 可选模型列表 */
  models: {
    type: Array,
    default: () => [
      { value: 'deepseek-v4-flash', label: 'DeepSeek V4 Flash', desc: '默认模型' },
      { value: 'qwen3.5-plus', label: '通义千问', desc: '通用问答' },
      { value: 'MiniMax-M2.7-highspeed', label: 'MiniMax', desc: '高速备用' },
      { value: 'qwen3.6:latest', label: 'qwen3.6（ollama）', desc: '本地 Ollama 模型' }
    ]
  },
  /** 可选问答范围 */
  scopes: {
    type: Array,
    default: () => [
      { value: 'internet', label: '互联网', desc: '按原有模型能力回答' },
      { value: 'local_knowledge', label: '本地知识库', desc: '依据已发布知识库回答' }
    ]
  },
  /** 输入最大长度 */
  maxLength: {
    type: Number,
    default: 2000
  }
})

const emit = defineEmits(['update:modelValue', 'update:scopeValue', 'update:inputValue', 'send', 'stop'])

/**
 * 模型选择受控代理，保证下拉框与父组件状态始终一致。
 */
const selectedModelValue = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const selectedScopeValue = computed({
  get: () => props.scopeValue,
  set: value => emit('update:scopeValue', value)
})

const localTextValue = ref(props.inputValue || '')

/**
 * 父组件清空输入时同步到本地值，例如发送成功后置空。
 */
watch(() => props.inputValue, (value) => {
  const nextValue = value || ''
  if (nextValue !== localTextValue.value) {
    localTextValue.value = nextValue
  }
})

/**
 * 本地输入变化同步给父组件，避免频繁创建额外对象。
 */
watch(localTextValue, (value) => {
  if (value !== props.inputValue) {
    emit('update:inputValue', value || '')
  }
})

/**
 * 处理快捷键：Enter 发送，Shift + Enter 保留换行。
 */
const handleKeydown = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

/**
 * 触发发送事件并交由父组件处理会话状态。
 */
const handleSend = () => {
  const message = localTextValue.value?.trim()
  if (!message || props.disabled) return
  emit('send', {
    message,
    model: selectedModelValue.value,
    scope: selectedScopeValue.value
  })
  // 回车发送与按钮发送都先清空本地值，避免键盘事件后textarea内部状态回填旧文本。
  localTextValue.value = ''
  emit('update:inputValue', '')
}
</script>

<style scoped>
.composer {
  padding: 0 28px 24px;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 10px;
}

.model-picker,
.scope-picker {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-label,
.count {
  color: rgba(16, 35, 63, 0.58);
  font-size: 12px;
}

.model-select {
  min-width: 210px;
}

.scope-select {
  min-width: 150px;
}

.model-select :deep(.ant-select-selector),
.scope-select :deep(.ant-select-selector) {
  min-height: 38px !important;
  border-color: rgba(16, 35, 63, 0.14) !important;
  border-radius: 999px !important;
  background: rgba(255, 250, 240, 0.78) !important;
  box-shadow: none !important;
}

.model-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.model-option strong {
  color: #10233f;
  font-size: 13px;
}

.model-option span {
  color: #6b7f8f;
  font-size: 12px;
}

.composer-box {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 14px;
  padding: 14px;
  border: 1px solid rgba(16, 35, 63, 0.14);
  border-radius: 22px;
  background: rgba(255, 250, 240, 0.9);
  box-shadow: 0 16px 42px rgba(16, 35, 63, 0.1);
}

.composer-box.disabled {
  opacity: 0.78;
}

.composer-box :deep(textarea.ant-input) {
  padding: 4px 2px;
  border: none;
  color: #10233f;
  background: transparent;
  box-shadow: none;
  line-height: 1.7;
  resize: none;
}

.composer-box :deep(textarea.ant-input::placeholder) {
  color: rgba(16, 35, 63, 0.38);
}

.composer-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.count.warning {
  color: #d97706;
}

.send-btn,
.stop-btn {
  height: 42px;
  padding: 0 18px;
  border-radius: 999px;
}

.send-btn {
  border: none;
  background: linear-gradient(135deg, #10233f 0%, #087d78 100%);
  box-shadow: 0 10px 24px rgba(8, 125, 120, 0.24);
}

.send-btn:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(8, 125, 120, 0.3);
}

.stop-btn {
  color: #7f1d1d;
  border-color: rgba(220, 38, 38, 0.24);
  background: #fff1f0;
}

@media (max-width: 720px) {
  .composer {
    padding: 0 18px 18px;
  }

  .composer-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .model-picker,
  .scope-picker,
  .model-select,
  .scope-select {
    width: 100%;
  }

  .composer-box {
    grid-template-columns: 1fr;
  }

  .composer-actions {
    justify-content: space-between;
  }
}
</style>
