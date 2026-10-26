<template>
  <el-dialog v-model="visible" title="调整耗材库存" width="520px" append-to-body destroy-on-close @open="resetForm">
    <el-alert
      class="adjust-tip"
      type="warning"
      :closable="false"
      show-icon
      :title="`${material.materialName || '当前耗材'}，当前库存 ${quantity(material.stockQuantity)} ${material.unit || ''}`"
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-width="95px">
      <el-form-item label="调整方向" prop="changeType">
        <el-radio-group v-model="form.changeType">
          <el-radio value="ADJUST_IN">入库</el-radio>
          <el-radio value="ADJUST_OUT">出库</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="调整数量" prop="quantity">
        <el-input-number v-model="form.quantity" :min="0.01" :precision="2" :step="1" style="width: 100%" />
      </el-form-item>
      <el-form-item label="调整原因" prop="reason">
        <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="例如：采购入库、盘亏核减、过期报废" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="saving" @click="submit">确认调整</el-button>
      <el-button @click="visible = false">取消</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="MaterialStockAdjust">
import { adjustMaterialStock } from "@/api/module/material";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  material: { type: Object, default: () => ({}) }
});
const emit = defineEmits(["update:modelValue", "success"]);
const { proxy } = getCurrentInstance();
const formRef = ref(null);
const saving = ref(false);
const form = reactive({ changeType: "ADJUST_IN", quantity: 1, reason: "" });
const rules = {
  changeType: [{ required: true, message: "请选择调整方向", trigger: "change" }],
  quantity: [{ required: true, message: "请输入调整数量", trigger: "blur" }],
  reason: [{ required: true, message: "请填写调整原因", trigger: "blur" }]
};
const visible = computed({
  get: () => props.modelValue,
  set: value => emit("update:modelValue", value)
});

function resetForm() {
  Object.assign(form, { changeType: "ADJUST_IN", quantity: 1, reason: "" });
  formRef.value?.clearValidate();
}

function submit() {
  formRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    adjustMaterialStock(props.material.id, { ...form }).then(() => {
      proxy.$modal.msgSuccess("库存调整成功");
      visible.value = false;
      emit("success");
    }).finally(() => {
      saving.value = false;
    });
  });
}

function quantity(value) {
  return Number(value || 0).toFixed(2);
}
</script>

<style scoped>
.adjust-tip { margin-bottom: 16px; }
</style>
