<template>
  <section ref="panelRef" class="message-panel" v-loading="loading">
    <div v-if="!messages.length && !loading" class="welcome">
      <div class="welcome-mark">
        <el-icon><MagicStick /></el-icon>
      </div>
      <h2>实验室智能助手</h2>
      <p>可以查询预约、安全考试和信用状态，也可以描述实验需求来查询可约实验室。</p>
      <div class="prompt-grid">
        <button
          v-for="prompt in prompts"
          :key="prompt"
          type="button"
          class="prompt-item"
          @click="$emit('prompt', prompt)"
        >
          {{ prompt }}
        </button>
      </div>
    </div>

    <div v-else class="message-list">
      <article
        v-for="(message, index) in messages"
        :key="message.id || `${message.role}-${index}`"
        class="message-row"
        :class="message.role === 'USER' ? 'user' : 'assistant'"
      >
        <div class="avatar">{{ message.role === "USER" ? "我" : "AI" }}</div>
        <div class="bubble-wrap">
          <div class="bubble" :class="{ 'loading-bubble': message.streaming && !message.content }">
            <template v-if="message.streaming && !message.content">
              <span />
              <span />
              <span />
            </template>
            <template v-else>{{ message.content }}</template>
          </div>
          <div v-if="message.role !== 'USER'" class="message-meta">
            <el-tag v-if="message.intent" size="small" effect="plain">
              {{ intentLabel(message.intent) }}
            </el-tag>
            <span v-if="message.model">{{ message.model }}</span>
          </div>
          <div v-if="message.toolCalls?.length" class="tool-list">
            <el-tag
              v-for="tool in message.toolCalls"
              :key="`${tool.toolName}-${tool.status}`"
              size="small"
              :type="tool.status === 'succeeded' ? 'success' : 'danger'"
              effect="plain"
              :title="tool.error || ''"
            >
              工具 · {{ toolLabel(tool.toolName) }}
              {{ tool.status === "succeeded" ? "完成" : "失败" }}
            </el-tag>
          </div>
        </div>
      </article>

    </div>
  </section>
</template>

<script setup name="AgentMessagePanel">
import { nextTick, ref, watch } from "vue"

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  loading: Boolean,
  sending: Boolean
})

defineEmits(["prompt"])

const panelRef = ref()
const prompts = [
  "查询我的预约",
  "我的信用分是多少",
  "我的安全考试通过了吗",
  "下周三下午想预约化学实验室，两个人"
]

watch(
  () =>
    [
      props.messages.length,
      props.sending,
      props.messages.map(message => `${message.content?.length || 0}:${message.toolCalls?.length || 0}`).join("|")
    ],
  async () => {
    await nextTick()
    if (panelRef.value) {
      panelRef.value.scrollTop = panelRef.value.scrollHeight
    }
  }
)

function intentLabel(intent) {
  return {
    BUSINESS_QUERY: "业务查询",
    RESERVATION: "预约咨询",
    SAFETY_KNOWLEDGE: "安全知识",
    GENERAL: "普通对话"
  }[intent] || intent
}

function toolLabel(toolName) {
  return {
    queryMyReservations: "本人预约",
    queryMyCredit: "信用查询",
    checkSafetyExamStatus: "安全考试",
    queryAvailableRooms: "可用实验室",
    queryAvailableDevices: "可用设备",
    createReservationDraft: "预约草稿"
  }[toolName] || toolName
}
</script>

<style lang="scss" scoped>
.message-panel {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 28px clamp(20px, 5vw, 72px);
  background:
    radial-gradient(circle at 12% 8%, rgba(64, 158, 255, 0.08), transparent 28%),
    #fff;
}

.welcome {
  max-width: 680px;
  margin: 8vh auto 0;
  text-align: center;
}

.welcome-mark {
  display: inline-flex;
  width: 58px;
  height: 58px;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: linear-gradient(145deg, #3b82f6, #60c8c2);
  color: #fff;
  font-size: 28px;
  box-shadow: 0 14px 32px rgba(59, 130, 246, 0.24);
}

.welcome h2 {
  margin: 18px 0 8px;
  color: #1e293b;
}

.welcome p {
  margin: 0 auto;
  max-width: 560px;
  color: #64748b;
  line-height: 1.7;
}

.prompt-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 28px;
}

.prompt-item {
  padding: 13px 15px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  color: #475569;
  cursor: pointer;
  text-align: left;
  transition: 0.2s ease;
}

.prompt-item:hover {
  border-color: #93c5fd;
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.1);
  color: #2563eb;
  transform: translateY(-1px);
}

.message-list {
  max-width: 900px;
  margin: 0 auto;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 20px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  display: flex;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.user .avatar {
  background: #0f766e;
  color: #fff;
}

.bubble-wrap {
  max-width: min(76%, 720px);
}

.bubble {
  padding: 12px 15px;
  border-radius: 4px 14px 14px;
  background: #f5f7fa;
  color: #334155;
  line-height: 1.72;
  white-space: pre-wrap;
}

.user .bubble {
  border-radius: 14px 4px 14px 14px;
  background: #e8f3ff;
  color: #1e3a5f;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 7px;
  color: #94a3b8;
  font-size: 11px;
}

.tool-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.loading-bubble {
  display: flex;
  gap: 5px;
}

.loading-bubble span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  animation: bounce 1.2s infinite ease-in-out;
}

.loading-bubble span:nth-child(2) {
  animation-delay: 0.15s;
}

.loading-bubble span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-4px); opacity: 1; }
}

@media (max-width: 760px) {
  .message-panel {
    padding: 20px 14px;
  }

  .prompt-grid {
    grid-template-columns: 1fr;
  }

  .bubble-wrap {
    max-width: 84%;
  }
}
</style>
