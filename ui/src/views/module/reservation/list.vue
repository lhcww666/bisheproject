<template>
  <div class="app-container">
    <el-alert
      v-if="isStudent"
      class="reservation-tip"
      type="info"
      :closable="false"
      show-icon
      title="学生视图：这里只展示你本人的预约。点击任意一行进入预约详情。"
    />
    <el-alert
      v-else-if="isTeacher"
      class="reservation-tip"
      type="info"
      :closable="false"
      show-icon
      title="教师视图：这里展示本人班级学生的预约。点击任意一行进入预约详情并审核。"
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
        <el-input v-model="reservationQueryParams.reservationNoLike" placeholder="请输入预约编号" clearable @keyup.enter="reservationHandleQuery" />
      </el-form-item>
      <el-form-item v-if="!isStudent" label="申请人" prop="applicantId">
        <el-select v-model="reservationQueryParams.applicantId" filterable clearable placeholder="请选择申请人" style="width: 190px;">
          <el-option
            v-for="applicant in applicantOptions"
            :key="applicant.userId"
            :label="applicantOptionLabel(applicant)"
            :value="applicant.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!isStudent" label="实验室" prop="roomId">
        <el-select v-model="reservationQueryParams.roomId" filterable clearable placeholder="请选择实验室" style="width: 210px;">
          <el-option
            v-for="room in roomOptions"
            :key="room.id"
            :label="roomOptionLabel(room)"
            :value="room.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="实验名称" prop="experimentNameLike">
        <el-input v-model="reservationQueryParams.experimentNameLike" placeholder="请输入实验名称" clearable @keyup.enter="reservationHandleQuery" />
      </el-form-item>
      <el-form-item label="预约状态" prop="status">
        <el-select v-model="reservationQueryParams.status" placeholder="请选择预约状态" clearable>
          <el-option v-for="dict in module_reservation_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker clearable v-model="reservationQueryParams.startTime" type="date" value-format="YYYY-MM-DD" placeholder="请选择开始时间"></el-date-picker>
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker clearable v-model="reservationQueryParams.endTime" type="date" value-format="YYYY-MM-DD" placeholder="请选择结束时间"></el-date-picker>
      </el-form-item>
      <el-form-item v-if="!isStudent" label="参与人数" prop="participantCountLike">
        <el-input v-model="reservationQueryParams.participantCountLike" placeholder="请输入参与人数" clearable @keyup.enter="reservationHandleQuery" />
      </el-form-item>
      <el-form-item v-if="!isStudent" label="风险等级" prop="riskLevelLike">
        <el-input v-model="reservationQueryParams.riskLevelLike" placeholder="请输入风险等级" clearable @keyup.enter="reservationHandleQuery" />
      </el-form-item>
      <el-form-item v-if="!isStudent" label="是否需要审核" prop="reviewRequired">
        <el-select v-model="reservationQueryParams.reviewRequired" placeholder="请选择是否需要审核" clearable>
          <el-option v-for="dict in module_yes_no" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="reservationHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="reservationResetQuery">重置</el-button>
        <module-help module="reservation" />
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col v-if="isStudent" :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['module:reservation:apply']">申请预约</el-button>
      </el-col>
      <el-col v-if="canManage" :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['module:reservation:add']">新增</el-button>
      </el-col>
      <el-col v-if="canManage" :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="reservationSingle" @click="handleUpdate" v-hasPermi="['module:reservation:edit']">修改</el-button>
      </el-col>
      <el-col v-if="canManage" :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="reservationMultiple" @click="reservationHandleDelete" v-hasPermi="['module:reservation:remove']">删除</el-button>
      </el-col>
      <el-col v-if="canManage" :span="1.5">
        <el-button type="warning" plain icon="Download" @click="reservationHandleExport" v-hasPermi="['module:reservation:export']">导出</el-button>
      </el-col>
      <el-col v-if="canManage" :span="1.5">
        <el-button type="info" plain icon="Download" @click="reservationImportTemplate" v-hasPermi="['module:reservation:import']">下载模板</el-button>
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
      <right-toolbar v-model:showSearch="reservationShowSearch" @queryTable="reservationGetList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="reservationLoading"
      :data="reservationList"
      :row-class-name="reservationRowClassName"
      @row-click="handleReservationRowClick"
      @selection-change="reservationHandleSelectionChange"
      border
    >
      <el-table-column v-if="canManage" type="selection" width="55" align="center" />
      <el-table-column label="预约编号" align="center" prop="reservationNo" min-width="170" />
      <el-table-column label="申请人" align="center" min-width="110">
        <template #default="scope"><span>{{ scope.row.applicantName || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="预约类型" align="center" width="110">
        <template #default="scope"><dict-tag :options="module_reservation_type" :value="scope.row.reservationType || 'INDIVIDUAL'"/></template>
      </el-table-column>
      <el-table-column label="预约班级" align="center" min-width="120">
        <template #default="scope"><span>{{ scope.row.classDeptName || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="实验室" align="center" min-width="150">
        <template #default="scope"><span>{{ roomLabel(scope.row) }}</span></template>
      </el-table-column>
      <el-table-column label="实验名称" align="center" prop="experimentName" min-width="150" />
      <el-table-column label="使用设备" align="center" min-width="150">
        <template #default="scope"><span>{{ (scope.row.deviceNames || []).join('、') || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" width="180">
        <template #default="scope"><span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span></template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="180">
        <template #default="scope"><span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span></template>
      </el-table-column>
      <el-table-column label="参与人数" align="center" prop="participantCount" width="90" />
      <el-table-column label="预约状态" align="center" prop="status" width="100">
        <template #default="scope"><dict-tag :options="module_reservation_status" :value="scope.row.status"/></template>
      </el-table-column>
      <el-table-column label="审核状态" align="center" width="100">
        <template #default="scope"><dict-tag :options="module_check_status" :value="scope.row.checkStatus || 'no_check'"/></template>
      </el-table-column>
      <el-table-column label="审核人" align="center" prop="checkName" width="100">
        <template #default="scope"><span>{{ scope.row.checkName || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="审核意见" align="center" prop="reviewComment" min-width="150">
        <template #default="scope"><span>{{ scope.row.reviewComment || '—' }}</span></template>
      </el-table-column>
      <el-table-column v-if="isStudent || canManage" fixed="right" width="260" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button v-if="isStudent && scope.row.status === '1'" link type="success" icon="Checked" @click="handleCheckIn(scope.row)" v-hasPermi="['module:reservation:apply']">签到</el-button>
          <el-button v-if="isStudent && scope.row.status === '5'" link type="primary" icon="Finished" @click="handleFinish(scope.row)" v-hasPermi="['module:reservation:apply']">结束实验</el-button>
          <el-button v-if="isStudent && canCancel(scope.row)" link type="danger" icon="CircleClose" @click="handleCancel(scope.row)" v-hasPermi="['module:reservation:cancel']">取消预约</el-button>
          <el-button v-if="canManage" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['module:reservation:edit']">修改</el-button>
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
  </div>
</template>

<script setup name="ReservationList">
import {
  listReservation,
  delReservation,
  cancelReservation,
  checkInReservation,
  finishReservation,
  listReservationApplicantOptions,
  listReservationRoomOptions
} from "@/api/module/reservation";
import useUserStore from "@/store/modules/user";

const dateRange = ref([]);
const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const roleKeys = computed(() => userStore.roles || []);
const isStudent = computed(() => roleKeys.value.includes("student") && !roleKeys.value.includes("teacher") && !roleKeys.value.includes("admin"));
const isTeacher = computed(() => roleKeys.value.includes("teacher") && !roleKeys.value.includes("admin"));
const canManage = computed(() => roleKeys.value.length > 0 && !isStudent.value && !isTeacher.value);

const { proxy } = getCurrentInstance();
const { module_reservation_status, module_yes_no, module_reservation_type, module_check_status } = proxy.useDict('module_reservation_status', 'module_yes_no', 'module_reservation_type', 'module_check_status');

const importRes = ref(null);
const reservationList = ref([]);
const reservationLoading = ref(true);
const reservationShowSearch = ref(true);
const reservationIds = ref([]);
const reservationSingle = ref(true);
const reservationMultiple = ref(true);
const reservationTotal = ref(0);
const applicantOptions = ref([]);
const roomOptions = ref([]);
const reservationData = reactive({
  reservationQueryParams: {
    pageNum: 1, pageSize: 10, reservationNoLike: null, applicantId: null, roomId: null,
    experimentNameLike: null, status: null, startTime: null, endTime: null,
    participantCountLike: null, riskLevelLike: null, reviewRequired: null
  }
});
const { reservationQueryParams } = toRefs(reservationData);

watch(() => importRes.value, (val) => {
  if (val && val.code == 200) reservationHandleQuery();
});

onMounted(() => {
  if (!isStudent.value) {
    loadFilterOptions();
  }
  reservationHandleQuery();
});

function loadFilterOptions() {
  listReservationApplicantOptions().then(response => {
    applicantOptions.value = response.data || [];
  });
  listReservationRoomOptions().then(response => {
    roomOptions.value = response.data || [];
  });
}

function reservationGetList() {
  reservationLoading.value = true;
  listReservation(proxy.addDateRange(reservationQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    reservationList.value = response.rows;
    reservationTotal.value = response.total;
  }).finally(() => {
    reservationLoading.value = false;
  });
}

function reservationHandleQuery() {
  reservationQueryParams.value.pageNum = 1;
  reservationGetList();
}

function reservationResetQuery() {
  dateRange.value = [];
  proxy.resetForm("reservationQueryRef");
  reservationHandleQuery();
}

function reservationHandleSelectionChange(selection) {
  reservationIds.value = selection.map(item => item.id);
  reservationSingle.value = selection.length != 1;
  reservationMultiple.value = !selection.length;
}

function handleAdd() {
  router.push("/reservationAdd");
}

function handleUpdate(row) {
  const id = row.id || reservationIds.value;
  router.push("/reservationEdit/" + id);
}

function handleCancel(row) {
  proxy.$modal.confirm('是否确认取消预约“' + row.reservationNo + '”？').then(() => cancelReservation(row.id)).then(() => {
    proxy.$modal.msgSuccess("预约已取消");
    reservationGetList();
  }).catch(() => {});
}

function handleCheckIn(row) {
  proxy.$modal.confirm('确认签到实验室预约“' + row.reservationNo + '”吗？').then(() => checkInReservation(row.id)).then(() => {
    proxy.$modal.msgSuccess("签到成功");
    reservationGetList();
  }).catch(() => {});
}

function handleFinish(row) {
  proxy.$modal.prompt('请输入实际参与人数', '结束实验').then(({ value }) => {
    const actualCount = Number(value);
    if (!actualCount || actualCount < 1) {
      proxy.$modal.msgError("实际参与人数必须大于 0");
      return Promise.reject();
    }
    return finishReservation(row.id, { actualParticipantCount: actualCount });
  }).then(() => {
    proxy.$modal.msgSuccess("实验已结束，信用分已按规则更新");
    reservationGetList();
  }).catch(() => {});
}

function reservationHandleDelete(row) {
  const ids = row.id || reservationIds.value;
  proxy.$modal.confirm('是否确认删除选中的预约？').then(() => delReservation(ids)).then(() => {
    reservationGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

function reservationHandleExport() {
  proxy.download('module/reservation/export', { ...reservationQueryParams.value }, `实验室预约_${new Date().getTime()}.xlsx`);
}

function reservationImportTemplate() {
  proxy.download("module/reservation/importTemplate", {}, `实验室预约_${new Date().getTime()}.xlsx`);
}

function applicantOptionLabel(applicant) {
  const name = applicant.nickName || applicant.userName || "未命名用户";
  const account = applicant.userName ? "（" + applicant.userName + "）" : "";
  const dept = applicant.deptName ? " · " + applicant.deptName : "";
  return name + account + dept;
}

function roomOptionLabel(room) {
  return [room.roomCode, room.roomName].filter(Boolean).join(" · ");
}

function roomLabel(reservation) {
  const label = [reservation.roomCode, reservation.roomName].filter(Boolean).join(" · ");
  return label || "—";
}

function canCancel(row) {
  return row.status === "0" || row.status === "1";
}

function handleReservationRowClick(row, column) {
  if (column && (column.type === "selection" || column.label === "操作")) return;
  router.push("/reservationView/" + row.id);
}

function reservationRowClassName() {
  return "reservation-clickable-row";
}
</script>
<style lang="scss" scoped>
.reservation-tip { margin-bottom: 12px; }
:deep(.reservation-clickable-row) { cursor: pointer; }
</style>
