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
        <el-input
          v-model="reservationReviewQueryParams.reservationIdLike"
          placeholder="请输入预约ID"
          clearable
          @keyup.enter="reservationReviewHandleQuery"
        />
      </el-form-item>
      <el-form-item label="审核人ID" prop="reviewerIdLike">
        <el-input
          v-model="reservationReviewQueryParams.reviewerIdLike"
          placeholder="请输入审核人ID（sys_user.user_id）"
          clearable
          @keyup.enter="reservationReviewHandleQuery"
        />
      </el-form-item>
      <el-form-item label="审核结果" prop="result">
        <el-select v-model="reservationReviewQueryParams.result" placeholder="请选择审核结果" clearable>
          <el-option
            v-for="dict in module_review_result"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审核时间" prop="reviewTime">
        <el-date-picker clearable
          v-model="reservationReviewQueryParams.reviewTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择审核时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="reservationReviewHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="reservationReviewResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="reservationReviewHandleAdd"
          v-hasPermi="['module:reservationReview:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="reservationReviewSingle"
          @click="reservationReviewHandleUpdate"
          v-hasPermi="['module:reservationReview:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="reservationReviewMultiple"
          @click="reservationReviewHandleDelete"
          v-hasPermi="['module:reservationReview:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="reservationReviewHandleExport"
          v-hasPermi="['module:reservationReview:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="info"
            plain
            icon="Download"
            @click="reservationReviewImportTemplate"
            v-hasPermi="['module:reservationReview:import']"
        >下载模板</el-button>
      </el-col>
      <el-col :span="1.5">
      <file-upload
            v-model="importRes"
            v-hasPermi="['module:reservationReview:import']"
            :limit="1"
            uploadBtnText="导入"
            :fileType="['xlsx']"
            accept=".xlsx"
            :action="'/module/reservationReview/importData'"
            :isShowTip="false"
            :isShowFileList="false"
            isImport
          />
      </el-col>
      <right-toolbar v-model:showSearch="reservationReviewShowSearch" @queryTable="reservationReviewGetList"></right-toolbar>
    </el-row>

    <el-table v-loading="reservationReviewLoading" :data="reservationReviewList" @selection-change="reservationReviewHandleSelectionChange" border>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="id" />
      <el-table-column label="预约ID" align="center" prop="reservationId" />
      <el-table-column label="审核人ID" align="center" prop="reviewerId" />
      <el-table-column label="审核结果" align="center" prop="result">
        <template #default="scope">
          <dict-tag :options="module_review_result" :value="scope.row.result"/>
        </template>
      </el-table-column>
      <el-table-column label="审核意见" align="center" prop="reviewComment" />
      <el-table-column label="审核时间" align="center" prop="reviewTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.reviewTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column width="130" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="reservationReviewHandleUpdate(scope.row)" v-hasPermi="['module:reservationReview:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="reservationReviewHandleDelete(scope.row)" v-hasPermi="['module:reservationReview:remove']">删除</el-button>
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

    <!-- 添加或修改预约审核对话框 -->
    <el-dialog :title="reservationReviewTitle" v-model="reservationReviewOpen" width="500px" append-to-body destroy-on-close>
      <el-form @submit.prevent ref="reservationReviewRef" :model="reservationReviewForm" :rules="reservationReviewRules" label-width="auto">
        <el-form-item label="预约ID" prop="reservationId">
          <el-input v-model="reservationReviewForm.reservationId" placeholder="请输入预约ID" />
        </el-form-item>
        <el-form-item label="审核人ID" prop="reviewerId">
          <el-input v-model="reservationReviewForm.reviewerId" placeholder="请输入审核人ID（sys_user.user_id）" />
        </el-form-item>
        <el-form-item label="审核结果" prop="result">
          <el-radio-group v-model="reservationReviewForm.result">
            <el-radio
              v-for="dict in module_review_result"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" prop="reviewComment">
          <el-input v-model="reservationReviewForm.reviewComment" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="审核时间" prop="reviewTime">
          <el-date-picker clearable style="width: 100%;"
            v-model="reservationReviewForm.reviewTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择审核时间">
          </el-date-picker>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="reservationReviewSubmitForm">确 定</el-button>
          <el-button @click="reservationReviewCancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ReservationReview">
import { listReservationReview, getReservationReview, delReservationReview, addReservationReview, updateReservationReview } from "@/api/module/reservationReview";
import dayjs from "dayjs";
import xeUtils from "xe-utils";

const dateRange = ref([]);

const route = useRoute();
const action = ref(null);

onMounted(() => {
  reservationReviewHandleQuery();
});

const { proxy } = getCurrentInstance();
const { sys_common_status, module_review_result } = proxy.useDict('sys_common_status', 'module_review_result');

const importRes = ref(null);
watch(
  () => importRes,
  (val) => {
    if (val.value && val.value.code == 200) {
      reservationReviewHandleQuery();
    }
  },
  { deep: true, immediate: true }
);

const reservationReviewList = ref([]);
const reservationReviewOpen = ref(false);
const reservationReviewLoading = ref(true);
const reservationReviewShowSearch = ref(true);
const reservationReviewIds = ref([]);
const reservationReviewSingle = ref(true);
const reservationReviewMultiple = ref(true);
const reservationReviewTotal = ref(0);
const reservationReviewTitle = ref("");

const reservationReviewData = reactive({
  reservationReviewForm: {},
  reservationReviewQueryParams: {
    pageNum: 1,
    pageSize: 10,
    reservationId: null,
    reviewerId: null,
    result: null,
    reviewComment: null,
    reviewTime: null
  },
  reservationReviewRules: {
    reservationId: [
      { required: true, message: "预约ID不能为空", trigger: "blur" }
    ],
    reviewerId: [
      { required: true, message: "审核人ID不能为空", trigger: "blur" }
    ],
    result: [
      { required: true, message: "审核结果不能为空", trigger: "change" }
    ],
  }
});

const { reservationReviewQueryParams, reservationReviewForm, reservationReviewRules } = toRefs(reservationReviewData);

watch(
  route,
  (newRoute) => {
    action.value = newRoute.query && newRoute.query.action;
  },
  { immediate: true }
);

/** 查询预约审核列表 */
function reservationReviewGetList() {
  reservationReviewLoading.value = true;
  listReservationReview(proxy.addDateRange(reservationReviewQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    reservationReviewList.value = response.rows;
    reservationReviewTotal.value = response.total;
    reservationReviewLoading.value = false;
  });
}

// 取消按钮
function reservationReviewCancel() {
  reservationReviewOpen.value = false;
  reservationReviewReset();
}

// 表单重置
function reservationReviewReset() {
  reservationReviewForm.value = {
    id: null,
    reservationId: null,
    reviewerId: null,
    result: null,
    reviewComment: null,
    reviewTime: null
  };
  proxy.resetForm("reservationReviewRef");
}

/** 搜索按钮操作 */
function reservationReviewHandleQuery() {
  reservationReviewQueryParams.value.pageNum = 1;
  reservationReviewGetList();
}

/** 重置按钮操作 */
function reservationReviewResetQuery() {
  dateRange.value = [];
  proxy.resetForm("reservationReviewQueryRef");
  reservationReviewHandleQuery();
}

// 多选框选中数据
function reservationReviewHandleSelectionChange(selection) {
  reservationReviewIds.value = selection.map(item => item.id);
  reservationReviewSingle.value = selection.length != 1;
  reservationReviewMultiple.value = !selection.length;
}

/** 新增按钮操作 */
function reservationReviewHandleAdd() {
  reservationReviewReset();
  reservationReviewOpen.value = true;
  reservationReviewTitle.value = "添加";
}

/** 修改按钮操作 */
function reservationReviewHandleUpdate(row) {
  reservationReviewReset();
  const _id = row.id || reservationReviewIds.value
  getReservationReview(_id).then(response => {
    reservationReviewForm.value = response.data;
    reservationReviewOpen.value = true;
    reservationReviewTitle.value = "修改";
  });
}

/** 提交按钮 */
function reservationReviewSubmitForm() {
  proxy.$refs["reservationReviewRef"].validate(valid => {
    if (valid) {
        const submitForm = xeUtils.clone(reservationReviewForm.value, true);
      if (submitForm.id != null) {
        updateReservationReview(submitForm).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          reservationReviewOpen.value = false;
          reservationReviewGetList();
        });
      } else {
        addReservationReview(submitForm).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          reservationReviewOpen.value = false;
          reservationReviewGetList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function reservationReviewHandleDelete(row) {
  const _ids = row.id || reservationReviewIds.value;
  proxy.$modal.confirm('是否确认删除编号为"' + _ids + '"的数据项？').then(function() {
    return delReservationReview(_ids);
  }).then(() => {
    reservationReviewGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function reservationReviewHandleExport() {
  proxy.download('module/reservationReview/export', {
    ...reservationReviewQueryParams.value
  }, `预约审核_${new Date().getTime()}.xlsx`)
}

/** 下载模板操作 */
function reservationReviewImportTemplate() {
  proxy.download(
      "module/reservationReview/importTemplate",{},
      `预约审核_${new Date().getTime()}.xlsx`
  );
}
</script>
<style lang="scss" scoped>

</style>
