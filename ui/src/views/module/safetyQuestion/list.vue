<template>
  <div class="app-container">
    <el-form @submit.prevent :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="auto">
      <el-form-item label="题型" prop="questionType">
        <el-select v-model="queryParams.questionType" placeholder="请选择题型" clearable>
          <el-option v-for="dict in module_exam_question_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="题干" prop="contentLike">
        <el-input v-model="queryParams.contentLike" placeholder="请输入题干关键字" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openAdd" v-hasPermi="['module:safetyQuestion:add']">新增题目</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="题目编号" align="center" prop="questionCode" width="160" />
      <el-table-column label="题型" align="center" width="100">
        <template #default="scope"><dict-tag :options="module_exam_question_type" :value="scope.row.questionType" /></template>
      </el-table-column>
      <el-table-column label="题干" align="center" prop="content" min-width="280" show-overflow-tooltip />
      <el-table-column label="分值" align="center" prop="score" width="80" />
      <el-table-column label="安全等级" align="center" prop="safetyLevel" width="100" />
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" width="170">
        <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="openEdit(scope.row)" v-hasPermi="['module:safetyQuestion:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['module:safetyQuestion:remove']">删除</el-button>
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

    <el-dialog :title="form.id ? '修改题目' : '新增题目'" v-model="open" width="720px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="题目编号" prop="questionCode">
          <el-input v-model="form.questionCode" placeholder="可不填，系统自动生成" />
        </el-form-item>
        <el-form-item label="题型" prop="questionType">
          <el-radio-group v-model="form.questionType" @change="handleTypeChange">
            <el-radio label="SINGLE">单选题</el-radio>
            <el-radio label="MULTIPLE">多选题</el-radio>
            <el-radio label="JUDGE">判断题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="2" placeholder="请输入题干" />
        </el-form-item>

        <el-form-item label="选项" required>
          <div class="option-editor">
            <div v-for="(option, index) in form.options" :key="index" class="option-row">
              <span class="option-key">{{ optionKey(index) }}</span>
              <el-input v-model="form.options[index]" :placeholder="'选项 ' + optionKey(index)" />
              <el-button link type="danger" icon="Delete" :disabled="form.options.length <= 2" @click="removeOption(index)">删除</el-button>
            </div>
            <el-button link type="primary" icon="Plus" @click="addOption">添加选项</el-button>
          </div>
        </el-form-item>

        <el-form-item label="正确答案" prop="answer">
          <el-radio-group
            v-if="form.questionType !== 'MULTIPLE'"
            :model-value="form.answer[0]"
            @update:model-value="value => form.answer = value ? [value] : []"
          >
            <el-radio v-for="(option, index) in form.options" :key="index" :label="optionKey(index)">{{ optionKey(index) }}</el-radio>
          </el-radio-group>
          <el-checkbox-group v-else v-model="form.answer">
            <el-checkbox v-for="(option, index) in form.options" :key="index" :label="optionKey(index)">{{ optionKey(index) }}</el-checkbox>
          </el-checkbox-group>
          <div class="form-hint">单选题和判断题只能选一个答案；多选题可选多个。</div>
        </el-form-item>

        <el-form-item label="分值" prop="score">
          <el-input-number v-model="form.score" :min="1" :max="100" :step="1" />
        </el-form-item>
        <el-form-item label="安全等级" prop="safetyLevel">
          <el-select v-model="form.safetyLevel" style="width: 180px;">
            <el-option label="1 - 低风险" :value="1" />
            <el-option label="2 - 中风险" :value="2" />
            <el-option label="3 - 高风险" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
        <el-button @click="open = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SafetyQuestionList">
import { listSafetyQuestion, getSafetyQuestion, addSafetyQuestion, updateSafetyQuestion, delSafetyQuestion } from "@/api/module/safetyQuestion";

const { proxy } = getCurrentInstance();
const { module_exam_question_type } = proxy.useDict('module_exam_question_type');
const loading = ref(true);
const saving = ref(false);
const showSearch = ref(true);
const list = ref([]);
const total = ref(0);
const open = ref(false);
const formRef = ref(null);
const queryRef = ref(null);
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  questionType: null,
  contentLike: null,
  status: null
});
const form = reactive(defaultForm());
const rules = {
  questionType: [{ required: true, message: "请选择题型", trigger: "change" }],
  content: [{ required: true, message: "请输入题干", trigger: "blur" }],
  score: [{ required: true, message: "请输入分值", trigger: "change" }],
  safetyLevel: [{ required: true, message: "请选择安全等级", trigger: "change" }]
};

onMounted(getList);

function defaultForm() {
  return {
    id: null,
    questionCode: null,
    questionType: "SINGLE",
    content: null,
    options: ["", "", "", ""],
    answer: [],
    score: 10,
    safetyLevel: 1,
    status: "0",
    remark: null
  };
}

function getList() {
  loading.value = true;
  listSafetyQuestion(queryParams).then(response => {
    list.value = response.rows;
    total.value = response.total;
  }).finally(() => {
    loading.value = false;
  });
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function openAdd() {
  Object.assign(form, defaultForm());
  open.value = true;
}

function openEdit(row) {
  getSafetyQuestion(row.id).then(response => {
    Object.assign(form, defaultForm(), response.data || {});
    if (!form.options || form.options.length < 2) {
      form.options = ["", "", "", ""];
    }
    open.value = true;
  });
}

function handleTypeChange() {
  form.answer = [];
}

function addOption() {
  if (form.options.length >= 6) {
    proxy.$modal.msgWarning("最多支持 6 个选项");
    return;
  }
  form.options.push("");
}

function removeOption(index) {
  if (form.options.length <= 2) return;
  form.options.splice(index, 1);
  const validKeys = form.options.map((_, i) => optionKey(i));
  form.answer = (form.answer || []).filter(key => validKeys.includes(key));
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return;
    const options = (form.options || []).map(item => (item || "").trim());
    if (options.length < 2 || options.some(item => !item)) {
      proxy.$modal.msgError("至少填写两个非空选项");
      return;
    }
    if (!form.answer || form.answer.length === 0) {
      proxy.$modal.msgError("请选择正确答案");
      return;
    }
    if (form.questionType !== "MULTIPLE" && form.answer.length !== 1) {
      proxy.$modal.msgError("单选题和判断题只能选择一个答案");
      return;
    }
    saving.value = true;
    const payload = {
      id: form.id,
      questionCode: form.questionCode,
      questionType: form.questionType,
      content: form.content,
      options,
      answer: form.answer,
      score: form.score,
      safetyLevel: form.safetyLevel,
      status: form.status,
      remark: form.remark
    };
    const request = form.id ? updateSafetyQuestion : addSafetyQuestion;
    request(payload).then(() => {
      proxy.$modal.msgSuccess(form.id ? "修改成功" : "新增成功");
      open.value = false;
      getList();
    }).finally(() => {
      saving.value = false;
    });
  });
}

function handleDelete(row) {
  proxy.$modal.confirm('确认删除题目“' + row.questionCode + '”吗？已被考试使用的题目需要停用而不是删除。').then(() => delSafetyQuestion(row.id)).then(() => {
    proxy.$modal.msgSuccess("删除成功");
    getList();
  }).catch(() => {});
}

function optionKey(index) {
  return String.fromCharCode(65 + index);
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>

<style lang="scss" scoped>
.option-editor { width: 100%; }
.option-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.option-key { width: 22px; color: #409eff; font-weight: 600; }
.form-hint { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
