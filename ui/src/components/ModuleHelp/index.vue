<template>
  <span class="module-help">
    <el-button type="info" plain icon="QuestionFilled" @click="helpOpen = true">操作说明</el-button>
    <el-drawer
      v-model="helpOpen"
      :title="help.title ? `操作说明 · ${help.title}` : '操作说明'"
      size="600px"
      append-to-body
    >
      <div class="module-help__body">
        <p v-if="help.summary" class="module-help__summary">{{ help.summary }}</p>

        <template v-if="help.steps && help.steps.length">
          <h4 class="module-help__heading">怎么操作</h4>
          <ol class="module-help__list">
            <li v-for="(step, index) in help.steps" :key="index">{{ step }}</li>
          </ol>
        </template>

        <template v-if="help.fields && help.fields.length">
          <h4 class="module-help__heading">字段含义</h4>
          <el-table :data="help.fields" size="small" border>
            <el-table-column prop="name" label="字段" width="170" />
            <el-table-column prop="desc" label="含义" />
          </el-table>
        </template>

        <template v-if="help.notes && help.notes.length">
          <h4 class="module-help__heading">注意事项</h4>
          <ul class="module-help__list">
            <li v-for="(note, index) in help.notes" :key="index">{{ note }}</li>
          </ul>
        </template>
      </div>
    </el-drawer>
  </span>
</template>

<script setup name="ModuleHelp">
import helpContent from "./content";

const props = defineProps({
  /* 模块名，对应 content.js 里的键，如 room、device、reservation */
  module: {
    type: String,
    required: true,
  },
});

const helpOpen = ref(false);
const help = computed(() => helpContent[props.module] || {});
</script>

<style lang="scss" scoped>
.module-help__body {
  font-size: 13px;
  color: #303133;
  line-height: 1.7;
}

.module-help__summary {
  margin: 0 0 16px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.module-help__heading {
  margin: 20px 0 8px;
  font-size: 14px;
  color: #409eff;
}

.module-help__list {
  margin: 0;
  padding-left: 20px;

  li {
    margin-bottom: 6px;
  }
}
</style>
