<template>
  <div class="app-container">
    <el-alert
      v-if="route.query.materialId"
      class="material-tip"
      type="info"
      :closable="false"
      show-icon
      title="当前仅展示所选耗材的库存流水。"
    >
      <template #default>
        <el-button link type="primary" @click="clearMaterialFilter">查看全部流水</el-button>
      </template>
    </el-alert>

    <el-form v-show="showSearch" ref="queryRef" :model="queryParams" :inline="true" label-width="auto" @submit.prevent>
      <el-form-item label="变动时间">
        <el-date-picker
          v-model="dateRange"
          style="width: 230px"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item label="变动类型" prop="changeType">
        <el-select v-model="queryParams.changeType" placeholder="请选择变动类型" clearable>
          <el-option
            v-for="dict in module_material_stock_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="变动原因" prop="reasonLike">
        <el-input v-model="queryParams.reasonLike" placeholder="请输入变动原因" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['module:material:stock:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table
      v-loading="loading"
      :data="recordList"
      border
      row-class-name="material-clickable-row"
      @row-click="openView"
    >
      <el-table-column label="耗材编号" prop="materialCode" min-width="145" />
      <el-table-column label="耗材名称" min-width="165">
        <template #default="scope">
          <div>{{ scope.row.materialName || "—" }}</div>
          <small class="secondary-text">{{ scope.row.specification || "" }}</small>
        </template>
      </el-table-column>
      <el-table-column label="实验室" min-width="155">
        <template #default="scope">{{ roomLabel(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="变动类型" width="110" align="center">
        <template #default="scope">
          <dict-tag :options="module_material_stock_type" :value="scope.row.changeType" />
        </template>
      </el-table-column>
      <el-table-column label="变动数量" width="115" align="right">
        <template #default="scope">
          <span :class="signedClass(scope.row.changeQuantity)">{{ signedQuantity(scope.row.changeQuantity) }}</span>
          <span class="unit-text">{{ scope.row.unit }}</span>
        </template>
      </el-table-column>
      <el-table-column label="变动前" width="100" align="right">
        <template #default="scope">{{ quantity(scope.row.beforeQuantity) }}</template>
      </el-table-column>
      <el-table-column label="变动后" width="100" align="right">
        <template #default="scope">{{ quantity(scope.row.afterQuantity) }}</template>
      </el-table-column>
      <el-table-column label="操作人" prop="operatorName" width="110" />
      <el-table-column label="变动原因" prop="reason" min-width="190" show-overflow-tooltip />
      <el-table-column label="变动时间" width="170" align="center">
        <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click.stop="openView(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="MaterialStockList">
import { listMaterialStockRecord } from "@/api/module/materialStockRecord";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_material_stock_type } = proxy.useDict("module_material_stock_type");
const loading = ref(true);
const showSearch = ref(true);
const recordList = ref([]);
const total = ref(0);
const dateRange = ref([]);
const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    materialId: route.query.materialId ? Number(route.query.materialId) : null,
    changeType: null,
    reasonLike: null
  }
});
const { queryParams } = toRefs(data);

onMounted(getList);

function getList() {
  loading.value = true;
  listMaterialStockRecord(proxy.addDateRange(queryParams.value, dateRange.value, "create_time", true)).then(response => {
    recordList.value = response.rows || [];
    total.value = response.total || 0;
  }).finally(() => {
    loading.value = false;
  });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  dateRange.value = [];
  proxy.resetForm("queryRef");
  handleQuery();
}

function clearMaterialFilter() {
  queryParams.value.materialId = null;
  router.replace({ path: route.path });
  handleQuery();
}

function openView(row, column) {
  if (!row?.id || column?.label === "操作") return;
  router.push("/materialStockView/" + row.id);
}

function handleExport() {
  proxy.download("module/material/stock-record/export", { ...queryParams.value }, `耗材库存流水_${Date.now()}.xlsx`);
}

function roomLabel(row) {
  return [row.roomCode, row.roomName].filter(Boolean).join(" · ") || "—";
}

function quantity(value) {
  return Number(value || 0).toFixed(2);
}

function signedQuantity(value) {
  const number = Number(value || 0);
  return (number > 0 ? "+" : "") + number.toFixed(2);
}

function signedClass(value) {
  return Number(value || 0) > 0 ? "positive" : "negative";
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>

<style lang="scss" scoped>
.material-tip { margin-bottom: 12px; }
.secondary-text { color: #909399; }
.unit-text { margin-left: 4px; color: #909399; }
.positive { color: #67c23a; font-weight: 600; }
.negative { color: #f56c6c; font-weight: 600; }
:deep(.material-clickable-row) { cursor: pointer; }
</style>
