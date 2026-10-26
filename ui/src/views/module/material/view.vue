<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goList">返回列表</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" @click="goEdit" v-hasPermi="['module:material:edit']">编辑</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Sort" @click="adjustVisible = true" v-hasPermi="['module:material:adjust']">调整库存</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="List" @click="openRecords" v-hasPermi="['module:material:stock:list']">库存流水</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" @click="handleDelete" v-hasPermi="['module:material:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-alert
      v-if="lowStock"
      class="material-tip"
      type="error"
      :closable="false"
      show-icon
      :title="`当前库存 ${quantity(detail.stockQuantity)} ${detail.unit || ''}，已达到或低于最低库存 ${quantity(detail.minimumStock)}`"
    />

    <el-card>
      <el-descriptions title="耗材档案" :column="2" border>
        <el-descriptions-item label="耗材编号">{{ detail.materialCode || "—" }}</el-descriptions-item>
        <el-descriptions-item label="耗材名称">{{ detail.materialName || "—" }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detail.specification || "—" }}</el-descriptions-item>
        <el-descriptions-item label="计量单位">{{ detail.unit || "—" }}</el-descriptions-item>
        <el-descriptions-item label="所属实验室">{{ roomLabel }}</el-descriptions-item>
        <el-descriptions-item label="耗材状态">
          <dict-tag :options="module_material_status" :value="detail.status" />
        </el-descriptions-item>
        <el-descriptions-item label="当前库存">
          <span :class="{ 'low-stock': lowStock }">{{ quantity(detail.stockQuantity) }} {{ detail.unit }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="最低库存">{{ quantity(detail.minimumStock) }} {{ detail.unit }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detail.createBy || "—" }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新人">{{ detail.updateBy || "—" }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDate(detail.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || "—" }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <stock-adjust v-model="adjustVisible" :material="detail" @success="loadDetail" />
  </div>
</template>

<script setup name="MaterialView">
import { delMaterial, getMaterial } from "@/api/module/material";
import StockAdjust from "./components/stockAdjust.vue";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_material_status } = proxy.useDict("module_material_status");

const pageBase = "/business/module/material";
const loading = ref(true);
const detail = ref({});
const adjustVisible = ref(false);
const roomLabel = computed(() => [detail.value.roomCode, detail.value.roomName].filter(Boolean).join(" · ") || "—");
const lowStock = computed(() => Number(detail.value.stockQuantity || 0) <= Number(detail.value.minimumStock || 0));

onMounted(loadDetail);

function loadDetail() {
  loading.value = true;
  getMaterial(route.params.id).then(response => {
    detail.value = response.data || {};
  }).finally(() => {
    loading.value = false;
  });
}

function goList() {
  router.push("/module/material");
}

function goEdit() {
  router.push(pageBase + "/edit/" + detail.value.id);
}

function openRecords() {
  router.push({ path: "/module/material-stock", query: { materialId: detail.value.id } });
}

function handleDelete() {
  proxy.$modal.confirm(`是否确认删除耗材“${detail.value.materialName}”？`).then(() => delMaterial(detail.value.id)).then(() => {
    proxy.$modal.msgSuccess("删除成功");
    goList();
  }).catch(() => {});
}

function quantity(value) {
  return Number(value || 0).toFixed(2);
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>

<style lang="scss" scoped>
.material-tip { margin-bottom: 12px; }
.low-stock { color: #f56c6c; font-weight: 600; }
</style>
