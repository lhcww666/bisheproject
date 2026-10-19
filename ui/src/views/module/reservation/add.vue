<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>{{ isStudent ? "申请预约" : "新增预约" }}</span>
          <el-button icon="Back" @click="goBack">返回列表</el-button>
        </div>
      </template>

      <el-alert
        v-if="isStudent"
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

      <!-- 学生申请 -->
      <el-form v-if="isStudent" ref="applyRef" :model="applyForm" :rules="applyRules" label-width="110px" style="max-width: 680px;">
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
          <el-date-picker v-model="applyForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择开始时间" style="width: 100%;" @change="loadAvailableRooms" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="applyForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择结束时间" style="width: 100%;" @change="loadAvailableRooms" />
        </el-form-item>
        <el-form-item label="参与人数" prop="participantCount">
          <el-input-number v-model="applyForm.participantCount" :min="1" :max="1000" :disabled="applyForm.reservationType === 'CLASS'" style="width: 100%;" @change="loadAvailableRooms" />
        </el-form-item>
        <el-form-item label="实验室" prop="roomId">
          <el-select v-model="applyForm.roomId" filterable clearable style="width: 100%;" :loading="roomsLoading" placeholder="请先选择时间，再选择可用实验室" no-data-text="该时段没有你可预约的实验室" @change="loadDevices">
            <el-option v-for="room in availableRooms" :key="room.id" :label="room.roomCode + ' ' + room.roomName + '（容量 ' + room.capacity + ' 人）'" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="实验名称" prop="experimentName">
          <el-input v-model="applyForm.experimentName" placeholder="例如：有机溶剂萃取实验" />
        </el-form-item>
        <el-form-item label="使用设备" prop="deviceIds">
          <el-select v-model="applyForm.deviceIds" multiple filterable clearable style="width: 100%;" :disabled="!applyForm.roomId" :loading="devicesLoading" placeholder="可不选；选择实验室后可挑设备">
            <el-option v-for="device in availableDevices" :key="device.id" :label="device.deviceName + '（' + device.deviceCode + '）'" :value="device.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="实验说明" prop="remark">
          <el-input v-model="applyForm.remark" type="textarea" :rows="3" placeholder="补充实验内容、防护要求等" />
        </el-form-item>
      </el-form>

      <!-- 管理员新增 -->
      <el-form v-else ref="adminRef" :model="adminForm" :rules="adminRules" label-width="110px" style="max-width: 680px;">
        <el-form-item label="预约编号" prop="reservationNo"><el-input v-model="adminForm.reservationNo" placeholder="请输入预约编号" /></el-form-item>
        <el-form-item label="申请人ID" prop="applicantId"><el-input v-model="adminForm.applicantId" placeholder="请输入申请人用户ID" /></el-form-item>
        <el-form-item label="实验室ID" prop="roomId"><el-input v-model="adminForm.roomId" placeholder="请输入实验室ID" /></el-form-item>
        <el-form-item label="实验名称" prop="experimentName"><el-input v-model="adminForm.experimentName" placeholder="请输入实验名称" /></el-form-item>
        <el-form-item label="开始时间" prop="startTime"><el-date-picker v-model="adminForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择开始时间" style="width: 100%;" /></el-form-item>
        <el-form-item label="结束时间" prop="endTime"><el-date-picker v-model="adminForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择结束时间" style="width: 100%;" /></el-form-item>
        <el-form-item label="参与人数" prop="participantCount"><el-input v-model="adminForm.participantCount" placeholder="请输入参与人数" /></el-form-item>
        <el-form-item label="风险等级" prop="riskLevel"><el-input v-model="adminForm.riskLevel" placeholder="请输入风险等级" /></el-form-item>
        <el-form-item label="实验说明" prop="remark"><el-input v-model="adminForm.remark" type="textarea" :rows="3" placeholder="请输入内容" /></el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" :loading="saving" @click="submit">{{ isStudent ? "提交申请" : "保存" }}</el-button>
        <el-button @click="goBack">取 消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="ReservationAdd">
import { applyReservation, addReservation, listAvailableRooms, listAvailableDevices, getMyClassRepresentative } from "@/api/module/reservation";
import useUserStore from "@/store/modules/user";

const router = useRouter();
const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const roleKeys = computed(() => userStore.roles || []);
const isStudent = computed(() => roleKeys.value.includes("student") && !roleKeys.value.includes("teacher") && !roleKeys.value.includes("admin"));

const saving = ref(false);
const roomsLoading = ref(false);
const devicesLoading = ref(false);
const availableRooms = ref([]);
const availableDevices = ref([]);
const classInfo = ref({ representative: false, deptName: null, studentCount: 0 });
const isClassRepresentative = computed(() => classInfo.value.representative === true);

const applyForm = reactive({
  reservationType: "INDIVIDUAL", startTime: null, endTime: null, participantCount: 1,
  roomId: null, experimentName: null, deviceIds: [], remark: null
});
const applyRules = {
  startTime: [{ required: true, message: "开始时间不能为空", trigger: "change" }],
  endTime: [{ required: true, message: "结束时间不能为空", trigger: "change" }],
  participantCount: [{ required: true, message: "参与人数不能为空", trigger: "change" }],
  roomId: [{ required: true, message: "实验室不能为空", trigger: "change" }],
  experimentName: [{ required: true, message: "实验名称不能为空", trigger: "blur" }]
};

const adminForm = reactive({
  reservationNo: null, applicantId: null, roomId: null, experimentName: null,
  startTime: null, endTime: null, participantCount: null, riskLevel: null, remark: null
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
  if (isStudent.value) {
    getMyClassRepresentative().then(response => {
      classInfo.value = response.data || { representative: false };
    });
  }
});

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
  if (!applyForm.startTime || !applyForm.endTime) return;
  roomsLoading.value = true;
  listAvailableRooms({ startTime: applyForm.startTime, endTime: applyForm.endTime, participantCount: applyForm.participantCount }).then(response => {
    availableRooms.value = response.data || [];
  }).finally(() => {
    roomsLoading.value = false;
  });
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

function submit() {
  if (isStudent.value) {
    proxy.$refs.applyRef.validate(valid => {
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
        router.push("/reservationView/" + response.data.id);
      }).finally(() => {
        saving.value = false;
      });
    });
  } else {
    proxy.$refs.adminRef.validate(valid => {
      if (!valid) return;
      saving.value = true;
      addReservation({ ...adminForm }).then(response => {
        proxy.$modal.msgSuccess("新增成功");
        router.push("/reservationView/" + response.data.id);
      }).finally(() => {
        saving.value = false;
      });
    });
  }
}

function goBack() {
  router.push("/module/reservation");
}
</script>
<style lang="scss" scoped>
.reservation-tip { margin-bottom: 12px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.form-footer { margin-top: 20px; padding-left: 110px; }
</style>
