<template>
  <div class="app-container" v-loading="loading">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>{{ pageTitle }}</span>
          <el-button icon="Back" @click="goBack">返回</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="实验室" prop="roomId">
          <el-select v-model="form.roomId" placeholder="请选择实验室" filterable style="width: 100%;">
            <el-option v-for="room in roomOptions" :key="room.id" :label="room.roomCode + ' ' + room.roomName" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权对象" prop="subjectType">
          <el-select v-model="form.subjectType" placeholder="请选择授权对象" style="width: 100%;" @change="handleSubjectTypeChange">
            <el-option v-for="dict in module_room_access_subject" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.subjectType === 'USER'" label="学生" prop="subjectId">
          <el-select v-model="form.subjectId" placeholder="请选择学生" filterable style="width: 100%;">
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
            v-model="form.subjectId"
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
          <el-date-picker v-model="form.validFrom" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="留空表示立即生效" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="失效时间" prop="validUntil">
          <el-date-picker v-model="form.validUntil" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="留空表示长期有效" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="例如：本学期课程、毕设导师授权" />
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" :loading="saving" @click="submitForm">{{ isEdit ? "保存" : "新增" }}</el-button>
        <el-button @click="goBack">取 消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="RoomAccessDetail">
import { getRoomAccess, addRoomAccess, updateRoomAccess, getRoomAccessOptions } from "@/api/module/roomAccess";

const props = defineProps({
  isEdit: { type: Boolean, default: false }
});

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_room_access_subject } = proxy.useDict('module_room_access_subject');

const roomAccessPageBase = "/business/module/roomAccess";
const roomAccessListPath = "/module/roomAccess";
const pageTitle = computed(() => props.isEdit ? "修改准入规则" : "新增准入规则");
const loading = ref(false);
const saving = ref(false);
const formRef = ref(null);
const roomOptions = ref([]);
const deptOptions = ref([]);
const userOptions = ref([]);
const form = reactive({
  id: null,
  roomId: null,
  subjectType: null,
  subjectId: null,
  validFrom: null,
  validUntil: null,
  remark: null
});
const rules = {
  roomId: [{ required: true, message: "实验室不能为空", trigger: "change" }],
  subjectType: [{ required: true, message: "授权对象不能为空", trigger: "change" }],
  subjectId: [{ required: true, message: "请选择授权对象", trigger: "change" }]
};

onMounted(() => {
  resetForm();
  loadOptions();
  if (props.isEdit) {
    loadDetail();
  }
});

function loadOptions() {
  getRoomAccessOptions().then(response => {
    const options = response.data || {};
    roomOptions.value = options.rooms || [];
    deptOptions.value = options.depts || [];
    userOptions.value = options.students || [];
  });
}

function resetForm() {
  Object.assign(form, {
    id: null,
    roomId: null,
    subjectType: null,
    subjectId: null,
    validFrom: null,
    validUntil: null,
    remark: null
  });
  formRef.value?.clearValidate();
}

function loadDetail() {
  loading.value = true;
  getRoomAccess(route.params.id).then(response => {
    Object.assign(form, response.data || {});
  }).finally(() => {
    loading.value = false;
  });
}

function handleSubjectTypeChange() {
  form.subjectId = null;
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    const request = props.isEdit ? updateRoomAccess : addRoomAccess;
    request({ ...form }).then(response => {
      proxy.$modal.msgSuccess(props.isEdit ? "修改成功" : "新增成功");
      const id = form.id || response?.data?.id;
      router.push(id ? roomAccessPageBase + "/view/" + id : roomAccessListPath);
    }).finally(() => {
      saving.value = false;
    });
  });
}

function goBack() {
  if (props.isEdit && route.params.id) {
    router.push(roomAccessPageBase + "/view/" + route.params.id);
    return;
  }
  router.push(roomAccessListPath);
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.form-footer { margin-top: 20px; }
</style>
