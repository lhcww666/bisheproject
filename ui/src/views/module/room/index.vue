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
        <el-input
          v-model="roomQueryParams.roomCodeLike"
          placeholder="请输入实验室编号"
          clearable
          @keyup.enter="roomHandleQuery"
        />
      </el-form-item>
      <el-form-item label="实验室名称" prop="roomNameLike">
        <el-input
          v-model="roomQueryParams.roomNameLike"
          placeholder="请输入实验室名称"
          clearable
          @keyup.enter="roomHandleQuery"
        />
      </el-form-item>
      <el-form-item label="实验室类型" prop="roomType">
        <el-select v-model="roomQueryParams.roomType" placeholder="请选择实验室类型" clearable>
          <el-option
            v-for="dict in module_room_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="位置" prop="locationLike">
        <el-input
          v-model="roomQueryParams.locationLike"
          placeholder="请输入位置"
          clearable
          @keyup.enter="roomHandleQuery"
        />
      </el-form-item>
      <el-form-item label="最大人数" prop="capacityLike">
        <el-input
          v-model="roomQueryParams.capacityLike"
          placeholder="请输入最大人数"
          clearable
          @keyup.enter="roomHandleQuery"
        />
      </el-form-item>
      <el-form-item label="安全等级" prop="safetyLevelLike">
        <el-input
          v-model="roomQueryParams.safetyLevelLike"
          placeholder="请输入安全等级"
          clearable
          @keyup.enter="roomHandleQuery"
        />
      </el-form-item>
      <el-form-item label="开放开始时间" prop="openStartTimeLike">
        <el-input
          v-model="roomQueryParams.openStartTimeLike"
          placeholder="请输入开放开始时间"
          clearable
          @keyup.enter="roomHandleQuery"
        />
      </el-form-item>
      <el-form-item label="开放结束时间" prop="openEndTimeLike">
        <el-input
          v-model="roomQueryParams.openEndTimeLike"
          placeholder="请输入开放结束时间"
          clearable
          @keyup.enter="roomHandleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="roomHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="roomResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="roomHandleAdd"
          v-hasPermi="['module:room:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="roomSingle"
          @click="roomHandleUpdate"
          v-hasPermi="['module:room:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="roomMultiple"
          @click="roomHandleDelete"
          v-hasPermi="['module:room:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="roomHandleExport"
          v-hasPermi="['module:room:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="info"
            plain
            icon="Download"
            @click="roomImportTemplate"
            v-hasPermi="['module:room:import']"
        >下载模版</el-button>
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
      <right-toolbar v-model:showSearch="roomShowSearch" @queryTable="roomGetList"></right-toolbar>
    </el-row>

    <el-table v-loading="roomLoading" :data="roomList" @selection-change="roomHandleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column width="50" label="主键" align="center" prop="id" />
      <el-table-column label="实验室编号" align="center" prop="roomCode" />
      <el-table-column label="实验室名称" align="center" prop="roomName" />
      <el-table-column label="实验室类型" align="center" prop="roomType">
        <template #default="scope">
          <dict-tag :options="module_room_type" :value="scope.row.roomType"/>
        </template>
      </el-table-column>
      <el-table-column label="位置" align="center" prop="location" />
      <el-table-column label="最大人数" align="center" prop="capacity" />
      <el-table-column label="安全等级" align="center" prop="safetyLevel" />
      <el-table-column label="开放开始时间" align="center" prop="openStartTime" />
      <el-table-column label="开放结束时间" align="center" prop="openEndTime" />
      <el-table-column label="说明" align="center" prop="description" />
      <el-table-column width="130" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="roomHandleUpdate(scope.row)" v-hasPermi="['module:room:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="roomHandleDelete(scope.row)" v-hasPermi="['module:room:remove']">删除</el-button>
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

    <!-- 添加或修改实验室信息对话框 -->
    <el-dialog :title="roomTitle" v-model="roomOpen" width="500px" append-to-body destroy-on-close>
      <el-form @submit.prevent ref="roomRef" :model="roomForm" :rules="roomRules" label-width="auto">
        <el-form-item label="实验室编号" prop="roomCode">
          <el-input v-model="roomForm.roomCode" placeholder="请输入实验室编号" />
        </el-form-item>
        <el-form-item label="实验室名称" prop="roomName">
          <el-input v-model="roomForm.roomName" placeholder="请输入实验室名称" />
        </el-form-item>
        <el-form-item label="实验室类型" prop="roomType">
          <el-select v-model="roomForm.roomType" placeholder="请选择实验室类型">
            <el-option
              v-for="dict in module_room_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="roomForm.location" placeholder="请输入位置" />
        </el-form-item>
        <el-form-item label="最大人数" prop="capacity">
          <el-input v-model="roomForm.capacity" placeholder="请输入最大人数" />
        </el-form-item>
        <el-form-item label="安全等级" prop="safetyLevel">
          <el-input v-model="roomForm.safetyLevel" placeholder="请输入安全等级" />
        </el-form-item>
        <el-form-item label="开放开始时间" prop="openStartTime">
          <el-input v-model="roomForm.openStartTime" placeholder="请输入开放开始时间" />
        </el-form-item>
        <el-form-item label="开放结束时间" prop="openEndTime">
          <el-input v-model="roomForm.openEndTime" placeholder="请输入开放结束时间" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="roomForm.status">
            <el-radio
              v-for="dict in module_room_status"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="roomForm.description" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="roomForm.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="roomSubmitForm">确 定</el-button>
          <el-button @click="roomCancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Room">
import { listRoom, getRoom, delRoom, addRoom, updateRoom } from "@/api/module/room";
import dayjs from "dayjs";
import xeUtils from "xe-utils";

const dateRange = ref([]);

const route = useRoute();
const action = ref(null);

onMounted(() => {
  roomHandleQuery();
});

const { proxy } = getCurrentInstance();
const { sys_common_status, module_room_type, module_room_status } = proxy.useDict('sys_common_status', 'module_room_type', 'module_room_status');

const importRes = ref(null);
watch(
  () => importRes,
  (val) => {
    if (val.value && val.value.code == 200) {
      roomHandleQuery();
    }
  },
  { deep: true, immediate: true }
);

const roomList = ref([]);
const roomOpen = ref(false);
const roomLoading = ref(true);
const roomShowSearch = ref(true);
const roomIds = ref([]);
const roomSingle = ref(true);
const roomMultiple = ref(true);
const roomTotal = ref(0);
const roomTitle = ref("");

const roomData = reactive({
  roomForm: {},
  roomQueryParams: {
    pageNum: 1,
    pageSize: 10,
    roomCode: null,
    roomName: null,
    roomType: null,
    location: null,
    capacity: null,
    safetyLevel: null,
    openStartTime: null,
    openEndTime: null,
    description: null,
  },
  roomRules: {
    roomCode: [
      { required: true, message: "实验室编号不能为空", trigger: "blur" }
    ],
    roomName: [
      { required: true, message: "实验室名称不能为空", trigger: "blur" }
    ],
    roomType: [
      { required: true, message: "实验室类型不能为空", trigger: "change" }
    ],
    location: [
      { required: true, message: "位置不能为空", trigger: "blur" }
    ],
    capacity: [
      { required: true, message: "最大人数不能为空", trigger: "blur" }
    ],
    safetyLevel: [
      { required: true, message: "安全等级不能为空", trigger: "blur" }
    ],
    openStartTime: [
      { required: true, message: "开放开始时间不能为空", trigger: "blur" }
    ],
    openEndTime: [
      { required: true, message: "开放结束时间不能为空", trigger: "blur" }
    ],
    status: [
      { required: true, message: "状态不能为空", trigger: "change" }
    ],
  }
});

const { roomQueryParams, roomForm, roomRules } = toRefs(roomData);

watch(
  route,
  (newRoute) => {
    action.value = newRoute.query && newRoute.query.action;
  },
  { immediate: true }
);

/** 查询实验室信息列表 */
function roomGetList() {
  roomLoading.value = true;
  listRoom(proxy.addDateRange(roomQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    roomList.value = response.rows;
    roomTotal.value = response.total;
    roomLoading.value = false;
  });
}

// 取消按钮
function roomCancel() {
  roomOpen.value = false;
  roomReset();
}

// 表单重置
function roomReset() {
  roomForm.value = {
    id: null,
    roomCode: null,
    roomName: null,
    roomType: null,
    location: null,
    capacity: null,
    safetyLevel: null,
    openStartTime: null,
    openEndTime: null,
    status: null,
    description: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm("roomRef");
}

/** 搜索按钮操作 */
function roomHandleQuery() {
  roomQueryParams.value.pageNum = 1;
  roomGetList();
}

/** 重置按钮操作 */
function roomResetQuery() {
  dateRange.value = [];
  proxy.resetForm("roomQueryRef");
  roomHandleQuery();
}

// 多选框选中数据
function roomHandleSelectionChange(selection) {
  roomIds.value = selection.map(item => item.id);
  roomSingle.value = selection.length != 1;
  roomMultiple.value = !selection.length;
}

/** 新增按钮操作 */
function roomHandleAdd() {
  roomReset();
  roomOpen.value = true;
  roomTitle.value = "添加";
}

/** 修改按钮操作 */
function roomHandleUpdate(row) {
  roomReset();
  const _id = row.id || roomIds.value
  getRoom(_id).then(response => {
    roomForm.value = response.data;
    roomOpen.value = true;
    roomTitle.value = "修改";
  });
}

/** 提交按钮 */
function roomSubmitForm() {
  proxy.$refs["roomRef"].validate(valid => {
    if (valid) {
        const submitForm = xeUtils.clone(roomForm.value, true);
      if (submitForm.id != null) {
        updateRoom(submitForm).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          roomOpen.value = false;
          roomGetList();
        });
      } else {
        addRoom(submitForm).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          roomOpen.value = false;
          roomGetList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function roomHandleDelete(row) {
  const _ids = row.id || roomIds.value;
  proxy.$modal.confirm('是否确认删除编号为"' + _ids + '"的数据项？').then(function() {
    return delRoom(_ids);
  }).then(() => {
    roomGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function roomHandleExport() {
  proxy.download('module/room/export', {
    ...roomQueryParams.value
  }, `实验室信息_${new Date().getTime()}.xlsx`)
}

/** 下载模板操作 */
function roomImportTemplate() {
  proxy.download(
      "module/room/importTemplate",{},
      `实验室信息_${new Date().getTime()}.xlsx`
  );
}
</script>
<style lang="scss" scoped>

</style>