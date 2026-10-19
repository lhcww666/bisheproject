<template>
  <div class="app-container" v-loading="loading">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>{{ pageTitle }}</span>
          <el-button icon="Back" @click="goBack">返回</el-button>
        </div>
      </template>

      <el-alert
        class="credit-tip"
        type="info"
        :closable="false"
        show-icon
        title="新增时记录对象取自当前登录账号；学生只能查看本人记录，教师只能查看本班学生记录。"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
        <el-form-item label="变动分值" prop="changeScore">
          <el-input v-model="form.changeScore" placeholder="请输入变动分值（可正可负）" />
        </el-form-item>
        <el-form-item label="变动后分值" prop="balanceScore">
          <el-input v-model="form.balanceScore" placeholder="请输入变动后分值" />
        </el-form-item>
        <el-form-item label="变动原因" prop="reason">
          <el-input v-model="form.reason" placeholder="请输入变动原因" />
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" :loading="saving" @click="submitForm">{{ isEdit ? "保存" : "新增" }}</el-button>
        <el-button @click="goBack">取 消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="CreditRecordDetail">
import { getCreditRecord, addCreditRecord, updateCreditRecord } from "@/api/module/creditRecord";

const props = defineProps({
  isEdit: { type: Boolean, default: false }
});

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();

const creditRecordPageBase = "/business/module/creditRecord";
const creditRecordListPath = "/module/creditRecord";
const pageTitle = computed(() => props.isEdit ? "修改信用分流水" : "新增信用分流水");
const loading = ref(false);
const saving = ref(false);
const formRef = ref(null);
const form = reactive({
  id: null,
  userId: null,
  changeScore: null,
  balanceScore: null,
  reason: null
});
const rules = {
  changeScore: [{ required: true, message: "变动分值不能为空", trigger: "blur" }],
  balanceScore: [{ required: true, message: "变动后分值不能为空", trigger: "blur" }],
  reason: [{ required: true, message: "变动原因不能为空", trigger: "blur" }]
};

onMounted(() => {
  resetForm();
  if (props.isEdit) {
    loadDetail();
  }
});

function resetForm() {
  Object.assign(form, { id: null, userId: null, changeScore: null, balanceScore: null, reason: null });
  formRef.value?.clearValidate();
}

function loadDetail() {
  loading.value = true;
  getCreditRecord(route.params.id).then(response => {
    Object.assign(form, response.data || {});
  }).finally(() => {
    loading.value = false;
  });
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    const request = props.isEdit ? updateCreditRecord : addCreditRecord;
    request({ ...form }).then(response => {
      proxy.$modal.msgSuccess(props.isEdit ? "修改成功" : "新增成功");
      const id = form.id || response?.data?.id;
      router.push(id ? creditRecordPageBase + "/view/" + id : creditRecordListPath);
    }).finally(() => {
      saving.value = false;
    });
  });
}

function goBack() {
  if (props.isEdit && route.params.id) {
    router.push(creditRecordPageBase + "/view/" + route.params.id);
    return;
  }
  router.push(creditRecordListPath);
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.credit-tip { margin-bottom: 12px; }
.form-footer { margin-top: 20px; }
</style>
