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
        <el-form-item label="所属实验室" prop="roomId">
          <el-select v-model="form.roomId" filterable clearable placeholder="请选择实验室" style="width: 100%;">
            <el-option
              v-for="room in roomOptions"
              :key="room.id"
              :label="roomOptionLabel(room)"
              :value="room.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="设备编号" prop="deviceCode">
          <el-input v-model="form.deviceCode" placeholder="请输入设备编号" />
        </el-form-item>
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="form.deviceName" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="设备类型" prop="deviceType">
          <el-select v-model="form.deviceType" placeholder="请选择设备类型" style="width: 100%;">
            <el-option v-for="dict in module_device_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input v-model="form.quantity" placeholder="请输入数量" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in module_device_status" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否需要培训" prop="requiresTraining">
          <el-radio-group v-model="form.requiresTraining">
            <el-radio v-for="dict in module_yes_no" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" :loading="saving" @click="submitForm">{{ isEdit ? "保存" : "新增" }}</el-button>
        <el-button @click="goBack">取 消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="DeviceDetail">
import { getDevice, addDevice, updateDevice, listDeviceRoomOptions } from "@/api/module/device";

const props = defineProps({
  isEdit: { type: Boolean, default: false }
});

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_device_type, module_device_status, module_yes_no } = proxy.useDict('module_device_type', 'module_device_status', 'module_yes_no');

const devicePageBase = "/business/module/device";
const deviceListPath = "/module/device";
const pageTitle = computed(() => props.isEdit ? "修改设备档案" : "新增设备档案");
const loading = ref(false);
const saving = ref(false);
const roomOptions = ref([]);
const formRef = ref(null);
const form = reactive({
  id: null,
  roomId: null,
  deviceCode: null,
  deviceName: null,
  deviceType: null,
  quantity: null,
  status: null,
  requiresTraining: null,
  remark: null
});
const rules = {
  roomId: [{ required: true, message: "所属实验室不能为空", trigger: "blur" }],
  deviceCode: [{ required: true, message: "设备编号不能为空", trigger: "blur" }],
  deviceName: [{ required: true, message: "设备名称不能为空", trigger: "blur" }],
  deviceType: [{ required: true, message: "设备类型不能为空", trigger: "change" }],
  quantity: [{ required: true, message: "数量不能为空", trigger: "blur" }],
  status: [{ required: true, message: "状态不能为空", trigger: "change" }],
  requiresTraining: [{ required: true, message: "是否需要培训不能为空", trigger: "change" }]
};

onMounted(() => {
  resetForm();
  loadRoomOptions();
  if (props.isEdit) {
    loadDetail();
  }
});

function loadRoomOptions() {
  listDeviceRoomOptions().then(response => {
    roomOptions.value = response.data || [];
  }).catch(error => {
    roomOptions.value = [];
    proxy.$modal.msgError("实验室选项加载失败：" + (error?.message || "请稍后重试"));
  });
}

function roomOptionLabel(room) {
  return [room.roomCode, room.roomName].filter(Boolean).join(" · ");
}

function resetForm() {
  Object.assign(form, {
    id: null,
    roomId: null,
    deviceCode: null,
    deviceName: null,
    deviceType: null,
    quantity: null,
    status: null,
    requiresTraining: null,
    remark: null
  });
  formRef.value?.clearValidate();
}

function loadDetail() {
  loading.value = true;
  getDevice(route.params.id).then(response => {
    Object.assign(form, response.data || {});
  }).finally(() => {
    loading.value = false;
  });
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    const request = props.isEdit ? updateDevice : addDevice;
    request({ ...form }).then(response => {
      proxy.$modal.msgSuccess(props.isEdit ? "修改成功" : "新增成功");
      const id = form.id || response?.data?.id;
      router.push(id ? devicePageBase + "/view/" + id : deviceListPath);
    }).finally(() => {
      saving.value = false;
    });
  });
}

function goBack() {
  if (props.isEdit && route.params.id) {
    router.push(devicePageBase + "/view/" + route.params.id);
    return;
  }
  router.push(deviceListPath);
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.form-footer { margin-top: 20px; }
</style>
