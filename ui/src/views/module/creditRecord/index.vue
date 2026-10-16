<template>
  <div class="app-container">
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
      <el-form-item label="用户ID" prop="userIdLike">
        <el-input
          v-model="creditRecordQueryParams.userIdLike"
          placeholder="请输入用户ID"
          clearable
          @keyup.enter="creditRecordHandleQuery"
        />
      </el-form-item>
      <el-form-item label="变动分值" prop="changeScoreLike">
        <el-input
          v-model="creditRecordQueryParams.changeScoreLike"
          placeholder="请输入变动分值（可正可负）"
          clearable
          @keyup.enter="creditRecordHandleQuery"
        />
      </el-form-item>
      <el-form-item label="变动后分值" prop="balanceScoreLike">
        <el-input
          v-model="creditRecordQueryParams.balanceScoreLike"
          placeholder="请输入变动后分值"
          clearable
          @keyup.enter="creditRecordHandleQuery"
        />
      </el-form-item>
      <el-form-item label="变动原因" prop="reasonLike">
        <el-input
          v-model="creditRecordQueryParams.reasonLike"
          placeholder="请输入变动原因"
          clearable
          @keyup.enter="creditRecordHandleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="creditRecordHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="creditRecordResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="creditRecordHandleAdd"
          v-hasPermi="['module:creditRecord:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="creditRecordSingle"
          @click="creditRecordHandleUpdate"
          v-hasPermi="['module:creditRecord:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="creditRecordMultiple"
          @click="creditRecordHandleDelete"
          v-hasPermi="['module:creditRecord:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="creditRecordHandleExport"
          v-hasPermi="['module:creditRecord:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="info"
            plain
            icon="Download"
            @click="creditRecordImportTemplate"
            v-hasPermi="['module:creditRecord:import']"
        >下载模板</el-button>
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

    <el-table v-loading="creditRecordLoading" :data="creditRecordList" @selection-change="creditRecordHandleSelectionChange" border>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户ID" align="center" prop="userId" />
      <el-table-column label="变动分值" align="center" prop="changeScore" />
      <el-table-column label="变动后分值" align="center" prop="balanceScore" />
      <el-table-column label="变动原因" align="center" prop="reason" />
      <el-table-column width="130" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="creditRecordHandleUpdate(scope.row)" v-hasPermi="['module:creditRecord:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="creditRecordHandleDelete(scope.row)" v-hasPermi="['module:creditRecord:remove']">删除</el-button>
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

    <!-- 添加或修改信用分流水对话框 -->
    <el-dialog :title="creditRecordTitle" v-model="creditRecordOpen" width="500px" append-to-body destroy-on-close>
      <el-form @submit.prevent ref="creditRecordRef" :model="creditRecordForm" :rules="creditRecordRules" label-width="auto">
        <el-form-item label="变动分值" prop="changeScore">
          <el-input v-model="creditRecordForm.changeScore" placeholder="请输入变动分值（可正可负）" />
        </el-form-item>
        <el-form-item label="变动后分值" prop="balanceScore">
          <el-input v-model="creditRecordForm.balanceScore" placeholder="请输入变动后分值" />
        </el-form-item>
        <el-form-item label="变动原因" prop="reason">
          <el-input v-model="creditRecordForm.reason" placeholder="请输入变动原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="creditRecordSubmitForm">确 定</el-button>
          <el-button @click="creditRecordCancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CreditRecord">
import { listCreditRecord, getCreditRecord, delCreditRecord, addCreditRecord, updateCreditRecord } from "@/api/module/creditRecord";
import dayjs from "dayjs";
import xeUtils from "xe-utils";

const dateRange = ref([]);

const route = useRoute();
const action = ref(null);

onMounted(() => {
  creditRecordHandleQuery();
});

const { proxy } = getCurrentInstance();
const { sys_common_status } = proxy.useDict('sys_common_status');

const importRes = ref(null);
watch(
  () => importRes,
  (val) => {
    if (val.value && val.value.code == 200) {
      creditRecordHandleQuery();
    }
  },
  { deep: true, immediate: true }
);

const creditRecordList = ref([]);
const creditRecordOpen = ref(false);
const creditRecordLoading = ref(true);
const creditRecordShowSearch = ref(true);
const creditRecordIds = ref([]);
const creditRecordSingle = ref(true);
const creditRecordMultiple = ref(true);
const creditRecordTotal = ref(0);
const creditRecordTitle = ref("");

const creditRecordData = reactive({
  creditRecordForm: {},
  creditRecordQueryParams: {
    pageNum: 1,
    pageSize: 10,
    changeScore: null,
    balanceScore: null,
    reason: null,
  },
  creditRecordRules: {
    userId: [
      { required: true, message: "用户ID不能为空", trigger: "blur" }
    ],
    changeScore: [
      { required: true, message: "变动分值不能为空", trigger: "blur" }
    ],
    balanceScore: [
      { required: true, message: "变动后分值不能为空", trigger: "blur" }
    ],
    reason: [
      { required: true, message: "变动原因不能为空", trigger: "blur" }
    ],
  }
});

const { creditRecordQueryParams, creditRecordForm, creditRecordRules } = toRefs(creditRecordData);

watch(
  route,
  (newRoute) => {
    action.value = newRoute.query && newRoute.query.action;
  },
  { immediate: true }
);

/** 查询信用分流水列表 */
function creditRecordGetList() {
  creditRecordLoading.value = true;
  listCreditRecord(proxy.addDateRange(creditRecordQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    creditRecordList.value = response.rows;
    creditRecordTotal.value = response.total;
    creditRecordLoading.value = false;
  });
}

// 取消按钮
function creditRecordCancel() {
  creditRecordOpen.value = false;
  creditRecordReset();
}

// 表单重置
function creditRecordReset() {
  creditRecordForm.value = {
    id: null,
    userId: null,
    changeScore: null,
    balanceScore: null,
    reason: null,
    createTime: null
  };
  proxy.resetForm("creditRecordRef");
}

/** 搜索按钮操作 */
function creditRecordHandleQuery() {
  creditRecordQueryParams.value.pageNum = 1;
  creditRecordGetList();
}

/** 重置按钮操作 */
function creditRecordResetQuery() {
  dateRange.value = [];
  proxy.resetForm("creditRecordQueryRef");
  creditRecordHandleQuery();
}

// 多选框选中数据
function creditRecordHandleSelectionChange(selection) {
  creditRecordIds.value = selection.map(item => item.id);
  creditRecordSingle.value = selection.length != 1;
  creditRecordMultiple.value = !selection.length;
}

/** 新增按钮操作 */
function creditRecordHandleAdd() {
  creditRecordReset();
  creditRecordOpen.value = true;
  creditRecordTitle.value = "添加";
}

/** 修改按钮操作 */
function creditRecordHandleUpdate(row) {
  creditRecordReset();
  const _id = row.id || creditRecordIds.value
  getCreditRecord(_id).then(response => {
    creditRecordForm.value = response.data;
    creditRecordOpen.value = true;
    creditRecordTitle.value = "修改";
  });
}

/** 提交按钮 */
function creditRecordSubmitForm() {
  proxy.$refs["creditRecordRef"].validate(valid => {
    if (valid) {
        const submitForm = xeUtils.clone(creditRecordForm.value, true);
      if (submitForm.id != null) {
        updateCreditRecord(submitForm).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          creditRecordOpen.value = false;
          creditRecordGetList();
        });
      } else {
        addCreditRecord(submitForm).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          creditRecordOpen.value = false;
          creditRecordGetList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function creditRecordHandleDelete(row) {
  const _ids = row.id || creditRecordIds.value;
  proxy.$modal.confirm('是否确认删除编号为"' + _ids + '"的数据项？').then(function() {
    return delCreditRecord(_ids);
  }).then(() => {
    creditRecordGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function creditRecordHandleExport() {
  proxy.download('module/creditRecord/export', {
    ...creditRecordQueryParams.value
  }, `信用分流水_${new Date().getTime()}.xlsx`)
}

/** 下载模板操作 */
function creditRecordImportTemplate() {
  proxy.download(
      "module/creditRecord/importTemplate",{},
      `信用分流水_${new Date().getTime()}.xlsx`
  );
}
</script>
<style lang="scss" scoped>

</style>
