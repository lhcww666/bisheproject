<template>
  <div class="app-container">
    <el-alert
      class="scope-tip"
      type="info"
      :closable="false"
      show-icon
      title="实验室准入：院系授权会包含下级班级；班级只匹配本班；学生白名单适合毕设、竞赛等例外场景。"
    />

    <el-form @submit.prevent :model="roomAccessQueryParams" ref="roomAccessQueryRef" :inline="true" v-show="roomAccessShowSearch" label-width="auto">
      <el-form-item label="实验室" prop="roomId">
        <el-select v-model="roomAccessQueryParams.roomId" placeholder="请选择实验室" clearable filterable style="width: 220px;">
          <el-option
            v-for="room in roomOptions"
            :key="room.id"
            :label="room.roomCode + ' ' + room.roomName"
            :value="room.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="授权对象" prop="subjectType">
        <el-select v-model="roomAccessQueryParams.subjectType" placeholder="请选择授权对象" clearable>
          <el-option
            v-for="dict in module_room_access_subject"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="对象ID" prop="subjectId">
        <el-input
          v-model="roomAccessQueryParams.subjectId"
          placeholder="请输入授权对象ID"
          clearable
          @keyup.enter="roomAccessHandleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="roomAccessHandleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="roomAccessResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="roomAccessHandleAdd"
          v-hasPermi="['module:roomAccess:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="roomAccessSingle"
          @click="roomAccessHandleUpdate"
          v-hasPermi="['module:roomAccess:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="roomAccessMultiple"
          @click="roomAccessHandleDelete"
          v-hasPermi="['module:roomAccess:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <module-help module="roomAccess" />
      </el-col>
      <right-toolbar v-model:showSearch="roomAccessShowSearch" @queryTable="roomAccessGetList"></right-toolbar>
    </el-row>

    <el-table v-loading="roomAccessLoading" :data="roomAccessList" @selection-change="roomAccessHandleSelectionChange" border>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="实验室" align="center" prop="roomName" min-width="180" />
      <el-table-column label="授权对象" align="center" prop="subjectType" width="110">
        <template #default="scope">
          <dict-tag :options="module_room_access_subject" :value="scope.row.subjectType"/>
        </template>
      </el-table-column>
      <el-table-column label="对象名称" align="center" prop="subjectName" min-width="160" />
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
      <el-table-column label="备注" align="center" prop="remark" min-width="150" />
      <el-table-column width="130" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="roomAccessHandleUpdate(scope.row)" v-hasPermi="['module:roomAccess:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="roomAccessHandleDelete(scope.row)" v-hasPermi="['module:roomAccess:remove']">删除</el-button>
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

    <el-dialog :title="roomAccessTitle" v-model="roomAccessOpen" width="560px" append-to-body destroy-on-close>
      <el-form @submit.prevent ref="roomAccessRef" :model="roomAccessForm" :rules="roomAccessRules" label-width="110px">
        <el-form-item label="实验室" prop="roomId">
          <el-select v-model="roomAccessForm.roomId" placeholder="请选择实验室" filterable style="width: 100%;">
            <el-option
              v-for="room in roomOptions"
              :key="room.id"
              :label="room.roomCode + ' ' + room.roomName"
              :value="room.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="授权对象" prop="subjectType">
          <el-select v-model="roomAccessForm.subjectType" placeholder="请选择授权对象" style="width: 100%;" @change="handleSubjectTypeChange">
            <el-option
              v-for="dict in module_room_access_subject"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="roomAccessForm.subjectType === 'USER'" label="学生" prop="subjectId">
          <el-select v-model="roomAccessForm.subjectId" placeholder="请选择学生" filterable style="width: 100%;">
            <el-option
              v-for="user in userOptions"
              :key="user.userId"
              :label="(user.nickName || user.userName) + '（' + user.userName + '）'"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="院系/班级" prop="subjectId">
          <el-tree-select
            v-model="roomAccessForm.subjectId"
            :data="deptOptions"
            :props="{ value: 'id', label: 'label', children: 'children' }"
            value-key="id"
            placeholder="请选择院系或班级"
            clearable
            check-strictly
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="生效时间" prop="validFrom">
          <el-date-picker
            v-model="roomAccessForm.validFrom"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空表示立即生效"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="失效时间" prop="validUntil">
          <el-date-picker
            v-model="roomAccessForm.validUntil"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空表示长期有效"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="roomAccessForm.remark" type="textarea" :rows="3" placeholder="例如：本学期课程、毕设导师授权" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="roomAccessSubmitForm">确 定</el-button>
          <el-button @click="roomAccessCancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="RoomAccess">
import { listRoomAccess, getRoomAccess, delRoomAccess, addRoomAccess, updateRoomAccess, getRoomAccessOptions } from "@/api/module/roomAccess";

const { proxy } = getCurrentInstance();
const { module_room_access_subject } = proxy.useDict('module_room_access_subject');

const roomAccessList = ref([]);
const roomAccessOpen = ref(false);
const roomAccessLoading = ref(true);
const roomAccessShowSearch = ref(true);
const roomAccessIds = ref([]);
const roomAccessSingle = ref(true);
const roomAccessMultiple = ref(true);
const roomAccessTotal = ref(0);
const roomAccessTitle = ref("");
const roomOptions = ref([]);
const deptOptions = ref([]);
const userOptions = ref([]);

const roomAccessData = reactive({
  roomAccessForm: {},
  roomAccessQueryParams: {
    pageNum: 1,
    pageSize: 10,
    roomId: null,
    subjectType: null,
    subjectId: null,
  },
  roomAccessRules: {
    roomId: [{ required: true, message: "实验室不能为空", trigger: "change" }],
    subjectType: [{ required: true, message: "授权对象不能为空", trigger: "change" }],
    subjectId: [{ required: true, message: "请选择授权对象", trigger: "change" }]
  }
});
const { roomAccessQueryParams, roomAccessForm, roomAccessRules } = toRefs(roomAccessData);

onMounted(() => {
  loadOptions();
  roomAccessHandleQuery();
});

function loadOptions() {
  getRoomAccessOptions().then(response => {
    const options = response.data || {};
    roomOptions.value = options.rooms || [];
    deptOptions.value = options.depts || [];
    userOptions.value = options.students || [];
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

function roomAccessCancel() {
  roomAccessOpen.value = false;
  roomAccessReset();
}

function roomAccessReset() {
  roomAccessForm.value = {
    id: null,
    roomId: null,
    subjectType: null,
    subjectId: null,
    validFrom: null,
    validUntil: null,
    remark: null
  };
  proxy.resetForm("roomAccessRef");
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
  roomAccessReset();
  roomAccessOpen.value = true;
  roomAccessTitle.value = "新增准入规则";
}

function roomAccessHandleUpdate(row) {
  roomAccessReset();
  const _id = row.id || roomAccessIds.value;
  getRoomAccess(_id).then(response => {
    roomAccessForm.value = response.data;
    roomAccessOpen.value = true;
    roomAccessTitle.value = "修改准入规则";
  });
}

function handleSubjectTypeChange() {
  roomAccessForm.value.subjectId = null;
}

function roomAccessSubmitForm() {
  proxy.$refs["roomAccessRef"].validate(valid => {
    if (!valid) {
      return;
    }
    const submitForm = { ...roomAccessForm.value };
    if (submitForm.id != null) {
      updateRoomAccess(submitForm).then(() => {
        proxy.$modal.msgSuccess("修改成功");
        roomAccessOpen.value = false;
        roomAccessGetList();
      });
    } else {
      addRoomAccess(submitForm).then(() => {
        proxy.$modal.msgSuccess("新增成功");
        roomAccessOpen.value = false;
        roomAccessGetList();
      });
    }
  });
}

function roomAccessHandleDelete(row) {
  const _ids = row.id || roomAccessIds.value;
  proxy.$modal.confirm('是否确认删除选中的准入规则？').then(function() {
    return delRoomAccess(_ids);
  }).then(() => {
    roomAccessGetList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}
</script>
<style lang="scss" scoped>
.scope-tip {
  margin-bottom: 12px;
}
</style>
