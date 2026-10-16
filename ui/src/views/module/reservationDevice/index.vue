<template>
  <div class="app-container">
    <el-form @submit.prevent :model="reservationDeviceQueryParams" ref="reservationDeviceQueryRef" :inline="true" v-show="reservationDeviceShowSearch" label-width="auto">
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
          v-model="reservationDeviceQueryParams.reservationIdLike"
          placeholder="请输入预约ID"
          clearable
          @keyup.enter="reservationDeviceHandleQuery"
        />
      </el-form-item>
      <el-form-item label="设备ID" prop="deviceIdLike">
        <el-input
          v-model="reservationDeviceQueryParams.deviceIdLike"
          placeholder="请输入设备ID"
          clearable
          @keyup.enter="reservationDeviceHandleQuery"
        />
      </el-form-item>
      <el-form-item label="使用数量" prop="quantityLike">
        <el-input
          v-model="reservationDeviceQueryParams.quantityLike"
          placeholder="请输入使用数量"
          clearable
          @keyup.enter="reservationDeviceHandleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="reservationDeviceHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="reservationDeviceResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="reservationDeviceHandleAdd"
          v-hasPermi="['module:reservationDevice:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="reservationDeviceSingle"
          @click="reservationDeviceHandleUpdate"
          v-hasPermi="['module:reservationDevice:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="reservationDeviceMultiple"
          @click="reservationDeviceHandleDelete"
          v-hasPermi="['module:reservationDevice:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="reservationDeviceHandleExport"
          v-hasPermi="['module:reservationDevice:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="info"
            plain
            icon="Download"
            @click="reservationDeviceImportTemplate"
            v-hasPermi="['module:reservationDevice:import']"
        >下载模板</el-button>
      </el-col>
      <el-col :span="1.5">
      <file-upload
            v-model="importRes"
            v-hasPermi="['module:reservationDevice:import']"
            :limit="1"
            uploadBtnText="导入"
            :fileType="['xlsx']"
            accept=".xlsx"
            :action="'/module/reservationDevice/importData'"
            :isShowTip="false"
            :isShowFileList="false"
            isImport
          />
      </el-col>
      <el-col :span="1.5">
        <module-help module="reservationDevice" />
      </el-col>
      <right-toolbar v-model:showSearch="reservationDeviceShowSearch" @queryTable="reservationDeviceGetList"></right-toolbar>
    </el-row>

    <el-table v-loading="reservationDeviceLoading" :data="reservationDeviceList" @selection-change="reservationDeviceHandleSelectionChange" border>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="预约ID" align="center" prop="reservationId" />
      <el-table-column label="设备ID" align="center" prop="deviceId" />
      <el-table-column label="使用数量" align="center" prop="quantity" />
      <el-table-column width="130" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="reservationDeviceHandleUpdate(scope.row)" v-hasPermi="['module:reservationDevice:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="reservationDeviceHandleDelete(scope.row)" v-hasPermi="['module:reservationDevice:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="reservationDeviceTotal>0"
      :total="reservationDeviceTotal"
      v-model:page="reservationDeviceQueryParams.pageNum"
      v-model:limit="reservationDeviceQueryParams.pageSize"
      @pagination="reservationDeviceGetList"
    />

    <!-- 添加或修改预约设备关联对话框 -->
    <el-dialog :title="reservationDeviceTitle" v-model="reservationDeviceOpen" width="500px" append-to-body destroy-on-close>
      <el-form @submit.prevent ref="reservationDeviceRef" :model="reservationDeviceForm" :rules="reservationDeviceRules" label-width="auto">
        <el-form-item label="预约ID" prop="reservationId">
          <el-input v-model="reservationDeviceForm.reservationId" placeholder="请输入预约ID" />
        </el-form-item>
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="reservationDeviceForm.deviceId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="使用数量" prop="quantity">
          <el-input v-model="reservationDeviceForm.quantity" placeholder="请输入使用数量" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="reservationDeviceSubmitForm">确 定</el-button>
          <el-button @click="reservationDeviceCancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ReservationDevice">
import { listReservationDevice, getReservationDevice, delReservationDevice, addReservationDevice, updateReservationDevice } from "@/api/module/reservationDevice";
import dayjs from "dayjs";
import xeUtils from "xe-utils";

const dateRange = ref([]);

const route = useRoute();
const action = ref(null);

onMounted(() => {
  reservationDeviceHandleQuery();
});

const { proxy } = getCurrentInstance();
const { sys_common_status } = proxy.useDict('sys_common_status');

const importRes = ref(null);
watch(
  () => importRes,
  (val) => {
    if (val.value && val.value.code == 200) {
      reservationDeviceHandleQuery();
    }
  },
  { deep: true, immediate: true }
);

const reservationDeviceList = ref([]);
const reservationDeviceOpen = ref(false);
const reservationDeviceLoading = ref(true);
const reservationDeviceShowSearch = ref(true);
const reservationDeviceIds = ref([]);
const reservationDeviceSingle = ref(true);
const reservationDeviceMultiple = ref(true);
const reservationDeviceTotal = ref(0);
const reservationDeviceTitle = ref("");

const reservationDeviceData = reactive({
  reservationDeviceForm: {},
  reservationDeviceQueryParams: {
    pageNum: 1,
    pageSize: 10,
    reservationId: null,
    deviceId: null,
    quantity: null
  },
  reservationDeviceRules: {
    reservationId: [
      { required: true, message: "预约ID不能为空", trigger: "blur" }
    ],
    deviceId: [
      { required: true, message: "设备ID不能为空", trigger: "blur" }
    ],
    quantity: [
      { required: true, message: "使用数量不能为空", trigger: "blur" }
    ]
  }
});

const { reservationDeviceQueryParams, reservationDeviceForm, reservationDeviceRules } = toRefs(reservationDeviceData);

watch(
  route,
  (newRoute) => {
    action.value = newRoute.query && newRoute.query.action;
  },
  { immediate: true }
);

/** 查询预约设备关联列表 */
function reservationDeviceGetList() {
  reservationDeviceLoading.value = true;
  listReservationDevice(proxy.addDateRange(reservationDeviceQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    reservationDeviceList.value = response.rows;
    reservationDeviceTotal.value = response.total;
    reservationDeviceLoading.value = false;
  });
}

// 取消按钮
function reservationDeviceCancel() {
  reservationDeviceOpen.value = false;
  reservationDeviceReset();
}

// 表单重置
function reservationDeviceReset() {
  reservationDeviceForm.value = {
    id: null,
    reservationId: null,
    deviceId: null,
    quantity: null
  };
  proxy.resetForm("reservationDeviceRef");
}

/** 搜索按钮操作 */
function reservationDeviceHandleQuery() {
  reservationDeviceQueryParams.value.pageNum = 1;
  reservationDeviceGetList();
}

/** 重置按钮操作 */
function reservationDeviceResetQuery() {
  dateRange.value = [];
  proxy.resetForm("reservationDeviceQueryRef");
  reservationDeviceHandleQuery();
}

// 多选框选中数据
function reservationDeviceHandleSelectionChange(selection) {
  reservationDeviceIds.value = selection.map(item => item.id);
  reservationDeviceSingle.value = selection.length != 1;
  reservationDeviceMultiple.value = !selection.length;
}

/** 新增按钮操作 */
function reservationDeviceHandleAdd() {
  reservationDeviceReset();
  reservationDeviceOpen.value = true;
  reservationDeviceTitle.value = "添加";
}

/** 修改按钮操作 */
function reservationDeviceHandleUpdate(row) {
  reservationDeviceReset();
  const _id = row.id || reservationDeviceIds.value
  getReservationDevice(_id).then(response => {
    reservationDeviceForm.value = response.data;
    reservationDeviceOpen.value = true;
    reservationDeviceTitle.value = "修改";
  });
}

/** 提交按钮 */
function reservationDeviceSubmitForm() {
  proxy.$refs["reservationDeviceRef"].validate(valid => {
    if (valid) {
        const submitForm = xeUtils.clone(reservationDeviceForm.value, true);
      if (submitForm.id != null) {
        updateReservationDevice(submitForm).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          reservationDeviceOpen.value = false;
          reservationDeviceGetList();
        });
      } else {
        addReservationDevice(submitForm).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          reservationDeviceOpen.value = false;
          reservationDeviceGetList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function reservationDeviceHandleDelete(row) {
  const _ids = row.id || reservationDeviceIds.value;
  proxy.$modal.confirm('是否确认删除编号为"' + _ids + '"的数据项？').then(function() {
    return delReservationDevice(_ids);
  }).then(() => {
    reservationDeviceGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function reservationDeviceHandleExport() {
  proxy.download('module/reservationDevice/export', {
    ...reservationDeviceQueryParams.value
  }, `预约设备关联_${new Date().getTime()}.xlsx`)
}

/** 下载模板操作 */
function reservationDeviceImportTemplate() {
  proxy.download(
      "module/reservationDevice/importTemplate",{},
      `预约设备关联_${new Date().getTime()}.xlsx`
  );
}
</script>
<style lang="scss" scoped>

</style>
