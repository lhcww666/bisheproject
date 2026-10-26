<template>
  <div class="app-container" v-loading="loading">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>申请领用耗材</span>
          <el-button icon="Back" @click="goBack">返回列表</el-button>
        </div>
      </template>

      <el-alert
        class="form-alert"
        type="info"
        :closable="false"
        show-icon
        title="仅可对本人的待审核或已通过预约申请耗材，耗材必须属于预约实验室。审核通过后才会扣减库存。"
      />

      <el-alert
        v-if="reservationOptions.length === 0 && !loading"
        class="form-alert"
        type="warning"
        :closable="false"
        show-icon
        title="当前没有可申请耗材的预约，请先提交预约并等待其进入待审核或已通过状态。"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="105px">
        <el-form-item label="关联预约" prop="reservationId">
          <el-select
            v-model="form.reservationId"
            filterable
            placeholder="请选择本人预约"
            style="width: 100%"
            @change="handleReservationChange"
          >
            <el-option
              v-for="item in reservationOptions"
              :key="item.id"
              :label="reservationLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属实验室">
          <el-input :model-value="selectedReservation ? reservationRoomLabel(selectedReservation) : ''" disabled />
        </el-form-item>
        <el-form-item label="领用耗材" prop="materialId">
          <el-select
            v-model="form.materialId"
            filterable
            :disabled="!form.reservationId"
            :loading="materialLoading"
            placeholder="请选择实验室耗材"
            style="width: 100%"
            @change="handleMaterialChange"
          >
            <el-option
              v-for="item in materialOptions"
              :key="item.id"
              :label="materialLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="当前库存">
          <span v-if="selectedMaterial">
            {{ quantity(selectedMaterial.stockQuantity) }} {{ selectedMaterial.unit }}
            <el-tag v-if="isLowStock(selectedMaterial)" class="stock-tag" type="danger">低库存</el-tag>
          </span>
          <span v-else class="secondary-text">选择耗材后显示</span>
        </el-form-item>
        <el-form-item label="申请数量" prop="applyQuantity">
          <el-input-number
            v-model="form.applyQuantity"
            :min="0.01"
            :max="selectedMaterial ? Number(selectedMaterial.stockQuantity) : undefined"
            :precision="2"
            :step="1"
            style="width: 220px"
          />
          <span class="form-hint">不能超过当前库存；审核时仍会再次校验。</span>
        </el-form-item>
        <el-form-item label="申请说明" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="可说明实验用途或领用原因" />
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" :loading="saving" :disabled="!form.reservationId || !form.materialId" @click="submitForm">提交申请</el-button>
        <el-button @click="goBack">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="MaterialApplyAdd">
import {
  applyMaterial,
  listAvailableMaterials,
  listMaterialReservationOptions
} from "@/api/module/materialApply";

const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_reservation_status } = proxy.useDict("module_reservation_status");
const loading = ref(true);
const saving = ref(false);
const materialLoading = ref(false);
const formRef = ref(null);
const reservationOptions = ref([]);
const materialOptions = ref([]);
const form = reactive({
  reservationId: null,
  materialId: null,
  applyQuantity: 1,
  remark: null
});
const selectedReservation = computed(() => reservationOptions.value.find(item => item.id === form.reservationId));
const selectedMaterial = computed(() => materialOptions.value.find(item => item.id === form.materialId));
const rules = {
  reservationId: [{ required: true, message: "请选择关联预约", trigger: "change" }],
  materialId: [{ required: true, message: "请选择领用耗材", trigger: "change" }],
  applyQuantity: [{ required: true, message: "请输入申请数量", trigger: "blur" }]
};

onMounted(loadReservations);

function loadReservations() {
  loading.value = true;
  listMaterialReservationOptions().then(response => {
    reservationOptions.value = response.data || [];
  }).finally(() => {
    loading.value = false;
  });
}

function handleReservationChange() {
  form.materialId = null;
  form.applyQuantity = 1;
  materialOptions.value = [];
  if (!form.reservationId) return;
  materialLoading.value = true;
  listAvailableMaterials(form.reservationId).then(response => {
    materialOptions.value = response.data || [];
  }).finally(() => {
    materialLoading.value = false;
  });
}

function handleMaterialChange() {
  form.applyQuantity = 1;
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return;
    if (Number(form.applyQuantity) <= 0) {
      proxy.$modal.msgError("申请数量必须大于0");
      return;
    }
    saving.value = true;
    applyMaterial({ ...form }).then(response => {
      proxy.$modal.msgSuccess("领用申请已提交，等待管理员审核");
      const id = response?.data?.id;
      router.push(id ? "/materialApplyView/" + id : "/module/material-apply");
    }).finally(() => {
      saving.value = false;
    });
  });
}

function goBack() {
  router.push("/module/material-apply");
}

function reservationLabel(item) {
  const room = reservationRoomLabel(item);
  const time = item.startTime ? proxy.parseTime(item.startTime, "{y}-{m}-{d} {h}:{i}") : "";
  const status = statusLabel(item.status);
  return [item.reservationNo, item.experimentName, room, time, status].filter(Boolean).join(" · ");
}

function reservationRoomLabel(item) {
  return [item?.roomCode, item?.roomName].filter(Boolean).join(" · ");
}

function materialLabel(item) {
  return [
    item.materialCode,
    item.materialName,
    item.specification,
    `库存 ${quantity(item.stockQuantity)} ${item.unit}`
  ].filter(Boolean).join(" · ");
}

function statusLabel(value) {
  return module_reservation_status.value.find(item => item.value === value)?.label || "";
}

function quantity(value) {
  return Number(value || 0).toFixed(2);
}

function isLowStock(item) {
  return Number(item.stockQuantity || 0) <= Number(item.minimumStock || 0);
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.form-alert { margin-bottom: 16px; }
.form-footer { margin-top: 20px; }
.form-hint { margin-left: 12px; color: #909399; font-size: 12px; }
.secondary-text { color: #909399; }
.stock-tag { margin-left: 8px; }
</style>
