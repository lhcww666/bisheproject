<template>
  <div class="app-container">
    <el-alert
      class="scope-tip"
      type="info"
      :closable="false"
      show-icon
      title="实验室准入：开放预约的实验室不需要配置规则；院系授权包含下级班级，班级只匹配本班，学生白名单用于毕设、竞赛等例外场景。"
    />

    <el-form @submit.prevent :model="roomAccessQueryParams" ref="roomAccessQueryRef" :inline="true" v-show="roomAccessShowSearch" label-width="auto">
      <el-form-item label="实验室" prop="roomId">
        <el-select v-model="roomAccessQueryParams.roomId" placeholder="请选择实验室" clearable filterable style="width: 220px;">
          <el-option v-for="room in roomOptions" :key="room.id" :label="room.roomCode + ' ' + room.roomName" :value="room.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="授权对象" prop="subjectType">
        <el-select v-model="roomAccessQueryParams.subjectType" placeholder="请选择授权对象" clearable>
          <el-option v-for="dict in module_room_access_subject" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="对象ID" prop="subjectId">
        <el-input v-model="roomAccessQueryParams.subjectId" placeholder="请输入授权对象ID" clearable @keyup.enter="roomAccessHandleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="roomAccessHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="roomAccessResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="roomAccessHandleAdd" v-hasPermi="['module:roomAccess:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="roomAccessSingle" @click="roomAccessHandleUpdate" v-hasPermi="['module:roomAccess:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="roomAccessMultiple" @click="roomAccessHandleDelete" v-hasPermi="['module:roomAccess:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <module-help module="roomAccess" />
      </el-col>
      <right-toolbar v-model:showSearch="roomAccessShowSearch" @queryTable="roomAccessGetList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="roomAccessLoading"
      :data="roomAccessList"
      border
      @selection-change="roomAccessHandleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="实验室" align="center" prop="roomName" min-width="180" show-overflow-tooltip />
      <el-table-column label="授权对象" align="center" prop="subjectType" width="110">
        <template #default="scope">
          <dict-tag :options="module_room_access_subject" :value="scope.row.subjectType"/>
        </template>
      </el-table-column>
      <el-table-column label="对象名称" align="center" prop="subjectName" min-width="160" show-overflow-tooltip />
      <el-table-column label="对象ID" align="center" prop="subjectId" width="100" />
      <el-table-column label="生效时间" align="center" prop="validFrom" width="180">
        <template #default="scope">
          <span>{{ scope.row.validFrom ? parseTime(scope.row.validFrom, '{y}-{m}-{d} {h}:{i}:{s}') : '立即生效' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="失效时间" align="center" prop="validUntil" width="180">
        <template #default="scope">
          <span>{{ scope.row.validUntil ? parseTime(scope.row.validUntil, '{y}-{m}-{d} {h}:{i}:{s}') : '长期有效' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="150" show-overflow-tooltip />
      <el-table-column fixed="right" width="150" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click.stop="roomAccessHandleUpdate(scope.row)" v-hasPermi="['module:roomAccess:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click.stop="roomAccessHandleDelete(scope.row)" v-hasPermi="['module:roomAccess:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="roomAccessTotal>0"
      :total="roomAccessTotal"
      v-model:page="roomAccessQueryParams.pageNum"
      v-model:limit="roomAccessQueryParams.pageSize"
      @pagination="roomAccessGetList"
    />
  </div>
</template>

<script setup name="RoomAccessList">
import { listRoomAccess, delRoomAccess, getRoomAccessOptions } from "@/api/module/roomAccess";

const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_room_access_subject } = proxy.useDict('module_room_access_subject');
const roomAccessPageBase = "/business/module/roomAccess";
const roomAccessList = ref([]);
const roomAccessLoading = ref(true);
const roomAccessShowSearch = ref(true);
const roomAccessIds = ref([]);
const roomAccessSingle = ref(true);
const roomAccessMultiple = ref(true);
const roomAccessTotal = ref(0);
const roomOptions = ref([]);
const roomAccessData = reactive({
  roomAccessQueryParams: {
    pageNum: 1,
    pageSize: 10,
    roomId: null,
    subjectType: null,
    subjectId: null
  }
});
const { roomAccessQueryParams } = toRefs(roomAccessData);

onMounted(() => {
  loadOptions();
  roomAccessHandleQuery();
});

function loadOptions() {
  getRoomAccessOptions().then(response => {
    roomOptions.value = response.data?.rooms || [];
  });
}

function roomAccessGetList() {
  roomAccessLoading.value = true;
  listRoomAccess(roomAccessQueryParams.value).then(response => {
    roomAccessList.value = response.rows;
    roomAccessTotal.value = response.total;
  }).finally(() => {
    roomAccessLoading.value = false;
  });
}

function roomAccessHandleQuery() {
  roomAccessQueryParams.value.pageNum = 1;
  roomAccessGetList();
}

function roomAccessResetQuery() {
  proxy.resetForm("roomAccessQueryRef");
  roomAccessHandleQuery();
}

function roomAccessHandleSelectionChange(selection) {
  roomAccessIds.value = selection.map(item => item.id);
  roomAccessSingle.value = selection.length != 1;
  roomAccessMultiple.value = !selection.length;
}

function roomAccessHandleAdd() {
  router.push(roomAccessPageBase + "/add");
}

function roomAccessHandleUpdate(row) {
  const id = row?.id || roomAccessIds.value?.[0];
  if (!id) {
    proxy.$modal.msgWarning("请先选择一条记录");
    return;
  }
  router.push(roomAccessPageBase + "/edit/" + id);
}

function roomAccessHandleDelete(row) {
  const ids = row?.id || roomAccessIds.value;
  if (!ids || (Array.isArray(ids) && ids.length === 0)) {
    proxy.$modal.msgWarning("请先选择要删除的记录");
    return;
  }
  proxy.$modal.confirm('是否确认删除选中的准入规则？').then(() => delRoomAccess(ids)).then(() => {
    roomAccessGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

</script>

<style lang="scss" scoped>
.scope-tip { margin-bottom: 12px; }
</style>
