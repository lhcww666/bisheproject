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
        <el-input v-model="reservationDeviceQueryParams.reservationIdLike" placeholder="请输入预约ID" clearable @keyup.enter="reservationDeviceHandleQuery" />
      </el-form-item>
      <el-form-item label="设备ID" prop="deviceIdLike">
        <el-input v-model="reservationDeviceQueryParams.deviceIdLike" placeholder="请输入设备ID" clearable @keyup.enter="reservationDeviceHandleQuery" />
      </el-form-item>
      <el-form-item label="使用数量" prop="quantityLike">
        <el-input v-model="reservationDeviceQueryParams.quantityLike" placeholder="请输入使用数量" clearable @keyup.enter="reservationDeviceHandleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="reservationDeviceHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="reservationDeviceResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="reservationDeviceHandleAdd" v-hasPermi="['module:reservationDevice:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="reservationDeviceSingle" @click="reservationDeviceHandleUpdate" v-hasPermi="['module:reservationDevice:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="reservationDeviceMultiple" @click="reservationDeviceHandleDelete" v-hasPermi="['module:reservationDevice:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="reservationDeviceHandleExport" v-hasPermi="['module:reservationDevice:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Download" @click="reservationDeviceImportTemplate" v-hasPermi="['module:reservationDevice:import']">下载模板</el-button>
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

    <el-table
      v-loading="reservationDeviceLoading"
      :data="reservationDeviceList"
      :row-class-name="reservationDeviceRowClassName"
      border
      @row-click="reservationDeviceHandleRowClick"
      @selection-change="reservationDeviceHandleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="预约ID" align="center" prop="reservationId" />
      <el-table-column label="设备ID" align="center" prop="deviceId" />
      <el-table-column label="使用数量" align="center" prop="quantity" />
      <el-table-column width="150" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click.stop="reservationDeviceHandleUpdate(scope.row)" v-hasPermi="['module:reservationDevice:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click.stop="reservationDeviceHandleDelete(scope.row)" v-hasPermi="['module:reservationDevice:remove']">删除</el-button>
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
  </div>
</template>

<script setup name="ReservationDeviceList">
import { listReservationDevice, delReservationDevice } from "@/api/module/reservationDevice";

const dateRange = ref([]);
const router = useRouter();
const { proxy } = getCurrentInstance();

const reservationDevicePageBase = "/business/module/reservationDevice";
const reservationDeviceListPath = "/module/reservationDevice";
const importRes = ref(null);
const reservationDeviceList = ref([]);
const reservationDeviceLoading = ref(true);
const reservationDeviceShowSearch = ref(true);
const reservationDeviceIds = ref([]);
const reservationDeviceSingle = ref(true);
const reservationDeviceMultiple = ref(true);
const reservationDeviceTotal = ref(0);
const reservationDeviceData = reactive({
  reservationDeviceQueryParams: {
    pageNum: 1,
    pageSize: 10,
    reservationIdLike: null,
    deviceIdLike: null,
    quantityLike: null
  }
});
const { reservationDeviceQueryParams } = toRefs(reservationDeviceData);

watch(() => importRes.value, (val) => {
  if (val && val.code == 200) {
    reservationDeviceHandleQuery();
  }
}, { deep: true });

onMounted(() => reservationDeviceHandleQuery());

function reservationDeviceGetList() {
  reservationDeviceLoading.value = true;
  listReservationDevice(proxy.addDateRange(reservationDeviceQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    reservationDeviceList.value = response.rows;
    reservationDeviceTotal.value = response.total;
  }).finally(() => {
    reservationDeviceLoading.value = false;
  });
}

function reservationDeviceHandleQuery() {
  reservationDeviceQueryParams.value.pageNum = 1;
  reservationDeviceGetList();
}

function reservationDeviceResetQuery() {
  dateRange.value = [];
  proxy.resetForm("reservationDeviceQueryRef");
  reservationDeviceHandleQuery();
}

function reservationDeviceHandleSelectionChange(selection) {
  reservationDeviceIds.value = selection.map(item => item.id);
  reservationDeviceSingle.value = selection.length != 1;
  reservationDeviceMultiple.value = !selection.length;
}

function reservationDeviceHandleAdd() {
  router.push(reservationDevicePageBase + "/add");
}

function reservationDeviceHandleUpdate(row) {
  const id = row?.id || reservationDeviceIds.value?.[0];
  if (!id) {
    proxy.$modal.msgWarning("请先选择一条记录");
    return;
  }
  router.push(reservationDevicePageBase + "/edit/" + id);
}

function reservationDeviceHandleDelete(row) {
  const ids = row?.id || reservationDeviceIds.value;
  if (!ids || (Array.isArray(ids) && ids.length === 0)) {
    proxy.$modal.msgWarning("请先选择要删除的记录");
    return;
  }
  proxy.$modal.confirm('是否确认删除编号为"' + ids + '"的数据项？').then(() => delReservationDevice(ids)).then(() => {
    reservationDeviceGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

function reservationDeviceHandleRowClick(row, column) {
  if (!row || !row.id) return;
  if (column && (column.type === "selection" || column.label === "操作")) return;
  router.push(reservationDevicePageBase + "/view/" + row.id);
}

function reservationDeviceRowClassName() {
  return "reservation-device-clickable-row";
}

function reservationDeviceHandleExport() {
  proxy.download('module/reservationDevice/export', { ...reservationDeviceQueryParams.value }, `预约设备关联_${new Date().getTime()}.xlsx`);
}

function reservationDeviceImportTemplate() {
  proxy.download("module/reservationDevice/importTemplate", {}, `预约设备关联_${new Date().getTime()}.xlsx`);
}
</script>

<style lang="scss" scoped>
:deep(.reservation-device-clickable-row) {
  cursor: pointer;
}
</style>
