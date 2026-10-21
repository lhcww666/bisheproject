<template>
  <div class="app-container eval-page">
    <div class="eval-header">
      <div>
        <h2>Agent 离线评测</h2>
        <p>固定用例回归意图路由、工具选择和 RAG 检索，不消耗模型额度。</p>
      </div>
      <div class="eval-actions">
        <el-button icon="Refresh" :loading="historyLoading" @click="loadRuns">刷新</el-button>
        <el-button type="primary" icon="VideoPlay" :loading="runLoading" @click="runEvaluation">运行离线评测</el-button>
      </div>
    </div>

    <el-alert
      class="eval-tip"
      type="info"
      :closable="false"
      show-icon
      title="离线模式不调用大模型，结果可重复；接入真实模型后可扩展为 LLM 实际工具调用评测。"
    />

    <el-empty v-if="!currentRun && !detailLoading" description="还没有评测批次，点击“运行离线评测”建立第一条基线" />

    <div v-loading="detailLoading">
      <template v-if="currentRun">
        <el-card shadow="never" class="run-card">
          <template #header>
            <div class="run-title">
              <div>
                <span class="run-no">{{ currentRun.runNo }}</span>
                <el-tag size="small" type="info">{{ currentRun.mode }}</el-tag>
              </div>
              <span class="run-meta">{{ formatDate(currentRun.createTime) }} · {{ currentRun.createBy || '系统' }}</span>
            </div>
          </template>

          <el-row :gutter="12">
            <el-col v-for="metric in metrics" :key="metric.key" :xs="12" :sm="8" :md="4">
              <el-card shadow="never" class="metric-card">
                <div class="metric-label">{{ metric.label }}</div>
                <div class="metric-value" :class="metric.tone">
                  {{ metric.value }}<span class="metric-suffix">{{ metric.suffix }}</span>
                </div>
                <div class="metric-hint">{{ metric.hint }}</div>
              </el-card>
            </el-col>
          </el-row>

          <el-table :data="currentRun.results || []" border row-key="id" class="result-table">
            <el-table-column type="expand">
              <template #default="scope">
                <div class="result-detail">
                  <div><strong>实际输出：</strong>{{ scope.row.actualAnswer || '—' }}</div>
                  <div><strong>错误信息：</strong>{{ scope.row.errorMessage || '—' }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="用例" prop="caseId" width="80" align="center" />
            <el-table-column label="分类" width="120" align="center">
              <template #default="scope">
                <el-tag size="small" :type="categoryTag(scope.row.category)">{{ categoryLabel(scope.row.category) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="问题" prop="question" min-width="240" show-overflow-tooltip />
            <el-table-column label="意图（期望 / 实际）" min-width="190">
              <template #default="scope">
                <span>{{ scope.row.expectedIntent }}</span>
                <span class="separator">/</span>
                <span :class="{ failed: scope.row.expectedIntent !== scope.row.actualIntent }">{{ scope.row.actualIntent }}</span>
              </template>
            </el-table-column>
            <el-table-column label="工具（期望 / 实际）" min-width="290">
              <template #default="scope">
                <span>{{ scope.row.expectedTool }}</span>
                <span class="separator">/</span>
                <span :class="{ failed: scope.row.expectedTool !== scope.row.actualTool }">{{ scope.row.actualTool }}</span>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="90" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.passed === '1' ? 'success' : 'danger'">
                  {{ scope.row.passed === '1' ? '通过' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="90" align="center">
              <template #default="scope">{{ scope.row.durationMs || 0 }} ms</template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </div>

    <el-card shadow="never" class="history-card">
      <template #header>
        <div class="section-title">
          <span>最近评测批次</span>
          <span class="section-hint">最多展示最近 20 次</span>
        </div>
      </template>
      <el-table
        v-loading="historyLoading"
        :data="runs"
        border
        row-key="id"
        highlight-current-row
        @row-click="selectRun"
      >
        <el-table-column label="批次号" prop="runNo" min-width="210" />
        <el-table-column label="模式" prop="mode" width="90" align="center" />
        <el-table-column label="通过" width="110" align="center">
          <template #default="scope">{{ scope.row.passed }} / {{ scope.row.total }}</template>
        </el-table-column>
        <el-table-column label="意图准确率" width="120" align="center">
          <template #default="scope">{{ formatRate(scope.row.intentAccuracy) }}%</template>
        </el-table-column>
        <el-table-column label="工具准确率" width="120" align="center">
          <template #default="scope">{{ formatRate(scope.row.toolAccuracy) }}%</template>
        </el-table-column>
        <el-table-column label="RAG 命中率" width="120" align="center">
          <template #default="scope">{{ formatRate(scope.row.ragHitRate) }}%</template>
        </el-table-column>
        <el-table-column label="综合通过率" width="120" align="center">
          <template #default="scope">
            <el-tag :type="Number(scope.row.passRate) === 100 ? 'success' : 'warning'">
              {{ formatRate(scope.row.passRate) }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="执行人" prop="createBy" width="110" align="center" />
        <el-table-column label="执行时间" width="170" align="center">
          <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="scope">
            <el-button link type="primary" @click.stop="selectRun(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="AgentEval">
import { getAgentEvalRun, listAgentEvalCases, listAgentEvalRuns, runAgentEval } from "@/api/module/agentEval";

const { proxy } = getCurrentInstance();
const runs = ref([]);
const currentRun = ref(null);
const historyLoading = ref(false);
const detailLoading = ref(false);
const runLoading = ref(false);
const caseCount = ref(0);

const metrics = computed(() => {
  const run = currentRun.value || {};
  return [
    { key: "passed", label: "通过用例", value: run.passed ?? 0, suffix: `/ ${run.total ?? 0}`, hint: "逐条综合判定", tone: "primary" },
    { key: "intent", label: "意图准确率", value: formatRate(run.intentAccuracy), suffix: "%", hint: "期望意图命中", tone: rateTone(run.intentAccuracy) },
    { key: "tool", label: "工具路由准确率", value: formatRate(run.toolAccuracy), suffix: "%", hint: "工具选择与可用性", tone: rateTone(run.toolAccuracy) },
    { key: "rag", label: "RAG 命中率", value: formatRate(run.ragHitRate), suffix: "%", hint: "安全知识检索命中", tone: rateTone(run.ragHitRate) },
    { key: "pass", label: "综合通过率", value: formatRate(run.passRate), suffix: "%", hint: "意图、工具、关键词", tone: rateTone(run.passRate) }
  ];
});

onMounted(() => {
  loadCaseCount();
  loadRuns();
});

function loadCaseCount() {
  return listAgentEvalCases().then(response => {
    caseCount.value = (response.data || []).length;
  });
}

function loadRuns() {
  historyLoading.value = true;
  return listAgentEvalRuns().then(response => {
    runs.value = response.data || [];
    if (runs.value.length) {
      const selected = runs.value.find(item => item.id === currentRun.value?.id) || runs.value[0];
      return loadRun(selected.id);
    }
    currentRun.value = null;
  }).finally(() => {
    historyLoading.value = false;
  });
}

function loadRun(runId) {
  detailLoading.value = true;
  return getAgentEvalRun(runId).then(response => {
    currentRun.value = response.data || null;
  }).finally(() => {
    detailLoading.value = false;
  });
}

function selectRun(row) {
  loadRun(row.id);
}

async function runEvaluation() {
  if (caseCount.value === 0) {
    await loadCaseCount();
  }
  const totalText = caseCount.value > 0 ? caseCount.value + " 条" : "全部";
  proxy.$modal.confirm("本次会写入一条评测批次和 " + totalText + "明细，确认开始吗？").then(() => {
    runLoading.value = true;
    return runAgentEval();
  }).then(response => {
    currentRun.value = response.data || null;
    proxy.$modal.msgSuccess("离线评测完成");
    return loadRuns();
  }).catch(() => {}).finally(() => {
    runLoading.value = false;
  });
}

function formatRate(value) {
  return Number(value || 0).toFixed(2);
}

function rateTone(value) {
  const rate = Number(value || 0);
  if (rate === 100) return "success";
  if (rate >= 80) return "warning";
  return "danger";
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}

function categoryLabel(value) {
  return {
    EXAM: "安全考试",
    CREDIT: "信用分",
    MY_RESERVATION: "我的预约",
    REVIEW: "审核解释",
    ROOMS: "实验室查询",
    DEVICES: "设备查询",
    SAFETY: "安全知识",
    SAFETY_HELDOUT: "安全泛化",
    OUT_OF_SCOPE: "非业务问题",
    DRAFT: "预约草稿"
  }[value] || value;
}

function categoryTag(value) {
  return {
    EXAM: "success",
    CREDIT: "warning",
    MY_RESERVATION: "info",
    REVIEW: "danger",
    ROOMS: "primary",
    DEVICES: "success",
    SAFETY: "warning",
    SAFETY_HELDOUT: "warning",
    OUT_OF_SCOPE: "info",
    DRAFT: "primary"
  }[value] || "info";
}
</script>

<style lang="scss" scoped>
.eval-page { padding-bottom: 24px; }
.eval-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 14px; }
.eval-header h2 { margin: 0 0 6px; font-size: 22px; }
.eval-header p { margin: 0; color: #909399; font-size: 13px; }
.eval-actions { display: flex; gap: 8px; }
.eval-tip { margin-bottom: 14px; }
.run-card { margin-bottom: 14px; }
.run-title { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.run-title > div { display: flex; align-items: center; gap: 8px; }
.run-no { font-weight: 600; font-size: 16px; }
.run-meta, .section-hint { color: #909399; font-size: 13px; }
.metric-card { margin-bottom: 4px; min-height: 118px; }
.metric-label { color: #909399; font-size: 13px; }
.metric-value { margin-top: 8px; font-size: 28px; font-weight: 600; line-height: 1.1; color: #303133; }
.metric-value.primary { color: #409eff; }
.metric-value.success { color: #67c23a; }
.metric-value.warning { color: #e6a23c; }
.metric-value.danger { color: #f56c6c; }
.metric-suffix { margin-left: 4px; font-size: 13px; font-weight: 400; color: #909399; }
.metric-hint { margin-top: 7px; color: #b1b3b8; font-size: 12px; }
.result-table { margin-top: 16px; }
.separator { margin: 0 6px; color: #c0c4cc; }
.failed { color: #f56c6c; font-weight: 600; }
.result-detail { padding: 8px 24px 14px; line-height: 1.8; color: #606266; word-break: break-word; }
.history-card { margin-top: 14px; }
.section-title { display: flex; align-items: center; justify-content: space-between; }
@media (max-width: 768px) {
  .eval-header { flex-direction: column; }
  .eval-actions { width: 100%; justify-content: space-between; }
  .run-title { align-items: flex-start; flex-direction: column; }
}
</style>
