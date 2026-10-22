<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goList">返回列表</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="goAdd" v-hasPermi="['module:device:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" @click="goEdit" v-hasPermi="['module:device:edit']">编辑</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" @click="handleDelete" v-hasPermi="['module:device:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-card class="detail-card">
      <el-descriptions title="设备档案" :column="2" border>
        <el-descriptions-item label="所属实验室">{{ roomLabel }}</el-descriptions-item>
        <el-descriptions-item label="设备编号">{{ detail.deviceCode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detail.deviceName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">
          <dict-tag :options="module_device_type" :value="detail.deviceType"/>
        </el-descriptions-item>
        <el-descriptions-item label="数量">{{ detail.quantity ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="是否需要培训">
          <dict-tag :options="module_yes_no" :value="detail.requiresTraining"/>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :options="module_device_status" :value="detail.status"/>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup name="DeviceView">
import { getDevice, delDevice } from "@/api/module/device";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_device_type, module_device_status, module_yes_no } = proxy.useDict('module_device_type', 'module_device_status', 'module_yes_no');

const devicePageBase = "/business/module/device";
const deviceListPath = "/module/device";
const loading = ref(true);
const detail = ref({});
const roomLabel = computed(() => {
  const label = [detail.value.roomCode, detail.value.roomName].filter(Boolean).join(" · ");
  return label || "—";
});

onMounted(loadDetail);

function loadDetail() {
  loading.value = true;
  getDevice(route.params.id).then(response => {
    detail.value = response.data || {};
  }).finally(() => {
    loading.value = false;
  });
}

function goList() {
  router.push(deviceListPath);
}

function goAdd() {
  router.push(devicePageBase + "/add");
}

function goEdit() {
  router.push(devicePageBase + "/edit/" + detail.value.id);
}

function handleDelete() {
  proxy.$modal.confirm('是否确认删除编号为"' + detail.value.id + '"的数据项？').then(() => delDevice(detail.value.id)).then(() => {
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
