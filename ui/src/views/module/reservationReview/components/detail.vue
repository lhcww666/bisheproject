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
        <el-form-item label="预约ID" prop="reservationId">
          <el-input v-model="form.reservationId" placeholder="请输入预约ID" />
        </el-form-item>
        <el-form-item label="审核人ID" prop="reviewerId">
          <el-input v-model="form.reviewerId" placeholder="请输入审核人ID（sys_user.user_id）" />
        </el-form-item>
        <el-form-item label="审核结果" prop="result">
          <el-radio-group v-model="form.result">
            <el-radio v-for="dict in module_review_result" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" prop="reviewComment">
          <el-input v-model="form.reviewComment" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="审核时间" prop="reviewTime">
          <el-date-picker clearable style="width: 100%;" v-model="form.reviewTime" type="date" value-format="YYYY-MM-DD" placeholder="请选择审核时间"></el-date-picker>
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" :loading="saving" @click="submitForm">{{ isEdit ? "保存" : "新增" }}</el-button>
        <el-button @click="goBack">取 消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="ReservationReviewDetail">
import { getReservationReview, addReservationReview, updateReservationReview } from "@/api/module/reservationReview";

const props = defineProps({
  isEdit: { type: Boolean, default: false }
});

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_review_result } = proxy.useDict('module_review_result');

const reservationReviewPageBase = "/business/module/reservationReview";
const reservationReviewListPath = "/module/reservationReview";
const pageTitle = computed(() => props.isEdit ? "修改预约审核" : "新增预约审核");
const loading = ref(false);
const saving = ref(false);
const formRef = ref(null);
const form = reactive({
  id: null,
  reservationId: null,
  reviewerId: null,
  result: null,
  reviewComment: null,
  reviewTime: null
});
const rules = {
  reservationId: [{ required: true, message: "预约ID不能为空", trigger: "blur" }],
  reviewerId: [{ required: true, message: "审核人ID不能为空", trigger: "blur" }],
  result: [{ required: true, message: "审核结果不能为空", trigger: "change" }]
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
    reservationId: null,
    reviewerId: null,
    result: null,
    reviewComment: null,
    reviewTime: null
  });
  formRef.value?.clearValidate();
}

function loadDetail() {
  loading.value = true;
  getReservationReview(route.params.id).then(response => {
    Object.assign(form, response.data || {});
  }).finally(() => {
    loading.value = false;
  });
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    const request = props.isEdit ? updateReservationReview : addReservationReview;
    request({ ...form }).then(response => {
      proxy.$modal.msgSuccess(props.isEdit ? "修改成功" : "新增成功");
      const id = form.id || response?.data?.id;
      router.push(id ? reservationReviewPageBase + "/view/" + id : reservationReviewListPath);
    }).finally(() => {
      saving.value = false;
    });
  });
}

function goBack() {
  if (props.isEdit && route.params.id) {
    router.push(reservationReviewPageBase + "/view/" + route.params.id);
    return;
  }
  router.push(reservationReviewListPath);
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.form-footer { margin-top: 20px; }
</style>
