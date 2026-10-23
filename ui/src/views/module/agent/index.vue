<template>
  <div class="app-container agent-page" :class="{ 'theme-candy': skin === 'candy' }">
    <!-- 左侧：会话列表。所有会话都属于当前登录用户。 -->
    <aside class="session-panel">
      <div class="session-header">
        <div class="session-branding">
          <span class="session-brand-icon">
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
          </span>
          <div>
            <strong>会话记录</strong>
            <small>{{ sessions.length }} 个对话</small>
          </div>
        </div>
        <el-button class="new-session-button" circle plain icon="Plus" title="新建对话" @click="newSession" />
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
          <span class="session-indicator"></span>
          <div class="session-copy">
            <div class="session-title" :title="session.title">{{ session.title }}</div>
            <div class="session-time">{{ formatSessionTime(session.updateTime) }}</div>
          </div>
          <el-button link type="danger" icon="Delete" class="session-delete" @click.stop="removeSession(session)" />
        </div>
        <el-empty v-if="sessions.length === 0" description="还没有对话，点击右上角开始" :image-size="64" />
      </div>
      <div class="session-tip">
        <span class="session-tip-dot"></span>
        查询结果均可追溯
      </div>
    </aside>

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
          <el-button size="small" plain icon="MagicStick" class="skin-toggle" @click="toggleSkin">
            {{ skin === "candy" ? "清爽模式" : "糖果模式" }}
          </el-button>
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

      <div ref="messageBoxRef" class="message-box" v-loading="loading">
        <div v-if="messages.length === 0" class="welcome">
          <div class="welcome-glow"></div>
          <div class="welcome-icon">
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
          </div>
          <span class="welcome-kicker">LABORATORY COPILOT</span>
          <h2>你好，我是{{ assistantTitle }}</h2>
          <p>基于平台真实业务数据回答，复杂操作会调用受控工具完成。</p>
          <div class="welcome-capabilities">
            <span>真实数据</span>
            <span>工具可追溯</span>
            <span>写操作需确认</span>
          </div>
          <div class="example-list">
            <button
              v-for="(example, exampleIndex) in examples"
              :key="example"
              type="button"
              class="example-card"
              @click="useExample(example)"
            >
              <span class="example-index">{{ String(exampleIndex + 1).padStart(2, '0') }}</span>
              <span class="example-text">{{ example }}</span>
              <span class="example-arrow">→</span>
            </button>
          </div>
        </div>

        <div v-for="(message, index) in messages" :key="index" class="message-row" :class="message.role.toLowerCase()">
          <div class="avatar">
            <span v-if="message.role === 'USER'">我</span>
            <svg v-else class="lab-mascot" viewBox="0 0 64 64" aria-hidden="true">
              <path class="mascot-antenna" d="M32 18V9" />
              <circle class="mascot-bulb" cx="32" cy="8" r="3" />
              <rect class="mascot-head" x="12" y="18" width="40" height="32" rx="13" />
              <circle class="mascot-eye" cx="24" cy="33" r="2.8" />
              <circle class="mascot-eye" cx="40" cy="33" r="2.8" />
              <path class="mascot-smile" d="M26 40c3.8 4.2 8.2 4.2 12 0" />
              <circle class="mascot-cheek" cx="18" cy="39" r="2.2" />
              <circle class="mascot-cheek" cx="46" cy="39" r="2.2" />
            </svg>
          </div>
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
              <el-collapse-item :title="'本次查询记录 · ' + message.toolCalls.length + ' 次工具调用'">
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
          <div class="avatar">
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
          </div>
          <div class="bubble typing">正在查询系统数据...</div>
        </div>
      </div>

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
const skin = ref(window.localStorage.getItem("agent-page-skin") === "candy" ? "candy" : "fresh");

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

function handleComposerKeydown(event) {
  if (event.key !== "Enter" || event.isComposing || event.shiftKey) {
    return;
  }
  event.preventDefault();
  send();
}

function scrollToBottom() {
  nextTick(() => {
    if (messageBoxRef.value) {
      messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight;
    }
  });
}

function formatSessionTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const today = new Date();
  const isSameDay = date.toDateString() === today.toDateString();
  if (isSameDay) {
    return "今天 " + proxy.parseTime(value, "{h}:{i}");
  }
  return proxy.parseTime(value, "{m}-{d}");
}

function toggleSkin() {
  skin.value = skin.value === "candy" ? "fresh" : "candy";
  window.localStorage.setItem("agent-page-skin", skin.value);
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
  gap: 0;
  height: calc(100vh - 104px);
  min-height: 620px;
  border: 1px solid #e6ebf3;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 18px 48px rgba(30, 55, 90, 0.08);
  overflow: hidden;
}
.session-panel {
  width: 258px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e9edf5;
  background: linear-gradient(180deg, #f8faff 0%, #f4f7fc 100%);
  overflow: hidden;
}
.session-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 18px 16px;
}
.session-branding {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.session-branding > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.session-branding strong {
  color: #1f2d3d;
  font-size: 15px;
  font-weight: 700;
}
.session-branding small {
  color: #98a4b8;
  font-size: 11px;
}
.session-brand-icon {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 11px;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.25);
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: -0.5px;
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
.new-session-button {
  border-color: #dbe5f5 !important;
  background: rgba(255, 255, 255, 0.8) !important;
  color: #2563eb !important;
  box-shadow: 0 4px 12px rgba(45, 83, 140, 0.08);
}
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 16px;
}
.session-list::-webkit-scrollbar,
.message-box::-webkit-scrollbar {
  width: 6px;
}
.session-list::-webkit-scrollbar-thumb,
.message-box::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #d7dfeb;
}
.inline-error { margin-bottom: 8px; }
.session-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 58px;
  padding: 10px 12px;
  margin-bottom: 7px;
  border: 1px solid transparent;
  border-radius: 13px;
  cursor: pointer;
  color: #566176;
  transition: all 0.18s ease;
}
.session-item:hover {
  background: rgba(255, 255, 255, 0.72);
  color: #25324a;
}
.session-item.active {
  border-color: #dbe8ff;
  background: #fff;
  color: #2563eb;
  box-shadow: 0 8px 20px rgba(50, 82, 132, 0.09);
}
.session-indicator {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #c5cfdd;
  transition: all 0.18s ease;
}
.session-item.active .session-indicator {
  background: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
}
.session-copy {
  min-width: 0;
  flex: 1;
}
.session-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 500;
}
.session-time {
  margin-top: 4px;
  color: #9aa6b8;
  font-size: 10px;
}
.session-delete {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.18s ease;
}
.session-item:hover .session-delete { opacity: 1; }
.session-tip {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 18px 16px;
  padding-top: 13px;
  border-top: 1px solid #e4eaf3;
  color: #99a4b5;
  font-size: 11px;
}
.session-tip-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.11);
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
.message-box {
  flex: 1;
  overflow-y: auto;
  padding: 30px 32px 18px;
  background:
    radial-gradient(circle at 85% 8%, rgba(56, 189, 248, 0.08), transparent 26%),
    linear-gradient(180deg, #f8faff 0%, #f5f7fb 100%);
}
.welcome {
  position: relative;
  max-width: 980px;
  margin: 18px auto 0;
  padding: 48px 42px 34px;
  border: 1px solid rgba(218, 227, 241, 0.9);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 20px 55px rgba(35, 66, 112, 0.08);
  overflow: hidden;
  text-align: center;
}
.welcome-glow {
  position: absolute;
  top: -120px;
  left: 50%;
  width: 340px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.16), transparent 68%);
  transform: translateX(-50%);
  pointer-events: none;
}
.welcome-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 62px; height: 62px; border-radius: 19px;
  background: linear-gradient(145deg, #1d4ed8, #38bdf8);
  box-shadow: 0 14px 30px rgba(37, 99, 235, 0.25);
  color: #fff; font-weight: 800; font-size: 20px; margin-bottom: 13px;
}
.welcome-kicker {
  display: block;
  margin-bottom: 8px;
  color: #6b8bc4;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.8px;
}
.welcome h2 {
  margin: 0 0 9px;
  color: #172033;
  font-size: 24px;
}
.welcome p {
  margin: 0;
  color: #7f8ba0;
  font-size: 13px;
}
.welcome-capabilities {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 18px 0 22px;
}
.welcome-capabilities span {
  padding: 5px 11px;
  border: 1px solid #e9eef6;
  border-radius: 999px;
  background: #f8faff;
  color: #67758d;
  font-size: 11px;
}
.example-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  text-align: left;
}
.example-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 14px;
  border: 1px solid #e7edf7;
  border-radius: 13px;
  background: #fff;
  color: #445168;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.18s ease;
}
.example-card:hover {
  border-color: #bcd3fb;
  box-shadow: 0 10px 24px rgba(41, 83, 148, 0.1);
  color: #2457b8;
  transform: translateY(-1px);
}
.example-index {
  color: #9bb2d5;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.8px;
}
.example-text {
  flex: 1;
  font-size: 12px;
  line-height: 1.5;
}
.example-arrow {
  color: #a8b7cd;
  font-size: 15px;
}
.message-row {
  display: flex;
  gap: 12px;
  max-width: 1040px;
  margin: 0 auto 24px;
}
.message-row.user { flex-direction: row-reverse; }
.avatar {
  width: 36px; height: 36px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: #e8edf5; color: #607089; font-size: 11px; font-weight: 700;
}
.message-row.assistant .avatar {
  background: linear-gradient(145deg, #1d4ed8, #38bdf8);
  color: #fff;
  box-shadow: 0 7px 16px rgba(37, 99, 235, 0.2);
}
.message-content { max-width: 76%; }
.message-row.user .message-content { text-align: right; }
.bubble {
  display: inline-block;
  padding: 13px 16px;
  border: 1px solid #e8edf5;
  border-radius: 5px 17px 17px 17px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(35, 60, 100, 0.06);
  color: #2f3b4f;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
  text-align: left;
}
.message-row.user .bubble {
  border: 0;
  border-radius: 17px 5px 17px 17px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.2);
  color: #fff;
}
.bubble.typing {
  border-style: dashed;
  color: #7e8aa0;
}
.markdown-bubble { width: 100%; }
.md-heading { margin: 13px 0 7px; color: #1f2d3d; font-size: 15px; line-height: 1.5; }
.md-heading:first-child { margin-top: 0; }
.md-paragraph { margin: 5px 0; line-height: 1.7; }
.md-list { margin: 7px 0; padding-left: 20px; }
.md-list li { margin: 3px 0; line-height: 1.7; }
.markdown-bubble code {
  padding: 1px 4px; border-radius: 3px; background: #f0f2f5;
  color: #c7254e; font-family: Consolas, monospace; font-size: 12px;
}
.message-meta {
  margin-top: 6px;
  color: #a6b0c0;
  font-size: 10px;
}
.message-row.user .message-meta { text-align: right; }
.tool-collapse {
  margin-top: 10px;
  padding: 0 12px;
  border: 1px solid #e8edf5;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.78);
  text-align: left;
}
.tool-collapse :deep(.el-collapse),
.tool-collapse :deep(.el-collapse-item__header) {
  border: 0;
  background: transparent;
}
.tool-collapse :deep(.el-collapse-item__header) {
  height: 42px;
  color: #6b778b;
  font-size: 12px;
}
.tool-collapse :deep(.el-collapse-item__wrap) {
  border: 0;
  background: transparent;
}
.tool-item { margin-bottom: 12px; }
.tool-name { display: flex; align-items: center; gap: 8px; color: #909399; font-size: 12px; }
.tool-label { margin: 6px 0 2px; color: #909399; font-size: 12px; }
.tool-item pre {
  margin: 0; padding: 9px; background: #f5f7fa; border-radius: 8px;
  font-size: 12px; white-space: pre-wrap; word-break: break-word; max-height: 160px; overflow: auto;
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
.draft-card {
  margin-top: 10px;
  border-color: #c9ddfb;
  border-radius: 13px;
  background: linear-gradient(180deg, #f7fbff, #f1f7ff);
  text-align: left;
}
.draft-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; font-weight: 600; color: #303133; }
.draft-line { display: flex; gap: 10px; margin: 4px 0; color: #606266; font-size: 13px; }
.draft-line span:first-child { width: 48px; flex-shrink: 0; color: #909399; }
.draft-actions { display: flex; gap: 8px; margin-top: 12px; }
.draft-result { margin-top: 10px; color: #67c23a; font-size: 13px; }

/* 奶油糖果皮肤：保留专业信息层级，只用柔和色彩和小 IP 增加亲和力。 */
.agent-page.theme-candy {
  border-color: #f2dfeb;
  background: #fffaf7;
  box-shadow: 0 18px 52px rgba(159, 94, 132, 0.12);
}
.theme-candy .session-panel {
  border-color: #f1e1eb;
  background:
    radial-gradient(circle at 0 0, rgba(255, 195, 214, 0.2), transparent 34%),
    linear-gradient(180deg, #fff8fb 0%, #fbf5ff 100%);
}
.theme-candy .session-brand-icon,
.theme-candy .assistant-avatar,
.theme-candy .welcome-icon,
.theme-candy .message-row.assistant .avatar,
.theme-candy .composer-submit :deep(.el-button) {
  background: linear-gradient(145deg, #f472b6, #a78bfa);
  box-shadow: 0 10px 24px rgba(206, 100, 166, 0.25);
}
.theme-candy .new-session-button {
  border-color: #f2cfe1 !important;
  color: #db5c98 !important;
}
.theme-candy .session-item.active {
  border-color: #f5cee2;
  color: #d84d91;
  box-shadow: 0 8px 22px rgba(186, 92, 142, 0.1);
}
.theme-candy .session-item.active .session-indicator {
  background: #ec6aa9;
  box-shadow: 0 0 0 4px rgba(236, 106, 169, 0.11);
}
.theme-candy .chat-header {
  border-color: #f5e7ef;
  background: rgba(255, 252, 250, 0.97);
}
.theme-candy .chat-header p,
.theme-candy .session-branding small,
.theme-candy .session-time {
  color: #a88e9d;
}
.theme-candy .model-state.ready {
  background: #ecfdf5;
  color: #3f9a72;
}
.theme-candy .message-box {
  background:
    radial-gradient(circle at 12% 12%, rgba(255, 204, 222, 0.22), transparent 24%),
    radial-gradient(circle at 88% 18%, rgba(210, 190, 255, 0.2), transparent 25%),
    linear-gradient(180deg, #fffafc 0%, #fbf8ff 100%);
}
.theme-candy .welcome {
  border-color: #f2dfeb;
  background: rgba(255, 252, 249, 0.94);
  box-shadow: 0 20px 55px rgba(156, 102, 132, 0.1);
}
.theme-candy .welcome-glow {
  background: radial-gradient(circle, rgba(244, 114, 182, 0.18), transparent 68%);
}
.theme-candy .welcome-kicker {
  color: #c76a9e;
}
.theme-candy .welcome-capabilities span {
  border-color: #f1dfeb;
  background: #fff9fc;
  color: #926e83;
}
.theme-candy .example-card {
  border-color: #f1e3eb;
  color: #63546a;
}
.theme-candy .example-card:hover {
  border-color: #f2b8d5;
  color: #c44786;
  box-shadow: 0 10px 24px rgba(190, 100, 148, 0.1);
}
.theme-candy .message-row.user .bubble {
  background: linear-gradient(135deg, #ec4899, #8b5cf6);
  box-shadow: 0 10px 24px rgba(193, 79, 165, 0.2);
}
.theme-candy .tool-collapse {
  border-color: #f2e2eb;
  background: rgba(255, 252, 250, 0.86);
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
@media (max-width: 900px) {
  .agent-page {
    flex-direction: column;
    height: calc(100vh - 100px);
    min-height: 650px;
    border-radius: 12px;
  }
  .session-panel {
    width: 100%;
    max-height: 260px;
    flex-shrink: 0;
    border-right: 0;
    border-bottom: 1px solid #e9edf5;
  }
  .session-header { padding: 13px 14px 10px; }
  .session-list { display: flex; gap: 8px; padding: 0 12px 12px; overflow-x: auto; overflow-y: hidden; }
  .session-item { min-width: 215px; margin-bottom: 0; }
  .session-tip { display: none; }
  .chat-panel { flex: 1; min-height: 0; }
  .chat-header { align-items: flex-start; flex-direction: column; padding: 15px 16px; }
  .chat-header-actions { width: 100%; justify-content: flex-start; }
  .message-box { padding: 22px 16px 12px; }
  .message-content { max-width: 88%; }
  .welcome { margin-top: 0; padding: 36px 20px 24px; border-radius: 18px; }
  .example-list { grid-template-columns: 1fr; }
  .composer { padding: 12px 14px 15px; }
}
@media (max-width: 600px) {
  .welcome h2 { font-size: 21px; }
  .welcome p { line-height: 1.6; }
  .chat-title-line { flex-wrap: wrap; }
  .chat-header p { white-space: normal; }
  .composer-hint { display: none; }
  .composer-submit { width: 100%; justify-content: flex-end; }
}
</style>
