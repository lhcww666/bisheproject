<template>
  <div class="app-container">
    <el-form @submit.prevent :model="deviceQueryParams" ref="deviceQueryRef" :inline="true" v-show="deviceShowSearch" label-width="auto">
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
      <el-form-item label="所属实验室" prop="roomId">
        <el-select v-model="deviceQueryParams.roomId" filterable clearable placeholder="请选择实验室" style="width: 220px;">
          <el-option
            v-for="room in roomOptions"
            :key="room.id"
            :label="roomOptionLabel(room)"
            :value="room.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="设备编号" prop="deviceCodeLike">
        <el-input v-model="deviceQueryParams.deviceCodeLike" placeholder="请输入设备编号" clearable @keyup.enter="deviceHandleQuery" />
      </el-form-item>
      <el-form-item label="设备名称" prop="deviceNameLike">
        <el-input v-model="deviceQueryParams.deviceNameLike" placeholder="请输入设备名称" clearable @keyup.enter="deviceHandleQuery" />
      </el-form-item>
      <el-form-item label="设备类型" prop="deviceType">
        <el-select v-model="deviceQueryParams.deviceType" placeholder="请选择设备类型" clearable>
          <el-option v-for="dict in module_device_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="数量" prop="quantityLike">
        <el-input v-model="deviceQueryParams.quantityLike" placeholder="请输入数量" clearable @keyup.enter="deviceHandleQuery" />
      </el-form-item>
      <el-form-item label="是否需要培训" prop="requiresTraining">
        <el-select v-model="deviceQueryParams.requiresTraining" placeholder="请选择是否需要培训" clearable>
          <el-option v-for="dict in module_yes_no" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="deviceHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="deviceResetQuery">重置</el-button>
        <module-help module="device" />
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="deviceHandleAdd" v-hasPermi="['module:device:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="deviceSingle" @click="deviceHandleUpdate" v-hasPermi="['module:device:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="deviceMultiple" @click="deviceHandleDelete" v-hasPermi="['module:device:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="deviceHandleExport" v-hasPermi="['module:device:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Download" @click="deviceImportTemplate" v-hasPermi="['module:device:import']">下载模板</el-button>
      </el-col>
      <el-col :span="1.5">
        <file-upload
          v-model="importRes"
          v-hasPermi="['module:device:import']"
          :limit="1"
          uploadBtnText="导入"
          :fileType="['xlsx']"
          accept=".xlsx"
          :action="'/module/device/importData'"
          :isShowTip="false"
          :isShowFileList="false"
          isImport
        />
      </el-col>
      <right-toolbar v-model:showSearch="deviceShowSearch" @queryTable="deviceGetList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="deviceLoading"
      :data="deviceList"
      :row-class-name="deviceRowClassName"
      border
      @row-click="deviceHandleRowClick"
      @selection-change="deviceHandleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="所属实验室" align="center" min-width="190">
        <template #default="scope">{{ roomLabel(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="设备编号" align="center" prop="deviceCode" />
      <el-table-column label="设备名称" align="center" prop="deviceName" />
      <el-table-column label="设备类型" align="center" prop="deviceType">
        <template #default="scope">
          <dict-tag :options="module_device_type" :value="scope.row.deviceType"/>
        </template>
      </el-table-column>
      <el-table-column label="数量" align="center" prop="quantity" />
      <el-table-column label="是否需要培训" align="center" prop="requiresTraining">
        <template #default="scope">
          <dict-tag :options="module_yes_no" :value="scope.row.requiresTraining"/>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="module_device_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column width="150" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click.stop="deviceHandleUpdate(scope.row)" v-hasPermi="['module:device:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click.stop="deviceHandleDelete(scope.row)" v-hasPermi="['module:device:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="deviceTotal>0"
      :total="deviceTotal"
      v-model:page="deviceQueryParams.pageNum"
      v-model:limit="deviceQueryParams.pageSize"
      @pagination="deviceGetList"
    />
  </div>
</template>

<script setup name="DeviceList">
import { listDevice, delDevice, listDeviceRoomOptions } from "@/api/module/device";

const dateRange = ref([]);
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_device_type, module_device_status, module_yes_no } = proxy.useDict('module_device_type', 'module_device_status', 'module_yes_no');

const devicePageBase = "/business/module/device";
const deviceListPath = "/module/device";
const importRes = ref(null);
const deviceList = ref([]);
const deviceLoading = ref(true);
const deviceShowSearch = ref(true);
const deviceIds = ref([]);
const deviceSingle = ref(true);
const deviceMultiple = ref(true);
const deviceTotal = ref(0);
const roomOptions = ref([]);
const deviceData = reactive({
  deviceQueryParams: {
    pageNum: 1,
    pageSize: 10,
    roomId: null,
    deviceCodeLike: null,
    deviceNameLike: null,
    deviceType: null,
    quantityLike: null,
    requiresTraining: null
  }
});
const { deviceQueryParams } = toRefs(deviceData);

watch(() => importRes.value, (val) => {
  if (val && val.code == 200) {
    deviceHandleQuery();
  }
}, { deep: true });

onMounted(() => {
  loadRoomOptions();
  deviceHandleQuery();
});

function loadRoomOptions() {
  listDeviceRoomOptions().then(response => {
    roomOptions.value = response.data || [];
  });
}

function deviceGetList() {
  deviceLoading.value = true;
  listDevice(proxy.addDateRange(deviceQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    deviceList.value = response.rows;
    deviceTotal.value = response.total;
  }).finally(() => {
    deviceLoading.value = false;
  });
}

function deviceHandleQuery() {
  deviceQueryParams.value.pageNum = 1;
  deviceGetList();
}

function deviceResetQuery() {
  dateRange.value = [];
  proxy.resetForm("deviceQueryRef");
  deviceHandleQuery();
}

function deviceHandleSelectionChange(selection) {
  deviceIds.value = selection.map(item => item.id);
  deviceSingle.value = selection.length != 1;
  deviceMultiple.value = !selection.length;
}

function deviceHandleAdd() {
  router.push(devicePageBase + "/add");
}

function deviceHandleUpdate(row) {
  const id = row?.id || deviceIds.value?.[0];
  if (!id) {
    proxy.$modal.msgWarning("请先选择一条记录");
    return;
  }
  router.push(devicePageBase + "/edit/" + id);
}

function deviceHandleDelete(row) {
  const ids = row?.id || deviceIds.value;
  if (!ids || (Array.isArray(ids) && ids.length === 0)) {
    proxy.$modal.msgWarning("请先选择要删除的记录");
    return;
  }
  proxy.$modal.confirm('是否确认删除编号为"' + ids + '"的数据项？').then(() => delDevice(ids)).then(() => {
    deviceGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

function deviceHandleRowClick(row, column) {
  if (!row || !row.id) return;
  if (column && (column.type === "selection" || column.label === "操作")) return;
  router.push(devicePageBase + "/view/" + row.id);
}

function deviceRowClassName() {
  return "device-clickable-row";
}

function deviceHandleExport() {
  proxy.download('module/device/export', { ...deviceQueryParams.value }, `设备档案_${new Date().getTime()}.xlsx`);
}

function roomOptionLabel(room) {
  return [room.roomCode, room.roomName].filter(Boolean).join(" · ");
}

function roomLabel(device) {
  const label = [device.roomCode, device.roomName].filter(Boolean).join(" · ");
  return label || "—";
}

function deviceImportTemplate() {
  proxy.download("module/device/importTemplate", {}, `设备档案_${new Date().getTime()}.xlsx`);
}
</script>

<style lang="scss" scoped>
:deep(.device-clickable-row) {
  cursor: pointer;
}
</style>
