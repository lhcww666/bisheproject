<template>
  <div class="agent-page">
    <div class="agent-shell">
      <session-panel
        :sessions="sessions"
        :active-id="currentSessionId"
        :loading="sessionsLoading"
        :creating="creatingSession"
        @create="handleCreateSession"
        @select="handleSelectSession"
        @delete="handleDeleteSession"
      />

      <main class="chat-area">
        <header class="chat-head">
          <div>
            <div class="chat-title">
              {{ currentSession?.title || "智能助手" }}
            </div>
            <div class="chat-subtitle">
              {{ status.role ? roleLabel(status.role) : "实验室业务助手" }}
            </div>
          </div>
          <div class="service-state">
            <el-tag :type="serviceTagType" effect="light">
              {{ serviceLabel }}
            </el-tag>
            <el-button circle icon="Refresh" :loading="statusLoading" @click="loadStatus" />
          </div>
        </header>

        <el-alert
          v-if="serviceMessage"
          class="service-alert"
          type="warning"
          :closable="false"
          show-icon
          :title="serviceMessage"
        />

        <message-panel
          :messages="messages"
          :loading="messagesLoading"
          :sending="sending"
          @prompt="handlePrompt"
        />

        <footer class="composer">
          <el-input
            v-model="input"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="8000"
            show-word-limit
            :disabled="!currentSessionId || sending"
            placeholder="描述你的问题，Enter 发送，Shift + Enter 换行"
            @keydown.enter.exact.prevent="handleSend"
          />
          <div class="composer-actions">
            <span>回答中的业务数据来自受控工具，请以业务页面最终结果为准。</span>
            <el-button
              type="primary"
              icon="Promotion"
              :loading="sending"
              :disabled="!currentSessionId || !input.trim()"
              @click="handleSend"
            >
              发送
            </el-button>
          </div>
        </footer>
      </main>
    </div>
  </div>
</template>

<script setup name="AgentAssistant">
import {
  createAgentSession,
  deleteAgentSession,
  getAgentMessages,
  getAgentStatus,
  listAgentSessions,
  sendAgentChatStream
} from "@/api/module/agent"
import SessionPanel from "./components/SessionPanel.vue"
import MessagePanel from "./components/MessagePanel.vue"

const { proxy } = getCurrentInstance()
const sessions = ref([])
const messages = ref([])
const currentSessionId = ref(null)
const input = ref("")
const status = ref({})
const sessionsLoading = ref(false)
const messagesLoading = ref(false)
const creatingSession = ref(false)
const sending = ref(false)
const statusLoading = ref(false)

const currentSession = computed(() =>
  sessions.value.find(session => session.id === currentSessionId.value)
)

const serviceLabel = computed(() => {
  if (!status.value.enabled) return "助手已停用"
  if (status.value.pythonAvailable) return "Python 服务在线"
  return "Python 服务未连接"
})

const serviceTagType = computed(() => {
  if (!status.value.enabled) return "info"
  return status.value.pythonAvailable ? "success" : "warning"
})

const serviceMessage = computed(() => {
  if (!status.value.enabled) return "智能助手当前未启用。"
  if (status.value.pythonAvailable) return ""
  return "Python Agent 服务未启动，或模型 Key 尚未配置。会话管理仍可使用。"
})

onMounted(async () => {
  await Promise.all([loadStatus(), loadSessions()])
  const first = sessions.value[0]
  if (first) {
    await handleSelectSession(first.id)
  }
})

async function loadStatus() {
  statusLoading.value = true
  try {
    const response = await getAgentStatus()
    status.value = response.data || {}
  } catch {
    status.value = { enabled: false }
  } finally {
    statusLoading.value = false
  }
}

async function loadSessions() {
  sessionsLoading.value = true
  try {
    const response = await listAgentSessions()
    sessions.value = response.data || []
  } catch {
    sessions.value = []
  } finally {
    sessionsLoading.value = false
  }
}

async function handleCreateSession() {
  creatingSession.value = true
  try {
    const response = await createAgentSession({ engine: "python" })
    await loadSessions()
    currentSessionId.value = response.data?.sessionId
    messages.value = []
    const created = sessions.value.find(item => item.id === currentSessionId.value)
    if (created) {
      created.title = "新会话"
    }
  } catch {
    currentSessionId.value = null
  } finally {
    creatingSession.value = false
  }
}

async function handleSelectSession(sessionId) {
  if (currentSessionId.value === sessionId) return
  currentSessionId.value = sessionId
  messagesLoading.value = true
  try {
    const response = await getAgentMessages(sessionId)
    messages.value = response.data || []
  } catch {
    messages.value = []
  } finally {
    messagesLoading.value = false
  }
}

function handleDeleteSession(session) {
  proxy.$modal.confirm(`是否删除会话“${session.title || "未命名会话"}”？`).then(async () => {
    await deleteAgentSession(session.id)
    if (currentSessionId.value === session.id) {
      currentSessionId.value = null
      messages.value = []
    }
    await loadSessions()
    proxy.$modal.msgSuccess("会话已删除")
  }).catch(() => {})
}

async function handlePrompt(prompt) {
  if (!currentSessionId.value) {
    await handleCreateSession()
  }
  input.value = prompt
  await handleSend()
}

async function handleSend() {
  const text = input.value.trim()
  if (!text || !currentSessionId.value || sending.value) return
  const sessionId = currentSessionId.value
  input.value = ""
  messages.value.push({
    id: `local-${Date.now()}`,
    role: "USER",
    content: text
  })
  const streamMessage = {
    id: `stream-${Date.now()}`,
    role: "ASSISTANT",
    content: "",
    intent: "",
    toolCalls: [],
    streaming: true
  }
  messages.value.push(streamMessage)
  sending.value = true
  try {
    await sendAgentChatStream(
      {
        sessionId,
        message: text,
        engine: "python"
      },
      {
        onTool(tool) {
          streamMessage.toolCalls.push(tool)
        },
        onDelta(delta) {
          streamMessage.content += delta.content || ""
          streamMessage.intent = delta.intent || streamMessage.intent
        },
        onDone(answer) {
          streamMessage.id = answer.messageId || streamMessage.id
          streamMessage.intent = answer.intent || streamMessage.intent
          streamMessage.toolCalls = answer.toolCalls || streamMessage.toolCalls
          streamMessage.model = answer.model
          streamMessage.traceId = answer.traceId
          streamMessage.streaming = false
        },
        onError(error) {
          streamMessage.content = error.message || "助手响应失败"
          streamMessage.streaming = false
        }
      }
    )
    await loadSessions()
  } catch (error) {
    const detail = error?.response?.data?.msg || error?.message || "助手响应失败"
    streamMessage.content = detail
    streamMessage.streaming = false
  } finally {
    sending.value = false
  }
}

function roleLabel(role) {
  return {
    STUDENT: "学生助手",
    TEACHER: "教师审核助手",
    ADMIN: "平台管理助手",
    USER: "通用助手"
  }[role] || "实验室业务助手"
}
</script>

<style lang="scss" scoped>
.agent-page {
  height: calc(100vh - 84px);
  min-height: 560px;
  padding: 14px;
  background: #eef3f8;
}

.agent-shell {
  display: flex;
  height: 100%;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
}

.chat-area {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid #eef2f7;
}

.chat-title {
  color: #1e293b;
  font-size: 18px;
  font-weight: 700;
}

.chat-subtitle {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.service-state {
  display: flex;
  align-items: center;
  gap: 8px;
}

.service-alert {
  margin: 12px 24px 0;
}

.composer {
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #fff;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 10px;
  color: #94a3b8;
  font-size: 12px;
}

@media (max-width: 760px) {
  .agent-page {
    height: calc(100vh - 60px);
    padding: 0;
  }

  .agent-shell {
    border: 0;
    border-radius: 0;
  }

  :deep(.session-panel) {
    width: 210px;
    min-width: 210px;
  }

  .composer-actions > span {
    display: none;
  }
}
</style>
