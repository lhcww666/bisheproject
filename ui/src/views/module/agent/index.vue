<template>
  <div class="app-container agent-page">
    <!-- 左侧：会话列表。所有会话都属于当前登录用户。 -->
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

    <!-- 右侧：当前会话的消息、工具调用和输入框。 -->
    <section class="chat-panel">
      <header class="chat-header">
        <div>
          <h3>实验室智能助手</h3>
          <p>查询实验室、设备、考试、信用和预约审核结果</p>
        </div>
        <div class="chat-header-actions">
          <el-button size="small" icon="Search" @click="knowledgeOpen = true">安全知识库</el-button>
          <el-tag v-if="!modelConfigured" type="warning">未配置模型密钥</el-tag>
        </div>
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
            <!-- 预约草稿卡片：只有用户点击确认后，才调用后端提交接口。 -->
            <el-card v-if="message.draft" shadow="never" class="draft-card">
              <div class="draft-title">
                <span>预约草稿</span>
                <el-tag size="small" :type="draftStatusType(message.draft.status)">
                  {{ draftStatusLabel(message.draft.status) }}
                </el-tag>
              </div>
              <div class="draft-line"><span>实验室</span>{{ message.draft.roomName || message.draft.roomId }}</div>
              <div class="draft-line"><span>时间</span>{{ formatDateTime(message.draft.startTime) }} 至 {{ formatDateTime(message.draft.endTime) }}</div>
              <div class="draft-line"><span>人数</span>{{ message.draft.participantCount }}</div>
              <div class="draft-line"><span>设备</span>{{ (message.draft.deviceNames || []).join('、') || '未选择' }}</div>
              <div class="draft-line"><span>说明</span>{{ message.draft.remark || '无' }}</div>
              <div v-if="message.draft.status === 'DRAFT'" class="draft-actions">
                <el-button type="primary" size="small" :loading="draftLoading === message.draft.id" @click="confirmDraft(message)">
                  确认提交预约
                </el-button>
                <el-button size="small" @click="cancelDraft(message)">取消草稿</el-button>
              </div>
              <div v-else-if="message.draft.status === 'CONFIRMED'" class="draft-result">
                已提交：{{ message.draft.reservationNo }}，状态 {{ reservationStatusLabel(message.draft.reservationStatus) }}
              </div>
            </el-card>
            <div class="message-meta">{{ formatTime(message.createTime) }}</div>
            <!-- 工具调用详情：让用户看到 Agent 实际查了哪些数据。 -->
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

    <!-- 不经过模型也能验证 RAG 召回结果的知识库检索弹窗。 -->
    <el-dialog title="安全知识库检索" v-model="knowledgeOpen" width="720px" append-to-body destroy-on-close>
      <div class="knowledge-search">
        <el-input
          v-model="knowledgeQuery"
          placeholder="例如：酒精灯打翻着火怎么办"
          clearable
          @keyup.enter="searchKnowledge"
        />
        <el-button type="primary" :loading="knowledgeLoading" @click="searchKnowledge">检索</el-button>
      </div>
      <div v-if="knowledgeResults.length" class="knowledge-results">
        <el-card v-for="item in knowledgeResults" :key="item.chunkId" shadow="never" class="knowledge-card">
          <template #header>
            <div class="knowledge-title">
              <span>{{ item.documentTitle }}</span>
              <el-tag size="small" type="success">相关度 {{ item.score }}</el-tag>
            </div>
          </template>
          <p class="knowledge-content">{{ item.content }}</p>
          <div class="knowledge-source">来源：{{ item.source || '演示安全资料' }}</div>
        </el-card>
      </div>
      <el-empty v-else-if="knowledgeSearched" description="未检索到相关资料" />
    </el-dialog>
  </div>
</template>

<script setup name="Agent">
import { getAgentStatus, listAgentSessions, createAgentSession, listAgentMessages, chatWithAgent, deleteAgentSession, searchAgentKnowledge, confirmAgentDraft, cancelAgentDraft } from "@/api/module/agent";

/**
 * 页面状态：
 * - sessions/currentSession：左侧会话和当前选中的会话；
 * - messages：当前会话的消息，toolCalls 只对本次新回复保留；
 * - sending/loading：请求和加载状态；
 * - modelConfigured：后端是否配置了 DEEPSEEK_API_KEY。
 */
const { proxy } = getCurrentInstance();
const sessions = ref([]);
const currentSession = ref(null);
const messages = ref([]);
const input = ref("");
const loading = ref(false);
const sending = ref(false);
const messageBoxRef = ref(null);
const modelConfigured = ref(true);
const knowledgeOpen = ref(false);
const knowledgeQuery = ref("");
const knowledgeLoading = ref(false);
const knowledgeSearched = ref(false);
const knowledgeResults = ref([]);
const draftLoading = ref(null);

const examples = [
  "我通过安全考试了吗？",
  "我的信用分够预约吗？",
  "查看我最近的预约",
  "我的预约为什么被驳回？"
];

// 页面初始化：先查询模型状态，再加载当前用户的会话。
onMounted(() => {
  getAgentStatus().then(response => {
    modelConfigured.value = response.data?.configured !== false;
  });
  loadSessions();
});

function loadSessions() {
  return listAgentSessions().then(response => {
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

// 只刷新左侧会话标题和时间，不重新拉取消息，避免把本次工具调用详情覆盖掉。
function refreshSessionsOnly() {
  return listAgentSessions().then(response => {
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
  listAgentMessages(session.id).then(response => {
    messages.value = (response.data || []).map(item => ({
      role: item.role,
      content: item.content,
      createTime: item.createTime,
      toolCalls: [],
      draft: null
    }));
    scrollToBottom();
  }).finally(() => {
    loading.value = false;
  });
}

// 新建空会话，登录用户第一次发送消息时由后端自动补标题。
function newSession() {
  createAgentSession("新对话").then(response => {
    const session = response.data;
    sessions.value.unshift(session);
    currentSession.value = session;
    messages.value = [];
    input.value = "";
  });
}

function removeSession(session) {
  proxy.$modal.confirm('确认删除对话“' + session.title + '”吗？').then(() => deleteAgentSession(session.id)).then(() => {
    sessions.value = sessions.value.filter(item => item.id !== session.id);
    if (currentSession.value && currentSession.value.id === session.id) {
      currentSession.value = null;
      messages.value = [];
      loadSessions();
    }
    proxy.$modal.msgSuccess("对话已删除");
  }).catch(() => {});
}

// 点击示例问题只填入输入框，由用户确认后发送。
function useExample(example) {
  input.value = example;
}

/**
 * 发送流程：
 * 1. 先乐观渲染用户消息；
 * 2. 没有会话时先创建会话；
 * 3. 调用 /module/agent/chat；
 * 4. 把助手回复和工具调用摘要追加到消息列表；
 * 5. 只刷新会话列表，不重新拉取消息。
 */
function send() {
  const content = input.value.trim();
  if (!content || sending.value) return;
  input.value = "";
  messages.value.push({ role: "USER", content, createTime: new Date().toISOString(), toolCalls: [] });
  sending.value = true;
  scrollToBottom();

  const request = currentSession.value
    ? Promise.resolve(currentSession.value)
    : createAgentSession(content).then(response => {
        currentSession.value = response.data;
        sessions.value.unshift(response.data);
        return response.data;
      });

  request.then(session => chatWithAgent({ sessionId: session.id, message: content })).then(response => {
    const data = response.data || {};
    if (data.sessionId && (!currentSession.value || currentSession.value.id !== data.sessionId)) {
      currentSession.value = { id: data.sessionId, title: content.slice(0, 30) };
    }
    messages.value.push({
      role: "ASSISTANT",
      content: data.content || "模型没有返回内容。",
      createTime: new Date().toISOString(),
      toolCalls: data.toolCalls || [],
      draft: data.draft || null
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
      toolCalls: [],
      draft: null
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

// 用户点击草稿卡片后才会提交，模型不能把“草稿”当成“预约成功”来回答。
function confirmDraft(message) {
  draftLoading.value = message.draft.id;
  confirmAgentDraft(message.draft.id).then(response => {
    message.draft = response.data || message.draft;
    proxy.$modal.msgSuccess("预约已提交，后端已完成二次校验");
  }).finally(() => {
    draftLoading.value = null;
  });
}

function cancelDraft(message) {
  cancelAgentDraft(message.draft.id).then(response => {
    message.draft = response.data || message.draft;
    proxy.$modal.msgSuccess("预约草稿已取消");
  });
}

function draftStatusLabel(status) {
  if (status === "DRAFT") return "待确认";
  if (status === "CONFIRMED") return "已提交";
  if (status === "CANCELED") return "已取消";
  if (status === "EXPIRED") return "已过期";
  return status || "未知";
}

function draftStatusType(status) {
  if (status === "CONFIRMED") return "success";
  if (status === "DRAFT") return "warning";
  if (status === "EXPIRED") return "danger";
  return "info";
}

function reservationStatusLabel(status) {
  const labels = { "0": "待审核", "1": "已通过", "2": "已驳回", "3": "已取消", "4": "已完成", "5": "已签到", "6": "已爽约" };
  return labels[status] || status || "未知";
}

function formatDateTime(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}") : "";
}

// 直接调用知识库检索接口，不需要模型参与，便于演示和排查 RAG 召回。
function searchKnowledge() {
  const query = knowledgeQuery.value.trim();
  if (!query) return;
  knowledgeLoading.value = true;
  knowledgeSearched.value = false;
  searchAgentKnowledge(query, 3).then(response => {
    knowledgeResults.value = response.data || [];
  }).finally(() => {
    knowledgeLoading.value = false;
    knowledgeSearched.value = true;
  });
}
</script>

<style lang="scss" scoped>
.agent-page {
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
.chat-header-actions { display: flex; align-items: center; gap: 10px; }
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
.knowledge-search { display: flex; gap: 10px; margin-bottom: 14px; }
.knowledge-results { max-height: 480px; overflow-y: auto; }
.knowledge-card { margin-bottom: 10px; }
.knowledge-title { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.knowledge-content { margin: 0; line-height: 1.8; white-space: pre-wrap; }
.knowledge-source { margin-top: 8px; color: #909399; font-size: 12px; }
.draft-card { margin-top: 10px; border-color: #b3d8ff; background: #f4f9ff; }
.draft-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; font-weight: 600; color: #303133; }
.draft-line { display: flex; gap: 10px; margin: 4px 0; color: #606266; font-size: 13px; }
.draft-line span:first-child { width: 48px; flex-shrink: 0; color: #909399; }
.draft-actions { display: flex; gap: 8px; margin-top: 12px; }
.draft-result { margin-top: 10px; color: #67c23a; font-size: 13px; }
@media (max-width: 900px) {
  .agent-page { flex-direction: column; height: auto; }
  .session-panel { width: 100%; max-height: 180px; }
  .chat-panel { min-height: 600px; }
  .message-content { max-width: 88%; }
}
</style>
