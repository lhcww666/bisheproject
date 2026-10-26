<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goBack">返回流水</el-button>
      </el-col>
    </el-row>

    <el-card>
      <el-descriptions title="库存流水详情" :column="2" border>
        <el-descriptions-item label="耗材编号">{{ detail.materialCode || "—" }}</el-descriptions-item>
        <el-descriptions-item label="耗材名称">{{ detail.materialName || "—" }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detail.specification || "—" }}</el-descriptions-item>
        <el-descriptions-item label="所属实验室">{{ roomLabel }}</el-descriptions-item>
        <el-descriptions-item label="变动类型">
          <dict-tag :options="module_material_stock_type" :value="detail.changeType" />
        </el-descriptions-item>
        <el-descriptions-item label="变动数量">
          <span :class="Number(detail.changeQuantity || 0) > 0 ? 'positive' : 'negative'">
            {{ signedQuantity(detail.changeQuantity) }} {{ detail.unit }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="变动前库存">{{ quantity(detail.beforeQuantity) }} {{ detail.unit }}</el-descriptions-item>
        <el-descriptions-item label="变动后库存">{{ quantity(detail.afterQuantity) }} {{ detail.unit }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operatorName || "—" }}</el-descriptions-item>
        <el-descriptions-item label="变动时间">{{ formatDate(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="关联领用申请">{{ detail.applyId || "—" }}</el-descriptions-item>
        <el-descriptions-item label="关联预约">{{ detail.reservationId || "—" }}</el-descriptions-item>
        <el-descriptions-item label="变动原因" :span="2">{{ detail.reason || "—" }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup name="MaterialStockView">
import { getMaterialStockRecord } from "@/api/module/materialStockRecord";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_material_stock_type } = proxy.useDict("module_material_stock_type");
const loading = ref(true);
const detail = ref({});
const roomLabel = computed(() => [detail.value.roomCode, detail.value.roomName].filter(Boolean).join(" · ") || "—");

onMounted(() => {
  getMaterialStockRecord(route.params.id).then(response => {
    detail.value = response.data || {};
  }).finally(() => {
    loading.value = false;
  });
});

function goBack() {
  router.push({
    path: "/module/material-stock",
    query: detail.value.materialId ? { materialId: detail.value.materialId } : {}
  });
}

function quantity(value) {
  return Number(value || 0).toFixed(2);
}

function signedQuantity(value) {
  const number = Number(value || 0);
  return (number > 0 ? "+" : "") + number.toFixed(2);
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>

<style scoped>
.positive { color: #67c23a; font-weight: 600; }
.negative { color: #f56c6c; font-weight: 600; }
</style>
