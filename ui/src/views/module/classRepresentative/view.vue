<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goList">返回列表</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="goAdd" v-hasPermi="['module:classRepresentative:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" @click="goEdit" v-hasPermi="['module:classRepresentative:edit']">编辑</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" @click="handleDelete" v-hasPermi="['module:classRepresentative:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-card class="detail-card">
      <el-descriptions title="班级负责人" :column="2" border>
        <el-descriptions-item label="班级">{{ detail.deptName || detail.deptId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">
          {{ (detail.studentName || detail.studentUserName || detail.studentId || '—') + (detail.studentUserName ? '（' + detail.studentUserName + '）' : '') }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :options="sys_normal_disable" :value="detail.status"/>
        </el-descriptions-item>
        <el-descriptions-item label="生效时间">{{ detail.validFrom ? formatDate(detail.validFrom) : '立即生效' }}</el-descriptions-item>
        <el-descriptions-item label="失效时间">{{ detail.validUntil ? formatDate(detail.validUntil) : '长期有效' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup name="ClassRepresentativeView">
import { getClassRepresentative, delClassRepresentative } from "@/api/module/classRepresentative";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const pageBase = "/business/module/classRepresentative";
const listPath = "/module/classRepresentative";
const loading = ref(true);
const detail = ref({});

onMounted(loadDetail);

function loadDetail() {
  loading.value = true;
  getClassRepresentative(route.params.id).then(response => {
    detail.value = response.data || {};
  }).finally(() => {
    loading.value = false;
  });
}

function goList() {
  router.push(listPath);
}

function goAdd() {
  router.push(pageBase + "/add");
}

function goEdit() {
  router.push(pageBase + "/edit/" + detail.value.id);
}

function handleDelete() {
  proxy.$modal.confirm('是否确认删除选中的班级负责人？').then(() => delClassRepresentative(detail.value.id)).then(() => {
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
