<template>
  <div class="app-container">
    <el-form @submit.prevent :model="roomQueryParams" ref="roomQueryRef" :inline="true" v-show="roomShowSearch" label-width="auto">
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
      <el-form-item label="实验室编号" prop="roomCodeLike">
        <el-input v-model="roomQueryParams.roomCodeLike" placeholder="请输入实验室编号" clearable @keyup.enter="roomHandleQuery" />
      </el-form-item>
      <el-form-item label="实验室名称" prop="roomNameLike">
        <el-input v-model="roomQueryParams.roomNameLike" placeholder="请输入实验室名称" clearable @keyup.enter="roomHandleQuery" />
      </el-form-item>
      <el-form-item label="实验室类型" prop="roomType">
        <el-select v-model="roomQueryParams.roomType" placeholder="请选择实验室类型" clearable>
          <el-option v-for="dict in module_room_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="位置" prop="locationLike">
        <el-input v-model="roomQueryParams.locationLike" placeholder="请输入位置" clearable @keyup.enter="roomHandleQuery" />
      </el-form-item>
      <el-form-item label="最大人数" prop="capacityLike">
        <el-input v-model="roomQueryParams.capacityLike" placeholder="请输入最大人数" clearable @keyup.enter="roomHandleQuery" />
      </el-form-item>
      <el-form-item label="安全等级" prop="safetyLevelLike">
        <el-input v-model="roomQueryParams.safetyLevelLike" placeholder="请输入安全等级" clearable @keyup.enter="roomHandleQuery" />
      </el-form-item>
      <el-form-item label="开放开始时间" prop="openStartTimeLike">
        <el-input v-model="roomQueryParams.openStartTimeLike" placeholder="请输入开放开始时间" clearable @keyup.enter="roomHandleQuery" />
      </el-form-item>
      <el-form-item label="开放结束时间" prop="openEndTimeLike">
        <el-input v-model="roomQueryParams.openEndTimeLike" placeholder="请输入开放结束时间" clearable @keyup.enter="roomHandleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="roomHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="roomResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="roomHandleAdd" v-hasPermi="['module:room:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="roomSingle" @click="roomHandleUpdate" v-hasPermi="['module:room:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="roomMultiple" @click="roomHandleDelete" v-hasPermi="['module:room:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="roomHandleExport" v-hasPermi="['module:room:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Download" @click="roomImportTemplate" v-hasPermi="['module:room:import']">下载模板</el-button>
      </el-col>
      <el-col :span="1.5">
        <file-upload
          v-model="importRes"
          v-hasPermi="['module:room:import']"
          :limit="1"
          uploadBtnText="导入"
          :fileType="['xlsx']"
          accept=".xlsx"
          :action="'/module/room/importData'"
          :isShowTip="false"
          :isShowFileList="false"
          isImport
        />
      </el-col>
      <el-col :span="1.5">
        <module-help module="room" />
      </el-col>
      <right-toolbar v-model:showSearch="roomShowSearch" @queryTable="roomGetList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="roomLoading"
      :data="roomList"
      :row-class-name="roomRowClassName"
      border
      @row-click="roomHandleRowClick"
      @selection-change="roomHandleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="实验室编号" align="center" prop="roomCode" />
      <el-table-column label="实验室名称" align="center" prop="roomName" />
      <el-table-column label="实验室类型" align="center" prop="roomType">
        <template #default="scope">
          <dict-tag :options="module_room_type" :value="scope.row.roomType"/>
        </template>
      </el-table-column>
      <el-table-column label="归属院系" align="center" prop="deptName" />
      <el-table-column label="位置" align="center" prop="location" />
      <el-table-column label="最大人数" align="center" prop="capacity" />
      <el-table-column label="安全等级" align="center" prop="safetyLevel" />
      <el-table-column label="开放开始时间" align="center" prop="openStartTime" />
      <el-table-column label="开放结束时间" align="center" prop="openEndTime" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="module_room_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="预约模式" align="center" prop="reservationMode">
        <template #default="scope">
          <dict-tag :options="module_reservation_mode" :value="scope.row.reservationMode"/>
        </template>
      </el-table-column>
      <el-table-column label="说明" align="center" prop="description" show-overflow-tooltip />
      <el-table-column width="150" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click.stop="roomHandleUpdate(scope.row)" v-hasPermi="['module:room:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click.stop="roomHandleDelete(scope.row)" v-hasPermi="['module:room:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="roomTotal>0"
      :total="roomTotal"
      v-model:page="roomQueryParams.pageNum"
      v-model:limit="roomQueryParams.pageSize"
      @pagination="roomGetList"
    />
  </div>
</template>

<script setup name="RoomList">
import { listRoom, delRoom } from "@/api/module/room";

const dateRange = ref([]);
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_room_type, module_room_status, module_reservation_mode } = proxy.useDict('module_room_type', 'module_room_status', 'module_reservation_mode');

const roomPageBase = "/business/module/room";
const roomListPath = "/module/room";
const importRes = ref(null);
const roomList = ref([]);
const roomLoading = ref(true);
const roomShowSearch = ref(true);
const roomIds = ref([]);
const roomSingle = ref(true);
const roomMultiple = ref(true);
const roomTotal = ref(0);
const roomData = reactive({
  roomQueryParams: {
    pageNum: 1,
    pageSize: 10,
    roomCodeLike: null,
    roomNameLike: null,
    roomType: null,
    locationLike: null,
    capacityLike: null,
    safetyLevelLike: null,
    openStartTimeLike: null,
    openEndTimeLike: null
  }
});
const { roomQueryParams } = toRefs(roomData);

watch(() => importRes.value, (val) => {
  if (val && val.code == 200) {
    roomHandleQuery();
  }
}, { deep: true });

onMounted(() => roomHandleQuery());

function roomGetList() {
  roomLoading.value = true;
  listRoom(proxy.addDateRange(roomQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    roomList.value = response.rows;
    roomTotal.value = response.total;
  }).finally(() => {
    roomLoading.value = false;
  });
}

function roomHandleQuery() {
  roomQueryParams.value.pageNum = 1;
  roomGetList();
}

function roomResetQuery() {
  dateRange.value = [];
  proxy.resetForm("roomQueryRef");
  roomHandleQuery();
}

function roomHandleSelectionChange(selection) {
  roomIds.value = selection.map(item => item.id);
  roomSingle.value = selection.length != 1;
  roomMultiple.value = !selection.length;
}

function roomHandleAdd() {
  router.push(roomPageBase + "/add");
}

function roomHandleUpdate(row) {
  const id = row?.id || roomIds.value?.[0];
  if (!id) {
    proxy.$modal.msgWarning("请先选择一条记录");
    return;
  }
  router.push(roomPageBase + "/edit/" + id);
}

function roomHandleDelete(row) {
  const ids = row?.id || roomIds.value;
  if (!ids || (Array.isArray(ids) && ids.length === 0)) {
    proxy.$modal.msgWarning("请先选择要删除的记录");
    return;
  }
  proxy.$modal.confirm('是否确认删除编号为"' + ids + '"的数据项？').then(() => delRoom(ids)).then(() => {
    roomGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

function roomHandleRowClick(row, column) {
  if (!row || !row.id) return;
  if (column && (column.type === "selection" || column.label === "操作")) return;
  router.push(roomPageBase + "/view/" + row.id);
}

function roomRowClassName() {
  return "room-clickable-row";
}

function roomHandleExport() {
  proxy.download('module/room/export', { ...roomQueryParams.value }, `实验室档案_${new Date().getTime()}.xlsx`);
}

function roomImportTemplate() {
  proxy.download("module/room/importTemplate", {}, `实验室档案_${new Date().getTime()}.xlsx`);
}
</script>

<style lang="scss" scoped>
:deep(.room-clickable-row) { cursor: pointer; }
</style>
