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
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="form.deviceId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="使用数量" prop="quantity">
          <el-input v-model="form.quantity" placeholder="请输入使用数量" />
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" :loading="saving" @click="submitForm">{{ isEdit ? "保存" : "新增" }}</el-button>
        <el-button @click="goBack">取 消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="ReservationDeviceDetail">
import { getReservationDevice, addReservationDevice, updateReservationDevice } from "@/api/module/reservationDevice";

const props = defineProps({
  isEdit: { type: Boolean, default: false }
});

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();

const reservationDevicePageBase = "/business/module/reservationDevice";
const reservationDeviceListPath = "/module/reservationDevice";
const pageTitle = computed(() => props.isEdit ? "修改预约设备关联" : "新增预约设备关联");
const loading = ref(false);
const saving = ref(false);
const formRef = ref(null);
const form = reactive({
  id: null,
  reservationId: null,
  deviceId: null,
  quantity: null
});
const rules = {
  reservationId: [{ required: true, message: "预约ID不能为空", trigger: "blur" }],
  deviceId: [{ required: true, message: "设备ID不能为空", trigger: "blur" }],
  quantity: [{ required: true, message: "使用数量不能为空", trigger: "blur" }]
};

onMounted(() => {
  resetForm();
  if (props.isEdit) {
    loadDetail();
  }
});

function resetForm() {
  Object.assign(form, { id: null, reservationId: null, deviceId: null, quantity: null });
  formRef.value?.clearValidate();
}

function loadDetail() {
  loading.value = true;
  getReservationDevice(route.params.id).then(response => {
    Object.assign(form, response.data || {});
  }).finally(() => {
    loading.value = false;
  });
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    const request = props.isEdit ? updateReservationDevice : addReservationDevice;
    request({ ...form }).then(response => {
      proxy.$modal.msgSuccess(props.isEdit ? "修改成功" : "新增成功");
      const id = form.id || response?.data?.id;
      router.push(id ? reservationDevicePageBase + "/view/" + id : reservationDeviceListPath);
    }).finally(() => {
      saving.value = false;
    });
  });
}

function goBack() {
  if (props.isEdit && route.params.id) {
    router.push(reservationDevicePageBase + "/view/" + route.params.id);
    return;
  }
  router.push(reservationDeviceListPath);
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.form-footer { margin-top: 20px; }
</style>
