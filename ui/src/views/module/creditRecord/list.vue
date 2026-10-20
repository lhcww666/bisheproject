<template>
  <div class="app-container">
    <el-alert
      v-if="isStudent"
      class="scope-tip"
      type="info"
      :closable="false"
      show-icon
      title="学生视图：只展示本人的信用分流水，其他学生的记录不可见。"
    />
    <el-alert
      v-else-if="isTeacher"
      class="scope-tip"
      type="info"
      :closable="false"
      show-icon
      title="教师视图：只展示本人班级学生的信用分流水。"
    />

    <el-card v-if="summary" class="credit-summary" shadow="never">
      <el-row :gutter="20" align="middle">
        <el-col :span="6">
          <div class="summary-label">当前信用分</div>
          <div class="summary-value" :class="{ danger: !summary.canBook }">{{ summary.score }}</div>
        </el-col>
        <el-col :span="6">
          <div class="summary-label">预约门槛</div>
          <div class="summary-value">{{ summary.minScore }} 分</div>
        </el-col>
        <el-col :span="12">
          <div class="summary-label">预约资格</div>
          <el-tag :type="summary.canBook ? 'success' : 'danger'">
            {{ summary.canBook ? '可以提交预约' : '信用分不足，暂不能预约' }}
          </el-tag>
        </el-col>
      </el-row>
    </el-card>

    <el-form @submit.prevent :model="creditRecordQueryParams" ref="creditRecordQueryRef" :inline="true" v-show="creditRecordShowSearch" label-width="auto">
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
      <el-form-item v-if="!isStudent" label="用户ID" prop="userIdLike">
        <el-input v-model="creditRecordQueryParams.userIdLike" placeholder="请输入用户ID" clearable @keyup.enter="creditRecordHandleQuery" />
      </el-form-item>
      <el-form-item label="变动分值" prop="changeScoreLike">
        <el-input v-model="creditRecordQueryParams.changeScoreLike" placeholder="请输入变动分值（可正可负）" clearable @keyup.enter="creditRecordHandleQuery" />
      </el-form-item>
      <el-form-item label="变动后分值" prop="balanceScoreLike">
        <el-input v-model="creditRecordQueryParams.balanceScoreLike" placeholder="请输入变动后分值" clearable @keyup.enter="creditRecordHandleQuery" />
      </el-form-item>
      <el-form-item label="变动原因" prop="reasonLike">
        <el-input v-model="creditRecordQueryParams.reasonLike" placeholder="请输入变动原因" clearable @keyup.enter="creditRecordHandleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="creditRecordHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="creditRecordResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col v-if="canManage" :span="1.5">
        <el-button type="primary" plain icon="Edit" @click="openAdjust" v-hasPermi="['module:creditRecord:adjust']">调整信用分</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="creditRecordHandleExport" v-hasPermi="['module:creditRecord:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Download" @click="creditRecordImportTemplate" v-hasPermi="['module:creditRecord:import']">下载模板</el-button>
      </el-col>
      <el-col :span="1.5">
        <file-upload
          v-model="importRes"
          v-hasPermi="['module:creditRecord:import']"
          :limit="1"
          uploadBtnText="导入"
          :fileType="['xlsx']"
          accept=".xlsx"
          :action="'/module/creditRecord/importData'"
          :isShowTip="false"
          :isShowFileList="false"
          isImport
        />
      </el-col>
      <el-col :span="1.5">
        <module-help module="creditRecord" />
      </el-col>
      <right-toolbar v-model:showSearch="creditRecordShowSearch" @queryTable="creditRecordGetList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="creditRecordLoading"
      :data="creditRecordList"
      :row-class-name="creditRecordRowClassName"
      border
      @row-click="creditRecordHandleRowClick"
    >
      <el-table-column v-if="!isStudent" label="用户ID" align="center" prop="userId" />
      <el-table-column label="学生姓名" align="center" prop="nickName" min-width="120" />
      <el-table-column label="学生账号" align="center" prop="userName" min-width="110" />
      <el-table-column label="班级" align="center" prop="deptName" min-width="130" />
      <el-table-column label="变动分值" align="center" prop="changeScore" />
      <el-table-column label="变动后分值" align="center" prop="balanceScore" />
      <el-table-column label="变动原因" align="center" prop="reason" show-overflow-tooltip />
    </el-table>

    <pagination
      v-show="creditRecordTotal>0"
      :total="creditRecordTotal"
      v-model:page="creditRecordQueryParams.pageNum"
      v-model:limit="creditRecordQueryParams.pageSize"
      @pagination="creditRecordGetList"
    />

    <el-dialog title="调整信用分" v-model="adjustOpen" width="520px" append-to-body destroy-on-close>
      <el-form ref="adjustRef" :model="adjustForm" :rules="adjustRules" label-width="90px">
        <el-form-item label="学生ID" prop="userId">
          <el-input v-model="adjustForm.userId" placeholder="请输入 sys_user.user_id" />
        </el-form-item>
        <el-form-item label="变动分值" prop="changeScore">
          <el-input-number v-model="adjustForm.changeScore" :min="-20" :max="20" :step="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="调整原因" prop="reason">
          <el-input v-model="adjustForm.reason" type="textarea" :rows="3" placeholder="请填写调整依据" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="adjustLoading" @click="submitAdjust">确定</el-button>
        <el-button @click="adjustOpen = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CreditRecordList">
import { listCreditRecord, getCreditSummary, adjustCredit } from "@/api/module/creditRecord";
import useUserStore from "@/store/modules/user";

const dateRange = ref([]);
const router = useRouter();
const userStore = useUserStore();
const { proxy } = getCurrentInstance();
const isStudent = computed(() =>
  (userStore.roles || []).includes("student") &&
  !(userStore.roles || []).includes("teacher") &&
  !(userStore.roles || []).includes("admin")
);
const isTeacher = computed(() =>
  (userStore.roles || []).includes("teacher") &&
  !(userStore.roles || []).includes("admin")
);
const canManage = computed(() => !isStudent.value && !isTeacher.value);

const creditRecordPageBase = "/business/module/creditRecord";
const importRes = ref(null);
const creditRecordList = ref([]);
const creditRecordLoading = ref(true);
const creditRecordShowSearch = ref(true);
const creditRecordTotal = ref(0);
const summary = ref(null);
const adjustOpen = ref(false);
const adjustLoading = ref(false);
const adjustRef = ref(null);
const adjustForm = reactive({ userId: null, changeScore: -5, reason: null });
const adjustRules = {
  userId: [{ required: true, message: "学生ID不能为空", trigger: "blur" }],
  changeScore: [{ required: true, message: "变动分值不能为空", trigger: "change" }],
  reason: [{ required: true, message: "调整原因不能为空", trigger: "blur" }]
};
const creditRecordData = reactive({
  creditRecordQueryParams: {
    pageNum: 1,
    pageSize: 10,
    userIdLike: null,
    changeScoreLike: null,
    balanceScoreLike: null,
    reasonLike: null
  }
});
const { creditRecordQueryParams } = toRefs(creditRecordData);

watch(() => importRes.value, (val) => {
  if (val && val.code == 200) {
    creditRecordHandleQuery();
  }
}, { deep: true });

onMounted(() => {
  creditRecordHandleQuery();
  getCreditSummary().then(response => {
    summary.value = response.data || null;
  });
});

function creditRecordGetList() {
  creditRecordLoading.value = true;
  listCreditRecord(proxy.addDateRange(creditRecordQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    creditRecordList.value = response.rows;
    creditRecordTotal.value = response.total;
  }).finally(() => {
    creditRecordLoading.value = false;
  });
}

function creditRecordHandleQuery() {
  creditRecordQueryParams.value.pageNum = 1;
  creditRecordGetList();
}

function creditRecordResetQuery() {
  dateRange.value = [];
  proxy.resetForm("creditRecordQueryRef");
  creditRecordHandleQuery();
}

function creditRecordHandleRowClick(row, column) {
  if (!row || !row.id) return;
  if (column && (column.type === "selection" || column.label === "操作")) return;
  router.push(creditRecordPageBase + "/view/" + row.id);
}

function creditRecordRowClassName() {
  return "credit-record-clickable-row";
}

function creditRecordHandleExport() {
  proxy.download('module/creditRecord/export', { ...creditRecordQueryParams.value }, `信用分流水_${new Date().getTime()}.xlsx`);
}

function creditRecordImportTemplate() {
  proxy.download("module/creditRecord/importTemplate", {}, `信用分流水_${new Date().getTime()}.xlsx`);
}

function openAdjust() {
  Object.assign(adjustForm, { userId: null, changeScore: -5, reason: null });
  adjustOpen.value = true;
}

function submitAdjust() {
  adjustRef.value.validate(valid => {
    if (!valid) return;
    adjustLoading.value = true;
    adjustCredit({
      userId: Number(adjustForm.userId),
      changeScore: adjustForm.changeScore,
      reason: adjustForm.reason
    }).then(() => {
      proxy.$modal.msgSuccess("信用分已调整");
      adjustOpen.value = false;
      creditRecordGetList();
      getCreditSummary().then(response => {
        summary.value = response.data || null;
      });
    }).finally(() => {
      adjustLoading.value = false;
    });
  });
}
</script>

<style lang="scss" scoped>
.scope-tip { margin-bottom: 12px; }
.credit-summary { margin-bottom: 12px; }
.summary-label { color: #909399; font-size: 13px; margin-bottom: 4px; }
.summary-value { font-size: 22px; font-weight: 600; color: #303133; }
.summary-value.danger { color: #f56c6c; }
:deep(.credit-record-clickable-row) { cursor: pointer; }
</style>
