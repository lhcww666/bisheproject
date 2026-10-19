<template>
  <div class="app-container" v-loading="loading">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>{{ pageTitle }}</span>
          <el-button icon="Back" @click="goBack">返回</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="班级" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptOptions"
            :props="{ value: 'id', label: 'label', children: 'children' }"
            value-key="id"
            placeholder="请选择班级"
            check-strictly
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="负责人" prop="studentId">
          <el-select v-model="form.studentId" placeholder="请选择本班学生" filterable style="width: 100%;">
            <el-option
              v-for="student in studentOptions"
              :key="student.userId"
              :label="(student.nickName || student.userName) + '（' + student.userName + '）'"
              :value="student.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生效时间" prop="validFrom">
          <el-date-picker v-model="form.validFrom" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="留空表示立即生效" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="失效时间" prop="validUntil">
          <el-date-picker v-model="form.validUntil" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="留空表示长期有效" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="例如：2026 级计算机 1 班班长" />
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" :loading="saving" @click="submitForm">{{ isEdit ? "保存" : "新增" }}</el-button>
        <el-button @click="goBack">取 消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="ClassRepresentativeDetail">
import { getClassRepresentative, addClassRepresentative, updateClassRepresentative } from "@/api/module/classRepresentative";
import { getRoomAccessOptions } from "@/api/module/roomAccess";

const props = defineProps({
  isEdit: { type: Boolean, default: false }
});

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const pageBase = "/business/module/classRepresentative";
const listPath = "/module/classRepresentative";
const pageTitle = computed(() => props.isEdit ? "修改班级负责人" : "新增班级负责人");
const loading = ref(false);
const saving = ref(false);
const formRef = ref(null);
const deptOptions = ref([]);
const studentOptions = ref([]);
const form = reactive({
  id: null,
  deptId: null,
  studentId: null,
  status: "0",
  validFrom: null,
  validUntil: null,
  remark: null
});
const rules = {
  deptId: [{ required: true, message: "班级不能为空", trigger: "change" }],
  studentId: [{ required: true, message: "负责人不能为空", trigger: "change" }],
  status: [{ required: true, message: "状态不能为空", trigger: "change" }]
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
    deptOptions.value = options.depts || [];
    studentOptions.value = options.students || [];
  });
}

function resetForm() {
  Object.assign(form, {
    id: null,
    deptId: null,
    studentId: null,
    status: "0",
    validFrom: null,
    validUntil: null,
    remark: null
  });
  formRef.value?.clearValidate();
}

function loadDetail() {
  loading.value = true;
  getClassRepresentative(route.params.id).then(response => {
    Object.assign(form, response.data || {});
  }).finally(() => {
    loading.value = false;
  });
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    const request = props.isEdit ? updateClassRepresentative : addClassRepresentative;
    request({ ...form }).then(response => {
      proxy.$modal.msgSuccess(props.isEdit ? "修改成功" : "新增成功");
      const id = form.id || response?.data?.id;
      router.push(id ? pageBase + "/view/" + id : listPath);
    }).finally(() => {
      saving.value = false;
    });
  });
}

function goBack() {
  if (props.isEdit && route.params.id) {
    router.push(pageBase + "/view/" + route.params.id);
    return;
  }
  router.push(listPath);
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.form-footer { margin-top: 20px; }
</style>
