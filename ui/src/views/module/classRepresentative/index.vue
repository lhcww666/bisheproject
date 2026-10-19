<template>
  <div class="app-container">
    <el-alert
      class="scope-tip"
      type="info"
      :closable="false"
      show-icon
      title="每个班级设置一名负责人。负责人可以代表本班提交班级预约；普通学生仍可提交个人预约。"
    />

    <el-form @submit.prevent :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="auto">
      <el-form-item label="班级" prop="deptId">
        <el-tree-select
          v-model="queryParams.deptId"
          :data="deptOptions"
          :props="{ value: 'id', label: 'label', children: 'children' }"
          value-key="id"
          placeholder="请选择班级"
          clearable
          check-strictly
          style="width: 220px;"
        />
      </el-form-item>
      <el-form-item label="学生" prop="studentId">
        <el-select v-model="queryParams.studentId" placeholder="请选择学生" clearable filterable style="width: 220px;">
          <el-option
            v-for="student in studentOptions"
            :key="student.userId"
            :label="(student.nickName || student.userName) + '（' + student.userName + '）'"
            :value="student.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option
            v-for="dict in sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['module:classRepresentative:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['module:classRepresentative:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['module:classRepresentative:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <module-help module="classRepresentative" />
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="representativeList" @selection-change="handleSelectionChange" border>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="班级" align="center" prop="deptName" min-width="180" show-overflow-tooltip />
      <el-table-column label="负责人" align="center" min-width="180">
        <template #default="scope">
          <span>{{ (scope.row.studentName || scope.row.studentUserName || scope.row.studentId) + (scope.row.studentUserName ? '（' + scope.row.studentUserName + '）' : '') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status"/>
        </template>
      </el-table-column>
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
      <el-table-column fixed="right" width="130" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['module:classRepresentative:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['module:classRepresentative:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="560px" append-to-body destroy-on-close>
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
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ClassRepresentative">
import { listClassRepresentative, getClassRepresentative, addClassRepresentative, updateClassRepresentative, delClassRepresentative } from "@/api/module/classRepresentative";
import { getRoomAccessOptions } from "@/api/module/roomAccess";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const representativeList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const deptOptions = ref([]);
const studentOptions = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    deptId: null,
    studentId: null,
    status: null
  },
  rules: {
    deptId: [{ required: true, message: "班级不能为空", trigger: "change" }],
    studentId: [{ required: true, message: "负责人不能为空", trigger: "change" }],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }]
  }
});
const { form, queryParams, rules } = toRefs(data);

onMounted(() => {
  loadOptions();
  getList();
});

function loadOptions() {
  getRoomAccessOptions().then(response => {
    const options = response.data || {};
    deptOptions.value = options.depts || [];
    studentOptions.value = options.students || [];
  });
}

function getList() {
  loading.value = true;
  listClassRepresentative(queryParams.value).then(response => {
    representativeList.value = response.rows;
    total.value = response.total;
  }).finally(() => {
    loading.value = false;
  });
}

function cancel() {
  open.value = false;
  reset();
}

function reset() {
  form.value = {
    id: null,
    deptId: null,
    studentId: null,
    status: "0",
    validFrom: null,
    validUntil: null,
    remark: null
  };
  proxy.resetForm("formRef");
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增班级负责人";
}

function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getClassRepresentative(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改班级负责人";
  });
}

function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (!valid) {
      return;
    }
    const submitFormData = { ...form.value };
    if (submitFormData.id != null) {
      updateClassRepresentative(submitFormData).then(() => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    } else {
      addClassRepresentative(submitFormData).then(() => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      });
    }
  });
}

function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除选中的班级负责人？').then(function() {
    return delClassRepresentative(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}
</script>
<style lang="scss" scoped>
.scope-tip {
  margin-bottom: 12px;
}
</style>
