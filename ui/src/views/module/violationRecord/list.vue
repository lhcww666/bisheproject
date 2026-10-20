<template>
  <div class="app-container">
    <el-alert
      v-if="isStudent"
      class="scope-tip"
      type="info"
      :closable="false"
      show-icon
      title="学生视图：只展示本人的违规记录。违规会影响信用分和后续预约资格。"
    />
    <el-alert
      v-else-if="isTeacher"
      class="scope-tip"
      type="info"
      :closable="false"
      show-icon
      title="教师视图：只展示本人班级学生的违规记录。"
    />

    <el-form @submit.prevent :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="auto">
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
      <el-form-item v-if="canManage" label="学生ID" prop="userIdLike">
        <el-input v-model="queryParams.userIdLike" placeholder="请输入学生ID" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="违规类型" prop="violationType">
        <el-select v-model="queryParams.violationType" placeholder="请选择违规类型" clearable>
          <el-option v-for="dict in module_violation_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option v-for="dict in module_violation_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col v-if="canManage" :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openAdd" v-hasPermi="['module:violationRecord:add']">登记违规</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column v-if="canManage" label="学生ID" align="center" prop="userId" width="90" />
      <el-table-column label="学生姓名" align="center" prop="nickName" min-width="120" />
      <el-table-column label="学生账号" align="center" prop="userName" min-width="110" />
      <el-table-column label="班级" align="center" prop="deptName" min-width="130" />
      <el-table-column label="违规类型" align="center" width="130">
        <template #default="scope"><dict-tag :options="module_violation_type" :value="scope.row.violationType" /></template>
      </el-table-column>
      <el-table-column label="违规说明" align="center" prop="description" show-overflow-tooltip min-width="180" />
      <el-table-column label="信用变动" align="center" prop="creditChange" width="100" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope"><dict-tag :options="module_violation_status" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column label="关联预约" align="center" prop="reservationNo" min-width="160">
        <template #default="scope">{{ scope.row.reservationNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="记录时间" align="center" width="170">
        <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" align="center" width="110" fixed="right">
        <template #default="scope">
          <el-button v-if="scope.row.status === '1'" link type="danger" icon="RefreshLeft" @click="handleRevoke(scope.row)" v-hasPermi="['module:violationRecord:edit']">撤销</el-button>
          <span v-else>—</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog title="登记违规" v-model="addOpen" width="560px" append-to-body destroy-on-close>
      <el-form ref="addRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-form-item label="学生ID" prop="userId">
          <el-input v-model="addForm.userId" placeholder="请输入 sys_user.user_id" />
        </el-form-item>
        <el-form-item label="关联预约" prop="reservationId">
          <el-input v-model="addForm.reservationId" placeholder="可不填" />
        </el-form-item>
        <el-form-item label="违规类型" prop="violationType">
          <el-select v-model="addForm.violationType" placeholder="请选择违规类型" style="width: 100%;">
            <el-option v-for="dict in module_violation_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="扣分" prop="creditChange">
          <el-input-number v-model="addForm.creditChange" :min="-20" :max="0" :step="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="违规说明" prop="description">
          <el-input v-model="addForm.description" type="textarea" :rows="3" placeholder="请填写违规经过和处理依据" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="addLoading" @click="submitAdd">确定</el-button>
        <el-button @click="addOpen = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ViolationRecordList">
import { listViolationRecord, addViolationRecord, revokeViolationRecord } from "@/api/module/violationRecord";
import useUserStore from "@/store/modules/user";

const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const roleKeys = computed(() => userStore.roles || []);
const isStudent = computed(() => roleKeys.value.includes("student") && !roleKeys.value.includes("teacher") && !roleKeys.value.includes("admin"));
const isTeacher = computed(() => roleKeys.value.includes("teacher") && !roleKeys.value.includes("admin"));
const canManage = computed(() => !isStudent.value && !isTeacher.value);
const { module_violation_type, module_violation_status } = proxy.useDict('module_violation_type', 'module_violation_status');

const dateRange = ref([]);
const list = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const addOpen = ref(false);
const addLoading = ref(false);
const addRef = ref(null);
const addForm = reactive({ userId: null, reservationId: null, violationType: null, creditChange: -5, description: null });
const addRules = {
  userId: [{ required: true, message: "学生ID不能为空", trigger: "blur" }],
  violationType: [{ required: true, message: "违规类型不能为空", trigger: "change" }],
  creditChange: [{ required: true, message: "扣分不能为空", trigger: "change" }]
};
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  userIdLike: null,
  violationType: null,
  status: null
});

onMounted(getList);

function getList() {
  loading.value = true;
  listViolationRecord(proxy.addDateRange(queryParams, dateRange.value, 'create_time', true)).then(response => {
    list.value = response.rows;
    total.value = response.total;
  }).finally(() => {
    loading.value = false;
  });
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  dateRange.value = [];
  proxy.resetForm("queryRef");
  handleQuery();
}

function openAdd() {
  Object.assign(addForm, { userId: null, reservationId: null, violationType: null, creditChange: -5, description: null });
  addOpen.value = true;
}

function submitAdd() {
  addRef.value.validate(valid => {
    if (!valid) return;
    addLoading.value = true;
    addViolationRecord({
      userId: Number(addForm.userId),
      reservationId: addForm.reservationId ? Number(addForm.reservationId) : null,
      violationType: addForm.violationType,
      creditChange: addForm.creditChange,
      description: addForm.description
    }).then(() => {
      proxy.$modal.msgSuccess("违规已登记，信用分已扣减");
      addOpen.value = false;
      getList();
    }).finally(() => {
      addLoading.value = false;
    });
  });
}

function handleRevoke(row) {
  proxy.$modal.prompt('请输入撤销原因').then(({ value }) => {
    if (!value) {
      proxy.$modal.msgError("撤销原因不能为空");
      return Promise.reject();
    }
    return revokeViolationRecord(row.id, value);
  }).then(() => {
    proxy.$modal.msgSuccess("违规已撤销，信用分已回补");
    getList();
  }).catch(() => {});
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>

<style lang="scss" scoped>
.scope-tip { margin-bottom: 12px; }
</style>
