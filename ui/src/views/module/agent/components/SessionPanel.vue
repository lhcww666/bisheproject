<template>
  <aside class="session-panel">
    <div class="session-head">
      <div>
        <div class="session-title">对话</div>
        <div class="session-subtitle">{{ sessions.length }} 个会话</div>
      </div>
      <el-button type="primary" circle icon="Plus" :loading="creating" @click="$emit('create')" />
    </div>

    <el-scrollbar class="session-scroll">
      <div v-if="loading" class="session-state">正在加载会话...</div>
      <button
        v-for="session in sessions"
        :key="session.id"
        type="button"
        class="session-item"
        :class="{ active: session.id === activeId }"
        @click="$emit('select', session.id)"
      >
        <div class="session-item-main">
          <span class="session-item-title">{{ session.title || '未命名会话' }}</span>
          <el-tag size="small" effect="plain">{{ engineLabel(session.engine) }}</el-tag>
        </div>
        <div class="session-item-meta">
          <span>{{ formatTime(session.updateTime || session.createTime) }}</span>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click.stop="$emit('delete', session)"
          />
        </div>
      </button>
      <div v-if="!loading && !sessions.length" class="session-empty">
        <el-icon><ChatDotRound /></el-icon>
        <span>还没有会话</span>
        <el-button type="primary" plain @click="$emit('create')">新建会话</el-button>
      </div>
    </el-scrollbar>
  </aside>
</template>

<script setup name="AgentSessionPanel">
const props = defineProps({
  sessions: {
    type: Array,
    default: () => []
  },
  activeId: {
    type: [Number, String],
    default: null
  },
  loading: Boolean,
  creating: Boolean
})

defineEmits(["select", "create", "delete"])

function engineLabel(engine) {
  return engine === "dify" ? "Dify" : "Python"
}

function formatTime(value) {
  if (!value) return ""
  return String(value).slice(5, 16)
}
</script>

<style lang="scss" scoped>
.session-panel {
  display: flex;
  width: 268px;
  min-width: 268px;
  height: 100%;
  flex-direction: column;
  border-right: 1px solid #ebeef5;
  background: #f8fafc;
}

.session-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 18px 16px;
}

.session-title {
  color: #1f2937;
  font-size: 18px;
  font-weight: 700;
}

.session-subtitle {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.session-scroll {
  flex: 1;
  min-height: 0;
  padding: 0 10px 12px;
}

.session-item {
  width: 100%;
  margin-bottom: 6px;
  padding: 12px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition: background 0.2s ease;
}

.session-item:hover {
  background: #eef4ff;
}

.session-item.active {
  background: #e7f0ff;
  box-shadow: inset 3px 0 #409eff;
}

.session-item-main,
.session-item-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.session-item-title {
  overflow: hidden;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item-meta {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 11px;
}

.session-state,
.session-empty {
  padding: 36px 12px;
  color: #94a3b8;
  text-align: center;
}

.session-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.session-empty .el-icon {
  color: #a8c7fa;
  font-size: 34px;
}
</style>
