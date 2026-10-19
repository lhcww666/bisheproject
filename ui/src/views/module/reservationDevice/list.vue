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
        <el-button type="warning" plain icon="Download" @click="reservationDeviceHandleExport" v-hasPermi="['module:reservationDevice:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <module-help module="reservationDevice" />
      </el-col>
      <right-toolbar v-model:showSearch="reservationDeviceShowSearch" @queryTable="reservationDeviceGetList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="reservationDeviceLoading"
      :data="reservationDeviceList"
      border
    >
      <el-table-column label="预约ID" align="center" prop="reservationId" />
      <el-table-column label="设备ID" align="center" prop="deviceId" />
      <el-table-column label="使用数量" align="center" prop="quantity" />
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
import { listReservationDevice } from "@/api/module/reservationDevice";

const dateRange = ref([]);
const { proxy } = getCurrentInstance();

const reservationDeviceList = ref([]);
const reservationDeviceLoading = ref(true);
const reservationDeviceShowSearch = ref(true);
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

function reservationDeviceHandleExport() {
  proxy.download('module/reservationDevice/export', { ...reservationDeviceQueryParams.value }, `预约设备关联_${new Date().getTime()}.xlsx`);
}
</script>
