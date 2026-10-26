<template>
  <div class="app-container">
    <el-alert
      class="material-tip"
      type="info"
      :closable="false"
      show-icon
      title="耗材库存只允许通过“调整库存”变更，每次变化都会生成库存流水。"
    />

    <el-form
      v-show="showSearch"
      ref="queryRef"
      :model="queryParams"
      :inline="true"
      label-width="auto"
      @submit.prevent
    >
      <el-form-item label="创建时间">
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
      <el-form-item label="耗材编号" prop="materialCodeLike">
        <el-input v-model="queryParams.materialCodeLike" placeholder="请输入耗材编号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="耗材名称" prop="materialNameLike">
        <el-input v-model="queryParams.materialNameLike" placeholder="请输入耗材名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="实验室" prop="roomId">
        <el-select v-model="queryParams.roomId" placeholder="请选择实验室" clearable filterable style="width: 220px">
          <el-option
            v-for="room in roomOptions"
            :key="room.id"
            :label="roomLabel(room)"
            :value="room.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option
            v-for="dict in module_material_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="goAdd" v-hasPermi="['module:material:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="editSelected" v-hasPermi="['module:material:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Sort" :disabled="single" @click="adjustSelected" v-hasPermi="['module:material:adjust']">调整库存</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['module:material:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['module:material:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table
      v-loading="loading"
      :data="materialList"
      border
      row-class-name="material-clickable-row"
      @row-click="openView"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="耗材编号" prop="materialCode" min-width="145" />
      <el-table-column label="耗材名称" prop="materialName" min-width="150" show-overflow-tooltip />
      <el-table-column label="规格型号" prop="specification" min-width="130" show-overflow-tooltip />
      <el-table-column label="实验室" min-width="165">
        <template #default="scope">{{ roomLabel(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="当前库存" width="110" align="right">
        <template #default="scope">
          <span :class="{ 'low-stock': isLowStock(scope.row) }">{{ quantity(scope.row.stockQuantity) }}</span>
          <span class="unit-text">{{ scope.row.unit }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最低库存" width="110" align="right">
        <template #default="scope">{{ quantity(scope.row.minimumStock) }}</template>
      </el-table-column>
      <el-table-column label="库存状态" width="105" align="center">
        <template #default="scope">
          <el-tag :type="isLowStock(scope.row) ? 'danger' : 'success'">
            {{ isLowStock(scope.row) ? "低库存" : "正常" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="耗材状态" width="100" align="center">
        <template #default="scope">
          <dict-tag :options="module_material_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="170" align="center">
        <template #default="scope">{{ formatDate(scope.row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click.stop="goEdit(scope.row)" v-hasPermi="['module:material:edit']">修改</el-button>
          <el-button link type="warning" icon="Sort" @click.stop="openAdjust(scope.row)" v-hasPermi="['module:material:adjust']">调库存</el-button>
          <el-button link type="primary" icon="List" @click.stop="openRecords(scope.row)" v-hasPermi="['module:material:stock:list']">流水</el-button>
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

    <stock-adjust
      v-model="adjustVisible"
      :material="adjustMaterial"
      @success="handleAdjustSuccess"
    />
  </div>
</template>

<script setup name="MaterialList">
import { delMaterial, listMaterial, listMaterialRoomOptions } from "@/api/module/material";
import StockAdjust from "./components/stockAdjust.vue";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_material_status } = proxy.useDict("module_material_status");

const pageBase = "/business/module/material";
const menuPath = "/module/material";
const loading = ref(true);
const showSearch = ref(true);
const materialList = ref([]);
const roomOptions = ref([]);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const dateRange = ref([]);
const adjustVisible = ref(false);
const adjustMaterial = ref({});
const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    materialCodeLike: null,
    materialNameLike: null,
    roomId: route.query.roomId ? Number(route.query.roomId) : null,
    status: null
  }
});
const { queryParams } = toRefs(data);

onMounted(() => {
  loadRooms();
  getList();
});

function loadRooms() {
  listMaterialRoomOptions().then(response => {
    roomOptions.value = response.data || [];
  }).catch(() => {
    roomOptions.value = [];
  });
}

function getList() {
  loading.value = true;
  listMaterial(proxy.addDateRange(queryParams.value, dateRange.value, "create_time", true)).then(response => {
    materialList.value = response.rows || [];
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

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length !== 1;
  multiple.value = selection.length === 0;
}

function goAdd() {
  router.push(pageBase + "/add");
}

function goEdit(row) {
  const id = row?.id || ids.value[0];
  if (!id) {
    proxy.$modal.msgWarning("请先选择一条耗材");
    return;
  }
  router.push(pageBase + "/edit/" + id);
}

function editSelected() {
  goEdit(materialList.value.find(item => item.id === ids.value[0]));
}

function openAdjust(row) {
  adjustMaterial.value = { ...row };
  adjustVisible.value = true;
}

function adjustSelected() {
  openAdjust(materialList.value.find(item => item.id === ids.value[0]));
}

function handleAdjustSuccess() {
  getList();
}

function handleDelete(row) {
  const deleteIds = row?.id || ids.value;
  if (!deleteIds || (Array.isArray(deleteIds) && deleteIds.length === 0)) {
    proxy.$modal.msgWarning("请先选择要删除的耗材");
    return;
  }
  proxy.$modal.confirm("耗材删除后无法恢复，是否确认删除？").then(() => delMaterial(deleteIds)).then(() => {
    proxy.$modal.msgSuccess("删除成功");
    getList();
  }).catch(() => {});
}

function handleExport() {
  proxy.download("module/material/export", { ...queryParams.value }, `耗材库存_${Date.now()}.xlsx`);
}

function openView(row, column) {
  if (!row?.id || column?.type === "selection" || column?.label === "操作") return;
  router.push(pageBase + "/view/" + row.id);
}

function openRecords(row) {
  router.push({ path: "/module/material-stock", query: { materialId: row.id } });
}

function roomLabel(row) {
  return [row.roomCode, row.roomName].filter(Boolean).join(" · ") || "—";
}

function quantity(value) {
  return Number(value || 0).toFixed(2);
}

function isLowStock(row) {
  return Number(row.stockQuantity || 0) <= Number(row.minimumStock || 0);
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>

<style lang="scss" scoped>
.material-tip { margin-bottom: 12px; }
.low-stock { color: #f56c6c; font-weight: 600; }
.unit-text { margin-left: 4px; color: #909399; }
:deep(.material-clickable-row) { cursor: pointer; }
</style>
