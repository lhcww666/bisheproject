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
          <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
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

    <el-table
      v-loading="loading"
      :data="representativeList"
      border
      @selection-change="handleSelectionChange"
    >
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
      <el-table-column fixed="right" width="150" label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click.stop="handleUpdate(scope.row)" v-hasPermi="['module:classRepresentative:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click.stop="handleDelete(scope.row)" v-hasPermi="['module:classRepresentative:remove']">删除</el-button>
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
  </div>
</template>

<script setup name="ClassRepresentativeList">
import { listClassRepresentative, delClassRepresentative } from "@/api/module/classRepresentative";
import { getRoomAccessOptions } from "@/api/module/roomAccess";

const router = useRouter();
const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict('sys_normal_disable');
const pageBase = "/business/module/classRepresentative";
const representativeList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const deptOptions = ref([]);
const studentOptions = ref([]);
const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    deptId: null,
    studentId: null,
    status: null
  }
});
const { queryParams } = toRefs(data);

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
  router.push(pageBase + "/add");
}

function handleUpdate(row) {
  const id = row?.id || ids.value?.[0];
  if (!id) {
    proxy.$modal.msgWarning("请先选择一条记录");
    return;
  }
  router.push(pageBase + "/edit/" + id);
}

function handleDelete(row) {
  const _ids = row?.id || ids.value;
  if (!_ids || (Array.isArray(_ids) && _ids.length === 0)) {
    proxy.$modal.msgWarning("请先选择要删除的记录");
    return;
  }
  proxy.$modal.confirm('是否确认删除选中的班级负责人？').then(() => delClassRepresentative(_ids)).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

</script>

<style lang="scss" scoped>
.scope-tip { margin-bottom: 12px; }
</style>
