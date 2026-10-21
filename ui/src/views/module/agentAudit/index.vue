<template>
  <div class="app-container">
    <el-alert
      class="audit-tip"
      type="info"
      :closable="false"
      show-icon
      title="Agent 审计只读：查看会话、消息和工具调用，便于追溯模型行为。"
    />

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="会话审计" name="sessions">
        <el-form :model="sessionQuery" inline label-width="auto">
          <el-form-item label="用户ID">
            <el-input v-model="sessionQuery.userId" placeholder="用户ID" clearable @keyup.enter="loadSessions" />
          </el-form-item>
          <el-form-item label="会话标题">
            <el-input v-model="sessionQuery.titleLike" placeholder="标题关键字" clearable @keyup.enter="loadSessions" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadSessions">搜索</el-button>
            <el-button icon="Refresh" @click="resetSessionQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="sessionLoading" :data="sessionList" border @row-click="showMessages">
          <el-table-column label="会话ID" prop="id" width="90" align="center" />
          <el-table-column label="用户" align="center" min-width="130">
            <template #default="scope">{{ scope.row.userName || scope.row.userId }}</template>
          </el-table-column>
          <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip />
          <el-table-column label="消息数" prop="messageCount" width="90" align="center" />
          <el-table-column label="工具调用" prop="toolCallCount" width="100" align="center" />
          <el-table-column label="更新时间" width="170" align="center">
            <template #default="scope">{{ formatDate(scope.row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="scope"><el-button link type="primary" @click.stop="showMessages(scope.row)">查看消息</el-button></template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="sessionTotal > 0"
          :total="sessionTotal"
          v-model:page="sessionQuery.pageNum"
          v-model:limit="sessionQuery.pageSize"
          @pagination="loadSessions"
        />
      </el-tab-pane>

      <el-tab-pane label="工具调用" name="tools">
        <el-form :model="toolQuery" inline label-width="auto">
          <el-form-item label="用户ID">
            <el-input v-model="toolQuery.userId" placeholder="用户ID" clearable @keyup.enter="loadToolCalls" />
          </el-form-item>
          <el-form-item label="工具名">
            <el-input v-model="toolQuery.toolName" placeholder="例如 queryMyCredit" clearable @keyup.enter="loadToolCalls" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="toolQuery.success" clearable placeholder="全部">
              <el-option label="成功" value="1" />
              <el-option label="失败" value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadToolCalls">搜索</el-button>
            <el-button icon="Refresh" @click="resetToolQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="toolLoading" :data="toolList" border>
          <el-table-column type="expand">
            <template #default="scope">
              <div class="tool-detail">
                <div><strong>参数：</strong><pre>{{ scope.row.arguments || '—' }}</pre></div>
                <div><strong>返回：</strong><pre>{{ scope.row.result || '—' }}</pre></div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="用户" align="center" min-width="120">
            <template #default="scope">{{ scope.row.userName || scope.row.userId || '系统' }}</template>
          </el-table-column>
          <el-table-column label="工具" prop="toolName" min-width="180" />
          <el-table-column label="结果" align="center" width="90">
            <template #default="scope">
              <el-tag :type="scope.row.success === '1' ? 'success' : 'danger'">
                {{ scope.row.success === '1' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="耗时" align="center" width="90">
            <template #default="scope">{{ scope.row.costMs || 0 }} ms</template>
          </el-table-column>
          <el-table-column label="调用时间" align="center" width="170">
            <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="toolTotal > 0"
          :total="toolTotal"
          v-model:page="toolQuery.pageNum"
          v-model:limit="toolQuery.pageSize"
          @pagination="loadToolCalls"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="messageOpen" title="会话消息审计" width="760px" append-to-body destroy-on-close>
      <el-timeline v-if="messageList.length">
        <el-timeline-item v-for="message in messageList" :key="message.id" :timestamp="formatDate(message.createTime)" placement="top">
          <el-card shadow="never">
            <div class="message-role">
              <el-tag :type="message.role === 'USER' ? 'primary' : 'success'">{{ message.role === 'USER' ? '用户' : '助手' }}</el-tag>
            </div>
            <div class="message-text">{{ message.content }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无消息" />
    </el-dialog>
  </div>
</template>

<script setup name="AgentAudit">
import { listAgentAuditSessions, getAgentAuditMessages, listAgentAuditToolCalls } from "@/api/module/agentAudit";

const { proxy } = getCurrentInstance();
const activeTab = ref("sessions");
const sessionList = ref([]);
const sessionTotal = ref(0);
const sessionLoading = ref(false);
const toolList = ref([]);
const toolTotal = ref(0);
const toolLoading = ref(false);
const messageOpen = ref(false);
const messageList = ref([]);
const sessionQuery = reactive({ pageNum: 1, pageSize: 10, userId: null, titleLike: null });
const toolQuery = reactive({ pageNum: 1, pageSize: 10, userId: null, toolName: null, success: null });

onMounted(loadSessions);

function loadSessions() {
  sessionLoading.value = true;
  listAgentAuditSessions(sessionQuery).then(response => {
    sessionList.value = response.rows;
    sessionTotal.value = response.total;
  }).finally(() => {
    sessionLoading.value = false;
  });
}

function loadToolCalls() {
  toolLoading.value = true;
  listAgentAuditToolCalls(toolQuery).then(response => {
    toolList.value = response.rows;
    toolTotal.value = response.total;
  }).finally(() => {
    toolLoading.value = false;
  });
}

function handleTabChange(name) {
  if (name === "tools") loadToolCalls();
}

function showMessages(row) {
  getAgentAuditMessages(row.id).then(response => {
    messageList.value = response.data || [];
    messageOpen.value = true;
  });
}

function resetSessionQuery() {
  Object.assign(sessionQuery, { pageNum: 1, pageSize: 10, userId: null, titleLike: null });
  loadSessions();
}

function resetToolQuery() {
  Object.assign(toolQuery, { pageNum: 1, pageSize: 10, userId: null, toolName: null, success: null });
  loadToolCalls();
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>

<style lang="scss" scoped>
.audit-tip { margin-bottom: 12px; }
.tool-detail { padding: 4px 18px 12px; }
.tool-detail pre { margin: 6px 0 12px; padding: 8px; background: #f5f7fa; white-space: pre-wrap; word-break: break-word; max-height: 180px; overflow: auto; }
.message-role { margin-bottom: 8px; }
.message-text { white-space: pre-wrap; line-height: 1.7; color: #303133; }
</style>
