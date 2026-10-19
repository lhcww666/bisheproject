<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goList">返回列表</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="goAdd" v-hasPermi="['module:safetyExamRecord:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" @click="goEdit" v-hasPermi="['module:safetyExamRecord:edit']">编辑</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" @click="handleDelete" v-hasPermi="['module:safetyExamRecord:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-card class="detail-card">
      <el-descriptions title="安全考试记录" :column="2" border>
        <el-descriptions-item label="学生ID">{{ detail.studentId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="学生姓名">{{ detail.nickName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="学生账号">{{ detail.userName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ detail.deptName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="考试成绩">{{ detail.score ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="是否通过">
          <dict-tag :options="module_yes_no" :value="detail.passed"/>
        </el-descriptions-item>
        <el-descriptions-item label="考试时间">{{ formatDate(detail.examTime) }}</el-descriptions-item>
        <el-descriptions-item label="有效期至">{{ formatDate(detail.validUntil) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(detail.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup name="SafetyExamRecordView">
import { getSafetyExamRecord, delSafetyExamRecord } from "@/api/module/safetyExamRecord";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_yes_no } = proxy.useDict('module_yes_no');

const safetyExamRecordPageBase = "/business/module/safetyExamRecord";
const safetyExamRecordListPath = "/module/safetyExamRecord";
const loading = ref(true);
const detail = ref({});

onMounted(loadDetail);

function loadDetail() {
  loading.value = true;
  getSafetyExamRecord(route.params.id).then(response => {
    detail.value = response.data || {};
  }).finally(() => {
    loading.value = false;
  });
}

function goList() {
  router.push(safetyExamRecordListPath);
}

function goAdd() {
  router.push(safetyExamRecordPageBase + "/add");
}

function goEdit() {
  router.push(safetyExamRecordPageBase + "/edit/" + detail.value.id);
}

function handleDelete() {
  proxy.$modal.confirm('是否确认删除编号为"' + detail.value.id + '"的数据项？').then(() => delSafetyExamRecord(detail.value.id)).then(() => {
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
