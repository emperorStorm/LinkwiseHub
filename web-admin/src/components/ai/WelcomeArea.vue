<template>
  <div class="welcome-area">
    <div class="hero-card">
      <div class="hero-kicker">{{ modelLabel }} 已就绪</div>
      <h2>把零散问题整理成可执行的办公答案</h2>
      <p>可以帮你总结制度文档、生成会议纪要、解释系统功能，也能继续处理菜谱类日常问题。</p>
    </div>

    <div class="quick-grid">
      <button
        v-for="action in quickActions"
        :key="action.title"
        class="quick-card"
        type="button"
        @click="$emit('quickMessage', action.message)"
      >
        <span class="quick-icon"><component :is="action.icon" /></span>
        <strong>{{ action.title }}</strong>
        <small>{{ action.desc }}</small>
      </button>
    </div>
  </div>
</template>

<script setup>
import {
  BookOutlined,
  FileTextOutlined,
  QuestionCircleOutlined,
  ScheduleOutlined
} from '@ant-design/icons-vue'

defineProps({
  /** 当前模型名称，用于给用户明确反馈 */
  modelLabel: {
    type: String,
    default: 'DeepSeek V4 Flash'
  }
})

defineEmits(['quickMessage'])

const quickActions = [
  {
    icon: FileTextOutlined,
    title: '总结制度文档',
    desc: '提炼重点和执行事项',
    message: '请帮我总结一份制度文档，输出重点、风险点和待办事项。'
  },
  {
    icon: ScheduleOutlined,
    title: '生成会议纪要',
    desc: '结构化会议结论',
    message: '请根据会议内容生成会议纪要模板，包含议题、结论、负责人和截止时间。'
  },
  {
    icon: QuestionCircleOutlined,
    title: '查询系统功能',
    desc: '快速了解OA模块',
    message: '请介绍一下OA系统里智能问答和文档分片模块可以做什么。'
  },
  {
    icon: BookOutlined,
    title: '推荐菜谱',
    desc: '保留日常助手能力',
    message: '帮我推荐一道适合工作日晚餐的家常菜，并列出步骤。'
  }
]
</script>

<style scoped>
.welcome-area {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(260px, 0.88fr) minmax(300px, 1.12fr);
  align-items: center;
  gap: 28px;
}

.hero-card {
  position: relative;
  overflow: hidden;
  padding: 34px;
  border: 1px solid rgba(16, 35, 63, 0.12);
  border-radius: 28px;
  color: #10233f;
  background:
    linear-gradient(145deg, rgba(255, 250, 240, 0.96), rgba(255, 244, 216, 0.84)),
    radial-gradient(circle at 86% 14%, rgba(32, 184, 160, 0.28), transparent 32%);
  box-shadow: 0 20px 48px rgba(16, 35, 63, 0.12);
}

.hero-card::after {
  content: '';
  position: absolute;
  right: -64px;
  bottom: -64px;
  width: 190px;
  height: 190px;
  border: 1px solid rgba(32, 184, 160, 0.28);
  border-radius: 50%;
}

.hero-kicker {
  display: inline-flex;
  margin-bottom: 18px;
  padding: 6px 12px;
  border-radius: 999px;
  color: #087d78;
  background: rgba(32, 184, 160, 0.12);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero-card h2 {
  margin: 0 0 14px;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: clamp(28px, 4vw, 46px);
  line-height: 1.08;
}

.hero-card p {
  position: relative;
  z-index: 1;
  margin: 0;
  color: rgba(16, 35, 63, 0.68);
  font-size: 15px;
  line-height: 1.9;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.quick-card {
  min-height: 154px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  padding: 20px;
  border: 1px solid rgba(16, 35, 63, 0.1);
  border-radius: 22px;
  color: #10233f;
  text-align: left;
  cursor: pointer;
  background: rgba(255, 250, 240, 0.78);
  box-shadow: 0 14px 34px rgba(16, 35, 63, 0.08);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.quick-card:hover {
  transform: translateY(-4px);
  border-color: rgba(32, 184, 160, 0.36);
  box-shadow: 0 22px 42px rgba(16, 35, 63, 0.14);
}

.quick-icon {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  color: #087d78;
  background: rgba(32, 184, 160, 0.14);
  font-size: 20px;
}

.quick-card strong {
  font-size: 16px;
}

.quick-card small {
  color: rgba(16, 35, 63, 0.56);
  line-height: 1.6;
}

@media (max-width: 1040px) {
  .welcome-area {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .hero-card,
  .quick-card {
    border-radius: 20px;
  }
}
</style>
