<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goList">返回列表</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="goAdd" v-hasPermi="['module:room:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" @click="goEdit" v-hasPermi="['module:room:edit']">编辑</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" @click="handleDelete" v-hasPermi="['module:room:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-card class="detail-card">
      <el-descriptions title="实验室档案" :column="2" border>
        <el-descriptions-item label="实验室编号">{{ detail.roomCode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="实验室名称">{{ detail.roomName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="实验室类型">
          <dict-tag :options="module_room_type" :value="detail.roomType"/>
        </el-descriptions-item>
        <el-descriptions-item label="归属院系">{{ detail.deptName || detail.deptId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="位置">{{ detail.location || '—' }}</el-descriptions-item>
        <el-descriptions-item label="最大人数">{{ detail.capacity ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="安全等级">{{ detail.safetyLevel || '—' }}</el-descriptions-item>
        <el-descriptions-item label="开放开始时间">{{ detail.openStartTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="开放结束时间">{{ detail.openEndTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :options="module_room_status" :value="detail.status"/>
        </el-descriptions-item>
        <el-descriptions-item label="预约模式">
          <dict-tag :options="module_reservation_mode" :value="detail.reservationMode"/>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="说明" :span="2">{{ detail.description || '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup name="RoomView">
import { getRoom, delRoom } from "@/api/module/room";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_room_type, module_room_status, module_reservation_mode } = proxy.useDict('module_room_type', 'module_room_status', 'module_reservation_mode');

const roomPageBase = "/business/module/room";
const roomListPath = "/module/room";
const loading = ref(true);
const detail = ref({});

onMounted(loadDetail);

function loadDetail() {
  loading.value = true;
  getRoom(route.params.id).then(response => {
    detail.value = response.data || {};
  }).finally(() => {
    loading.value = false;
  });
}

function goList() {
  router.push(roomListPath);
}

function goAdd() {
  router.push(roomPageBase + "/add");
}

function goEdit() {
  router.push(roomPageBase + "/edit/" + detail.value.id);
}

function handleDelete() {
  proxy.$modal.confirm('是否确认删除编号为"' + detail.value.id + '"的数据项？').then(() => delRoom(detail.value.id)).then(() => {
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
