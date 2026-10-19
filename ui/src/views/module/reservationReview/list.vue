<template>
  <div class="app-container">
    <el-form @submit.prevent :model="reservationReviewQueryParams" ref="reservationReviewQueryRef" :inline="true" v-show="reservationReviewShowSearch" label-width="auto">
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
      <el-form-item label="预约ID" prop="reservationIdLike">
        <el-input v-model="reservationReviewQueryParams.reservationIdLike" placeholder="请输入预约ID" clearable @keyup.enter="reservationReviewHandleQuery" />
      </el-form-item>
      <el-form-item label="审核人ID" prop="reviewerIdLike">
        <el-input v-model="reservationReviewQueryParams.reviewerIdLike" placeholder="请输入审核人ID（sys_user.user_id）" clearable @keyup.enter="reservationReviewHandleQuery" />
      </el-form-item>
      <el-form-item label="审核结果" prop="result">
        <el-select v-model="reservationReviewQueryParams.result" placeholder="请选择审核结果" clearable>
          <el-option v-for="dict in module_review_result" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="审核时间" prop="reviewTime">
        <el-date-picker clearable v-model="reservationReviewQueryParams.reviewTime" type="date" value-format="YYYY-MM-DD" placeholder="请选择审核时间"></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="reservationReviewHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="reservationReviewResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="reservationReviewHandleExport" v-hasPermi="['module:reservationReview:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <module-help module="reservationReview" />
      </el-col>
      <right-toolbar v-model:showSearch="reservationReviewShowSearch" @queryTable="reservationReviewGetList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="reservationReviewLoading"
      :data="reservationReviewList"
      :row-class-name="reservationReviewRowClassName"
      border
      @row-click="reservationReviewHandleRowClick"
    >
      <el-table-column label="预约ID" align="center" prop="reservationId" />
      <el-table-column label="审核人ID" align="center" prop="reviewerId" />
      <el-table-column label="审核结果" align="center" prop="result">
        <template #default="scope">
          <dict-tag :options="module_review_result" :value="scope.row.result"/>
        </template>
      </el-table-column>
      <el-table-column label="审核意见" align="center" prop="reviewComment" show-overflow-tooltip />
      <el-table-column label="审核时间" align="center" prop="reviewTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.reviewTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="reservationReviewTotal>0"
      :total="reservationReviewTotal"
      v-model:page="reservationReviewQueryParams.pageNum"
      v-model:limit="reservationReviewQueryParams.pageSize"
      @pagination="reservationReviewGetList"
    />
  </div>
</template>

<script setup name="ReservationReviewList">
import { listReservationReview } from "@/api/module/reservationReview";

const dateRange = ref([]);
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_review_result } = proxy.useDict('module_review_result');

const reservationReviewPageBase = "/business/module/reservationReview";
const reservationReviewList = ref([]);
const reservationReviewLoading = ref(true);
const reservationReviewShowSearch = ref(true);
const reservationReviewTotal = ref(0);
const reservationReviewData = reactive({
  reservationReviewQueryParams: {
    pageNum: 1,
    pageSize: 10,
    reservationIdLike: null,
    reviewerIdLike: null,
    result: null,
    reviewTime: null
  }
});
const { reservationReviewQueryParams } = toRefs(reservationReviewData);

onMounted(() => reservationReviewHandleQuery());

function reservationReviewGetList() {
  reservationReviewLoading.value = true;
  listReservationReview(proxy.addDateRange(reservationReviewQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    reservationReviewList.value = response.rows;
    reservationReviewTotal.value = response.total;
  }).finally(() => {
    reservationReviewLoading.value = false;
  });
}

function reservationReviewHandleQuery() {
  reservationReviewQueryParams.value.pageNum = 1;
  reservationReviewGetList();
}

function reservationReviewResetQuery() {
  dateRange.value = [];
  proxy.resetForm("reservationReviewQueryRef");
  reservationReviewHandleQuery();
}

function reservationReviewHandleRowClick(row, column) {
  if (!row || !row.id) return;
  if (column && (column.type === "selection" || column.label === "操作")) return;
  router.push(reservationReviewPageBase + "/view/" + row.id);
}

function reservationReviewRowClassName() {
  return "reservation-review-clickable-row";
}

function reservationReviewHandleExport() {
  proxy.download('module/reservationReview/export', { ...reservationReviewQueryParams.value }, `预约审核_${new Date().getTime()}.xlsx`);
}
</script>

<style lang="scss" scoped>
:deep(.reservation-review-clickable-row) {
  cursor: pointer;
}
</style>
