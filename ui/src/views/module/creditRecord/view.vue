<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goList">返回列表</el-button>
      </el-col>
    </el-row>

    <el-alert class="readonly-tip" type="info" :closable="false" show-icon title="信用分流水为只读记录，由业务动作生成，不支持手工新增、修改或删除。" />

    <el-card class="detail-card">
      <el-descriptions title="信用分流水" :column="2" border>
        <el-descriptions-item label="用户ID">{{ detail.userId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="学生姓名">{{ detail.nickName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="学生账号">{{ detail.userName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ detail.deptName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="变动分值">{{ detail.changeScore ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="变动后分值">{{ detail.balanceScore ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="变动原因" :span="2">{{ detail.reason || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup name="CreditRecordView">
import { getCreditRecord } from "@/api/module/creditRecord";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();

const creditRecordListPath = "/module/creditRecord";
const loading = ref(true);
const detail = ref({});

onMounted(loadDetail);

function loadDetail() {
  loading.value = true;
  getCreditRecord(route.params.id).then(response => {
    detail.value = response.data || {};
  }).finally(() => {
    loading.value = false;
  });
}

function goList() {
  router.push(creditRecordListPath);
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>

<style lang="scss" scoped>
.readonly-tip { margin-bottom: 12px; }
.detail-card { margin-top: 4px; }
</style>
