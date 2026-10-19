<template>
  <div class="app-container">
    <el-alert
      v-if="isStudent"
      class="scope-tip"
      type="info"
      :closable="false"
      show-icon
      title="学生视图：只展示本人的安全考试成绩，其他学生的记录不可见。"
    />
    <el-alert
      v-else-if="isTeacher"
      class="scope-tip"
      type="info"
      :closable="false"
      show-icon
      title="教师视图：只展示本人班级学生的安全考试记录。"
    />

    <el-form @submit.prevent :model="safetyExamRecordQueryParams" ref="safetyExamRecordQueryRef" :inline="true" v-show="safetyExamRecordShowSearch" label-width="auto">
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
      <el-form-item v-if="!isStudent" label="学生ID" prop="studentIdLike">
        <el-input v-model="safetyExamRecordQueryParams.studentIdLike" placeholder="请输入学生ID（sys_user.user_id）" clearable @keyup.enter="safetyExamRecordHandleQuery" />
      </el-form-item>
      <el-form-item label="考试成绩" prop="scoreLike">
        <el-input v-model="safetyExamRecordQueryParams.scoreLike" placeholder="请输入考试成绩" clearable @keyup.enter="safetyExamRecordHandleQuery" />
      </el-form-item>
      <el-form-item label="是否通过" prop="passed">
        <el-select v-model="safetyExamRecordQueryParams.passed" placeholder="请选择是否通过" clearable>
          <el-option v-for="dict in module_yes_no" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="考试时间" prop="examTime">
        <el-date-picker clearable v-model="safetyExamRecordQueryParams.examTime" type="date" value-format="YYYY-MM-DD" placeholder="请选择考试时间"></el-date-picker>
      </el-form-item>
      <el-form-item label="有效期至" prop="validUntil">
        <el-date-picker clearable v-model="safetyExamRecordQueryParams.validUntil" type="date" value-format="YYYY-MM-DD" placeholder="请选择有效期至"></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="safetyExamRecordHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="safetyExamRecordResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="safetyExamRecordHandleExport" v-hasPermi="['module:safetyExamRecord:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Download" @click="safetyExamRecordImportTemplate" v-hasPermi="['module:safetyExamRecord:import']">下载模板</el-button>
      </el-col>
      <el-col :span="1.5">
        <file-upload
          v-model="importRes"
          v-hasPermi="['module:safetyExamRecord:import']"
          :limit="1"
          uploadBtnText="导入"
          :fileType="['xlsx']"
          accept=".xlsx"
          :action="'/module/safetyExamRecord/importData'"
          :isShowTip="false"
          :isShowFileList="false"
          isImport
        />
      </el-col>
      <el-col :span="1.5">
        <module-help module="safetyExamRecord" />
      </el-col>
      <right-toolbar v-model:showSearch="safetyExamRecordShowSearch" @queryTable="safetyExamRecordGetList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="safetyExamRecordLoading"
      :data="safetyExamRecordList"
      :row-class-name="safetyExamRecordRowClassName"
      border
      @row-click="safetyExamRecordHandleRowClick"
    >
      <el-table-column v-if="!isStudent" label="学生ID" align="center" prop="studentId" />
      <el-table-column label="学生姓名" align="center" prop="nickName" min-width="120" />
      <el-table-column label="学生账号" align="center" prop="userName" min-width="110" />
      <el-table-column label="班级" align="center" prop="deptName" min-width="130" />
      <el-table-column label="考试成绩" align="center" prop="score" />
      <el-table-column label="是否通过" align="center" prop="passed">
        <template #default="scope">
          <dict-tag :options="module_yes_no" :value="scope.row.passed"/>
        </template>
      </el-table-column>
      <el-table-column label="考试时间" align="center" prop="examTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.examTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="有效期至" align="center" prop="validUntil" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.validUntil, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="safetyExamRecordTotal>0"
      :total="safetyExamRecordTotal"
      v-model:page="safetyExamRecordQueryParams.pageNum"
      v-model:limit="safetyExamRecordQueryParams.pageSize"
      @pagination="safetyExamRecordGetList"
    />
  </div>
</template>

<script setup name="SafetyExamRecordList">
import { listSafetyExamRecord } from "@/api/module/safetyExamRecord";
import useUserStore from "@/store/modules/user";

const dateRange = ref([]);
const router = useRouter();
const userStore = useUserStore();
const { proxy } = getCurrentInstance();
const { module_yes_no } = proxy.useDict('module_yes_no');
const isStudent = computed(() =>
  (userStore.roles || []).includes("student") &&
  !(userStore.roles || []).includes("teacher") &&
  !(userStore.roles || []).includes("admin")
);
const isTeacher = computed(() =>
  (userStore.roles || []).includes("teacher") &&
  !(userStore.roles || []).includes("admin")
);

const safetyExamRecordPageBase = "/business/module/safetyExamRecord";
const importRes = ref(null);
const safetyExamRecordList = ref([]);
const safetyExamRecordLoading = ref(true);
const safetyExamRecordShowSearch = ref(true);
const safetyExamRecordTotal = ref(0);
const safetyExamRecordData = reactive({
  safetyExamRecordQueryParams: {
    pageNum: 1,
    pageSize: 10,
    studentIdLike: null,
    scoreLike: null,
    passed: null,
    examTime: null,
    validUntil: null
  }
});
const { safetyExamRecordQueryParams } = toRefs(safetyExamRecordData);

watch(() => importRes.value, (val) => {
  if (val && val.code == 200) {
    safetyExamRecordHandleQuery();
  }
}, { deep: true });

onMounted(() => safetyExamRecordHandleQuery());

function safetyExamRecordGetList() {
  safetyExamRecordLoading.value = true;
  listSafetyExamRecord(proxy.addDateRange(safetyExamRecordQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    safetyExamRecordList.value = response.rows;
    safetyExamRecordTotal.value = response.total;
  }).finally(() => {
    safetyExamRecordLoading.value = false;
  });
}

function safetyExamRecordHandleQuery() {
  safetyExamRecordQueryParams.value.pageNum = 1;
  safetyExamRecordGetList();
}

function safetyExamRecordResetQuery() {
  dateRange.value = [];
  proxy.resetForm("safetyExamRecordQueryRef");
  safetyExamRecordHandleQuery();
}

function safetyExamRecordHandleRowClick(row, column) {
  if (!row || !row.id) return;
  if (column && (column.type === "selection" || column.label === "操作")) return;
  router.push(safetyExamRecordPageBase + "/view/" + row.id);
}

function safetyExamRecordRowClassName() {
  return "safety-exam-record-clickable-row";
}

function safetyExamRecordHandleExport() {
  proxy.download('module/safetyExamRecord/export', { ...safetyExamRecordQueryParams.value }, `安全考试记录_${new Date().getTime()}.xlsx`);
}

function safetyExamRecordImportTemplate() {
  proxy.download("module/safetyExamRecord/importTemplate", {}, `安全考试记录_${new Date().getTime()}.xlsx`);
}
</script>

<style lang="scss" scoped>
.scope-tip { margin-bottom: 12px; }
:deep(.safety-exam-record-clickable-row) { cursor: pointer; }
</style>
