<template>
  <div class="app-container knowledge-admin-page">
    <div class="page-heading">
      <div>
        <h3>Agent 知识库管理</h3>
        <p>维护安全知识和平台使用说明。可见范围决定哪些角色能在智能助手中检索到内容。</p>
      </div>
      <el-tag type="info" effect="plain">{{ documents.length }} 篇文档</el-tag>
    </div>

    <div class="knowledge-layout">
      <el-card class="panel document-panel" shadow="never">
        <template #header>
          <div class="panel-header">
            <span>知识文档</span>
            <el-button
              v-hasPermi="['module:agent:knowledge:add']"
              type="primary"
              size="small"
              icon="Plus"
              @click="openDocumentDialog()"
            >
              新增文档
            </el-button>
          </div>
        </template>
        <el-form :inline="true" class="search-form">
          <el-form-item>
            <el-input v-model="documentQuery.title" placeholder="文档标题" clearable @keyup.enter="loadDocuments" />
          </el-form-item>
          <el-form-item>
            <el-select v-model="documentQuery.knowledgeType" placeholder="类型" clearable style="width: 120px">
              <el-option label="安全知识" value="SAFETY" />
              <el-option label="平台帮助" value="PLATFORM" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="documentQuery.audience" placeholder="可见范围" clearable style="width: 120px">
              <el-option label="全部角色" value="ALL" />
              <el-option label="学生" value="STUDENT" />
              <el-option label="教师" value="TEACHER" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="documentQuery.status" placeholder="状态" clearable style="width: 110px">
              <el-option label="启用" value="0" />
              <el-option label="停用" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadDocuments">搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table
          v-loading="documentLoading"
          :data="documents"
          highlight-current-row
          height="560"
          @row-click="selectDocument"
        >
          <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column label="类型" width="88" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.knowledgeType === 'PLATFORM' ? 'warning' : 'success'">
                {{ row.knowledgeType === 'PLATFORM' ? '平台帮助' : '安全知识' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="可见" width="76" align="center">
            <template #default="{ row }">{{ audienceLabel(row.audience) }}</template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="片段" width="62" align="center" />
          <el-table-column label="状态" width="72" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === '0' ? 'success' : 'info'">
                {{ row.status === '0' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                v-hasPermi="['module:agent:knowledge:edit']"
                link
                type="primary"
                @click.stop="openDocumentDialog(row)"
              >
                修改
              </el-button>
              <el-button
                v-hasPermi="['module:agent:knowledge:remove']"
                link
                type="danger"
                @click.stop="removeDocument(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="panel chunk-panel" shadow="never">
        <template #header>
          <div class="panel-header">
            <span>{{ selectedDocument ? selectedDocument.title + ' · 知识片段' : '请选择左侧文档' }}</span>
            <el-button
              v-hasPermi="['module:agent:knowledge:add']"
              type="primary"
              size="small"
              icon="Plus"
              :disabled="!selectedDocument"
              @click="openChunkDialog()"
            >
              新增片段
            </el-button>
          </div>
        </template>
        <el-empty v-if="!selectedDocument" description="先选择一篇知识文档" />
        <el-table v-else v-loading="chunkLoading" :data="chunks" height="560">
          <el-table-column prop="chunkNo" label="序号" width="62" align="center" />
          <el-table-column prop="keywords" label="关键词" width="180" show-overflow-tooltip />
          <el-table-column prop="content" label="正文" min-width="260" show-overflow-tooltip />
          <el-table-column label="状态" width="72" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === '0' ? 'success' : 'info'">
                {{ row.status === '0' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                v-hasPermi="['module:agent:knowledge:edit']"
                link
                type="primary"
                @click="openChunkDialog(row)"
              >
                修改
              </el-button>
              <el-button
                v-hasPermi="['module:agent:knowledge:remove']"
                link
                type="danger"
                @click="removeChunk(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-dialog
      :title="documentForm.id ? '修改知识文档' : '新增知识文档'"
      v-model="documentDialogOpen"
      width="620px"
      append-to-body
    >
      <el-form ref="documentFormRef" :model="documentForm" :rules="documentRules" label-width="82px">
        <el-form-item label="文档标题" prop="title">
          <el-input v-model="documentForm.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="documentForm.category" maxlength="50" placeholder="例如：设备安全" />
        </el-form-item>
        <el-form-item label="知识类型">
          <el-radio-group v-model="documentForm.knowledgeType">
            <el-radio value="SAFETY">安全知识</el-radio>
            <el-radio value="PLATFORM">平台帮助</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="可见范围">
          <el-select v-model="documentForm.audience" style="width: 100%">
            <el-option label="全部角色" value="ALL" />
            <el-option label="仅学生" value="STUDENT" />
            <el-option label="仅教师" value="TEACHER" />
            <el-option label="仅管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="documentForm.source" maxlength="300" placeholder="制度名称或资料出处" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="documentForm.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="documentForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="documentDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveDocument">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      :title="chunkForm.id ? '修改知识片段' : '新增知识片段'"
      v-model="chunkDialogOpen"
      width="720px"
      append-to-body
    >
      <el-form ref="chunkFormRef" :model="chunkForm" :rules="chunkRules" label-width="82px">
        <el-form-item label="所属文档">
          <el-input :model-value="selectedDocument?.title" disabled />
        </el-form-item>
        <el-form-item label="片段序号">
          <el-input-number v-model="chunkForm.chunkNo" :min="1" :max="999" controls-position="right" />
          <span class="form-tip">留空时由后端自动追加序号</span>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="chunkForm.keywords" maxlength="500" placeholder="空格分隔，例如：离心机 配平 盖子" />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="chunkForm.content" type="textarea" :rows="9" maxlength="10000" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="chunkForm.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chunkDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveChunk">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="AgentKnowledgeAdmin">
import {
  listKnowledgeDocuments,
  getKnowledgeDocument,
  addKnowledgeDocument,
  updateKnowledgeDocument,
  deleteKnowledgeDocument,
  listKnowledgeChunks,
  addKnowledgeChunk,
  updateKnowledgeChunk,
  deleteKnowledgeChunk
} from "@/api/module/agent";

const { proxy } = getCurrentInstance();
const documents = ref([]);
const chunks = ref([]);
const selectedDocument = ref(null);
const documentLoading = ref(false);
const chunkLoading = ref(false);
const saving = ref(false);
const documentDialogOpen = ref(false);
const chunkDialogOpen = ref(false);
const documentFormRef = ref(null);
const chunkFormRef = ref(null);
const documentQuery = reactive({ title: "", knowledgeType: "", audience: "", status: "" });
const documentForm = reactive(emptyDocument());
const chunkForm = reactive(emptyChunk());
const documentRules = {
  title: [{ required: true, message: "请输入文档标题", trigger: "blur" }]
};
const chunkRules = {
  content: [{ required: true, message: "请输入知识片段正文", trigger: "blur" }]
};

onMounted(loadDocuments);

function emptyDocument() {
  return {
    id: null,
    title: "",
    category: "",
    knowledgeType: "SAFETY",
    audience: "ALL",
    source: "",
    status: "0",
    remark: ""
  };
}

function emptyChunk() {
  return { id: null, documentId: null, chunkNo: null, keywords: "", content: "", status: "0" };
}

function loadDocuments() {
  documentLoading.value = true;
  return listKnowledgeDocuments(documentQuery).then(response => {
    documents.value = response.data || [];
    if (selectedDocument.value) {
      const matched = documents.value.find(item => item.id === selectedDocument.value.id);
      selectedDocument.value = matched || null;
      if (matched) loadChunks();
      else chunks.value = [];
    }
  }).catch(error => {
    proxy.$modal.msgError("加载知识文档失败：" + (error?.message || "请稍后重试"));
  }).finally(() => {
    documentLoading.value = false;
  });
}

function selectDocument(row) {
  selectedDocument.value = row;
  loadChunks();
}

function loadChunks() {
  if (!selectedDocument.value) return Promise.resolve();
  chunkLoading.value = true;
  return listKnowledgeChunks({ documentId: selectedDocument.value.id }).then(response => {
    chunks.value = response.data || [];
  }).catch(error => {
    proxy.$modal.msgError("加载知识片段失败：" + (error?.message || "请稍后重试"));
  }).finally(() => {
    chunkLoading.value = false;
  });
}

function openDocumentDialog(row) {
  Object.assign(documentForm, emptyDocument());
  documentDialogOpen.value = true;
  if (!row) return;
  getKnowledgeDocument(row.id).then(response => {
    Object.assign(documentForm, emptyDocument(), response.data || {});
  }).catch(error => {
    documentDialogOpen.value = false;
    proxy.$modal.msgError("加载文档详情失败：" + (error?.message || "请稍后重试"));
  });
}

function saveDocument() {
  documentFormRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    const request = documentForm.id ? updateKnowledgeDocument(documentForm) : addKnowledgeDocument(documentForm);
    request.then(() => {
      proxy.$modal.msgSuccess("文档已保存");
      documentDialogOpen.value = false;
      loadDocuments();
    }).catch(error => {
      proxy.$modal.msgError("保存文档失败：" + (error?.message || "请检查填写内容"));
    }).finally(() => {
      saving.value = false;
    });
  });
}

function removeDocument(row) {
  proxy.$modal.confirm('删除文档“' + row.title + '”会同时删除其全部知识片段，确认继续吗？').then(() => {
    return deleteKnowledgeDocument(row.id);
  }).then(() => {
    if (selectedDocument.value?.id === row.id) {
      selectedDocument.value = null;
      chunks.value = [];
    }
    proxy.$modal.msgSuccess("文档已删除");
    loadDocuments();
  }).catch(() => {});
}

function openChunkDialog(row) {
  Object.assign(chunkForm, emptyChunk(), row || {});
  chunkForm.documentId = selectedDocument.value.id;
  chunkDialogOpen.value = true;
}

function saveChunk() {
  chunkFormRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    const request = chunkForm.id ? updateKnowledgeChunk(chunkForm) : addKnowledgeChunk(chunkForm);
    request.then(() => {
      proxy.$modal.msgSuccess("知识片段已保存");
      chunkDialogOpen.value = false;
      loadChunks();
      loadDocuments();
    }).catch(error => {
      proxy.$modal.msgError("保存片段失败：" + (error?.message || "请检查填写内容"));
    }).finally(() => {
      saving.value = false;
    });
  });
}

function removeChunk(row) {
  proxy.$modal.confirm("确认删除这个知识片段吗？").then(() => deleteKnowledgeChunk(row.id)).then(() => {
    proxy.$modal.msgSuccess("知识片段已删除");
    loadChunks();
    loadDocuments();
  }).catch(() => {});
}

function audienceLabel(audience) {
  const labels = {
    ALL: "全部",
    STUDENT: "学生",
    TEACHER: "教师",
    ADMIN: "管理员"
  };
  return labels[audience] || "全部";
}
</script>

<style lang="scss" scoped>
.knowledge-admin-page {
  min-height: calc(100vh - 104px);
}
.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.page-heading h3 {
  margin: 0 0 6px;
  color: #1f2d3d;
}
.page-heading p {
  margin: 0;
  color: #7f8ba0;
  font-size: 13px;
}
.knowledge-layout {
  display: grid;
  grid-template-columns: minmax(440px, 0.9fr) minmax(520px, 1.3fr);
  gap: 16px;
}
.panel {
  border-color: #e6ebf3;
  border-radius: 14px;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 600;
}
.search-form {
  margin-bottom: 8px;
}
.form-tip {
  margin-left: 10px;
  color: #9aa6b8;
  font-size: 12px;
}
@media (max-width: 1100px) {
  .knowledge-layout {
    grid-template-columns: 1fr;
  }
}
</style>
