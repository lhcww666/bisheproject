<template>
  <div class="app-container">
    <el-alert
      class="material-tip"
      type="info"
      :closable="false"
      show-icon
      :title="isStudent ? '这里只展示本人提交的耗材领用申请；可针对待审核或已通过的本人预约申请耗材。' : '管理员可查看全部领用申请，并在详情页完成审核。'"
    />

    <el-form v-show="showSearch" ref="queryRef" :model="queryParams" :inline="true" label-width="auto" @submit.prevent>
      <el-form-item label="创建时间">
        <el-date-picker
          v-model="dateRange"
          style="width: 230px"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item label="领用编号" prop="applyNoLike">
        <el-input v-model="queryParams.applyNoLike" placeholder="请输入领用编号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择审核状态" clearable>
          <el-option
            v-for="dict in module_material_apply_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="goApply" v-hasPermi="['module:material:apply']">申请领用</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['module:material:apply:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table
      v-loading="loading"
      :data="applyList"
      border
      row-class-name="material-clickable-row"
      @row-click="openView"
    >
      <el-table-column label="领用编号" prop="applyNo" min-width="165" />
      <el-table-column label="预约编号" prop="reservationNo" min-width="165" />
      <el-table-column label="申请人" prop="applicantName" width="110" />
      <el-table-column label="耗材" min-width="190">
        <template #default="scope">
          <div>{{ scope.row.materialName || "—" }}</div>
          <small class="secondary-text">{{ scope.row.materialCode || "" }} {{ scope.row.specification ? "· " + scope.row.specification : "" }}</small>
        </template>
      </el-table-column>
      <el-table-column label="实验室" min-width="160">
        <template #default="scope">{{ roomLabel(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="申请数量" width="125" align="right">
        <template #default="scope">{{ quantity(scope.row.applyQuantity) }} {{ scope.row.unit }}</template>
      </el-table-column>
      <el-table-column label="审核状态" width="110" align="center">
        <template #default="scope">
          <dict-tag :options="module_material_apply_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="审核意见" prop="reviewComment" min-width="180" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.reviewComment || "—" }}</template>
      </el-table-column>
      <el-table-column label="申请时间" width="170" align="center">
        <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click.stop="openView(scope.row)">详情</el-button>
          <el-button
            v-if="canReview && scope.row.status === 'PENDING'"
            link
            type="warning"
            icon="Check"
            @click.stop="openView(scope.row)"
          >审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="MaterialApplyList">
import { listMaterialApply } from "@/api/module/materialApply";
import auth from "@/plugins/auth";
import useUserStore from "@/store/modules/user";

const router = useRouter();
const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const { module_material_apply_status } = proxy.useDict("module_material_apply_status");
const isStudent = computed(() => userStore.roles.includes("student") && !userStore.roles.includes("admin"));
const canReview = computed(() => auth.hasPermi("module:material:review"));
const loading = ref(true);
const showSearch = ref(true);
const applyList = ref([]);
const total = ref(0);
const dateRange = ref([]);
const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    applyNoLike: null,
    status: null
  }
});
const { queryParams } = toRefs(data);

onMounted(getList);

function getList() {
  loading.value = true;
  listMaterialApply(proxy.addDateRange(queryParams.value, dateRange.value, "create_time", true)).then(response => {
    applyList.value = response.rows || [];
    total.value = response.total || 0;
  }).finally(() => {
    loading.value = false;
  });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  dateRange.value = [];
  proxy.resetForm("queryRef");
  handleQuery();
}

function goApply() {
  router.push("/materialApplyAdd");
}

function openView(row, column) {
  if (!row?.id || column?.label === "操作") return;
  router.push("/materialApplyView/" + row.id);
}

function handleExport() {
  proxy.download("module/material/apply/export", { ...queryParams.value }, `耗材领用_${Date.now()}.xlsx`);
}

function roomLabel(row) {
  return [row.roomCode, row.roomName].filter(Boolean).join(" · ") || "—";
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
.secondary-text { color: #909399; }
:deep(.material-clickable-row) { cursor: pointer; }
</style>
