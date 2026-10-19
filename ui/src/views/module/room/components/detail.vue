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
        <el-form-item label="实验室编号" prop="roomCode">
          <el-input v-model="form.roomCode" placeholder="请输入实验室编号" />
        </el-form-item>
        <el-form-item label="实验室名称" prop="roomName">
          <el-input v-model="form.roomName" placeholder="请输入实验室名称" />
        </el-form-item>
        <el-form-item label="实验室类型" prop="roomType">
          <el-select v-model="form.roomType" placeholder="请选择实验室类型" style="width: 100%;">
            <el-option v-for="dict in module_room_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="归属院系" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptOptions"
            :props="{ value: 'id', label: 'label', children: 'children' }"
            value-key="id"
            placeholder="请选择归属院系"
            clearable
            check-strictly
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入位置" />
        </el-form-item>
        <el-form-item label="最大人数" prop="capacity">
          <el-input v-model="form.capacity" placeholder="请输入最大人数" />
        </el-form-item>
        <el-form-item label="安全等级" prop="safetyLevel">
          <el-input v-model="form.safetyLevel" placeholder="请输入安全等级" />
        </el-form-item>
        <el-form-item label="开放开始时间" prop="openStartTime">
          <el-input v-model="form.openStartTime" placeholder="请输入开放开始时间" />
        </el-form-item>
        <el-form-item label="开放结束时间" prop="openEndTime">
          <el-input v-model="form.openEndTime" placeholder="请输入开放结束时间" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in module_room_status" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="预约模式" prop="reservationMode">
          <el-select v-model="form.reservationMode" placeholder="请选择预约模式" style="width: 100%;">
            <el-option v-for="dict in module_reservation_mode" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
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

<script setup name="RoomDetail">
import { getRoom, addRoom, updateRoom } from "@/api/module/room";
import { deptTreeSelect } from "@/api/system/user";

const props = defineProps({
  isEdit: { type: Boolean, default: false }
});

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_room_type, module_room_status, module_reservation_mode } = proxy.useDict('module_room_type', 'module_room_status', 'module_reservation_mode');

const roomPageBase = "/business/module/room";
const roomListPath = "/module/room";
const pageTitle = computed(() => props.isEdit ? "修改实验室档案" : "新增实验室档案");
const loading = ref(false);
const saving = ref(false);
const formRef = ref(null);
const deptOptions = ref([]);
const form = reactive({
  id: null,
  roomCode: null,
  roomName: null,
  roomType: null,
  deptId: null,
  deptName: null,
  reservationMode: null,
  location: null,
  capacity: null,
  safetyLevel: null,
  openStartTime: null,
  openEndTime: null,
  status: null,
  description: null,
  remark: null
});
const rules = {
  roomCode: [{ required: true, message: "实验室编号不能为空", trigger: "blur" }],
  roomName: [{ required: true, message: "实验室名称不能为空", trigger: "blur" }],
  roomType: [{ required: true, message: "实验室类型不能为空", trigger: "change" }],
  deptId: [{ required: true, message: "归属院系不能为空", trigger: "change" }],
  location: [{ required: true, message: "位置不能为空", trigger: "blur" }],
  capacity: [{ required: true, message: "最大人数不能为空", trigger: "blur" }],
  safetyLevel: [{ required: true, message: "安全等级不能为空", trigger: "blur" }],
  openStartTime: [{ required: true, message: "开放开始时间不能为空", trigger: "blur" }],
  openEndTime: [{ required: true, message: "开放结束时间不能为空", trigger: "blur" }],
  status: [{ required: true, message: "状态不能为空", trigger: "change" }],
  reservationMode: [{ required: true, message: "预约模式不能为空", trigger: "change" }]
};

onMounted(() => {
  resetForm();
  loadDeptOptions();
  if (props.isEdit) {
    loadDetail();
  }
});

function resetForm() {
  Object.assign(form, {
    id: null,
    roomCode: null,
    roomName: null,
    roomType: null,
    deptId: null,
    deptName: null,
    reservationMode: null,
    location: null,
    capacity: null,
    safetyLevel: null,
    openStartTime: null,
    openEndTime: null,
    status: null,
    description: null,
    remark: null
  });
  formRef.value?.clearValidate();
}

function loadDeptOptions() {
  if (deptOptions.value.length > 0) return;
  deptTreeSelect().then(response => {
    deptOptions.value = response.data || [];
  });
}

function loadDetail() {
  loading.value = true;
  getRoom(route.params.id).then(response => {
    Object.assign(form, response.data || {});
  }).finally(() => {
    loading.value = false;
  });
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    const request = props.isEdit ? updateRoom : addRoom;
    request({ ...form }).then(response => {
      proxy.$modal.msgSuccess(props.isEdit ? "修改成功" : "新增成功");
      const id = form.id || response?.data?.id;
      router.push(id ? roomPageBase + "/view/" + id : roomListPath);
    }).finally(() => {
      saving.value = false;
    });
  });
}

function goBack() {
  if (props.isEdit && route.params.id) {
    router.push(roomPageBase + "/view/" + route.params.id);
    return;
  }
  router.push(roomListPath);
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.form-footer { margin-top: 20px; }
</style>
