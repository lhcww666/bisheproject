<template>
  <div ref="messageBoxRef" class="message-box" :class="'theme-' + skin" v-loading="loading">
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
          @click="emit('example', example)"
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
            <el-button
              type="primary"
              size="small"
              :loading="draftLoading === message.draft.id"
              @click="emit('confirm-draft', message)"
            >
              确认提交预约
            </el-button>
            <el-button size="small" @click="emit('cancel-draft', message)">取消草稿</el-button>
          </div>
          <div v-else-if="message.draft.status === 'CONFIRMED'" class="draft-result">
            已提交：{{ message.draft.reservationNo }}，状态 {{ reservationStatusLabel(message.draft.reservationStatus) }}
          </div>
        </el-card>
        <div class="message-meta">{{ formatTime(message.createTime) }}</div>

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
</template>

<script setup name="AgentMessagePanel">
const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  sending: {
    type: Boolean,
    default: false
  },
  examples: {
    type: Array,
    default: () => []
  },
  assistantTitle: {
    type: String,
    default: "实验室智能助手"
  },
  draftLoading: {
    type: [Number, String],
    default: null
  },
  skin: {
    type: String,
    default: "fresh"
  }
});
const emit = defineEmits(["example", "confirm-draft", "cancel-draft"]);
const { proxy } = getCurrentInstance();
const messageBoxRef = ref(null);

watch(
  [() => props.messages.length, () => props.sending],
  () => scrollToBottom(),
  { flush: "post" }
);

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

function formatDateTime(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}") : "";
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
  const labels = {
    "0": "待审核",
    "1": "已通过",
    "2": "已驳回",
    "3": "已取消",
    "4": "已完成",
    "5": "已签到",
    "6": "已爽约"
  };
  return labels[status] || status || "未知";
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
</script>

<style lang="scss" scoped>
.message-box {
  flex: 1;
  overflow-y: auto;
  padding: 30px 32px 18px;
  background:
    radial-gradient(circle at 85% 8%, rgba(56, 189, 248, 0.08), transparent 26%),
    linear-gradient(180deg, #f8faff 0%, #f5f7fb 100%);
}
.message-box::-webkit-scrollbar {
  width: 6px;
}
.message-box::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #d7dfeb;
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  margin-bottom: 13px;
  border-radius: 19px;
  background: linear-gradient(145deg, #1d4ed8, #38bdf8);
  box-shadow: 0 14px 30px rgba(37, 99, 235, 0.25);
  color: #fff;
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
.message-row.user {
  flex-direction: row-reverse;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e8edf5;
  color: #607089;
  font-size: 11px;
  font-weight: 700;
}
.message-row.assistant .avatar {
  background: linear-gradient(145deg, #1d4ed8, #38bdf8);
  color: #fff;
  box-shadow: 0 7px 16px rgba(37, 99, 235, 0.2);
}
.message-content {
  max-width: 76%;
}
.message-row.user .message-content {
  text-align: right;
}
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
.markdown-bubble {
  width: 100%;
}
.md-heading {
  margin: 13px 0 7px;
  color: #1f2d3d;
  font-size: 15px;
  line-height: 1.5;
}
.md-heading:first-child {
  margin-top: 0;
}
.md-paragraph {
  margin: 5px 0;
  line-height: 1.7;
}
.md-list {
  margin: 7px 0;
  padding-left: 20px;
}
.md-list li {
  margin: 3px 0;
  line-height: 1.7;
}
.markdown-bubble code {
  padding: 1px 4px;
  border-radius: 3px;
  background: #f0f2f5;
  color: #c7254e;
  font-family: Consolas, monospace;
  font-size: 12px;
}
.message-meta {
  margin-top: 6px;
  color: #a6b0c0;
  font-size: 10px;
}
.message-row.user .message-meta {
  text-align: right;
}
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
.tool-item {
  margin-bottom: 12px;
}
.tool-name {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 12px;
}
.tool-label {
  margin: 6px 0 2px;
  color: #909399;
  font-size: 12px;
}
.tool-item pre {
  margin: 0;
  padding: 9px;
  border-radius: 8px;
  background: #f5f7fa;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 160px;
  overflow: auto;
}
.draft-card {
  margin-top: 10px;
  border-color: #c9ddfb;
  border-radius: 13px;
  background: linear-gradient(180deg, #f7fbff, #f1f7ff);
  text-align: left;
}
.draft-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-weight: 600;
  color: #303133;
}
.draft-line {
  display: flex;
  gap: 10px;
  margin: 4px 0;
  color: #606266;
  font-size: 13px;
}
.draft-line span:first-child {
  width: 48px;
  flex-shrink: 0;
  color: #909399;
}
.draft-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.draft-result {
  margin-top: 10px;
  color: #67c23a;
  font-size: 13px;
}

.theme-candy.message-box {
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
.theme-candy .welcome-icon {
  background: linear-gradient(145deg, #f472b6, #a78bfa);
  box-shadow: 0 10px 24px rgba(206, 100, 166, 0.25);
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
.theme-candy .message-row.assistant .avatar {
  background: linear-gradient(145deg, #f472b6, #a78bfa);
  box-shadow: 0 10px 24px rgba(206, 100, 166, 0.25);
}
.theme-candy .message-row.user .bubble {
  background: linear-gradient(135deg, #ec4899, #8b5cf6);
  box-shadow: 0 10px 24px rgba(193, 79, 165, 0.2);
}
.theme-candy .tool-collapse {
  border-color: #f2e2eb;
  background: rgba(255, 252, 250, 0.86);
}

.theme-mint.message-box {
  background:
    radial-gradient(circle at 12% 10%, rgba(167, 243, 208, 0.2), transparent 25%),
    radial-gradient(circle at 90% 18%, rgba(153, 246, 228, 0.17), transparent 25%),
    linear-gradient(180deg, #f9fffc 0%, #f4fcf9 100%);
}
.theme-mint .welcome {
  border-color: #dcefe8;
  background: rgba(250, 255, 252, 0.94);
  box-shadow: 0 20px 55px rgba(43, 123, 96, 0.09);
}
.theme-mint .welcome-icon {
  background: linear-gradient(145deg, #059669, #2dd4bf);
  box-shadow: 0 10px 24px rgba(16, 185, 129, 0.24);
}
.theme-mint .welcome-glow {
  background: radial-gradient(circle, rgba(52, 211, 153, 0.18), transparent 68%);
}
.theme-mint .welcome-kicker {
  color: #398c73;
}
.theme-mint .welcome-capabilities span {
  border-color: #dcefe8;
  background: #f7fffc;
  color: #668b80;
}
.theme-mint .example-card {
  border-color: #e1f0eb;
  color: #526b63;
}
.theme-mint .example-card:hover {
  border-color: #9dddc9;
  color: #078a68;
  box-shadow: 0 10px 24px rgba(57, 137, 111, 0.09);
}
.theme-mint .message-row.assistant .avatar {
  background: linear-gradient(145deg, #059669, #2dd4bf);
  box-shadow: 0 10px 24px rgba(16, 185, 129, 0.24);
}
.theme-mint .message-row.user .bubble {
  background: linear-gradient(135deg, #0d9488, #10b981);
  box-shadow: 0 10px 24px rgba(13, 148, 136, 0.19);
}
.theme-mint .tool-collapse {
  border-color: #e0f0ea;
  background: rgba(252, 255, 253, 0.86);
}

@media (max-width: 900px) {
  .message-box {
    padding: 22px 16px 12px;
  }
  .message-content {
    max-width: 88%;
  }
  .welcome {
    margin-top: 0;
    padding: 36px 20px 24px;
    border-radius: 18px;
  }
  .example-list {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 600px) {
  .welcome h2 {
    font-size: 21px;
  }
  .welcome p {
    line-height: 1.6;
  }
}
</style>
