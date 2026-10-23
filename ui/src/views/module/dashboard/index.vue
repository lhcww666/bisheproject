<template>
  <div class="app-container dashboard-page">
    <div class="dashboard-header">
      <div>
        <h2>数据看板</h2>
        <p>{{ scopeLabelText }} · 最近 {{ days }} 天</p>
      </div>
      <div class="dashboard-actions">
        <el-radio-group v-model="days" @change="loadData">
          <el-radio-button :label="7">7 天</el-radio-button>
          <el-radio-button :label="30">30 天</el-radio-button>
          <el-radio-button :label="90">90 天</el-radio-button>
        </el-radio-group>
        <el-button icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      class="dashboard-error"
      type="error"
      :closable="false"
      show-icon
      :title="loadError"
    >
      <template #default>
        <el-button link type="primary" @click="loadData">重新加载</el-button>
      </template>
    </el-alert>

    <el-row :gutter="12" class="metric-row" v-loading="loading">
      <el-col v-for="metric in metrics" :key="metric.key" :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card">
          <div class="metric-label">{{ metric.label }}</div>
          <div class="metric-value">{{ metric.value }}<span class="metric-suffix">{{ metric.suffix }}</span></div>
          <div class="metric-hint">{{ metric.hint }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row" v-loading="loading">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <template #header>预约趋势</template>
          <div ref="trendChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never">
          <template #header>预约状态分布</template>
          <div ref="statusChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row" v-loading="loading">
      <el-col v-if="!isStudent" :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header>实验室使用率</template>
          <div ref="roomChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="isStudent ? 24 : 10">
        <el-card shadow="never">
          <template #header>{{ isStudent ? '我的违规趋势' : '违规趋势' }}</template>
          <div ref="violationChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="StatsDashboard">
import * as echarts from 'echarts';
import { getStatsDashboard } from "@/api/module/stats";
import useUserStore from "@/store/modules/user";

const userStore = useUserStore();
const roleKeys = computed(() => userStore.roles || []);
const isStudent = computed(() => roleKeys.value.includes("student") && !roleKeys.value.includes("teacher") && !roleKeys.value.includes("admin"));
const loading = ref(true);
const loadError = ref("");
const days = ref(30);
const dashboard = ref(null);
const trendChartRef = ref(null);
const statusChartRef = ref(null);
const roomChartRef = ref(null);
const violationChartRef = ref(null);
const charts = {};

const scopeLabelText = computed(() => (dashboard.value?.overview?.scopeLabel || (isStudent.value ? "我的" : "全平台")) + "视角");
const metrics = computed(() => {
  const overview = dashboard.value?.overview || {};
  return [
    { key: "total", label: isStudent.value ? "我的预约" : "预约总数", value: overview.totalReservations ?? 0, suffix: "条", hint: "当前数据范围" },
    { key: "today", label: "今日预约", value: overview.todayReservations ?? 0, suffix: "条", hint: "今天开始的预约" },
    { key: "pending", label: "待审核", value: overview.pendingReservations ?? 0, suffix: "条", hint: "需要教师处理" },
    { key: "completed", label: "已完成", value: overview.completedReservations ?? 0, suffix: "条", hint: "已结束的实验" },
    { key: "noShow", label: "爽约率", value: formatPercent(overview.noShowRate), suffix: "%", hint: "爽约 / 总预约" },
    { key: "exam", label: "考试通过率", value: formatPercent(overview.examPassRate), suffix: "%", hint: "有效通过 / 考试记录" }
  ];
});

onMounted(() => {
  loadData();
  window.addEventListener("resize", resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts);
  Object.values(charts).forEach(chart => chart.dispose());
});

function loadData() {
  loading.value = true;
  loadError.value = "";
  getStatsDashboard({ days: days.value }).then(response => {
    dashboard.value = response.data || {};
    nextTick(renderCharts);
  }).catch(error => {
    dashboard.value = null;
    loadError.value = "数据看板加载失败：" + (error?.message || "请检查后端或网络后重试");
    nextTick(renderCharts);
  }).finally(() => {
    loading.value = false;
  });
}

function ensureChart(key, element) {
  if (!element) return null;
  if (!charts[key]) {
    charts[key] = echarts.init(element);
  }
  return charts[key];
}

function renderCharts() {
  const data = dashboard.value || {};
  renderTrend(data.reservationTrend || []);
  renderStatus(data.statusDistribution || []);
  if (!isStudent.value) {
    renderRoomUsage(data.roomUsage || []);
  }
  renderViolation(data.violationTrend || []);
}

function renderTrend(points) {
  const chart = ensureChart("trend", trendChartRef.value);
  if (!chart) return;
  if (!points.length || points.every(item => !item.total && !item.approved && !item.completed)) {
    setEmptyChart(chart, "暂无预约趋势数据");
    return;
  }
  chart.setOption({
    tooltip: { trigger: "axis" },
    legend: { data: ["预约总数", "有效预约", "已完成"], bottom: 0 },
    grid: { left: 40, right: 18, top: 20, bottom: 48 },
    xAxis: { type: "category", data: points.map(item => item.date?.slice(5) || "") },
    yAxis: { type: "value", minInterval: 1 },
    series: [
      { name: "预约总数", type: "line", smooth: true, data: points.map(item => item.total || 0), areaStyle: { opacity: 0.08 } },
      { name: "有效预约", type: "line", smooth: true, data: points.map(item => item.approved || 0) },
      { name: "已完成", type: "line", smooth: true, data: points.map(item => item.completed || 0) }
    ]
  });
}

function renderStatus(items) {
  const chart = ensureChart("status", statusChartRef.value);
  if (!chart) return;
  const data = items.filter(item => item.count > 0);
  if (!data.length) {
    chart.setOption({
      title: { text: "暂无数据", left: "center", top: "45%", textStyle: { color: "#909399", fontSize: 14 } },
      series: []
    }, true);
    return;
  }
  chart.setOption({
    tooltip: { trigger: "item", formatter: "{b}: {c} ({d}%)" },
    legend: { type: "scroll", bottom: 0 },
    series: [{
      name: "预约状态",
      type: "pie",
      radius: ["38%", "68%"],
      center: ["50%", "45%"],
      avoidLabelOverlap: true,
      label: { formatter: "{b}\n{c}" },
      data: data.map(item => ({ name: item.label, value: item.count }))
    }]
  }, true);
}

function renderRoomUsage(items) {
  const chart = ensureChart("room", roomChartRef.value);
  if (!chart) return;
  const top = items.slice(0, 8);
  if (!top.length) {
    setEmptyChart(chart, "暂无实验室使用数据");
    return;
  }
  chart.setOption({
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      formatter: params => {
        const item = top[params[0].dataIndex];
        return `${item.roomName}<br/>使用率：${item.usageRate}%<br/>已用 ${item.bookedHours} / 开放 ${item.openHours} 小时<br/>预约 ${item.reservationCount} 次`;
      }
    },
    grid: { left: 48, right: 18, top: 20, bottom: 52 },
    xAxis: { type: "category", data: top.map(item => item.roomName), axisLabel: { interval: 0, rotate: 20 } },
    yAxis: { type: "value", max: 100, axisLabel: { formatter: "{value}%" } },
    series: [{ name: "使用率", type: "bar", barMaxWidth: 42, data: top.map(item => item.usageRate || 0), itemStyle: { color: "#409eff", borderRadius: [4, 4, 0, 0] } }]
  });
}

function renderViolation(points) {
  const chart = ensureChart("violation", violationChartRef.value);
  if (!chart) return;
  if (!points.length || points.every(item => !item.count && !item.creditChange)) {
    setEmptyChart(chart, "暂无违规记录");
    return;
  }
  chart.setOption({
    tooltip: { trigger: "axis" },
    legend: { data: ["违规次数", "信用变动"], bottom: 0 },
    grid: { left: 42, right: 42, top: 20, bottom: 48 },
    xAxis: { type: "category", data: points.map(item => item.date?.slice(5) || "") },
    yAxis: [
      { type: "value", minInterval: 1, name: "次数" },
      { type: "value", name: "分值" }
    ],
    series: [
      { name: "违规次数", type: "bar", barMaxWidth: 26, data: points.map(item => item.count || 0), itemStyle: { color: "#f56c6c" } },
      { name: "信用变动", type: "line", yAxisIndex: 1, smooth: true, data: points.map(item => item.creditChange || 0), itemStyle: { color: "#e6a23c" } }
    ]
  });
}

function resizeCharts() {
  Object.values(charts).forEach(chart => chart.resize());
}

function formatPercent(value) {
  return Number(value || 0).toFixed(1);
}

function setEmptyChart(chart, text) {
  chart.setOption({
    title: {
      text,
      left: "center",
      top: "45%",
      textStyle: { color: "#909399", fontSize: 14, fontWeight: 400 }
    },
    xAxis: { show: false },
    yAxis: { show: false },
    series: []
  }, true);
}
</script>

<style lang="scss" scoped>
.dashboard-page { padding-bottom: 24px; }
.dashboard-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.dashboard-header h2 { margin: 0 0 6px; font-size: 22px; }
.dashboard-header p { margin: 0; color: #909399; font-size: 13px; }
.dashboard-actions { display: flex; align-items: center; gap: 10px; }
.dashboard-error { margin-bottom: 14px; }
.metric-row { margin-bottom: 4px; }
.metric-card { margin-bottom: 12px; }
.metric-label { color: #909399; font-size: 13px; }
.metric-value { margin-top: 8px; font-size: 28px; font-weight: 600; color: #303133; line-height: 1.1; }
.metric-suffix { margin-left: 3px; font-size: 13px; font-weight: 400; color: #909399; }
.metric-hint { margin-top: 6px; color: #b1b3b8; font-size: 12px; }
.chart-row { margin-bottom: 4px; }
.chart { height: 330px; width: 100%; }
@media (max-width: 768px) {
  .dashboard-header { flex-direction: column; }
  .dashboard-actions { width: 100%; justify-content: space-between; }
}
</style>
