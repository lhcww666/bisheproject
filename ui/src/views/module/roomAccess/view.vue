<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goList">返回列表</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="goAdd" v-hasPermi="['module:roomAccess:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" @click="goEdit" v-hasPermi="['module:roomAccess:edit']">编辑</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" @click="handleDelete" v-hasPermi="['module:roomAccess:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-card class="detail-card">
      <el-descriptions title="实验室准入规则" :column="2" border>
        <el-descriptions-item label="实验室">{{ detail.roomName || detail.roomId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="授权对象">
          <dict-tag :options="module_room_access_subject" :value="detail.subjectType"/>
        </el-descriptions-item>
        <el-descriptions-item label="对象名称">{{ detail.subjectName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="对象ID">{{ detail.subjectId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="生效时间">{{ detail.validFrom ? formatDate(detail.validFrom) : '立即生效' }}</el-descriptions-item>
        <el-descriptions-item label="失效时间">{{ detail.validUntil ? formatDate(detail.validUntil) : '长期有效' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup name="RoomAccessView">
import { getRoomAccess, delRoomAccess } from "@/api/module/roomAccess";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_room_access_subject } = proxy.useDict('module_room_access_subject');

const roomAccessPageBase = "/business/module/roomAccess";
const roomAccessListPath = "/module/roomAccess";
const loading = ref(true);
const detail = ref({});

onMounted(loadDetail);

function loadDetail() {
  loading.value = true;
  getRoomAccess(route.params.id).then(response => {
    detail.value = response.data || {};
  }).finally(() => {
    loading.value = false;
  });
}

function goList() {
  router.push(roomAccessListPath);
}

function goAdd() {
  router.push(roomAccessPageBase + "/add");
}

function goEdit() {
  router.push(roomAccessPageBase + "/edit/" + detail.value.id);
}

function handleDelete() {
  proxy.$modal.confirm('是否确认删除选中的准入规则？').then(() => delRoomAccess(detail.value.id)).then(() => {
    proxy.$modal.msgSuccess("删除成功");
    goList();
  }).catch(() => {});
}

function formatDate(value) {
  return proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}");
}
</script>

<style lang="scss" scoped>
.detail-card { margin-top: 4px; }
</style>
