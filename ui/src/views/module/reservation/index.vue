<template>
  <div class="app-container">
    <el-alert
      v-if="isStudent"
      class="reservation-tip"
      type="info"
      :closable="false"
      show-icon
      title="学生视图：这里只展示你本人的预约。需要新预约时，点击上方“申请预约”。"
    />
    <el-alert
      v-else-if="isTeacher"
      class="reservation-tip"
      type="info"
      :closable="false"
      show-icon
      title="教师视图：这里展示本人班级学生的预约记录。"
    />

    <el-form @submit.prevent :model="reservationQueryParams" ref="reservationQueryRef" :inline="true" v-show="reservationShowSearch" label-width="auto">
      <el-form-item label="创建时间">
        <el-date-picker
            style="width: 230px;"
            v-model="dateRange"
            value-format="YYYY-MM-DD"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item label="预约编号" prop="reservationNoLike">
        <el-input
          v-model="reservationQueryParams.reservationNoLike"
          placeholder="请输入预约编号"
          clearable
          @keyup.enter="reservationHandleQuery"
        />
      </el-form-item>
      <el-form-item v-if="!isStudent" label="申请人ID" prop="applicantIdLike">
        <el-input
          v-model="reservationQueryParams.applicantIdLike"
          placeholder="请输入申请人ID（sys_user.user_id）"
          clearable
          @keyup.enter="reservationHandleQuery"
        />
      </el-form-item>
      <el-form-item v-if="!isStudent" label="实验室" prop="roomIdLike">
        <el-input
          v-model="reservationQueryParams.roomIdLike"
          placeholder="请输入实验室ID"
          clearable
          @keyup.enter="reservationHandleQuery"
        />
      </el-form-item>
      <el-form-item label="实验名称" prop="experimentNameLike">
        <el-input
          v-model="reservationQueryParams.experimentNameLike"
          placeholder="请输入实验名称"
          clearable
          @keyup.enter="reservationHandleQuery"
        />
      </el-form-item>
      <el-form-item label="预约状态" prop="status">
        <el-select v-model="reservationQueryParams.status" placeholder="请选择预约状态" clearable>
          <el-option
            v-for="dict in module_reservation_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker clearable
          v-model="reservationQueryParams.startTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择开始时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker clearable
          v-model="reservationQueryParams.endTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择结束时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item v-if="!isStudent" label="参与人数" prop="participantCountLike">
        <el-input
          v-model="reservationQueryParams.participantCountLike"
          placeholder="请输入参与人数"
          clearable
          @keyup.enter="reservationHandleQuery"
        />
      </el-form-item>
      <el-form-item v-if="!isStudent" label="风险等级" prop="riskLevelLike">
        <el-input
          v-model="reservationQueryParams.riskLevelLike"
          placeholder="请输入风险等级"
          clearable
          @keyup.enter="reservationHandleQuery"
        />
      </el-form-item>
      <el-form-item v-if="!isStudent" label="是否需要审核" prop="reviewRequired">
        <el-select v-model="reservationQueryParams.reviewRequired" placeholder="请选择是否需要审核" clearable>
          <el-option
            v-for="dict in module_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="reservationHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="reservationResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col v-if="isStudent" :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="openApplyDialog"
          v-hasPermi="['module:reservation:apply']"
        >申请预约</el-button>
      </el-col>
      <el-col v-if="canManage" :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="reservationHandleAdd"
          v-hasPermi="['module:reservation:add']"
        >新增</el-button>
      </el-col>
      <el-col v-if="canManage" :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="reservationSingle"
          @click="reservationHandleUpdate"
          v-hasPermi="['module:reservation:edit']"
        >修改</el-button>
      </el-col>
      <el-col v-if="canManage" :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="reservationMultiple"
          @click="reservationHandleDelete"
          v-hasPermi="['module:reservation:remove']"
        >删除</el-button>
      </el-col>
      <el-col v-if="canManage" :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="reservationHandleExport"
          v-hasPermi="['module:reservation:export']"
        >导出</el-button>
      </el-col>
      <el-col v-if="canManage" :span="1.5">
        <el-button
            type="info"
            plain
            icon="Download"
            @click="reservationImportTemplate"
            v-hasPermi="['module:reservation:import']"
        >下载模板</el-button>
      </el-col>
      <el-col v-if="canManage" :span="1.5">
      <file-upload
            v-model="importRes"
            v-hasPermi="['module:reservation:import']"
            :limit="1"
            uploadBtnText="导入"
            :fileType="['xlsx']"
            accept=".xlsx"
            :action="'/module/reservation/importData'"
            :isShowTip="false"
            :isShowFileList="false"
            isImport
          />
      </el-col>
      <el-col :span="1.5">
        <module-help module="reservation" />
      </el-col>
      <right-toolbar v-model:showSearch="reservationShowSearch" @queryTable="reservationGetList"></right-toolbar>
    </el-row>

    <el-table v-loading="reservationLoading" :data="reservationList" @selection-change="reservationHandleSelectionChange" border>
      <el-table-column v-if="canManage" type="selection" width="55" align="center" />
      <el-table-column label="预约编号" align="center" prop="reservationNo" min-width="170" />
      <el-table-column label="申请人" align="center" min-width="110">
        <template #default="scope">
          <span>{{ scope.row.applicantName || scope.row.applicantId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实验室" align="center" min-width="150">
        <template #default="scope">
          <span>{{ scope.row.roomName || scope.row.roomId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实验名称" align="center" prop="experimentName" min-width="150" />
      <el-table-column label="使用设备" align="center" min-width="150">
        <template #default="scope">
          <span>{{ (scope.row.deviceNames || []).join('、') || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="参与人数" align="center" prop="participantCount" width="90" />
      <el-table-column label="预约状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :options="module_reservation_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="审核意见" align="center" prop="reviewComment" min-width="150">
        <template #default="scope">
          <span>{{ scope.row.reviewComment || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="isStudent || canManage" width="120" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            v-if="isStudent && canCancel(scope.row)"
            link
            type="danger"
            icon="CircleClose"
            @click="handleCancel(scope.row)"
            v-hasPermi="['module:reservation:cancel']"
          >取消预约</el-button>
          <el-button v-if="canManage" link type="primary" icon="Edit" @click="reservationHandleUpdate(scope.row)" v-hasPermi="['module:reservation:edit']">修改</el-button>
          <el-button v-if="canManage" link type="primary" icon="Delete" @click="reservationHandleDelete(scope.row)" v-hasPermi="['module:reservation:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="reservationTotal>0"
      :total="reservationTotal"
      v-model:page="reservationQueryParams.pageNum"
      v-model:limit="reservationQueryParams.pageSize"
      @pagination="reservationGetList"
    />

    <!-- 学生预约申请对话框 -->
    <el-dialog title="申请预约" v-model="applyOpen" width="560px" append-to-body destroy-on-close>
      <el-form @submit.prevent ref="applyRef" :model="applyForm" :rules="applyRules" label-width="100px">
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            clearable
            style="width: 100%;"
            v-model="applyForm.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择开始时间"
            @change="loadAvailableRooms"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            clearable
            style="width: 100%;"
            v-model="applyForm.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择结束时间"
            @change="loadAvailableRooms"
          />
        </el-form-item>
        <el-form-item label="参与人数" prop="participantCount">
          <el-input-number
            v-model="applyForm.participantCount"
            :min="1"
            :max="200"
            style="width: 100%;"
            @change="loadAvailableRooms"
          />
        </el-form-item>
        <el-form-item label="实验室" prop="roomId">
          <el-select
            v-model="applyForm.roomId"
            filterable
            clearable
            style="width: 100%;"
            :loading="availableRoomsLoading"
            placeholder="请先选择时间，再选择可用实验室"
            no-data-text="该时段没有你可预约的实验室（可能时间冲突或未开通准入）"
            @change="handleApplyRoomChange"
          >
            <el-option
              v-for="room in availableRooms"
              :key="room.id"
              :label="room.roomCode + ' ' + room.roomName + '（容量 ' + room.capacity + ' 人）'"
              :value="room.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="实验名称" prop="experimentName">
          <el-input v-model="applyForm.experimentName" placeholder="例如：有机溶剂萃取实验" />
        </el-form-item>
        <el-form-item label="使用设备" prop="deviceIds">
          <el-select
            v-model="applyForm.deviceIds"
            multiple
            filterable
            clearable
            style="width: 100%;"
            :disabled="!applyForm.roomId"
            :loading="availableDevicesLoading"
            placeholder="可不选；选择实验室后可挑设备"
            no-data-text="该实验室暂无可预约设备"
          >
            <el-option
              v-for="device in availableDevices"
              :key="device.id"
              :label="device.deviceName + '（' + device.deviceCode + '）'"
              :value="device.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="实验说明" prop="remark">
          <el-input v-model="applyForm.remark" type="textarea" :rows="3" placeholder="补充实验内容、防护要求等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="applyLoading" @click="submitApply">提交申请</el-button>
          <el-button @click="applyOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 管理员添加或修改预约对话框 -->
    <el-dialog :title="reservationTitle" v-model="reservationOpen" width="500px" append-to-body destroy-on-close>
      <el-form @submit.prevent ref="reservationRef" :model="reservationForm" :rules="reservationRules" label-width="auto">
        <el-form-item label="预约编号" prop="reservationNo">
          <el-input v-model="reservationForm.reservationNo" placeholder="请输入预约编号" />
        </el-form-item>
        <el-form-item label="申请人ID" prop="applicantId">
          <el-input v-model="reservationForm.applicantId" placeholder="请输入申请人ID（sys_user.user_id）" />
        </el-form-item>
        <el-form-item label="实验室" prop="roomId">
          <el-input v-model="reservationForm.roomId" placeholder="请输入实验室" />
        </el-form-item>
        <el-form-item label="实验名称" prop="experimentName">
          <el-input v-model="reservationForm.experimentName" placeholder="请输入实验名称" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker clearable style="width: 100%;"
            v-model="reservationForm.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择开始时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker clearable style="width: 100%;"
            v-model="reservationForm.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择结束时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="参与人数" prop="participantCount">
          <el-input v-model="reservationForm.participantCount" placeholder="请输入参与人数" />
        </el-form-item>
        <el-form-item label="风险等级" prop="riskLevel">
          <el-input v-model="reservationForm.riskLevel" placeholder="请输入风险等级" />
        </el-form-item>
        <el-form-item label="预约状态" prop="status">
          <el-radio-group v-model="reservationForm.status">
            <el-radio
              v-for="dict in module_reservation_status"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否需要审核" prop="reviewRequired">
          <el-radio-group v-model="reservationForm.reviewRequired">
            <el-radio
              v-for="dict in module_yes_no"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="实验说明" prop="remark">
          <el-input v-model="reservationForm.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="reservationSubmitForm">确 定</el-button>
          <el-button @click="reservationCancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Reservation">
import {
  listReservation,
  getReservation,
  delReservation,
  addReservation,
  updateReservation,
  applyReservation,
  cancelReservation,
  listAvailableRooms,
  listAvailableDevices
} from "@/api/module/reservation";
import useUserStore from "@/store/modules/user";
import xeUtils from "xe-utils";

const dateRange = ref([]);
const route = useRoute();
const action = ref(null);
const userStore = useUserStore();

const roleKeys = computed(() => userStore.roles || []);
const isStudent = computed(() =>
  roleKeys.value.includes("student") &&
  !roleKeys.value.includes("teacher") &&
  !roleKeys.value.includes("admin")
);
const isTeacher = computed(() => roleKeys.value.includes("teacher") && !roleKeys.value.includes("admin"));
const canManage = computed(() => roleKeys.value.length > 0 && !isStudent.value && !isTeacher.value);

onMounted(() => {
  reservationHandleQuery();
});

const { proxy } = getCurrentInstance();
const { sys_common_status, module_reservation_status, module_yes_no } = proxy.useDict('sys_common_status', 'module_reservation_status', 'module_yes_no');

const importRes = ref(null);
watch(
  () => importRes,
  (val) => {
    if (val.value && val.value.code == 200) {
      reservationHandleQuery();
    }
  },
  { deep: true, immediate: true }
);

const reservationList = ref([]);
const reservationOpen = ref(false);
const reservationLoading = ref(true);
const reservationShowSearch = ref(true);
const reservationIds = ref([]);
const reservationSingle = ref(true);
const reservationMultiple = ref(true);
const reservationTotal = ref(0);
const reservationTitle = ref("");

const reservationData = reactive({
  reservationForm: {},
  reservationQueryParams: {
    pageNum: 1,
    pageSize: 10,
    reservationNoLike: null,
    applicantIdLike: null,
    roomIdLike: null,
    experimentNameLike: null,
    status: null,
    startTime: null,
    endTime: null,
    participantCountLike: null,
    riskLevelLike: null,
    reviewRequired: null,
  },
  reservationRules: {
    reservationNo: [
      { required: true, message: "预约编号不能为空", trigger: "blur" }
    ],
    applicantId: [
      { required: true, message: "申请人ID不能为空", trigger: "blur" }
    ],
    roomId: [
      { required: true, message: "实验室不能为空", trigger: "blur" }
    ],
    experimentName: [
      { required: true, message: "实验名称不能为空", trigger: "blur" }
    ],
    startTime: [
      { required: true, message: "开始时间不能为空", trigger: "blur" }
    ],
    endTime: [
      { required: true, message: "结束时间不能为空", trigger: "blur" }
    ],
    participantCount: [
      { required: true, message: "参与人数不能为空", trigger: "blur" }
    ],
    riskLevel: [
      { required: true, message: "风险等级不能为空", trigger: "blur" }
    ],
    status: [
      { required: true, message: "预约状态不能为空", trigger: "change" }
    ],
    reviewRequired: [
      { required: true, message: "是否需要审核不能为空", trigger: "change" }
    ],
  }
});

const { reservationQueryParams, reservationForm, reservationRules } = toRefs(reservationData);

const applyOpen = ref(false);
const applyLoading = ref(false);
const availableRooms = ref([]);
const availableRoomsLoading = ref(false);
const availableDevices = ref([]);
const availableDevicesLoading = ref(false);
const applyData = reactive({
  applyForm: {
    startTime: null,
    endTime: null,
    participantCount: 1,
    roomId: null,
    experimentName: null,
    deviceIds: [],
    remark: null
  },
  applyRules: {
    startTime: [{ required: true, message: "开始时间不能为空", trigger: "change" }],
    endTime: [{ required: true, message: "结束时间不能为空", trigger: "change" }],
    participantCount: [{ required: true, message: "参与人数不能为空", trigger: "change" }],
    roomId: [{ required: true, message: "实验室不能为空", trigger: "change" }],
    experimentName: [{ required: true, message: "实验名称不能为空", trigger: "blur" }]
  }
});
const { applyForm, applyRules } = toRefs(applyData);

watch(
  route,
  (newRoute) => {
    action.value = newRoute.query && newRoute.query.action;
  },
  { immediate: true }
);

/** 查询实验室预约列表 */
function reservationGetList() {
  reservationLoading.value = true;
  listReservation(proxy.addDateRange(reservationQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    reservationList.value = response.rows;
    reservationTotal.value = response.total;
  }).finally(() => {
    reservationLoading.value = false;
  });
}

function canCancel(row) {
  return row.status === "0" || row.status === "1";
}

function openApplyDialog() {
  applyForm.value = {
    startTime: null,
    endTime: null,
    participantCount: 1,
    roomId: null,
    experimentName: null,
    deviceIds: [],
    remark: null
  };
  availableRooms.value = [];
  availableDevices.value = [];
  applyOpen.value = true;
}

function loadAvailableRooms() {
  applyForm.value.roomId = null;
  applyForm.value.deviceIds = [];
  availableRooms.value = [];
  availableDevices.value = [];
  if (!applyForm.value.startTime || !applyForm.value.endTime) {
    return;
  }
  availableRoomsLoading.value = true;
  listAvailableRooms({
    startTime: applyForm.value.startTime,
    endTime: applyForm.value.endTime,
    participantCount: applyForm.value.participantCount
  }).then(response => {
    availableRooms.value = response.data || [];
  }).finally(() => {
    availableRoomsLoading.value = false;
  });
}

function handleApplyRoomChange(roomId) {
  applyForm.value.deviceIds = [];
  availableDevices.value = [];
  if (!roomId) {
    return;
  }
  availableDevicesLoading.value = true;
  listAvailableDevices({ roomId: roomId }).then(response => {
    availableDevices.value = response.data || [];
  }).finally(() => {
    availableDevicesLoading.value = false;
  });
}

function submitApply() {
  proxy.$refs["applyRef"].validate(valid => {
    if (!valid) {
      return;
    }
    if (applyForm.value.startTime >= applyForm.value.endTime) {
      proxy.$modal.msgError("结束时间必须晚于开始时间");
      return;
    }
    applyLoading.value = true;
    applyReservation({
      roomId: applyForm.value.roomId,
      experimentName: applyForm.value.experimentName,
      startTime: applyForm.value.startTime,
      endTime: applyForm.value.endTime,
      participantCount: applyForm.value.participantCount,
      deviceIds: applyForm.value.deviceIds,
      remark: applyForm.value.remark
    }).then(response => {
      const reservationNo = response.data && response.data.reservationNo ? response.data.reservationNo : "";
      proxy.$modal.msgSuccess(reservationNo ? "预约申请已提交，编号：" + reservationNo : "预约申请已提交");
      applyOpen.value = false;
      reservationGetList();
    }).finally(() => {
      applyLoading.value = false;
    });
  });
}

function handleCancel(row) {
  proxy.$modal.confirm('是否确认取消预约“' + row.reservationNo + '”？').then(function() {
    return cancelReservation(row.id);
  }).then(() => {
    proxy.$modal.msgSuccess("预约已取消");
    reservationGetList();
  }).catch(() => {});
}

// 取消按钮
function reservationCancel() {
  reservationOpen.value = false;
  reservationReset();
}

// 表单重置
function reservationReset() {
  reservationForm.value = {
    id: null,
    reservationNo: null,
    applicantId: null,
    roomId: null,
    experimentName: null,
    startTime: null,
    endTime: null,
    participantCount: null,
    riskLevel: null,
    status: null,
    reviewRequired: null,
    remark: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  };
  proxy.resetForm("reservationRef");
}

/** 搜索按钮操作 */
function reservationHandleQuery() {
  reservationQueryParams.value.pageNum = 1;
  reservationGetList();
}

/** 重置按钮操作 */
function reservationResetQuery() {
  dateRange.value = [];
  proxy.resetForm("reservationQueryRef");
  reservationHandleQuery();
}

// 多选框选中数据
function reservationHandleSelectionChange(selection) {
  reservationIds.value = selection.map(item => item.id);
  reservationSingle.value = selection.length != 1;
  reservationMultiple.value = !selection.length;
}

/** 新增按钮操作 */
function reservationHandleAdd() {
  reservationReset();
  reservationOpen.value = true;
  reservationTitle.value = "添加";
}

/** 修改按钮操作 */
function reservationHandleUpdate(row) {
  reservationReset();
  const _id = row.id || reservationIds.value
  getReservation(_id).then(response => {
    reservationForm.value = response.data;
    reservationOpen.value = true;
    reservationTitle.value = "修改";
  });
}

/** 提交按钮 */
function reservationSubmitForm() {
  proxy.$refs["reservationRef"].validate(valid => {
    if (valid) {
        const submitForm = xeUtils.clone(reservationForm.value, true);
      if (submitForm.id != null) {
        updateReservation(submitForm).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          reservationOpen.value = false;
          reservationGetList();
        });
      } else {
        addReservation(submitForm).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          reservationOpen.value = false;
          reservationGetList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function reservationHandleDelete(row) {
  const _ids = row.id || reservationIds.value;
  proxy.$modal.confirm('是否确认删除编号为"' + _ids + '"的数据项？').then(function() {
    return delReservation(_ids);
  }).then(() => {
    reservationGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function reservationHandleExport() {
  proxy.download('module/reservation/export', {
    ...reservationQueryParams.value
  }, `实验室预约_${new Date().getTime()}.xlsx`)
}

/** 下载模板操作 */
function reservationImportTemplate() {
  proxy.download(
      "module/reservation/importTemplate",{},
      `实验室预约_${new Date().getTime()}.xlsx`
  );
}
</script>
<style lang="scss" scoped>
.reservation-tip {
  margin-bottom: 12px;
}
</style>
