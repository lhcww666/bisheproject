<template>
  <div class="app-container" v-loading="loading">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>{{ pageTitle }}</span>
          <el-button icon="Back" @click="goBack">返回</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
        <el-form-item label="学生ID" prop="studentId">
          <el-input v-model="form.studentId" placeholder="请输入学生ID（sys_user.user_id）" />
        </el-form-item>
        <el-form-item label="考试成绩" prop="score">
          <el-input v-model="form.score" placeholder="请输入考试成绩" />
        </el-form-item>
        <el-form-item label="是否通过" prop="passed">
          <el-radio-group v-model="form.passed">
            <el-radio v-for="dict in module_yes_no" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="考试时间" prop="examTime">
          <el-date-picker clearable style="width: 100%;" v-model="form.examTime" type="date" value-format="YYYY-MM-DD" placeholder="请选择考试时间"></el-date-picker>
        </el-form-item>
        <el-form-item label="有效期至" prop="validUntil">
          <el-date-picker clearable style="width: 100%;" v-model="form.validUntil" type="date" value-format="YYYY-MM-DD" placeholder="请选择有效期至"></el-date-picker>
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" :loading="saving" @click="submitForm">{{ isEdit ? "保存" : "新增" }}</el-button>
        <el-button @click="goBack">取 消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="SafetyExamRecordDetail">
import { getSafetyExamRecord, addSafetyExamRecord, updateSafetyExamRecord } from "@/api/module/safetyExamRecord";

const props = defineProps({
  isEdit: { type: Boolean, default: false }
});

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_yes_no } = proxy.useDict('module_yes_no');

const safetyExamRecordPageBase = "/business/module/safetyExamRecord";
const safetyExamRecordListPath = "/module/safetyExamRecord";
const pageTitle = computed(() => props.isEdit ? "修改安全考试记录" : "新增安全考试记录");
const loading = ref(false);
const saving = ref(false);
const formRef = ref(null);
const form = reactive({
  id: null,
  studentId: null,
  score: null,
  passed: null,
  examTime: null,
  validUntil: null
});
const rules = {
  studentId: [{ required: true, message: "学生ID不能为空", trigger: "blur" }],
  score: [{ required: true, message: "考试成绩不能为空", trigger: "blur" }],
  passed: [{ required: true, message: "是否通过不能为空", trigger: "change" }],
  examTime: [{ required: true, message: "考试时间不能为空", trigger: "change" }]
};

onMounted(() => {
  resetForm();
  if (props.isEdit) {
    loadDetail();
  }
});

function resetForm() {
  Object.assign(form, {
    id: null,
    studentId: null,
    score: null,
    passed: null,
    examTime: null,
    validUntil: null
  });
  formRef.value?.clearValidate();
}

function loadDetail() {
  loading.value = true;
  getSafetyExamRecord(route.params.id).then(response => {
    Object.assign(form, response.data || {});
  }).finally(() => {
    loading.value = false;
  });
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    const request = props.isEdit ? updateSafetyExamRecord : addSafetyExamRecord;
    request({ ...form }).then(response => {
      proxy.$modal.msgSuccess(props.isEdit ? "修改成功" : "新增成功");
      const id = form.id || response?.data?.id;
      router.push(id ? safetyExamRecordPageBase + "/view/" + id : safetyExamRecordListPath);
    }).finally(() => {
      saving.value = false;
    });
  });
}

function goBack() {
  if (props.isEdit && route.params.id) {
    router.push(safetyExamRecordPageBase + "/view/" + route.params.id);
    return;
  }
  router.push(safetyExamRecordListPath);
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.form-footer { margin-top: 20px; }
</style>
