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
        v-if="isEdit"
        class="reservation-tip"
        type="info"
        :closable="false"
        show-icon
        title="修改后会重新进入待审核，原审核记录会被清除，需要教师重新审核。"
      />
      <el-alert
        v-else-if="isStudent"
        class="reservation-tip"
        type="info"
        :closable="false"
        show-icon
        title="提交后预约进入待审核；教师审核通过后才正式生效。"
      />
      <el-alert
        v-else
        class="reservation-tip"
        type="info"
        :closable="false"
        show-icon
        title="管理员新增的预约同样先进入待审核，不能直接指定为已通过。"
      />

      <el-form
        v-if="useStudentApplyForm"
        ref="applyRef"
        :model="applyForm"
        :rules="applyRules"
        label-width="110px"
        style="max-width: 680px;"
      >
        <el-form-item label="预约类型" prop="reservationType">
          <el-radio-group v-model="applyForm.reservationType" @change="handleTypeChange">
            <el-radio label="INDIVIDUAL">个人预约</el-radio>
            <el-radio v-if="isClassRepresentative" label="CLASS">班级预约</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert
          v-if="applyForm.reservationType === 'CLASS'"
          class="reservation-tip"
          type="warning"
          :closable="false"
          show-icon
          :title="'将代表 ' + (classInfo.deptName || '本班') + ' 提交班级预约，参与人数按班级学生数 ' + (classInfo.studentCount || 0) + ' 人计算。'"
        />
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="applyForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择开始时间" style="width: 100%;" :disabled-date="disabledPastDate" @change="loadAvailableRooms" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="applyForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择结束时间" style="width: 100%;" :disabled-date="disabledPastDate" @change="loadAvailableRooms" />
        </el-form-item>
        <el-form-item label="参与人数" prop="participantCount">
          <el-input-number v-model="applyForm.participantCount" :min="1" :max="1000" :disabled="applyForm.reservationType === 'CLASS'" style="width: 100%;" @change="loadAvailableRooms" />
        </el-form-item>
        <el-form-item label="实验室" prop="roomId">
          <el-select v-model="applyForm.roomId" filterable clearable style="width: 100%;" :loading="roomsLoading" placeholder="请先选择时间，再选择可用实验室" :no-data-text="roomAvailabilityNoDataText" @change="loadDevices">
            <el-option v-for="room in availableRooms" :key="room.id" :label="room.roomCode + ' ' + room.roomName + '（容量 ' + room.capacity + ' 人）'" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-alert
          v-if="roomAvailabilityMessage"
          class="room-availability-alert"
          :type="roomAvailabilityType"
          :title="roomAvailabilityMessage"
          :closable="false"
          show-icon
        />
        <el-form-item label="实验名称" prop="experimentName">
          <el-input v-model="applyForm.experimentName" placeholder="例如：有机溶剂萃取实验" />
        </el-form-item>
        <el-form-item label="使用设备" prop="deviceIds">
          <el-select v-model="applyForm.deviceIds" multiple filterable clearable collapse-tags collapse-tags-tooltip style="width: 100%;" :disabled="!applyForm.roomId" :loading="devicesLoading" placeholder="可不选；选择实验室后可多选设备">
            <el-option v-for="device in availableDevices" :key="device.id" :label="deviceOptionLabel(device)" :value="device.id" />
          </el-select>
          <div class="form-hint">支持多选；提交时会校验设备所属实验室、库存和培训要求。</div>
        </el-form-item>
        <el-form-item label="实验说明" prop="remark">
          <el-input v-model="applyForm.remark" type="textarea" :rows="3" placeholder="补充实验内容、防护要求等" />
        </el-form-item>
      </el-form>

      <el-form
        v-else
        ref="adminRef"
        :model="adminForm"
        :rules="adminRules"
        label-width="110px"
        style="max-width: 680px;"
      >
        <el-form-item label="预约编号" prop="reservationNo"><el-input v-model="adminForm.reservationNo" placeholder="请输入预约编号" /></el-form-item>
        <el-form-item label="申请人ID" prop="applicantId"><el-input v-model="adminForm.applicantId" placeholder="请输入申请人用户ID" /></el-form-item>
        <el-form-item label="实验室ID" prop="roomId"><el-input v-model="adminForm.roomId" placeholder="请输入实验室ID" @change="loadAdminDevices(adminForm.roomId)" /></el-form-item>
        <el-form-item label="使用设备" prop="deviceIds">
          <el-select v-model="adminForm.deviceIds" multiple filterable clearable collapse-tags collapse-tags-tooltip style="width: 100%;" :disabled="!adminForm.roomId" :loading="devicesLoading" placeholder="选择实验室后可多选设备">
            <el-option v-for="device in availableDevices" :key="device.id" :label="deviceOptionLabel(device)" :value="device.id" />
          </el-select>
          <div class="form-hint">支持多选；提交时会校验设备所属实验室、库存和培训要求。</div>
        </el-form-item>
        <el-form-item label="实验名称" prop="experimentName"><el-input v-model="adminForm.experimentName" placeholder="请输入实验名称" /></el-form-item>
        <el-form-item label="开始时间" prop="startTime"><el-date-picker v-model="adminForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择开始时间" style="width: 100%;" /></el-form-item>
        <el-form-item label="结束时间" prop="endTime"><el-date-picker v-model="adminForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择结束时间" style="width: 100%;" /></el-form-item>
        <el-form-item label="参与人数" prop="participantCount"><el-input v-model="adminForm.participantCount" placeholder="请输入参与人数" /></el-form-item>
        <el-form-item label="风险等级" prop="riskLevel"><el-input v-model="adminForm.riskLevel" placeholder="请输入风险等级" /></el-form-item>
        <el-form-item label="实验说明" prop="remark"><el-input v-model="adminForm.remark" type="textarea" :rows="3" placeholder="请输入内容" /></el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" :loading="saving" @click="submit">{{ submitText }}</el-button>
        <el-button @click="goBack">取 消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="ReservationDetail">
import { applyReservation, addReservation, getReservationDetail, updateReservation, listAvailableRooms, listAvailableDevices, getMyClassRepresentative } from "@/api/module/reservation";
import useUserStore from "@/store/modules/user";

const props = defineProps({
  isEdit: { type: Boolean, default: false }
});

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const roleKeys = computed(() => userStore.roles || []);
const isStudent = computed(() => roleKeys.value.includes("student") && !roleKeys.value.includes("teacher") && !roleKeys.value.includes("admin"));
const useStudentApplyForm = computed(() => !props.isEdit && isStudent.value);

const pageTitle = computed(() => props.isEdit ? "修改预约" : (isStudent.value ? "申请预约" : "新增预约"));
const submitText = computed(() => props.isEdit ? "保存" : (isStudent.value ? "提交申请" : "保存"));

const loading = ref(false);
const saving = ref(false);
const roomsLoading = ref(false);
const devicesLoading = ref(false);
const availableRooms = ref([]);
const availableDevices = ref([]);
const roomAvailabilityMessage = ref("");
const roomAvailabilityType = ref("info");
const roomAvailabilityNoDataText = computed(() => {
  if (roomsLoading.value) return "正在查询可用实验室";
  return roomAvailabilityMessage.value || "该时段没有符合容量、开放时间和准入范围的实验室";
});
const classInfo = ref({ representative: false, deptName: null, studentCount: 0 });
const isClassRepresentative = computed(() => classInfo.value.representative === true);
const applyRef = ref(null);
const adminRef = ref(null);

const applyForm = reactive({
  reservationType: "INDIVIDUAL",
  startTime: null,
  endTime: null,
  participantCount: 1,
  roomId: null,
  experimentName: null,
  deviceIds: [],
  remark: null
});
const applyRules = {
  startTime: [{ required: true, message: "开始时间不能为空", trigger: "change" }],
  endTime: [{ required: true, message: "结束时间不能为空", trigger: "change" }],
  participantCount: [{ required: true, message: "参与人数不能为空", trigger: "change" }],
  roomId: [{ required: true, message: "实验室不能为空", trigger: "change" }],
  experimentName: [{ required: true, message: "实验名称不能为空", trigger: "blur" }]
};

const adminForm = reactive({
  id: null,
  reservationNo: null,
  applicantId: null,
  roomId: null,
  experimentName: null,
  startTime: null,
  endTime: null,
  participantCount: null,
  riskLevel: null,
  deviceIds: [],
  remark: null
});
const adminRules = {
  reservationNo: [{ required: true, message: "预约编号不能为空", trigger: "blur" }],
  applicantId: [{ required: true, message: "申请人ID不能为空", trigger: "blur" }],
  roomId: [{ required: true, message: "实验室不能为空", trigger: "blur" }],
  experimentName: [{ required: true, message: "实验名称不能为空", trigger: "blur" }],
  startTime: [{ required: true, message: "开始时间不能为空", trigger: "change" }],
  endTime: [{ required: true, message: "结束时间不能为空", trigger: "change" }],
  participantCount: [{ required: true, message: "参与人数不能为空", trigger: "blur" }],
  riskLevel: [{ required: true, message: "风险等级不能为空", trigger: "blur" }]
};

onMounted(() => {
  if (props.isEdit) {
    loadReservation();
  } else if (isStudent.value) {
    getMyClassRepresentative().then(response => {
      classInfo.value = response.data || { representative: false };
    });
  }
});

function loadReservation() {
  loading.value = true;
  getReservationDetail(route.params.id).then(response => {
    const detail = response.data || {};
    const selectedDeviceIds = (detail.deviceList || []).map(item => item.deviceId);
    Object.assign(adminForm, detail);
    adminForm.deviceIds = selectedDeviceIds;
    if (adminForm.roomId) {
      loadAdminDevices(adminForm.roomId, selectedDeviceIds);
    }
    if (adminForm.status === "1") {
      proxy.$modal.msgError("该预约已审核通过，请先弃审再修改");
      router.replace("/reservationView/" + adminForm.id);
    }
  }).finally(() => {
    loading.value = false;
  });
}

function handleTypeChange(type) {
  applyForm.roomId = null;
  applyForm.deviceIds = [];
  availableRooms.value = [];
  availableDevices.value = [];
  applyForm.participantCount = type === "CLASS" ? Number(classInfo.value.studentCount || 1) : 1;
  loadAvailableRooms();
}

function loadAvailableRooms() {
  applyForm.roomId = null;
  applyForm.deviceIds = [];
  availableRooms.value = [];
  availableDevices.value = [];
  roomAvailabilityMessage.value = "";
  roomAvailabilityType.value = "info";
  if (!applyForm.startTime || !applyForm.endTime) return;
  const start = parseDateTime(applyForm.startTime);
  const end = parseDateTime(applyForm.endTime);
  if (!start || !end || end <= start) {
    roomAvailabilityType.value = "warning";
    roomAvailabilityMessage.value = "结束时间必须晚于开始时间，请重新选择预约时段。";
    return;
  }
  if (start < new Date()) {
    roomAvailabilityType.value = "warning";
    roomAvailabilityMessage.value = "开始时间已早于当前时间，不能查询过去时段。请选择当前时间之后的时段。";
    return;
  }
  if (!applyForm.participantCount || applyForm.participantCount < 1) {
    roomAvailabilityType.value = "warning";
    roomAvailabilityMessage.value = "参与人数必须大于 0。";
    return;
  }
  roomsLoading.value = true;
  roomAvailabilityMessage.value = "正在查询该时段可预约的实验室...";
  roomAvailabilityType.value = "info";
  listAvailableRooms({
    startTime: applyForm.startTime,
    endTime: applyForm.endTime,
    participantCount: applyForm.participantCount
  }).then(response => {
    availableRooms.value = response.data || [];
    if (availableRooms.value.length) {
      roomAvailabilityType.value = "success";
      roomAvailabilityMessage.value = "已找到 " + availableRooms.value.length + " 个可预约实验室，请选择目标实验室。";
    } else {
      roomAvailabilityType.value = "warning";
      roomAvailabilityMessage.value = "该时段没有可预约实验室，可能因为容量不足、不在开放时段、已有预约冲突或未获得实验室准入。请调整时间或人数后重试。";
    }
  }).catch(error => {
    roomAvailabilityType.value = "error";
    roomAvailabilityMessage.value = "查询可用实验室失败：" + (error?.message || "请稍后重试");
  }).finally(() => {
    roomsLoading.value = false;
  });
}

function disabledPastDate(date) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date.getTime() < today.getTime();
}

function parseDateTime(value) {
  if (!value) return null;
  const date = new Date(String(value).replace(/-/g, "/"));
  return Number.isNaN(date.getTime()) ? null : date;
}

function loadDevices(roomId) {
  applyForm.deviceIds = [];
  availableDevices.value = [];
  if (!roomId) return;
  devicesLoading.value = true;
  listAvailableDevices({ roomId }).then(response => {
    availableDevices.value = response.data || [];
  }).finally(() => {
    devicesLoading.value = false;
  });
}

function loadAdminDevices(roomId, selectedIds = []) {
  if (!roomId) {
    availableDevices.value = [];
    adminForm.deviceIds = [];
    return;
  }
  devicesLoading.value = true;
  listAvailableDevices({ roomId }).then(response => {
    availableDevices.value = response.data || [];
    const availableIds = new Set(availableDevices.value.map(item => item.id));
    adminForm.deviceIds = (selectedIds || []).filter(id => availableIds.has(id));
  }).finally(() => {
    devicesLoading.value = false;
  });
}

function deviceOptionLabel(device) {
  const training = "1" === String(device.requiresTraining) ? "，需培训" : "";
  return device.deviceName + "（" + device.deviceCode + "，可用 " + (device.quantity || 0) + training + "）";
}

function submit() {
  if (useStudentApplyForm.value) {
    submitStudentApply();
    return;
  }
  submitAdminForm();
}

function submitStudentApply() {
  applyRef.value.validate(valid => {
    if (!valid) return;
    if (applyForm.startTime >= applyForm.endTime) {
      proxy.$modal.msgError("结束时间必须晚于开始时间");
      return;
    }
    saving.value = true;
    applyReservation({
      reservationType: applyForm.reservationType,
      roomId: applyForm.roomId,
      experimentName: applyForm.experimentName,
      startTime: applyForm.startTime,
      endTime: applyForm.endTime,
      participantCount: applyForm.participantCount,
      deviceIds: applyForm.deviceIds,
      remark: applyForm.remark
    }).then(response => {
      proxy.$modal.msgSuccess("预约申请已提交");
      const id = response?.data?.id;
      router.push(id ? "/reservationView/" + id : "/module/reservation");
    }).finally(() => {
      saving.value = false;
    });
  });
}

function submitAdminForm() {
  adminRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    const request = props.isEdit ? updateReservation : addReservation;
    request({ ...adminForm }).then(response => {
      proxy.$modal.msgSuccess(props.isEdit ? "修改成功，预约已重新进入待审核" : "新增成功");
      const id = adminForm.id || response?.data?.id;
      router.push(id ? "/reservationView/" + id : "/module/reservation");
    }).finally(() => {
      saving.value = false;
    });
  });
}

function goBack() {
  if (props.isEdit && route.params.id) {
    router.push("/reservationView/" + route.params.id);
    return;
  }
  router.push("/module/reservation");
}
</script>

<style lang="scss" scoped>
.reservation-tip { margin-bottom: 12px; }
.room-availability-alert { margin: -4px 0 16px; }
.form-hint { margin-top: 4px; color: #909399; font-size: 12px; line-height: 1.5; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.form-footer { margin-top: 20px; padding-left: 110px; }
</style>
