<template>
  <div class="app-container">
    <el-form @submit.prevent :model="reservationQueryParams" ref="reservationQueryRef" :inline="true" v-show="reservationShowSearch" label-width="auto">
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
      <el-form-item label="预约编号" prop="reservationNoLike">
        <el-input
          v-model="reservationQueryParams.reservationNoLike"
          placeholder="请输入预约编号"
          clearable
          @keyup.enter="reservationHandleQuery"
        />
      </el-form-item>
      <el-form-item label="申请人，对应若依 sys_user.user_id" prop="applicantIdLike">
        <el-input
          v-model="reservationQueryParams.applicantIdLike"
          placeholder="请输入申请人，对应若依 sys_user.user_id"
          clearable
          @keyup.enter="reservationHandleQuery"
        />
      </el-form-item>
      <el-form-item label="实验室" prop="roomIdLike">
        <el-input
          v-model="reservationQueryParams.roomIdLike"
          placeholder="请输入实验室"
          clearable
          @keyup.enter="reservationHandleQuery"
        />
      </el-form-item>
      <el-form-item label="实验名称" prop="experimentNameLike">
        <el-input
          v-model="reservationQueryParams.experimentNameLike"
          placeholder="请输入实验名称"
          clearable
          @keyup.enter="reservationHandleQuery"
        />
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker clearable
          v-model="reservationQueryParams.startTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择开始时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker clearable
          v-model="reservationQueryParams.endTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择结束时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="参与人数" prop="participantCountLike">
        <el-input
          v-model="reservationQueryParams.participantCountLike"
          placeholder="请输入参与人数"
          clearable
          @keyup.enter="reservationHandleQuery"
        />
      </el-form-item>
      <el-form-item label="风险等级" prop="riskLevelLike">
        <el-input
          v-model="reservationQueryParams.riskLevelLike"
          placeholder="请输入风险等级"
          clearable
          @keyup.enter="reservationHandleQuery"
        />
      </el-form-item>
      <el-form-item label="是否需要审核" prop="reviewRequired">
        <el-select v-model="reservationQueryParams.reviewRequired" placeholder="请选择是否需要审核" clearable>
          <el-option
            v-for="dict in module_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="reservationHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="reservationResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="reservationHandleAdd"
          v-hasPermi="['module:reservation:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="reservationSingle"
          @click="reservationHandleUpdate"
          v-hasPermi="['module:reservation:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="reservationMultiple"
          @click="reservationHandleDelete"
          v-hasPermi="['module:reservation:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="reservationHandleExport"
          v-hasPermi="['module:reservation:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="info"
            plain
            icon="Download"
            @click="reservationImportTemplate"
            v-hasPermi="['module:reservation:import']"
        >下载模版</el-button>
      </el-col>
      <el-col :span="1.5">
      <file-upload
            v-model="importRes"
            v-hasPermi="['module:reservation:import']"
            :limit="1"
            uploadBtnText="导入"
            :fileType="['xlsx']"
            accept=".xlsx"
            :action="'/module/reservation/importData'"
            :isShowTip="false"
            :isShowFileList="false"
            isImport
          />
      </el-col>
      <right-toolbar v-model:showSearch="reservationShowSearch" @queryTable="reservationGetList"></right-toolbar>
    </el-row>

    <el-table v-loading="reservationLoading" :data="reservationList" @selection-change="reservationHandleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column width="50" label="主键" align="center" prop="id" />
      <el-table-column label="预约编号" align="center" prop="reservationNo" />
      <el-table-column label="申请人，对应若依 sys_user.user_id" align="center" prop="applicantId" />
      <el-table-column label="实验室" align="center" prop="roomId" />
      <el-table-column label="实验名称" align="center" prop="experimentName" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="参与人数" align="center" prop="participantCount" />
      <el-table-column label="风险等级" align="center" prop="riskLevel" />
      <el-table-column label="是否需要审核" align="center" prop="reviewRequired">
        <template #default="scope">
          <dict-tag :options="module_yes_no" :value="scope.row.reviewRequired"/>
        </template>
      </el-table-column>
      <el-table-column width="130" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="reservationHandleUpdate(scope.row)" v-hasPermi="['module:reservation:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="reservationHandleDelete(scope.row)" v-hasPermi="['module:reservation:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="reservationTotal>0"
      :total="reservationTotal"
      v-model:page="reservationQueryParams.pageNum"
      v-model:limit="reservationQueryParams.pageSize"
      @pagination="reservationGetList"
    />

    <!-- 添加或修改实验室预约对话框 -->
    <el-dialog :title="reservationTitle" v-model="reservationOpen" width="500px" append-to-body destroy-on-close>
      <el-form @submit.prevent ref="reservationRef" :model="reservationForm" :rules="reservationRules" label-width="auto">
        <el-form-item label="预约编号" prop="reservationNo">
          <el-input v-model="reservationForm.reservationNo" placeholder="请输入预约编号" />
        </el-form-item>
        <el-form-item label="申请人，对应若依 sys_user.user_id" prop="applicantId">
          <el-input v-model="reservationForm.applicantId" placeholder="请输入申请人，对应若依 sys_user.user_id" />
        </el-form-item>
        <el-form-item label="实验室" prop="roomId">
          <el-input v-model="reservationForm.roomId" placeholder="请输入实验室" />
        </el-form-item>
        <el-form-item label="实验名称" prop="experimentName">
          <el-input v-model="reservationForm.experimentName" placeholder="请输入实验名称" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker clearable style="width: 100%;"
            v-model="reservationForm.startTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择开始时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker clearable style="width: 100%;"
            v-model="reservationForm.endTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择结束时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="参与人数" prop="participantCount">
          <el-input v-model="reservationForm.participantCount" placeholder="请输入参与人数" />
        </el-form-item>
        <el-form-item label="风险等级" prop="riskLevel">
          <el-input v-model="reservationForm.riskLevel" placeholder="请输入风险等级" />
        </el-form-item>
        <el-form-item label="预约状态" prop="status">
          <el-radio-group v-model="reservationForm.status">
            <el-radio
              v-for="dict in module_reservation_status"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否需要审核" prop="reviewRequired">
          <el-radio-group v-model="reservationForm.reviewRequired">
            <el-radio
              v-for="dict in module_yes_no"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="实验说明" prop="remark">
          <el-input v-model="reservationForm.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="reservationSubmitForm">确 定</el-button>
          <el-button @click="reservationCancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Reservation">
import { listReservation, getReservation, delReservation, addReservation, updateReservation } from "@/api/module/reservation";
import dayjs from "dayjs";
import xeUtils from "xe-utils";

const dateRange = ref([]);

const route = useRoute();
const action = ref(null);

onMounted(() => {
  reservationHandleQuery();
});

const { proxy } = getCurrentInstance();
const { sys_common_status, module_reservation_status, module_yes_no } = proxy.useDict('sys_common_status', 'module_reservation_status', 'module_yes_no');

const importRes = ref(null);
watch(
  () => importRes,
  (val) => {
    if (val.value && val.value.code == 200) {
      reservationHandleQuery();
    }
  },
  { deep: true, immediate: true }
);

const reservationList = ref([]);
const reservationOpen = ref(false);
const reservationLoading = ref(true);
const reservationShowSearch = ref(true);
const reservationIds = ref([]);
const reservationSingle = ref(true);
const reservationMultiple = ref(true);
const reservationTotal = ref(0);
const reservationTitle = ref("");

const reservationData = reactive({
  reservationForm: {},
  reservationQueryParams: {
    pageNum: 1,
    pageSize: 10,
    reservationNo: null,
    applicantId: null,
    roomId: null,
    experimentName: null,
    startTime: null,
    endTime: null,
    participantCount: null,
    riskLevel: null,
    reviewRequired: null,
  },
  reservationRules: {
    reservationNo: [
      { required: true, message: "预约编号不能为空", trigger: "blur" }
    ],
    applicantId: [
      { required: true, message: "申请人，对应若依 sys_user.user_id不能为空", trigger: "blur" }
    ],
    roomId: [
      { required: true, message: "实验室不能为空", trigger: "blur" }
    ],
    experimentName: [
      { required: true, message: "实验名称不能为空", trigger: "blur" }
    ],
    startTime: [
      { required: true, message: "开始时间不能为空", trigger: "blur" }
    ],
    endTime: [
      { required: true, message: "结束时间不能为空", trigger: "blur" }
    ],
    participantCount: [
      { required: true, message: "参与人数不能为空", trigger: "blur" }
    ],
    riskLevel: [
      { required: true, message: "风险等级不能为空", trigger: "blur" }
    ],
    status: [
      { required: true, message: "预约状态不能为空", trigger: "change" }
    ],
    reviewRequired: [
      { required: true, message: "是否需要审核不能为空", trigger: "change" }
    ],
  }
});

const { reservationQueryParams, reservationForm, reservationRules } = toRefs(reservationData);

watch(
  route,
  (newRoute) => {
    action.value = newRoute.query && newRoute.query.action;
  },
  { immediate: true }
);

/** 查询实验室预约列表 */
function reservationGetList() {
  reservationLoading.value = true;
  listReservation(proxy.addDateRange(reservationQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    reservationList.value = response.rows;
    reservationTotal.value = response.total;
    reservationLoading.value = false;
  });
}

// 取消按钮
function reservationCancel() {
  reservationOpen.value = false;
  reservationReset();
}

// 表单重置
function reservationReset() {
  reservationForm.value = {
    id: null,
    reservationNo: null,
    applicantId: null,
    roomId: null,
    experimentName: null,
    startTime: null,
    endTime: null,
    participantCount: null,
    riskLevel: null,
    status: null,
    reviewRequired: null,
    remark: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  };
  proxy.resetForm("reservationRef");
}

/** 搜索按钮操作 */
function reservationHandleQuery() {
  reservationQueryParams.value.pageNum = 1;
  reservationGetList();
}

/** 重置按钮操作 */
function reservationResetQuery() {
  dateRange.value = [];
  proxy.resetForm("reservationQueryRef");
  reservationHandleQuery();
}

// 多选框选中数据
function reservationHandleSelectionChange(selection) {
  reservationIds.value = selection.map(item => item.id);
  reservationSingle.value = selection.length != 1;
  reservationMultiple.value = !selection.length;
}

/** 新增按钮操作 */
function reservationHandleAdd() {
  reservationReset();
  reservationOpen.value = true;
  reservationTitle.value = "添加";
}

/** 修改按钮操作 */
function reservationHandleUpdate(row) {
  reservationReset();
  const _id = row.id || reservationIds.value
  getReservation(_id).then(response => {
    reservationForm.value = response.data;
    reservationOpen.value = true;
    reservationTitle.value = "修改";
  });
}

/** 提交按钮 */
function reservationSubmitForm() {
  proxy.$refs["reservationRef"].validate(valid => {
    if (valid) {
        const submitForm = xeUtils.clone(reservationForm.value, true);
      if (submitForm.id != null) {
        updateReservation(submitForm).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          reservationOpen.value = false;
          reservationGetList();
        });
      } else {
        addReservation(submitForm).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          reservationOpen.value = false;
          reservationGetList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function reservationHandleDelete(row) {
  const _ids = row.id || reservationIds.value;
  proxy.$modal.confirm('是否确认删除编号为"' + _ids + '"的数据项？').then(function() {
    return delReservation(_ids);
  }).then(() => {
    reservationGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function reservationHandleExport() {
  proxy.download('module/reservation/export', {
    ...reservationQueryParams.value
  }, `实验室预约_${new Date().getTime()}.xlsx`)
}

/** 下载模板操作 */
function reservationImportTemplate() {
  proxy.download(
      "module/reservation/importTemplate",{},
      `实验室预约_${new Date().getTime()}.xlsx`
  );
}
</script>
<style lang="scss" scoped>

</style>