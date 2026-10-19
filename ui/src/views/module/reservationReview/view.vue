<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goList">返回列表</el-button>
      </el-col>
    </el-row>

    <el-alert class="readonly-tip" type="info" :closable="false" show-icon title="审核记录由预约审核动作生成，不支持手工新增、修改或删除。" />

    <el-card class="detail-card">
      <el-descriptions title="预约审核" :column="2" border>
        <el-descriptions-item label="预约ID">{{ detail.reservationId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="审核人ID">{{ detail.reviewerId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="审核结果">
          <dict-tag :options="module_review_result" :value="detail.result"/>
        </el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ formatDate(detail.reviewTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">{{ detail.reviewComment || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup name="ReservationReviewView">
import { getReservationReview } from "@/api/module/reservationReview";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_review_result } = proxy.useDict('module_review_result');

const reservationReviewListPath = "/module/reservationReview";
const loading = ref(true);
const detail = ref({});

onMounted(loadDetail);

function loadDetail() {
  loading.value = true;
  getReservationReview(route.params.id).then(response => {
    detail.value = response.data || {};
  }).finally(() => {
    loading.value = false;
  });
}

function goList() {
  router.push(reservationReviewListPath);
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>

<style lang="scss" scoped>
.readonly-tip { margin-bottom: 12px; }
.detail-card { margin-top: 4px; }
</style>
