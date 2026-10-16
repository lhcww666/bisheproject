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
      <el-form-item label="所属实验室" prop="roomIdLike">
        <el-input
          v-model="deviceQueryParams.roomIdLike"
          placeholder="请输入所属实验室"
          clearable
          @keyup.enter="deviceHandleQuery"
        />
      </el-form-item>
      <el-form-item label="设备编号" prop="deviceCodeLike">
        <el-input
          v-model="deviceQueryParams.deviceCodeLike"
          placeholder="请输入设备编号"
          clearable
          @keyup.enter="deviceHandleQuery"
        />
      </el-form-item>
      <el-form-item label="设备名称" prop="deviceNameLike">
        <el-input
          v-model="deviceQueryParams.deviceNameLike"
          placeholder="请输入设备名称"
          clearable
          @keyup.enter="deviceHandleQuery"
        />
      </el-form-item>
      <el-form-item label="设备类型" prop="deviceType">
        <el-select v-model="deviceQueryParams.deviceType" placeholder="请选择设备类型" clearable>
          <el-option
            v-for="dict in module_device_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="数量" prop="quantityLike">
        <el-input
          v-model="deviceQueryParams.quantityLike"
          placeholder="请输入数量"
          clearable
          @keyup.enter="deviceHandleQuery"
        />
      </el-form-item>
      <el-form-item label="是否需要培训" prop="requiresTraining">
        <el-select v-model="deviceQueryParams.requiresTraining" placeholder="请选择是否需要培训" clearable>
          <el-option
            v-for="dict in module_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="deviceHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="deviceResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="deviceHandleAdd"
          v-hasPermi="['module:device:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="deviceSingle"
          @click="deviceHandleUpdate"
          v-hasPermi="['module:device:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="deviceMultiple"
          @click="deviceHandleDelete"
          v-hasPermi="['module:device:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="deviceHandleExport"
          v-hasPermi="['module:device:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="info"
            plain
            icon="Download"
            @click="deviceImportTemplate"
            v-hasPermi="['module:device:import']"
        >下载模板</el-button>
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

    <el-table v-loading="deviceLoading" :data="deviceList" @selection-change="deviceHandleSelectionChange" border>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="id" />
      <el-table-column label="所属实验室" align="center" prop="roomId" />
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
      <el-table-column width="130" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="deviceHandleUpdate(scope.row)" v-hasPermi="['module:device:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="deviceHandleDelete(scope.row)" v-hasPermi="['module:device:remove']">删除</el-button>
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

    <!-- 添加或修改设备信息对话框 -->
    <el-dialog :title="deviceTitle" v-model="deviceOpen" width="500px" append-to-body destroy-on-close>
      <el-form @submit.prevent ref="deviceRef" :model="deviceForm" :rules="deviceRules" label-width="auto">
        <el-form-item label="所属实验室" prop="roomId">
          <el-input v-model="deviceForm.roomId" placeholder="请输入所属实验室" />
        </el-form-item>
        <el-form-item label="设备编号" prop="deviceCode">
          <el-input v-model="deviceForm.deviceCode" placeholder="请输入设备编号" />
        </el-form-item>
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="deviceForm.deviceName" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="设备类型" prop="deviceType">
          <el-select v-model="deviceForm.deviceType" placeholder="请选择设备类型">
            <el-option
              v-for="dict in module_device_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input v-model="deviceForm.quantity" placeholder="请输入数量" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="deviceForm.status">
            <el-radio
              v-for="dict in module_device_status"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否需要培训" prop="requiresTraining">
          <el-radio-group v-model="deviceForm.requiresTraining">
            <el-radio
              v-for="dict in module_yes_no"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="deviceForm.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="deviceSubmitForm">确 定</el-button>
          <el-button @click="deviceCancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Device">
import { listDevice, getDevice, delDevice, addDevice, updateDevice } from "@/api/module/device";
import dayjs from "dayjs";
import xeUtils from "xe-utils";

const dateRange = ref([]);

const route = useRoute();
const action = ref(null);

onMounted(() => {
  deviceHandleQuery();
});

const { proxy } = getCurrentInstance();
const { sys_common_status, module_device_type, module_device_status, module_yes_no } = proxy.useDict('sys_common_status', 'module_device_type', 'module_device_status', 'module_yes_no');

const importRes = ref(null);
watch(
  () => importRes,
  (val) => {
    if (val.value && val.value.code == 200) {
      deviceHandleQuery();
    }
  },
  { deep: true, immediate: true }
);

const deviceList = ref([]);
const deviceOpen = ref(false);
const deviceLoading = ref(true);
const deviceShowSearch = ref(true);
const deviceIds = ref([]);
const deviceSingle = ref(true);
const deviceMultiple = ref(true);
const deviceTotal = ref(0);
const deviceTitle = ref("");

const deviceData = reactive({
  deviceForm: {},
  deviceQueryParams: {
    pageNum: 1,
    pageSize: 10,
    roomId: null,
    deviceCode: null,
    deviceName: null,
    deviceType: null,
    quantity: null,
    requiresTraining: null,
  },
  deviceRules: {
    roomId: [
      { required: true, message: "所属实验室不能为空", trigger: "blur" }
    ],
    deviceCode: [
      { required: true, message: "设备编号不能为空", trigger: "blur" }
    ],
    deviceName: [
      { required: true, message: "设备名称不能为空", trigger: "blur" }
    ],
    deviceType: [
      { required: true, message: "设备类型不能为空", trigger: "change" }
    ],
    quantity: [
      { required: true, message: "数量不能为空", trigger: "blur" }
    ],
    status: [
      { required: true, message: "状态不能为空", trigger: "change" }
    ],
    requiresTraining: [
      { required: true, message: "是否需要培训不能为空", trigger: "change" }
    ],
  }
});

const { deviceQueryParams, deviceForm, deviceRules } = toRefs(deviceData);

watch(
  route,
  (newRoute) => {
    action.value = newRoute.query && newRoute.query.action;
  },
  { immediate: true }
);

/** 查询设备信息列表 */
function deviceGetList() {
  deviceLoading.value = true;
  listDevice(proxy.addDateRange(deviceQueryParams.value, dateRange.value, 'create_time', true)).then(response => {
    deviceList.value = response.rows;
    deviceTotal.value = response.total;
    deviceLoading.value = false;
  });
}

// 取消按钮
function deviceCancel() {
  deviceOpen.value = false;
  deviceReset();
}

// 表单重置
function deviceReset() {
  deviceForm.value = {
    id: null,
    roomId: null,
    deviceCode: null,
    deviceName: null,
    deviceType: null,
    quantity: null,
    status: null,
    requiresTraining: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm("deviceRef");
}

/** 搜索按钮操作 */
function deviceHandleQuery() {
  deviceQueryParams.value.pageNum = 1;
  deviceGetList();
}

/** 重置按钮操作 */
function deviceResetQuery() {
  dateRange.value = [];
  proxy.resetForm("deviceQueryRef");
  deviceHandleQuery();
}

// 多选框选中数据
function deviceHandleSelectionChange(selection) {
  deviceIds.value = selection.map(item => item.id);
  deviceSingle.value = selection.length != 1;
  deviceMultiple.value = !selection.length;
}

/** 新增按钮操作 */
function deviceHandleAdd() {
  deviceReset();
  deviceOpen.value = true;
  deviceTitle.value = "添加";
}

/** 修改按钮操作 */
function deviceHandleUpdate(row) {
  deviceReset();
  const _id = row.id || deviceIds.value
  getDevice(_id).then(response => {
    deviceForm.value = response.data;
    deviceOpen.value = true;
    deviceTitle.value = "修改";
  });
}

/** 提交按钮 */
function deviceSubmitForm() {
  proxy.$refs["deviceRef"].validate(valid => {
    if (valid) {
        const submitForm = xeUtils.clone(deviceForm.value, true);
      if (submitForm.id != null) {
        updateDevice(submitForm).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          deviceOpen.value = false;
          deviceGetList();
        });
      } else {
        addDevice(submitForm).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          deviceOpen.value = false;
          deviceGetList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function deviceHandleDelete(row) {
  const _ids = row.id || deviceIds.value;
  proxy.$modal.confirm('是否确认删除编号为"' + _ids + '"的数据项？').then(function() {
    return delDevice(_ids);
  }).then(() => {
    deviceGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function deviceHandleExport() {
  proxy.download('module/device/export', {
    ...deviceQueryParams.value
  }, `设备信息_${new Date().getTime()}.xlsx`)
}

/** 下载模板操作 */
function deviceImportTemplate() {
  proxy.download(
      "module/device/importTemplate",{},
      `设备信息_${new Date().getTime()}.xlsx`
  );
}
</script>
<style lang="scss" scoped>

</style>