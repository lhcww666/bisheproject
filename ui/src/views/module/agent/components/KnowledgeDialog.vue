<template>
  <el-dialog
    title="安全知识库检索"
    v-model="visible"
    width="720px"
    append-to-body
    destroy-on-close
  >
    <div class="knowledge-search">
      <el-input
        v-model="query"
        placeholder="例如：酒精灯打翻着火怎么办"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" :loading="loading" @click="search">检索</el-button>
    </div>
    <div v-if="results.length" class="knowledge-results">
      <el-card v-for="item in results" :key="item.chunkId" shadow="never" class="knowledge-card">
        <template #header>
          <div class="knowledge-title">
            <span>{{ item.documentTitle }}</span>
            <el-tag size="small" type="success">相关度 {{ item.score }}</el-tag>
          </div>
        </template>
        <p class="knowledge-content">{{ item.content }}</p>
        <div class="knowledge-source">来源：{{ item.source || '演示安全资料' }}</div>
      </el-card>
    </div>
    <el-alert
      v-else-if="error"
      type="error"
      :closable="false"
      show-icon
      :title="error"
    />
    <el-empty v-else-if="searched" description="未检索到相关资料" />
  </el-dialog>
</template>

<script setup name="AgentKnowledgeDialog">
import { searchAgentKnowledge } from "@/api/module/agent";

/**
 * 知识库检索弹窗独立维护查询、加载和结果状态；父页面只负责控制显隐。
 */
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
});
const emit = defineEmits(["update:modelValue"]);

const query = ref("");
const loading = ref(false);
const searched = ref(false);
const error = ref("");
const results = ref([]);
const visible = computed({
  get: () => props.modelValue,
  set: value => emit("update:modelValue", value)
});

// 直接调用知识库检索接口，不需要模型参与，便于演示和排查 RAG 召回。
function search() {
  const keyword = query.value.trim();
  if (!keyword) return;
  loading.value = true;
  searched.value = false;
  error.value = "";
  results.value = [];
  searchAgentKnowledge(keyword, 3).then(response => {
    results.value = response.data || [];
  }).catch(requestError => {
    error.value = "知识库检索失败：" + (requestError?.message || "请检查后端服务");
  }).finally(() => {
    loading.value = false;
    searched.value = true;
  });
}
</script>

<style lang="scss" scoped>
.knowledge-search {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.knowledge-results {
  max-height: 480px;
  overflow-y: auto;
}
.knowledge-card {
  margin-bottom: 10px;
}
.knowledge-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.knowledge-content {
  margin: 0;
  line-height: 1.8;
  white-space: pre-wrap;
}
.knowledge-source {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
