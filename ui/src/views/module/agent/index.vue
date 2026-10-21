<template>
  <div class="app-container assistant-page">
    <aside class="session-panel">
      <div class="session-header">
        <span>智能助手</span>
        <el-button type="primary" size="small" icon="Plus" @click="newSession">新对话</el-button>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: currentSession && currentSession.id === session.id }"
          @click="selectSession(session)"
        >
          <div class="session-title">{{ session.title }}</div>
          <el-button link type="danger" icon="Delete" class="session-delete" @click.stop="removeSession(session)" />
        </div>
        <el-empty v-if="sessions.length === 0" description="还没有对话" :image-size="70" />
      </div>
    </aside>

    <section class="chat-panel">
      <header class="chat-header">
        <div>
          <h3>实验室智能助手</h3>
          <p>查询实验室、设备、考试、信用和预约审核结果</p>
        </div>
        <el-tag v-if="!modelConfigured" type="warning">未配置模型密钥</el-tag>
      </header>

      <div ref="messageBoxRef" class="message-box" v-loading="loading">
        <div v-if="messages.length === 0" class="welcome">
          <div class="welcome-icon">AI</div>
          <h2>你好，我是实验室智能助手</h2>
          <p>我只会根据系统里的真实数据回答，不会编造实验室和预约信息。</p>
          <div class="example-list">
            <el-tag
              v-for="example in examples"
              :key="example"
              class="example-tag"
              @click="useExample(example)"
            >
              {{ example }}
            </el-tag>
          </div>
        </div>

        <div v-for="(message, index) in messages" :key="index" class="message-row" :class="message.role.toLowerCase()">
          <div class="avatar">{{ message.role === 'USER' ? '我' : 'AI' }}</div>
          <div class="message-content">
            <div class="bubble">{{ message.content }}</div>
            <div class="message-meta">{{ formatTime(message.createTime) }}</div>
            <el-collapse v-if="message.toolCalls && message.toolCalls.length" class="tool-collapse">
              <el-collapse-item :title="'工具调用（' + message.toolCalls.length + ' 次）'">
                <div v-for="(tool, toolIndex) in message.toolCalls" :key="toolIndex" class="tool-item">
                  <div class="tool-name">
                    <el-tag size="small" :type="tool.success ? 'success' : 'danger'">
                      {{ tool.toolName }}
                    </el-tag>
                    <span>{{ tool.costMs }} ms</span>
                  </div>
                  <div class="tool-label">参数</div>
                  <pre>{{ tool.arguments }}</pre>
                  <div class="tool-label">返回</div>
                  <pre>{{ tool.result }}</pre>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
        </div>

        <div v-if="sending" class="message-row assistant">
          <div class="avatar">AI</div>
          <div class="bubble typing">正在查询系统数据...</div>
        </div>
      </div>

      <footer class="composer">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          placeholder="例如：下周三下午有哪些实验室可以做有机实验？"
          @keydown.ctrl.enter.prevent="send"
          @keydown.meta.enter.prevent="send"
        />
        <el-button type="primary" :loading="sending" @click="send">发送</el-button>
      </footer>
    </section>
  </div>
</template>

<script setup name="Assistant">
import { getAssistantStatus, listAssistantSessions, createAssistantSession, listAssistantMessages, chatWithAssistant, deleteAssistantSession } from "@/api/module/assistant";

const { proxy } = getCurrentInstance();
const sessions = ref([]);
const currentSession = ref(null);
const messages = ref([]);
const input = ref("");
const loading = ref(false);
const sending = ref(false);
const messageBoxRef = ref(null);
const modelConfigured = ref(true);

const examples = [
  "我通过安全考试了吗？",
  "我的信用分够预约吗？",
  "查看我最近的预约",
  "我的预约为什么被驳回？"
];

onMounted(() => {
  getAssistantStatus().then(response => {
    modelConfigured.value = response.data?.configured !== false;
  });
  loadSessions();
});

function loadSessions() {
  return listAssistantSessions().then(response => {
    sessions.value = response.data || [];
    if (!sessions.value.length) {
      currentSession.value = null;
      messages.value = [];
      return;
    }
    const active = currentSession.value
      ? sessions.value.find(item => item.id === currentSession.value.id)
      : sessions.value[0];
    if (active) {
      selectSession(active);
    }
  });
}

function refreshSessionsOnly() {
  return listAssistantSessions().then(response => {
    sessions.value = response.data || [];
    if (currentSession.value) {
      const refreshed = sessions.value.find(item => item.id === currentSession.value.id);
      if (refreshed) {
        currentSession.value = refreshed;
      }
    }
  });
}

function selectSession(session) {
  currentSession.value = session;
  loading.value = true;
  listAssistantMessages(session.id).then(response => {
    messages.value = (response.data || []).map(item => ({
      role: item.role,
      content: item.content,
      createTime: item.createTime,
      toolCalls: []
    }));
    scrollToBottom();
  }).finally(() => {
    loading.value = false;
  });
}

function newSession() {
  createAssistantSession("新对话").then(response => {
    const session = response.data;
    sessions.value.unshift(session);
    currentSession.value = session;
    messages.value = [];
    input.value = "";
  });
}

function removeSession(session) {
  proxy.$modal.confirm('确认删除对话“' + session.title + '”吗？').then(() => deleteAssistantSession(session.id)).then(() => {
    sessions.value = sessions.value.filter(item => item.id !== session.id);
    if (currentSession.value && currentSession.value.id === session.id) {
      currentSession.value = null;
      messages.value = [];
      loadSessions();
    }
    proxy.$modal.msgSuccess("对话已删除");
  }).catch(() => {});
}

function useExample(example) {
  input.value = example;
}

function send() {
  const content = input.value.trim();
  if (!content || sending.value) return;
  input.value = "";
  messages.value.push({ role: "USER", content, createTime: new Date().toISOString(), toolCalls: [] });
  sending.value = true;
  scrollToBottom();

  const request = currentSession.value
    ? Promise.resolve(currentSession.value)
    : createAssistantSession(content).then(response => {
        currentSession.value = response.data;
        sessions.value.unshift(response.data);
        return response.data;
      });

  request.then(session => chatWithAssistant({ sessionId: session.id, message: content })).then(response => {
    const data = response.data || {};
    if (data.sessionId && (!currentSession.value || currentSession.value.id !== data.sessionId)) {
      currentSession.value = { id: data.sessionId, title: content.slice(0, 30) };
    }
    messages.value.push({
      role: "ASSISTANT",
      content: data.content || "模型没有返回内容。",
      createTime: new Date().toISOString(),
      toolCalls: data.toolCalls || []
    });
    if (String(data.content || "").includes("尚未配置模型密钥")) {
      modelConfigured.value = false;
    }
    refreshSessionsOnly();
    scrollToBottom();
  }).catch(error => {
    messages.value.push({
      role: "ASSISTANT",
      content: "请求失败：" + (error && error.message ? error.message : "请稍后重试"),
      createTime: new Date().toISOString(),
      toolCalls: []
    });
    scrollToBottom();
  }).finally(() => {
    sending.value = false;
  });
}

function scrollToBottom() {
  nextTick(() => {
    if (messageBoxRef.value) {
      messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight;
    }
  });
}

function formatTime(value) {
  return value ? proxy.parseTime(value, "{h}:{i}") : "";
}
</script>

<style lang="scss" scoped>
.assistant-page {
  display: flex;
  gap: 14px;
  height: calc(100vh - 110px);
  min-height: 560px;
}
.session-panel {
  width: 230px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
}
.session-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
}
.session-list { flex: 1; overflow-y: auto; padding: 8px; }
.session-item {
  position: relative;
  padding: 10px 24px 10px 10px;
  margin-bottom: 6px;
  border-radius: 6px;
  cursor: pointer;
  color: #606266;
}
.session-item:hover { background: #f5f7fa; }
.session-item.active { background: #ecf5ff; color: #409eff; }
.session-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.session-delete { position: absolute; right: 2px; top: 9px; }
.chat-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
}
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid #ebeef5;
}
.chat-header h3 { margin: 0 0 4px; }
.chat-header p { margin: 0; color: #909399; font-size: 12px; }
.message-box { flex: 1; overflow-y: auto; padding: 18px; background: #f7f8fa; }
.welcome { text-align: center; padding: 50px 20px; color: #606266; }
.welcome-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 56px; height: 56px; border-radius: 16px; background: #409eff;
  color: #fff; font-weight: 700; font-size: 20px; margin-bottom: 12px;
}
.welcome h2 { margin: 0 0 8px; }
.welcome p { color: #909399; }
.example-list { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin-top: 18px; }
.example-tag { cursor: pointer; }
.message-row { display: flex; gap: 10px; margin-bottom: 18px; }
.message-row.user { flex-direction: row-reverse; }
.avatar {
  width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: #e4e7ed; color: #606266; font-size: 12px;
}
.message-row.assistant .avatar { background: #409eff; color: #fff; }
.message-content { max-width: 78%; }
.message-row.user .message-content { text-align: right; }
.bubble {
  display: inline-block;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fff;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  text-align: left;
}
.message-row.user .bubble { background: #409eff; color: #fff; }
.bubble.typing { color: #909399; }
.message-meta { color: #c0c4cc; font-size: 11px; margin-top: 4px; }
.tool-collapse { margin-top: 8px; text-align: left; }
.tool-item { margin-bottom: 12px; }
.tool-name { display: flex; align-items: center; gap: 8px; color: #909399; font-size: 12px; }
.tool-label { margin: 6px 0 2px; color: #909399; font-size: 12px; }
.tool-item pre {
  margin: 0; padding: 8px; background: #f5f7fa; border-radius: 4px;
  font-size: 12px; white-space: pre-wrap; word-break: break-word; max-height: 160px; overflow: auto;
}
.composer { display: flex; gap: 10px; padding: 12px 16px; border-top: 1px solid #ebeef5; }
.composer .el-textarea { flex: 1; }
@media (max-width: 900px) {
  .assistant-page { flex-direction: column; height: auto; }
  .session-panel { width: 100%; max-height: 180px; }
  .chat-panel { min-height: 600px; }
  .message-content { max-width: 88%; }
}
</style>
