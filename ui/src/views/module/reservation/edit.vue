<template>
  <div class="app-container" v-loading="loading">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>修改预约</span>
          <el-button icon="Back" @click="goBack">返回列表</el-button>
        </div>
      </template>

      <el-alert
        class="reservation-tip"
        type="info"
        :closable="false"
        show-icon
        title="修改后会重新进入待审核，原审核记录会被清除，需要教师重新审核。"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width: 680px;">
        <el-form-item label="预约编号" prop="reservationNo"><el-input v-model="form.reservationNo" placeholder="请输入预约编号" /></el-form-item>
        <el-form-item label="申请人ID" prop="applicantId"><el-input v-model="form.applicantId" placeholder="请输入申请人用户ID" /></el-form-item>
        <el-form-item label="实验室ID" prop="roomId"><el-input v-model="form.roomId" placeholder="请输入实验室ID" /></el-form-item>
        <el-form-item label="实验名称" prop="experimentName"><el-input v-model="form.experimentName" placeholder="请输入实验名称" /></el-form-item>
        <el-form-item label="开始时间" prop="startTime"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择开始时间" style="width: 100%;" /></el-form-item>
        <el-form-item label="结束时间" prop="endTime"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择结束时间" style="width: 100%;" /></el-form-item>
        <el-form-item label="参与人数" prop="participantCount"><el-input v-model="form.participantCount" placeholder="请输入参与人数" /></el-form-item>
        <el-form-item label="风险等级" prop="riskLevel"><el-input v-model="form.riskLevel" placeholder="请输入风险等级" /></el-form-item>
        <el-form-item label="实验说明" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入内容" /></el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
        <el-button @click="goBack">取 消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="ReservationEdit">
import { getReservation, updateReservation } from "@/api/module/reservation";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const loading = ref(true);
const saving = ref(false);
const form = reactive({
  id: null, reservationNo: null, applicantId: null, roomId: null, experimentName: null,
  startTime: null, endTime: null, participantCount: null, riskLevel: null, remark: null
});
const rules = {
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
  getReservation(route.params.id).then(response => {
    Object.assign(form, response.data || {});
    if (form.status === "1") {
      proxy.$modal.msgError("该预约已审核通过，请先弃审再修改");
      router.replace("/reservationView/" + form.id);
    }
  }).finally(() => {
    loading.value = false;
  });
});

function submit() {
  proxy.$refs.formRef.validate(valid => {
    if (!valid) return;
    saving.value = true;
    updateReservation({ ...form }).then(() => {
      proxy.$modal.msgSuccess("修改成功，预约已重新进入待审核");
      router.push("/reservationView/" + form.id);
    }).finally(() => {
      saving.value = false;
    });
  });
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
