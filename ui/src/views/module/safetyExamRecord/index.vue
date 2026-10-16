<template>
  <div class="app-container">
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
      <el-form-item label="学生ID" prop="studentIdLike">
        <el-input
          v-model="safetyExamRecordQueryParams.studentIdLike"
          placeholder="请输入学生ID（sys_user.user_id）"
          clearable
          @keyup.enter="safetyExamRecordHandleQuery"
        />
      </el-form-item>
      <el-form-item label="考试成绩" prop="scoreLike">
        <el-input
          v-model="safetyExamRecordQueryParams.scoreLike"
          placeholder="请输入考试成绩"
          clearable
          @keyup.enter="safetyExamRecordHandleQuery"
        />
      </el-form-item>
      <el-form-item label="是否通过" prop="passed">
        <el-select v-model="safetyExamRecordQueryParams.passed" placeholder="请选择是否通过" clearable>
          <el-option
            v-for="dict in module_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="考试时间" prop="examTime">
        <el-date-picker clearable
          v-model="safetyExamRecordQueryParams.examTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择考试时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="有效期至" prop="validUntil">
        <el-date-picker clearable
          v-model="safetyExamRecordQueryParams.validUntil"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择有效期至">
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="safetyExamRecordHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="safetyExamRecordResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="safetyExamRecordHandleAdd"
          v-hasPermi="['module:safetyExamRecord:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="safetyExamRecordSingle"
          @click="safetyExamRecordHandleUpdate"
          v-hasPermi="['module:safetyExamRecord:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="safetyExamRecordMultiple"
          @click="safetyExamRecordHandleDelete"
          v-hasPermi="['module:safetyExamRecord:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="safetyExamRecordHandleExport"
          v-hasPermi="['module:safetyExamRecord:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="info"
            plain
            icon="Download"
            @click="safetyExamRecordImportTemplate"
            v-hasPermi="['module:safetyExamRecord:import']"
        >下载模板</el-button>
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
      <right-toolbar v-model:showSearch="safetyExamRecordShowSearch" @queryTable="safetyExamRecordGetList"></right-toolbar>
    </el-row>

    <el-table v-loading="safetyExamRecordLoading" :data="safetyExamRecordList" @selection-change="safetyExamRecordHandleSelectionChange" border>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="id" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
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
      <el-table-column width="130" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="safetyExamRecordHandleUpdate(scope.row)" v-hasPermi="['module:safetyExamRecord:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="safetyExamRecordHandleDelete(scope.row)" v-hasPermi="['module:safetyExamRecord:remove']">删除</el-button>
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

    <!-- 添加或修改安全考试记录对话框 -->
    <el-dialog :title="safetyExamRecordTitle" v-model="safetyExamRecordOpen" width="500px" append-to-body destroy-on-close>
      <el-form @submit.prevent ref="safetyExamRecordRef" :model="safetyExamRecordForm" :rules="safetyExamRecordRules" label-width="auto">
        <el-form-item label="学生ID" prop="studentId">
          <el-input v-model="safetyExamRecordForm.studentId" placeholder="请输入学生ID（sys_user.user_id）" />
        </el-form-item>
        <el-form-item label="考试成绩" prop="score">
          <el-input v-model="safetyExamRecordForm.score" placeholder="请输入考试成绩" />
        </el-form-item>
        <el-form-item label="是否通过" prop="passed">
          <el-radio-group v-model="safetyExamRecordForm.passed">
            <el-radio
              v-for="dict in module_yes_no"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="考试时间" prop="examTime">
          <el-date-picker clearable style="width: 100%;"
            v-model="safetyExamRecordForm.examTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择考试时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="有效期至" prop="validUntil">
          <el-date-picker clearable style="width: 100%;"
            v-model="safetyExamRecordForm.validUntil"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择有效期至">
          </el-date-picker>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="safetyExamRecordSubmitForm">确 定</el-button>
          <el-button @click="safetyExamRecordCancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SafetyExamRecord">
import { listSafetyExamRecord, getSafetyExamRecord, delSafetyExamRecord, addSafetyExamRecord, updateSafetyExamRecord } from "@/api/module/safetyExamRecord";
import dayjs from "dayjs";
import xeUtils from "xe-utils";

const dateRange = ref([]);

const route = useRoute();
const action = ref(null);

onMounted(() => {
  safetyExamRecordHandleQuery();
});

const { proxy } = getCurrentInstance();
const { sys_common_status, module_yes_no } = proxy.useDict('sys_common_status', 'module_yes_no');

const importRes = ref(null);
watch(
  () => importRes,
  (val) => {
    if (val.value && val.value.code == 200) {
      safetyExamRecordHandleQuery();
    }
  },
  { deep: true, immediate: true }
);

const safetyExamRecordList = ref([]);
const safetyExamRecordOpen = ref(false);
const safetyExamRecordLoading = ref(true);
const safetyExamRecordShowSearch = ref(true);
const safetyExamRecordIds = ref([]);
const safetyExamRecordSingle = ref(true);
const safetyExamRecordMultiple = ref(true);
const safetyExamRecordTotal = ref(0);
const safetyExamRecordTitle = ref("");

const safetyExamRecordData = reactive({
  safetyExamRecordForm: {},
  safetyExamRecordQueryParams: {
    pageNum: 1,
    pageSize: 10,
    studentId: null,
    score: null,
    passed: null,
    examTime: null,
    validUntil: null
  },
  safetyExamRecordRules: {
    studentId: [
      { required: true, message: "学生ID不能为空", trigger: "blur" }
    ],
    score: [
      { required: true, message: "考试成绩不能为空", trigger: "blur" }
    ],
    passed: [
      { required: true, message: "是否通过不能为空", trigger: "change" }
    ],
    examTime: [
      { required: true, message: "考试时间不能为空", trigger: "blur" }
    ],
  }
});

const { safetyExamRecordQueryParams, safetyExamRecordForm, safetyExamRecordRules } = toRefs(safetyExamRecordData);

watch(
  route,
  (newRoute) => {
    action.value = newRoute.query && newRoute.query.action;
  },
  { immediate: true }
);

/** 查询安全考试记录列表 */
function safetyExamRecordGetList() {
  safetyExamRecordLoading.value = true;
  listSafetyExamRecord(proxy.addDateRange(safetyExamRecordQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    safetyExamRecordList.value = response.rows;
    safetyExamRecordTotal.value = response.total;
    safetyExamRecordLoading.value = false;
  });
}

// 取消按钮
function safetyExamRecordCancel() {
  safetyExamRecordOpen.value = false;
  safetyExamRecordReset();
}

// 表单重置
function safetyExamRecordReset() {
  safetyExamRecordForm.value = {
    id: null,
    studentId: null,
    score: null,
    passed: null,
    examTime: null,
    validUntil: null
  };
  proxy.resetForm("safetyExamRecordRef");
}

/** 搜索按钮操作 */
function safetyExamRecordHandleQuery() {
  safetyExamRecordQueryParams.value.pageNum = 1;
  safetyExamRecordGetList();
}

/** 重置按钮操作 */
function safetyExamRecordResetQuery() {
  dateRange.value = [];
  proxy.resetForm("safetyExamRecordQueryRef");
  safetyExamRecordHandleQuery();
}

// 多选框选中数据
function safetyExamRecordHandleSelectionChange(selection) {
  safetyExamRecordIds.value = selection.map(item => item.id);
  safetyExamRecordSingle.value = selection.length != 1;
  safetyExamRecordMultiple.value = !selection.length;
}

/** 新增按钮操作 */
function safetyExamRecordHandleAdd() {
  safetyExamRecordReset();
  safetyExamRecordOpen.value = true;
  safetyExamRecordTitle.value = "添加";
}

/** 修改按钮操作 */
function safetyExamRecordHandleUpdate(row) {
  safetyExamRecordReset();
  const _id = row.id || safetyExamRecordIds.value
  getSafetyExamRecord(_id).then(response => {
    safetyExamRecordForm.value = response.data;
    safetyExamRecordOpen.value = true;
    safetyExamRecordTitle.value = "修改";
  });
}

/** 提交按钮 */
function safetyExamRecordSubmitForm() {
  proxy.$refs["safetyExamRecordRef"].validate(valid => {
    if (valid) {
        const submitForm = xeUtils.clone(safetyExamRecordForm.value, true);
      if (submitForm.id != null) {
        updateSafetyExamRecord(submitForm).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          safetyExamRecordOpen.value = false;
          safetyExamRecordGetList();
        });
      } else {
        addSafetyExamRecord(submitForm).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          safetyExamRecordOpen.value = false;
          safetyExamRecordGetList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function safetyExamRecordHandleDelete(row) {
  const _ids = row.id || safetyExamRecordIds.value;
  proxy.$modal.confirm('是否确认删除编号为"' + _ids + '"的数据项？').then(function() {
    return delSafetyExamRecord(_ids);
  }).then(() => {
    safetyExamRecordGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function safetyExamRecordHandleExport() {
  proxy.download('module/safetyExamRecord/export', {
    ...safetyExamRecordQueryParams.value
  }, `安全考试记录_${new Date().getTime()}.xlsx`)
}

/** 下载模板操作 */
function safetyExamRecordImportTemplate() {
  proxy.download(
      "module/safetyExamRecord/importTemplate",{},
      `安全考试记录_${new Date().getTime()}.xlsx`
  );
}
</script>
<style lang="scss" scoped>

</style>