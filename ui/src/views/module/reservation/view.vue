<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goList">返回列表</el-button>
      </el-col>
      <el-col v-if="canManage" :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="goAdd" v-hasPermi="['module:reservation:add']">新增</el-button>
      </el-col>
      <el-col v-if="canManage && detail.status !== '1'" :span="1.5">
        <el-button type="success" plain icon="Edit" @click="goEdit" v-hasPermi="['module:reservation:edit']">编辑</el-button>
      </el-col>
      <el-col v-if="canReview && detail.status === '0'" :span="1.5">
        <el-button type="warning" plain icon="Check" @click="reviewOpen = true" v-hasPermi="['module:reservation:review']">审核</el-button>
      </el-col>
      <el-col v-if="canReview && detail.status === '1' && detail.checkStatus === 'had_checked'" :span="1.5">
        <el-button type="danger" plain icon="RefreshLeft" @click="unauditOpen = true" v-hasPermi="['module:reservation:review']">弃审</el-button>
      </el-col>
      <el-col v-if="isStudent && detail.status === '1'" :span="1.5">
        <el-button type="success" plain icon="Checked" @click="handleCheckIn" v-hasPermi="['module:reservation:apply']">签到</el-button>
      </el-col>
      <el-col v-if="isStudent && detail.status === '5'" :span="1.5">
        <el-button type="primary" plain icon="Finished" @click="handleFinish" v-hasPermi="['module:reservation:apply']">结束实验</el-button>
      </el-col>
      <el-col v-if="canManage && detail.status !== '1'" :span="1.5">
        <el-button type="danger" plain icon="Delete" @click="removeReservation" v-hasPermi="['module:reservation:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-alert
      v-if="detail.status === '1'"
      class="reservation-tip"
      type="warning"
      :closable="false"
      show-icon
      title="单据已审核通过，不能直接编辑或删除；如需修改，请先弃审。"
    />

    <el-card class="detail-card">
      <el-descriptions title="预约信息" :column="2" border>
        <el-descriptions-item label="预约编号">{{ detail.reservationNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="预约类型">
          <dict-tag :options="module_reservation_type" :value="detail.reservationType || 'INDIVIDUAL'"/>
        </el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicantName || detail.applicantId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="预约班级">{{ detail.classDeptName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="实验室">{{ detail.roomName || detail.roomId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="实验名称">{{ detail.experimentName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDate(detail.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatDate(detail.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="参与人数">{{ detail.participantCount || '—' }}</el-descriptions-item>
        <el-descriptions-item label="风险等级">{{ detail.riskLevel || '—' }}</el-descriptions-item>
        <el-descriptions-item label="预约状态">
          <dict-tag :options="module_reservation_status" :value="detail.status"/>
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <dict-tag :options="module_check_status" :value="detail.checkStatus || 'no_check'"/>
        </el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detail.checkName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ formatDate(detail.checkDate) }}</el-descriptions-item>
        <el-descriptions-item label="签到时间">{{ formatDate(detail.checkInTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDate(detail.finishTime) }}</el-descriptions-item>
        <el-descriptions-item label="实际参与人数">{{ detail.actualParticipantCount || '—' }}</el-descriptions-item>
        <el-descriptions-item label="取消时间">{{ formatDate(detail.cancelTime) }}</el-descriptions-item>
        <el-descriptions-item label="取消原因">{{ detail.cancelReason || '—' }}</el-descriptions-item>
        <el-descriptions-item label="实验说明" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="detail-section-title">使用设备</h4>
      <el-table :data="detail.deviceList || []" border size="small" empty-text="未选择设备">
        <el-table-column label="设备编号" align="center" prop="deviceCode" />
        <el-table-column label="设备名称" align="center" prop="deviceName" />
        <el-table-column label="数量" align="center" prop="quantity" width="80" />
      </el-table>

      <h4 class="detail-section-title">审核历史</h4>
      <el-table :data="detail.reviewList || []" border size="small" empty-text="暂无审核记录">
        <el-table-column label="审核人" align="center" prop="reviewerName" width="120" />
        <el-table-column label="结果" align="center" width="90">
          <template #default="scope">
            <dict-tag :options="module_review_result" :value="scope.row.result"/>
          </template>
        </el-table-column>
        <el-table-column label="审核意见" align="center" prop="reviewComment" show-overflow-tooltip />
        <el-table-column label="审核时间" align="center" width="170">
          <template #default="scope">{{ formatDate(scope.row.reviewTime) }}</template>
        </el-table-column>
      </el-table>

      <h4 class="detail-section-title">状态变更历史</h4>
      <el-table :data="detail.statusLogList || []" border size="small" empty-text="暂无状态记录">
        <el-table-column label="动作" align="center" prop="action" width="140" />
        <el-table-column label="状态变化" align="center" width="150">
          <template #default="scope">{{ statusLabel(scope.row.fromStatus) }} → {{ statusLabel(scope.row.toStatus) }}</template>
        </el-table-column>
        <el-table-column label="操作人" align="center" prop="operatorName" width="120" />
        <el-table-column label="说明" align="center" prop="reason" show-overflow-tooltip />
        <el-table-column label="时间" align="center" width="170">
          <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog title="审核预约" v-model="reviewOpen" width="520px" append-to-body destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="预约编号">{{ detail.reservationNo }}</el-form-item>
        <el-form-item label="申请人">{{ detail.applicantName || detail.applicantId }}</el-form-item>
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="reviewForm.result">
            <el-radio label="1">通过</el-radio>
            <el-radio label="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" :required="reviewForm.result === '2'">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="3" placeholder="驳回时必须填写原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="reviewLoading" @click="submitReview">提交审核</el-button>
        <el-button @click="reviewOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <el-dialog title="弃审预约" v-model="unauditOpen" width="520px" append-to-body destroy-on-close>
      <el-alert
        class="reservation-tip"
        type="warning"
        :closable="false"
        show-icon
        title="弃审后预约退回待审核，本次审核记录删除，预约单保留。"
      />
      <el-form label-width="90px">
        <el-form-item label="预约编号">{{ detail.reservationNo }}</el-form-item>
        <el-form-item label="弃审原因" required>
          <el-input v-model="unauditForm.comment" type="textarea" :rows="3" placeholder="请填写弃审原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="danger" :loading="unauditLoading" @click="submitUnaudit">确认弃审</el-button>
        <el-button @click="unauditOpen = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ReservationView">
import { getReservationDetail, updateReservationStatus, delReservation, checkInReservation, finishReservation } from "@/api/module/reservation";
import useUserStore from "@/store/modules/user";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const { proxy } = getCurrentInstance();
const { module_reservation_status, module_check_status, module_reservation_type, module_review_result } = proxy.useDict('module_reservation_status', 'module_check_status', 'module_reservation_type', 'module_review_result');

const roleKeys = computed(() => userStore.roles || []);
const isStudent = computed(() => roleKeys.value.includes("student") && !roleKeys.value.includes("teacher") && !roleKeys.value.includes("admin"));
const isTeacher = computed(() => roleKeys.value.includes("teacher") && !roleKeys.value.includes("admin"));
const canManage = computed(() => roleKeys.value.length > 0 && !isStudent.value && !isTeacher.value);
const canReview = computed(() => isTeacher.value || canManage.value);

const loading = ref(true);
const detail = ref({});
const reviewOpen = ref(false);
const reviewLoading = ref(false);
const reviewForm = reactive({ result: "1", comment: "" });
const unauditOpen = ref(false);
const unauditLoading = ref(false);
const unauditForm = reactive({ comment: "" });

onMounted(loadDetail);

function loadDetail() {
  loading.value = true;
  getReservationDetail(route.params.id).then(response => {
    detail.value = response.data || {};
  }).finally(() => {
    loading.value = false;
  });
}

function goList() {
  router.push("/module/reservation");
}

function goAdd() {
  router.push("/reservationAdd");
}

function goEdit() {
  router.push("/reservationEdit/" + detail.value.id);
}

function submitReview() {
  if (!reviewForm.result) {
    proxy.$modal.msgError("请选择审核结果");
    return;
  }
  if (reviewForm.result === "2" && !reviewForm.comment) {
    proxy.$modal.msgError("驳回预约时必须填写审核意见");
    return;
  }
  reviewLoading.value = true;
  updateReservationStatus({
    id: detail.value.id,
    action: reviewForm.result === "1" ? "APPROVE" : "REJECT",
    comment: reviewForm.comment
  }).then(() => {
    proxy.$modal.msgSuccess("审核成功");
    reviewOpen.value = false;
    loadDetail();
  }).finally(() => {
    reviewLoading.value = false;
  });
}

function submitUnaudit() {
  if (!unauditForm.comment) {
    proxy.$modal.msgError("弃审必须填写原因");
    return;
  }
  unauditLoading.value = true;
  updateReservationStatus({
    id: detail.value.id,
    action: "UNAUDIT",
    comment: unauditForm.comment
  }).then(() => {
    proxy.$modal.msgSuccess("弃审成功，预约已回到待审核");
    unauditOpen.value = false;
    loadDetail();
  }).finally(() => {
    unauditLoading.value = false;
  });
}

function removeReservation() {
  proxy.$modal.confirm('是否确认删除预约“' + detail.value.reservationNo + '”？').then(() => delReservation(detail.value.id)).then(() => {
    proxy.$modal.msgSuccess("删除成功");
    goList();
  }).catch(() => {});
}

function handleCheckIn() {
  proxy.$modal.confirm('确认签到实验室预约“' + detail.value.reservationNo + '”吗？').then(() => checkInReservation(detail.value.id)).then(() => {
    proxy.$modal.msgSuccess("签到成功");
    loadDetail();
  }).catch(() => {});
}

function handleFinish() {
  proxy.$modal.prompt('请输入实际参与人数', '结束实验').then(({ value }) => {
    const actualCount = Number(value);
    if (!actualCount || actualCount < 1) {
      proxy.$modal.msgError("实际参与人数必须大于 0");
      return Promise.reject();
    }
    return finishReservation(detail.value.id, { actualParticipantCount: actualCount });
  }).then(() => {
    proxy.$modal.msgSuccess("实验已结束，信用分已按规则更新");
    loadDetail();
  }).catch(() => {});
}

function statusLabel(value) {
  const labels = { "0": "待审核", "1": "已通过", "2": "已驳回", "3": "已取消", "4": "已完成", "5": "已签到", "6": "已爽约" };
  return labels[value] || "—";
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>
<style lang="scss" scoped>
.reservation-tip { margin-bottom: 12px; }
.detail-card { margin-top: 4px; }
.detail-section-title { margin: 20px 0 10px; font-size: 14px; color: #409eff; }
</style>
