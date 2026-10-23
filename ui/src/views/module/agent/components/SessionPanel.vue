<template>
  <aside class="session-panel" :class="'theme-' + skin">
    <div class="session-header">
      <div class="session-branding">
        <span class="session-brand-icon">
          <svg class="lab-mascot" viewBox="0 0 64 64" aria-hidden="true">
            <path class="mascot-antenna" d="M32 18V9" />
            <circle class="mascot-bulb" cx="32" cy="8" r="3" />
            <rect class="mascot-head" x="12" y="18" width="40" height="32" rx="13" />
            <circle class="mascot-eye" cx="24" cy="33" r="2.8" />
            <circle class="mascot-eye" cx="40" cy="33" r="2.8" />
            <path class="mascot-smile" d="M26 40c3.8 4.2 8.2 4.2 12 0" />
            <circle class="mascot-cheek" cx="18" cy="39" r="2.2" />
            <circle class="mascot-cheek" cx="46" cy="39" r="2.2" />
          </svg>
        </span>
        <div>
          <strong>会话记录</strong>
          <small>{{ sessions.length }} 个对话</small>
        </div>
      </div>
      <el-button
        class="new-session-button"
        circle
        plain
        icon="Plus"
        title="新建对话"
        @click="emit('new')"
      />
    </div>
    <div class="session-list">
      <el-alert
        v-if="error"
        class="inline-error"
        type="error"
        :closable="false"
        show-icon
        :title="error"
      >
        <template #default>
          <el-button link type="primary" @click="emit('retry')">重试</el-button>
        </template>
      </el-alert>
      <div
        v-for="session in sessions"
        :key="session.id"
        class="session-item"
        :class="{ active: activeSessionId === session.id }"
        @click="emit('select', session)"
      >
        <span class="session-indicator"></span>
        <div class="session-copy">
          <div class="session-title" :title="session.title">{{ session.title }}</div>
          <div class="session-time">{{ formatSessionTime(session.updateTime) }}</div>
        </div>
        <el-button
          link
          type="danger"
          icon="Delete"
          class="session-delete"
          @click.stop="emit('delete', session)"
        />
      </div>
      <el-empty v-if="sessions.length === 0" description="还没有对话，点击右上角开始" :image-size="64" />
    </div>
    <div class="session-tip">
      <span class="session-tip-dot"></span>
      查询结果均可追溯
    </div>
  </aside>
</template>

<script setup name="AgentSessionPanel">
const props = defineProps({
  sessions: {
    type: Array,
    default: () => []
  },
  activeSessionId: {
    type: [Number, String],
    default: null
  },
  error: {
    type: String,
    default: ""
  },
  skin: {
    type: String,
    default: "fresh"
  }
});
const emit = defineEmits(["select", "new", "delete", "retry"]);
const { proxy } = getCurrentInstance();

function formatSessionTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const today = new Date();
  if (date.toDateString() === today.toDateString()) {
    return "今天 " + proxy.parseTime(value, "{h}:{i}");
  }
  return proxy.parseTime(value, "{m}-{d}");
}
</script>

<style lang="scss" scoped>
.session-panel {
  width: 258px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e9edf5;
  background: linear-gradient(180deg, #f8faff 0%, #f4f7fc 100%);
  overflow: hidden;
}
.session-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 18px 16px;
}
.session-branding {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.session-branding > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.session-branding strong {
  color: #1f2d3d;
  font-size: 15px;
  font-weight: 700;
}
.session-branding small {
  color: #98a4b8;
  font-size: 11px;
}
.session-brand-icon {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 11px;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.25);
  color: #fff;
}
.lab-mascot {
  width: 72%;
  height: 72%;
  display: block;
  color: #fff;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.6;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.mascot-bulb {
  fill: currentColor;
  stroke: none;
}
.mascot-head {
  fill: rgba(255, 255, 255, 0.14);
}
.mascot-eye {
  fill: currentColor;
  stroke: none;
}
.mascot-cheek {
  fill: currentColor;
  opacity: 0.48;
  stroke: none;
}
.new-session-button {
  border-color: #dbe5f5 !important;
  background: rgba(255, 255, 255, 0.8) !important;
  color: #2563eb !important;
  box-shadow: 0 4px 12px rgba(45, 83, 140, 0.08);
}
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 16px;
}
.session-list::-webkit-scrollbar {
  width: 6px;
}
.session-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #d7dfeb;
}
.inline-error {
  margin-bottom: 8px;
}
.session-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 58px;
  padding: 10px 12px;
  margin-bottom: 7px;
  border: 1px solid transparent;
  border-radius: 13px;
  cursor: pointer;
  color: #566176;
  transition: all 0.18s ease;
}
.session-item:hover {
  background: rgba(255, 255, 255, 0.72);
  color: #25324a;
}
.session-item.active {
  border-color: #dbe8ff;
  background: #fff;
  color: #2563eb;
  box-shadow: 0 8px 20px rgba(50, 82, 132, 0.09);
}
.session-indicator {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #c5cfdd;
  transition: all 0.18s ease;
}
.session-item.active .session-indicator {
  background: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
}
.session-copy {
  min-width: 0;
  flex: 1;
}
.session-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 500;
}
.session-time {
  margin-top: 4px;
  color: #9aa6b8;
  font-size: 10px;
}
.session-delete {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.18s ease;
}
.session-item:hover .session-delete {
  opacity: 1;
}
.session-tip {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 18px 16px;
  padding-top: 13px;
  border-top: 1px solid #e4eaf3;
  color: #99a4b5;
  font-size: 11px;
}
.session-tip-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.11);
}

.session-panel.theme-candy {
  border-color: #f1e1eb;
  background:
    radial-gradient(circle at 0 0, rgba(255, 195, 214, 0.2), transparent 34%),
    linear-gradient(180deg, #fff8fb 0%, #fbf5ff 100%);
}
.theme-candy .session-brand-icon {
  background: linear-gradient(145deg, #f472b6, #a78bfa);
  box-shadow: 0 10px 24px rgba(206, 100, 166, 0.25);
}
.theme-candy .new-session-button {
  border-color: #f2cfe1 !important;
  color: #db5c98 !important;
}
.theme-candy .session-item.active {
  border-color: #f5cee2;
  color: #d84d91;
  box-shadow: 0 8px 22px rgba(186, 92, 142, 0.1);
}
.theme-candy .session-item.active .session-indicator {
  background: #ec6aa9;
  box-shadow: 0 0 0 4px rgba(236, 106, 169, 0.11);
}
.theme-candy .session-branding small,
.theme-candy .session-time {
  color: #a88e9d;
}

.session-panel.theme-mint {
  border-color: #def0e9;
  background:
    radial-gradient(circle at 0 0, rgba(167, 243, 208, 0.24), transparent 35%),
    linear-gradient(180deg, #f6fffb 0%, #f1fbf8 100%);
}
.theme-mint .session-brand-icon {
  background: linear-gradient(145deg, #059669, #2dd4bf);
  box-shadow: 0 10px 24px rgba(16, 185, 129, 0.24);
}
.theme-mint .new-session-button {
  border-color: #cce9df !important;
  color: #0f9f76 !important;
}
.theme-mint .session-item.active {
  border-color: #cdeee2;
  color: #078a68;
  box-shadow: 0 8px 22px rgba(49, 130, 102, 0.09);
}
.theme-mint .session-item.active .session-indicator {
  background: #10b981;
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.1);
}
.theme-mint .session-branding small,
.theme-mint .session-time {
  color: #89a69b;
}

@media (max-width: 900px) {
  .session-panel {
    width: 100%;
    max-height: 260px;
    flex-shrink: 0;
    border-right: 0;
    border-bottom: 1px solid #e9edf5;
  }
  .session-header {
    padding: 13px 14px 10px;
  }
  .session-list {
    display: flex;
    gap: 8px;
    padding: 0 12px 12px;
    overflow-x: auto;
    overflow-y: hidden;
  }
  .session-item {
    min-width: 215px;
    margin-bottom: 0;
  }
  .session-tip {
    display: none;
  }
}
</style>
