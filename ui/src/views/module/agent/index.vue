<template>
  <div class="app-container agent-page">
    <!-- 左侧：会话列表。所有会话都属于当前登录用户。 -->
    <aside class="session-panel">
      <div class="session-header">
        <span>智能助手</span>
        <el-button type="primary" size="small" icon="Plus" @click="newSession">新对话</el-button>
      </div>
      <div class="session-list">
        <el-alert
          v-if="sessionError"
          class="inline-error"
          type="error"
          :closable="false"
          show-icon
          :title="sessionError"
        >
          <template #default>
            <el-button link type="primary" @click="loadSessions">重试</el-button>
          </template>
        </el-alert>
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
          <h3>{{ assistantTitle }}</h3>
          <p>{{ assistantDescription }}</p>
        </div>
        <div class="chat-header-actions">
          <el-tag v-if="roleLabel" type="info">{{ roleLabel }}</el-tag>
          <el-button size="small" icon="Search" @click="knowledgeOpen = true">安全知识库</el-button>
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
          <el-tag v-if="!modelStatusLoading" :type="modelConfigured ? 'success' : 'warning'">
            {{ modelConfigured ? (modelName || 'DeepSeek') + ' 已配置' : '未配置模型密钥' }}
          </el-tag>
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

      <div ref="messageBoxRef" class="message-box" v-loading="loading">
        <div v-if="messages.length === 0" class="welcome">
          <div class="welcome-icon">AI</div>
          <h2>你好，我是{{ assistantTitle }}</h2>
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
            <div v-if="message.role === 'ASSISTANT'" class="bubble markdown-bubble">
              <template v-for="(block, blockIndex) in parseMarkdown(message.content)" :key="blockIndex">
                <h4 v-if="block.type === 'heading'" class="md-heading">
                  <template v-for="(segment, segmentIndex) in block.segments" :key="segmentIndex">
                    <strong v-if="segment.type === 'strong'">{{ segment.text }}</strong>
                    <code v-else-if="segment.type === 'code'">{{ segment.text }}</code>
                    <span v-else>{{ segment.text }}</span>
                  </template>
                </h4>
                <ul v-else-if="block.type === 'list' && !block.ordered" class="md-list">
                  <li v-for="(item, itemIndex) in block.items" :key="itemIndex">
                    <template v-for="(segment, segmentIndex) in item" :key="segmentIndex">
                      <strong v-if="segment.type === 'strong'">{{ segment.text }}</strong>
                      <code v-else-if="segment.type === 'code'">{{ segment.text }}</code>
                      <span v-else>{{ segment.text }}</span>
                    </template>
                  </li>
                </ul>
                <ol v-else-if="block.type === 'list'" class="md-list">
                  <li v-for="(item, itemIndex) in block.items" :key="itemIndex">
                    <template v-for="(segment, segmentIndex) in item" :key="segmentIndex">
                      <strong v-if="segment.type === 'strong'">{{ segment.text }}</strong>
                      <code v-else-if="segment.type === 'code'">{{ segment.text }}</code>
                      <span v-else>{{ segment.text }}</span>
                    </template>
                  </li>
                </ol>
                <p v-else class="md-paragraph">
                  <template v-for="(segment, segmentIndex) in block.segments" :key="segmentIndex">
                    <strong v-if="segment.type === 'strong'">{{ segment.text }}</strong>
                    <code v-else-if="segment.type === 'code'">{{ segment.text }}</code>
                    <span v-else>{{ segment.text }}</span>
                  </template>
                </p>
              </template>
            </div>
            <div v-else class="bubble">{{ message.content }}</div>
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
      <el-alert
        v-else-if="knowledgeError"
        type="error"
        :closable="false"
        show-icon
        :title="knowledgeError"
      />
      <el-empty v-else-if="knowledgeSearched" description="未检索到相关资料" />
    </el-dialog>

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
import { getAgentStatus, checkAgentModel, listAgentSessions, createAgentSession, listAgentMessages, chatWithAgent, deleteAgentSession, searchAgentKnowledge, confirmAgentDraft, cancelAgentDraft, listPendingAgentDrafts } from "@/api/module/agent";

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
const modelConfigured = ref(false);
const modelStatusLoading = ref(true);
const statusError = ref("");
const sessionError = ref("");
const messageError = ref("");
const knowledgeError = ref("");
const modelName = ref("");
const checkingModel = ref(false);
const configOpen = ref(false);
const assistantRole = ref("");
const roleLabel = ref("");
const assistantTitle = ref("实验室智能助手");
const assistantDescription = ref("查询实验室、设备、考试、信用和预约审核结果");
const knowledgeOpen = ref(false);
const knowledgeQuery = ref("");
const knowledgeLoading = ref(false);
const knowledgeSearched = ref(false);
const knowledgeResults = ref([]);
const draftLoading = ref(null);

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
      toolCalls: [],
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
      scrollToBottom();
    }).catch(error => {
      messageError.value = "待确认草稿加载失败：" + (error?.message || "请重新进入会话");
    }).finally(() => {
      scrollToBottom();
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

/**
 * 轻量 Markdown 渲染：只支持标题、列表、粗体和行内代码。
 * 返回结构化片段，不使用 v-html，避免模型输出被当作 HTML 执行。
 */
function parseMarkdown(content) {
  const blocks = [];
  let currentList = null;
  const lines = String(content || "").replace(/\r/g, "").split("\n");

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) {
      currentList = null;
      continue;
    }
    const heading = line.match(/^#{1,6}\s+(.+)$/) || line.match(/^\*\*(.+)\*\*$/);
    if (heading) {
      currentList = null;
      blocks.push({ type: "heading", segments: parseInlineMarkdown(heading[1]) });
      continue;
    }
    const unordered = line.match(/^[-*+]\s+(.+)$/);
    const ordered = line.match(/^\d+[.)]\s+(.+)$/);
    if (unordered || ordered) {
      const isOrdered = Boolean(ordered);
      if (!currentList || currentList.ordered !== isOrdered) {
        currentList = { type: "list", ordered: isOrdered, items: [] };
        blocks.push(currentList);
      }
      currentList.items.push(parseInlineMarkdown((ordered || unordered)[1]));
      continue;
    }
    currentList = null;
    blocks.push({ type: "paragraph", segments: parseInlineMarkdown(line) });
  }
  return blocks.length ? blocks : [{ type: "paragraph", segments: parseInlineMarkdown(content) }];
}

function parseInlineMarkdown(text) {
  const segments = [];
  const pattern = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  let cursor = 0;
  let match;
  while ((match = pattern.exec(text)) !== null) {
    if (match.index > cursor) {
      segments.push({ type: "text", text: text.slice(cursor, match.index) });
    }
    const token = match[0];
    if (token.startsWith("**")) {
      segments.push({ type: "strong", text: token.slice(2, -2) });
    } else {
      segments.push({ type: "code", text: token.slice(1, -1) });
    }
    cursor = match.index + token.length;
  }
  if (cursor < text.length) {
    segments.push({ type: "text", text: text.slice(cursor) });
  }
  return segments.length ? segments : [{ type: "text", text: "" }];
}

// 直接调用知识库检索接口，不需要模型参与，便于演示和排查 RAG 召回。
function searchKnowledge() {
  const query = knowledgeQuery.value.trim();
  if (!query) return;
  knowledgeLoading.value = true;
  knowledgeSearched.value = false;
  knowledgeError.value = "";
  knowledgeResults.value = [];
  searchAgentKnowledge(query, 3).then(response => {
    knowledgeResults.value = response.data || [];
  }).catch(error => {
    knowledgeError.value = "知识库检索失败：" + (error?.message || "请检查后端服务");
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
.inline-error { margin-bottom: 8px; }
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
.model-alert { margin: 10px 14px 0; }
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
.markdown-bubble { width: 100%; }
.md-heading { margin: 12px 0 6px; color: #303133; font-size: 15px; line-height: 1.5; }
.md-heading:first-child { margin-top: 0; }
.md-paragraph { margin: 5px 0; line-height: 1.7; }
.md-list { margin: 5px 0; padding-left: 20px; }
.md-list li { margin: 3px 0; line-height: 1.7; }
.markdown-bubble code {
  padding: 1px 4px; border-radius: 3px; background: #f0f2f5;
  color: #c7254e; font-family: Consolas, monospace; font-size: 12px;
}
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
.config-guide { margin-top: 16px; line-height: 1.8; color: #606266; }
.config-guide h4 { margin: 16px 0 6px; color: #303133; }
.config-guide p { margin: 6px 0; }
.config-guide pre {
  margin: 8px 0; padding: 10px 12px; border-radius: 4px;
  background: #f5f7fa; color: #303133; white-space: pre-wrap; word-break: break-all;
}
.config-tip { color: #e6a23c; }
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
