<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goList">返回列表</el-button>
      </el-col>
      <el-col v-if="canReview && detail.status === 'PENDING'" :span="1.5">
        <el-button type="warning" plain icon="Check" @click="openReview" v-hasPermi="['module:material:review']">审核</el-button>
      </el-col>
    </el-row>

    <el-alert
      v-if="detail.status === 'PENDING'"
      class="material-tip"
      type="warning"
      :closable="false"
      show-icon
      title="申请待审核，尚未扣减库存。"
    />
    <el-alert
      v-else-if="detail.status === 'APPROVED'"
      class="material-tip"
      type="success"
      :closable="false"
      show-icon
      title="申请已通过，库存已扣减并记录领用出库流水。"
    />
    <el-alert
      v-else-if="detail.status === 'RETURNED'"
      class="material-tip"
      type="info"
      :closable="false"
      show-icon
      title="预约取消后耗材已归还，库存已回补并生成归还流水。"
    />

    <el-card>
      <el-descriptions title="耗材领用申请" :column="2" border>
        <el-descriptions-item label="领用编号">{{ detail.applyNo || "—" }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <dict-tag :options="module_material_apply_status" :value="detail.status" />
        </el-descriptions-item>
        <el-descriptions-item label="关联预约">{{ detail.reservationNo || "—" }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicantName || "—" }}</el-descriptions-item>
        <el-descriptions-item label="耗材编号">{{ detail.materialCode || "—" }}</el-descriptions-item>
        <el-descriptions-item label="耗材名称">{{ detail.materialName || "—" }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detail.specification || "—" }}</el-descriptions-item>
        <el-descriptions-item label="所属实验室">{{ roomLabel }}</el-descriptions-item>
        <el-descriptions-item label="申请数量">{{ quantity(detail.applyQuantity) }} {{ detail.unit }}</el-descriptions-item>
        <el-descriptions-item label="当前库存">{{ quantity(detail.currentStock) }} {{ detail.unit }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detail.reviewerName || "—" }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ formatDate(detail.reviewTime) }}</el-descriptions-item>
        <el-descriptions-item label="发放时间">{{ formatDate(detail.issueTime) }}</el-descriptions-item>
        <el-descriptions-item label="归还时间">{{ formatDate(detail.returnTime) }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatDate(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="申请说明">{{ detail.remark || "—" }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">{{ detail.reviewComment || "—" }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-dialog v-model="reviewVisible" title="审核耗材领用" width="540px" append-to-body destroy-on-close>
      <el-alert
        class="review-tip"
        type="info"
        :closable="false"
        show-icon
        title="通过时将立即扣减库存；库存不足会自动驳回。驳回时必须填写原因。"
      />
      <el-form label-width="90px">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="reviewForm.status">
            <el-radio value="APPROVED">通过</el-radio>
            <el-radio value="REJECTED">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" :required="reviewForm.status === 'REJECTED'">
          <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请填写审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="reviewing" @click="submitReview">提交审核</el-button>
        <el-button @click="reviewVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MaterialApplyView">
import { getMaterialApply, reviewMaterialApply } from "@/api/module/materialApply";
import auth from "@/plugins/auth";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_material_apply_status } = proxy.useDict("module_material_apply_status");
const loading = ref(true);
const detail = ref({});
const reviewVisible = ref(false);
const reviewing = ref(false);
const reviewForm = reactive({ status: "APPROVED", reviewComment: "" });
const canReview = computed(() => auth.hasPermi("module:material:review"));
const roomLabel = computed(() => [detail.value.roomCode, detail.value.roomName].filter(Boolean).join(" · ") || "—");

onMounted(loadDetail);

function loadDetail() {
  loading.value = true;
  getMaterialApply(route.params.id).then(response => {
    detail.value = response.data || {};
  }).finally(() => {
    loading.value = false;
  });
}

function openReview() {
  reviewForm.status = "APPROVED";
  reviewForm.reviewComment = "";
  reviewVisible.value = true;
}

function submitReview() {
  if (reviewForm.status === "REJECTED" && !reviewForm.reviewComment.trim()) {
    proxy.$modal.msgError("驳回时必须填写审核意见");
    return;
  }
  reviewing.value = true;
  reviewMaterialApply(detail.value.id, { ...reviewForm }).then(response => {
    const result = response?.data?.status;
    const label = result === "APPROVED" ? "已通过并扣减库存" : "已驳回";
    proxy.$modal.msgSuccess("审核完成：" + label);
    reviewVisible.value = false;
    loadDetail();
  }).finally(() => {
    reviewing.value = false;
  });
}

function goList() {
  router.push("/module/material-apply");
}

function quantity(value) {
  return Number(value || 0).toFixed(2);
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>

<style lang="scss" scoped>
.material-tip { margin-bottom: 12px; }
.review-tip { margin-bottom: 16px; }
</style>
