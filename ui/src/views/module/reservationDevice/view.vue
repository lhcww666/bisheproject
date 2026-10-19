<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goList">返回列表</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="goAdd" v-hasPermi="['module:reservationDevice:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" @click="goEdit" v-hasPermi="['module:reservationDevice:edit']">编辑</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" @click="handleDelete" v-hasPermi="['module:reservationDevice:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-card class="detail-card">
      <el-descriptions title="预约设备关联" :column="2" border>
        <el-descriptions-item label="预约ID">{{ detail.reservationId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="设备ID">{{ detail.deviceId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="使用数量">{{ detail.quantity ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup name="ReservationDeviceView">
import { getReservationDevice, delReservationDevice } from "@/api/module/reservationDevice";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();

const reservationDevicePageBase = "/business/module/reservationDevice";
const reservationDeviceListPath = "/module/reservationDevice";
const loading = ref(true);
const detail = ref({});

onMounted(loadDetail);

function loadDetail() {
  loading.value = true;
  getReservationDevice(route.params.id).then(response => {
    detail.value = response.data || {};
  }).finally(() => {
    loading.value = false;
  });
}

function goList() {
  router.push(reservationDeviceListPath);
}

function goAdd() {
  router.push(reservationDevicePageBase + "/add");
}

function goEdit() {
  router.push(reservationDevicePageBase + "/edit/" + detail.value.id);
}

function handleDelete() {
  proxy.$modal.confirm('是否确认删除编号为"' + detail.value.id + '"的数据项？').then(() => delReservationDevice(detail.value.id)).then(() => {
    proxy.$modal.msgSuccess("删除成功");
    goList();
  }).catch(() => {});
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>

<style lang="scss" scoped>
.detail-card { margin-top: 4px; }
</style>
