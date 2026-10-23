<template>
  <div class="app-container agent-page" :class="'theme-' + skin">
    <!-- 左侧：会话列表。所有会话都属于当前登录用户。 -->
    <AgentSessionPanel
      :sessions="sessions"
      :active-session-id="currentSession?.id"
      :error="sessionError"
      :skin="skin"
      @select="selectSession"
      @new="newSession"
      @delete="removeSession"
      @retry="loadSessions"
    />

    <!-- 右侧：当前会话的消息、工具调用和输入框。 -->
    <section class="chat-panel">
      <header class="chat-header">
        <div class="chat-heading">
          <div class="assistant-avatar">
            <svg class="lab-mascot" viewBox="0 0 64 64" aria-hidden="true">
              <path class="mascot-antenna" d="M32 18V9" />
              <circle class="mascot-bulb" cx="32" cy="8" r="3" />
              <rect class="mascot-head" x="12" y="18" width="40" height="32" rx="13" />
              <circle class="mascot-eye" cx="24" cy="33" r="2.8" />
              <circle class="mascot-eye" cx="40" cy="33" r="2.8" />
              <path class="mascot-smile" d="M26 40c3.8 4.2 8.2 4.2 12 0" />
              <circle class="mascot-cheek" cx="18" cy="39" r="2.2" />
              <circle class="mascot-cheek" cx="46" cy="39" r="2.2" />
            </svg>
            <i></i>
          </div>
          <div class="chat-heading-copy">
            <div class="chat-title-line">
              <h3>{{ assistantTitle }}</h3>
              <el-tag v-if="roleLabel" size="small" type="info" effect="plain">{{ roleLabel }}</el-tag>
            </div>
            <p>{{ assistantDescription }}</p>
          </div>
        </div>
        <div class="chat-header-actions">
          <el-dropdown trigger="click" placement="bottom-end" popper-class="agent-skin-popper" @command="setSkin">
            <el-button size="small" plain icon="MagicStick" class="skin-toggle">
              <span class="skin-current-dot" :class="'skin-' + skin"></span>
              {{ currentSkin.label }}
            </el-button>
            <template #dropdown>
              <el-dropdown-menu class="agent-skin-menu">
                <el-dropdown-item
                  v-for="option in skinOptions"
                  :key="option.key"
                  :command="option.key"
                  :class="{ active: skin === option.key }"
                >
                  <span class="skin-option-swatch" :class="'skin-' + option.key"></span>
                  <span class="skin-option-copy">
                    <strong>{{ option.label }}</strong>
                    <small>{{ option.description }}</small>
                  </span>
                  <span v-if="skin === option.key" class="skin-option-check">✓</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button size="small" plain icon="Search" @click="knowledgeOpen = true">知识库</el-button>
          <el-button
            v-if="!modelStatusLoading && modelConfigured"
            size="small"
            type="success"
            plain
            icon="Connection"
            :loading="checkingModel"
            @click="testModel"
          >
            测试模型连接
          </el-button>
          <el-button
            v-if="!modelStatusLoading && !modelConfigured"
            size="small"
            type="warning"
            plain
            icon="Setting"
            @click="configOpen = true"
          >
            配置模型 Key
          </el-button>
          <span v-if="!modelStatusLoading" class="model-state" :class="{ ready: modelConfigured }">
            <i></i>
            {{ modelConfigured ? (modelName || 'DeepSeek') + ' 已配置' : '未配置模型密钥' }}
          </span>
        </div>
      </header>

      <el-alert
        v-if="!modelStatusLoading && !modelConfigured"
        class="model-alert"
        type="warning"
        :closable="false"
        show-icon
        title="当前只能使用会话、知识库检索和审计功能；填写 DeepSeek Key 并重启后端后即可进行真实对话。"
      >
        <template #default>
          <el-button link type="primary" @click="configOpen = true">查看填写位置</el-button>
        </template>
      </el-alert>
      <el-alert
        v-if="statusError"
        class="model-alert"
        type="error"
        :closable="false"
        show-icon
        :title="statusError"
      >
        <template #default>
          <el-button link type="primary" @click="loadAgentStatus">重试</el-button>
        </template>
      </el-alert>
      <el-alert
        v-if="messageError"
        class="model-alert"
        type="error"
        :closable="false"
        show-icon
        :title="messageError"
      >
        <template #default>
          <el-button link type="primary" @click="messageError = ''">知道了</el-button>
        </template>
      </el-alert>

      <AgentMessagePanel
        :messages="messages"
        :loading="loading"
        :sending="sending"
        :examples="examples"
        :assistant-title="assistantTitle"
        :draft-loading="draftLoading"
        :skin="skin"
        @example="useExample"
        @confirm-draft="confirmDraft"
        @cancel-draft="cancelDraft"
      />

      <footer class="composer">
        <div class="composer-shell">
          <el-input
            v-model="input"
            class="composer-input"
            type="textarea"
            :rows="2"
            maxlength="2000"
            resize="none"
            placeholder="描述你的实验室问题，例如：下周三下午有哪些实验室可以做有机实验？"
            @keydown="handleComposerKeydown"
          />
          <div class="composer-footer">
            <span class="composer-hint">Enter 发送 · Shift + Enter 换行 · 查询结果以系统实时数据为准</span>
            <div class="composer-submit">
              <span>{{ input.length }}/2000</span>
              <el-button type="primary" icon="Promotion" :loading="sending" :disabled="!input.trim()" @click="send">
                发送
              </el-button>
            </div>
          </div>
        </div>
      </footer>
    </section>

    <!-- 不经过模型也能验证 RAG 召回结果的知识库检索弹窗。 -->
    <AgentKnowledgeDialog v-model="knowledgeOpen" />

    <el-dialog title="配置 DeepSeek API Key" v-model="configOpen" width="700px" append-to-body>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="Key 只保存在本机，配置文件已被 .gitignore 忽略，不会提交到 Git。"
      />
      <div class="config-guide">
        <h4>方式一：编辑本地配置文件（推荐）</h4>
        <p>打开下面这个文件：</p>
        <pre>D:\bishe_project\api\ruoyi-admin\application-local.yml</pre>
        <p>将 Key 填在引号内：</p>
        <pre>bishe:
  ai:
    deepseek-api-key: "sk-你的DeepSeekKey"</pre>
        <h4>方式二：IDEA 环境变量</h4>
        <p>在 Spring Boot 启动配置的 Environment variables 中填写：</p>
        <pre>DEEPSEEK_API_KEY=sk-你的DeepSeekKey</pre>
        <p class="config-tip">两种方式修改后都需要重启后端。回到本页面刷新，再点击“测试模型连接”。</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="configOpen = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Agent">
import AgentKnowledgeDialog from "./components/KnowledgeDialog.vue";
import AgentMessagePanel from "./components/MessagePanel.vue";
import AgentSessionPanel from "./components/SessionPanel.vue";
import { getAgentStatus, checkAgentModel, listAgentSessions, createAgentSession, listAgentMessages, chatWithAgent, deleteAgentSession, confirmAgentDraft, cancelAgentDraft, listPendingAgentDrafts } from "@/api/module/agent";

/**
 * 页面状态：
 * - sessions/currentSession：左侧会话和当前选中的会话；
 * - messages：当前会话的消息，历史消息和本次新回复都会展示工具调用；
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
const modelConfigured = ref(false);
const modelStatusLoading = ref(true);
const statusError = ref("");
const sessionError = ref("");
const messageError = ref("");
const modelName = ref("");
const checkingModel = ref(false);
const configOpen = ref(false);
const assistantRole = ref("");
const roleLabel = ref("");
const assistantTitle = ref("实验室智能助手");
const assistantDescription = ref("查询实验室、设备、考试、信用和预约审核结果");
const knowledgeOpen = ref(false);
const draftLoading = ref(null);
const skinOptions = [
  { key: "fresh", label: "清爽实验室", description: "蓝白科技，适合正式演示" },
  { key: "candy", label: "奶油糖果", description: "粉紫柔光，亲切活泼" },
  { key: "mint", label: "薄荷实验室", description: "薄荷绿，清爽耐看" }
];
const savedSkin = window.localStorage.getItem("agent-page-skin");
const skin = ref(skinOptions.some(option => option.key === savedSkin) ? savedSkin : "fresh");
const currentSkin = computed(() => skinOptions.find(option => option.key === skin.value) || skinOptions[0]);

const examples = ref([
  "我通过安全考试了吗？",
  "我的信用分够预约吗？",
  "我信用分够但没通过安全考试还能预约吗？",
  "查看我最近的预约",
  "我的预约为什么被驳回？"
]);

// 页面初始化：先查询模型状态，再加载当前用户的会话。
onMounted(() => {
  loadAgentStatus();
  loadSessions();
});

function loadAgentStatus() {
  modelStatusLoading.value = true;
  statusError.value = "";
  getAgentStatus().then(response => {
    const status = response.data || {};
    modelConfigured.value = status.configured === true;
    modelName.value = status.model || "";
    assistantRole.value = status.role || "";
    roleLabel.value = status.roleLabel || "";
    assistantTitle.value = status.assistantTitle || "实验室智能助手";
    assistantDescription.value = status.assistantDescription || "查询实验室、设备、考试、信用和预约审核结果";
    if (Array.isArray(status.examples) && status.examples.length) {
      examples.value = status.examples;
    }
  }).catch(error => {
    statusError.value = "助手状态加载失败：" + (error?.message || "请检查后端服务");
  }).finally(() => {
    modelStatusLoading.value = false;
  });
}

function loadSessions() {
  sessionError.value = "";
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
  }).catch(error => {
    sessionError.value = "会话列表加载失败：" + (error?.message || "请稍后重试");
    sessions.value = [];
    currentSession.value = null;
    messages.value = [];
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
  }).catch(error => {
    sessionError.value = "会话列表刷新失败：" + (error?.message || "请稍后重试");
  });
}

function selectSession(session) {
  currentSession.value = session;
  loading.value = true;
  messageError.value = "";
  listAgentMessages(session.id).then(response => {
    messages.value = (response.data || []).map(item => ({
      role: item.role,
      content: item.content,
      createTime: item.createTime,
      toolCalls: item.toolCalls || [],
      draft: null
    }));
    listPendingAgentDrafts(session.id).then(draftResponse => {
      (draftResponse.data || []).forEach(draft => {
        messages.value.push({
          role: "ASSISTANT",
          content: "这份预约草稿还没有确认，请核对信息后点击下方按钮。",
          createTime: new Date().toISOString(),
          toolCalls: [],
          draft
        });
      });
    }).catch(error => {
      messageError.value = "待确认草稿加载失败：" + (error?.message || "请重新进入会话");
    }).finally(() => {
    });
  }).catch(error => {
    messageError.value = "会话消息加载失败：" + (error?.message || "请稍后重试");
    messages.value = [];
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
  }).catch(error => {
    messageError.value = "新建会话失败：" + (error?.message || "请稍后重试");
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

function testModel() {
  checkingModel.value = true;
  checkAgentModel().then(response => {
    const result = response.data || {};
    if (result.ok) {
      proxy.$modal.msgSuccess("模型连接成功，" + (result.costMs || 0) + " ms");
    } else {
      proxy.$modal.msgError(result.message || "模型连接失败，请检查 Key、网络和模型配置");
    }
  }).catch(error => {
    proxy.$modal.msgError("模型连接失败：" + (error?.message || "请检查 Key、网络和模型配置"));
  }).finally(() => {
    checkingModel.value = false;
  });
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
  }).catch(error => {
    messages.value.push({
      role: "ASSISTANT",
      content: "请求失败：" + (error && error.message ? error.message : "请稍后重试"),
      createTime: new Date().toISOString(),
      toolCalls: [],
      draft: null
    });
  }).finally(() => {
    sending.value = false;
  });
}

function handleComposerKeydown(event) {
  if (event.key !== "Enter" || event.isComposing || event.shiftKey) {
    return;
  }
  event.preventDefault();
  send();
}

function setSkin(key) {
  if (!skinOptions.some(option => option.key === key)) {
    return;
  }
  skin.value = key;
  window.localStorage.setItem("agent-page-skin", key);
}

// 用户点击草稿卡片后才会提交，模型不能把“草稿”当成“预约成功”来回答。
function confirmDraft(message) {
  draftLoading.value = message.draft.id;
  confirmAgentDraft(message.draft.id).then(response => {
    message.draft = response.data || message.draft;
    proxy.$modal.msgSuccess("预约已提交，后端已完成二次校验");
  }).catch(error => {
    proxy.$modal.msgError("预约草稿提交失败：" + (error?.message || "请重新检查预约信息"));
  }).finally(() => {
    draftLoading.value = null;
  });
}

function cancelDraft(message) {
  cancelAgentDraft(message.draft.id).then(response => {
    message.draft = response.data || message.draft;
    proxy.$modal.msgSuccess("预约草稿已取消");
  }).catch(error => {
    proxy.$modal.msgError("取消预约草稿失败：" + (error?.message || "请稍后重试"));
  });
}

</script>

<style lang="scss" scoped>
.agent-page {
  display: flex;
  gap: 0;
  height: calc(100vh - 104px);
  min-height: 620px;
  border: 1px solid #e6ebf3;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 18px 48px rgba(30, 55, 90, 0.08);
  overflow: hidden;
}
.lab-mascot {
  width: 72%;
  height: 72%;
  display: block;
  color: #fff;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.6;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.mascot-bulb {
  fill: currentColor;
  stroke: none;
}
.mascot-head {
  fill: rgba(255, 255, 255, 0.14);
}
.mascot-eye {
  fill: currentColor;
  stroke: none;
}
.mascot-cheek {
  fill: currentColor;
  opacity: 0.48;
  stroke: none;
}
.chat-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
}
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 22px;
  border-bottom: 1px solid #edf1f6;
  background: rgba(255, 255, 255, 0.96);
}
.chat-heading {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.assistant-avatar {
  position: relative;
  width: 43px;
  height: 43px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 14px;
  background: linear-gradient(145deg, #1d4ed8, #38bdf8);
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.24);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}
.assistant-avatar i {
  position: absolute;
  right: -1px;
  bottom: -1px;
  width: 10px;
  height: 10px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #22c55e;
}
.chat-heading-copy { min-width: 0; }
.chat-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.chat-title-line h3 {
  margin: 0;
  color: #172033;
  font-size: 17px;
  font-weight: 700;
}
.chat-header p {
  margin: 4px 0 0;
  overflow: hidden;
  color: #8b97aa;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}
.chat-header-actions :deep(.el-button) {
  border-radius: 9px;
}
.skin-current-dot,
.skin-option-swatch {
  display: inline-block;
  flex-shrink: 0;
  border-radius: 50%;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.7);
}
.skin-current-dot {
  width: 8px;
  height: 8px;
  margin-right: 5px;
}
.skin-fresh {
  background: linear-gradient(135deg, #2563eb, #38bdf8);
}
.skin-candy {
  background: linear-gradient(135deg, #f472b6, #a78bfa);
}
.skin-mint {
  background: linear-gradient(135deg, #10b981, #2dd4bf);
}
.model-state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f5f7fa;
  color: #8b97aa;
  font-size: 11px;
  white-space: nowrap;
}
.model-state i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #e6a23c;
}
.model-state.ready {
  background: #f0f9eb;
  color: #529b2e;
}
.model-state.ready i { background: #67c23a; }
.model-alert {
  margin: 10px 18px 0;
  border-radius: 10px;
}
.composer {
  padding: 14px 22px 18px;
  border-top: 1px solid #edf1f6;
  background: #fff;
}
.composer-shell {
  max-width: 1040px;
  margin: 0 auto;
  padding: 11px 13px 10px 16px;
  border: 1px solid #dce5f1;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(35, 66, 112, 0.1);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}
.composer-shell:focus-within {
  border-color: #9fc1fa;
  box-shadow: 0 14px 36px rgba(37, 99, 235, 0.13);
}
.composer-input :deep(.el-textarea__inner) {
  padding: 0;
  border: 0;
  box-shadow: none;
  color: #263348;
  font-size: 13px;
  line-height: 1.65;
}
.composer-input :deep(.el-input__count) {
  display: none;
}
.composer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 7px;
}
.composer-hint {
  color: #9aa6b8;
  font-size: 10px;
}
.composer-submit {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #a4aec0;
  font-size: 10px;
}
.composer-submit :deep(.el-button) {
  min-width: 82px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.22);
}
.config-guide { margin-top: 16px; line-height: 1.8; color: #606266; }
.config-guide h4 { margin: 16px 0 6px; color: #303133; }
.config-guide p { margin: 6px 0; }
.config-guide pre {
  margin: 8px 0; padding: 10px 12px; border-radius: 4px;
  background: #f5f7fa; color: #303133; white-space: pre-wrap; word-break: break-all;
}
.config-tip { color: #e6a23c; }
/* 奶油糖果皮肤：保留专业信息层级，只用柔和色彩和小 IP 增加亲和力。 */
.agent-page.theme-candy {
  border-color: #f2dfeb;
  background: #fffaf7;
  box-shadow: 0 18px 52px rgba(159, 94, 132, 0.12);
}
.theme-candy .assistant-avatar,
.theme-candy .composer-submit :deep(.el-button) {
  background: linear-gradient(145deg, #f472b6, #a78bfa);
  box-shadow: 0 10px 24px rgba(206, 100, 166, 0.25);
}
.theme-candy .chat-header {
  border-color: #f5e7ef;
  background: rgba(255, 252, 250, 0.97);
}
.theme-candy .chat-header p {
  color: #a88e9d;
}
.theme-candy .model-state.ready {
  background: #ecfdf5;
  color: #3f9a72;
}
.theme-candy .composer-shell {
  border-color: #f0dce8;
  background: #fffcf9;
  box-shadow: 0 12px 32px rgba(161, 99, 134, 0.11);
}
.theme-candy .composer-shell:focus-within {
  border-color: #f0a8ca;
  box-shadow: 0 14px 36px rgba(221, 101, 165, 0.14);
}
.theme-candy .composer-submit {
  color: #b294a5;
}

.agent-page.theme-mint {
  border-color: #dcefe8;
  background: #f8fffc;
  box-shadow: 0 18px 52px rgba(48, 126, 101, 0.11);
}
.theme-mint .assistant-avatar,
.theme-mint .composer-submit :deep(.el-button) {
  background: linear-gradient(145deg, #059669, #2dd4bf);
  box-shadow: 0 10px 24px rgba(16, 185, 129, 0.24);
}
.theme-mint .chat-header {
  border-color: #e3f2ec;
  background: rgba(250, 255, 253, 0.97);
}
.theme-mint .chat-header p {
  color: #89a69b;
}
.theme-mint .model-state.ready {
  background: #ecfdf5;
  color: #278f69;
}
.theme-mint .composer-shell {
  border-color: #daeee7;
  background: #fbfffd;
  box-shadow: 0 12px 32px rgba(52, 118, 96, 0.1);
}
.theme-mint .composer-shell:focus-within {
  border-color: #8ed5c1;
  box-shadow: 0 14px 36px rgba(16, 185, 129, 0.13);
}
.theme-mint .composer-submit {
  color: #93aaa2;
}

:global(.agent-skin-popper.el-popper) {
  padding: 0;
  border: 1px solid #e5ebf3;
  border-radius: 14px;
  box-shadow: 0 18px 42px rgba(32, 58, 96, 0.16);
}
:global(.agent-skin-popper .el-dropdown-menu) {
  min-width: 242px;
  padding: 6px;
  border-radius: 13px;
}
:global(.agent-skin-popper .el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 52px;
  padding: 8px 10px;
  border-radius: 9px;
  line-height: 1.3;
}
:global(.agent-skin-popper .el-dropdown-menu__item.active) {
  background: #f2f7ff;
  color: #2563eb;
}
.skin-option-swatch {
  width: 22px;
  height: 22px;
}
.skin-option-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}
.skin-option-copy strong {
  font-size: 13px;
  font-weight: 600;
}
.skin-option-copy small {
  color: #9aa6b8;
  font-size: 10px;
}
.skin-option-check {
  color: #2563eb;
  font-size: 14px;
  font-weight: 700;
}
@media (max-width: 900px) {
  .agent-page {
    flex-direction: column;
    height: calc(100vh - 100px);
    min-height: 650px;
    border-radius: 12px;
  }
  .chat-panel {
    flex: 1;
    min-height: 0;
  }
  .chat-header {
    align-items: flex-start;
    flex-direction: column;
    padding: 15px 16px;
  }
  .chat-header-actions {
    width: 100%;
    justify-content: flex-start;
  }
  .composer {
    padding: 12px 14px 15px;
  }
}
@media (max-width: 600px) {
  .chat-title-line {
    flex-wrap: wrap;
  }
  .chat-header p {
    white-space: normal;
  }
  .composer-hint {
    display: none;
  }
  .composer-submit {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
