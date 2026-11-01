<template>
  <el-dialog
    :model-value="modelValue"
    title="确认预约草稿"
    width="560px"
    append-to-body
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <template v-if="draft?.request">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="草稿尚未提交。确认后系统会重新检查时间冲突、实验室准入、安全考试和信用分。"
      />
      <el-descriptions class="draft-detail" :column="2" border>
        <el-descriptions-item label="实验内容" :span="2">
          {{ draft.request.experimentName }}
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">
          {{ draft.request.startTime }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间">
          {{ draft.request.endTime }}
        </el-descriptions-item>
        <el-descriptions-item label="参与人数">
          {{ draft.request.participantCount }} 人
        </el-descriptions-item>
        <el-descriptions-item label="实验室 ID">
          {{ draft.request.roomId }}
        </el-descriptions-item>
        <el-descriptions-item label="设备 ID" :span="2">
          {{ draft.request.deviceIds?.length ? draft.request.deviceIds.join("、") : "无" }}
        </el-descriptions-item>
        <el-descriptions-item label="草稿有效期" :span="2">
          {{ draft.expiresAt }}
        </el-descriptions-item>
        <el-descriptions-item v-if="draft.request.remark" label="备注" :span="2">
          {{ draft.request.remark }}
        </el-descriptions-item>
      </el-descriptions>
    </template>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">稍后处理</el-button>
      <el-button type="danger" plain :loading="submitting" @click="$emit('cancel')">
        取消草稿
      </el-button>
      <el-button type="primary" :loading="submitting" @click="$emit('confirm')">
        确认提交预约
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup name="AgentDraftConfirmDialog">
defineProps({
  modelValue: Boolean,
  draft: {
    type: Object,
    default: null
  },
  submitting: Boolean
})

defineEmits(["update:modelValue", "confirm", "cancel"])
</script>

<style lang="scss" scoped>
.draft-detail {
  margin-top: 16px;
}
</style>
