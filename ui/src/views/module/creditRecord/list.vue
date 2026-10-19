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
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="creditRecordHandleAdd" v-hasPermi="['module:creditRecord:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="creditRecordSingle" @click="creditRecordHandleUpdate" v-hasPermi="['module:creditRecord:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="creditRecordMultiple" @click="creditRecordHandleDelete" v-hasPermi="['module:creditRecord:remove']">删除</el-button>
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
      @selection-change="creditRecordHandleSelectionChange"
    >
      <el-table-column v-if="!isStudent && !isTeacher" type="selection" width="55" align="center" />
      <el-table-column v-if="!isStudent" label="用户ID" align="center" prop="userId" />
      <el-table-column label="学生姓名" align="center" prop="nickName" min-width="120" />
      <el-table-column label="学生账号" align="center" prop="userName" min-width="110" />
      <el-table-column label="班级" align="center" prop="deptName" min-width="130" />
      <el-table-column label="变动分值" align="center" prop="changeScore" />
      <el-table-column label="变动后分值" align="center" prop="balanceScore" />
      <el-table-column label="变动原因" align="center" prop="reason" show-overflow-tooltip />
      <el-table-column width="150" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click.stop="creditRecordHandleUpdate(scope.row)" v-hasPermi="['module:creditRecord:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click.stop="creditRecordHandleDelete(scope.row)" v-hasPermi="['module:creditRecord:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="creditRecordTotal>0"
      :total="creditRecordTotal"
      v-model:page="creditRecordQueryParams.pageNum"
      v-model:limit="creditRecordQueryParams.pageSize"
      @pagination="creditRecordGetList"
    />
  </div>
</template>

<script setup name="CreditRecordList">
import { listCreditRecord, delCreditRecord } from "@/api/module/creditRecord";
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

const creditRecordPageBase = "/business/module/creditRecord";
const creditRecordListPath = "/module/creditRecord";
const importRes = ref(null);
const creditRecordList = ref([]);
const creditRecordLoading = ref(true);
const creditRecordShowSearch = ref(true);
const creditRecordIds = ref([]);
const creditRecordSingle = ref(true);
const creditRecordMultiple = ref(true);
const creditRecordTotal = ref(0);
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

onMounted(() => creditRecordHandleQuery());

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

function creditRecordHandleSelectionChange(selection) {
  creditRecordIds.value = selection.map(item => item.id);
  creditRecordSingle.value = selection.length != 1;
  creditRecordMultiple.value = !selection.length;
}

function creditRecordHandleAdd() {
  router.push(creditRecordPageBase + "/add");
}

function creditRecordHandleUpdate(row) {
  const id = row?.id || creditRecordIds.value?.[0];
  if (!id) {
    proxy.$modal.msgWarning("请先选择一条记录");
    return;
  }
  router.push(creditRecordPageBase + "/edit/" + id);
}

function creditRecordHandleDelete(row) {
  const ids = row?.id || creditRecordIds.value;
  if (!ids || (Array.isArray(ids) && ids.length === 0)) {
    proxy.$modal.msgWarning("请先选择要删除的记录");
    return;
  }
  proxy.$modal.confirm('是否确认删除编号为"' + ids + '"的数据项？').then(() => delCreditRecord(ids)).then(() => {
    creditRecordGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
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
</script>

<style lang="scss" scoped>
.scope-tip { margin-bottom: 12px; }
:deep(.credit-record-clickable-row) { cursor: pointer; }
</style>
