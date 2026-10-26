<template>
  <div class="app-container" v-loading="loading">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>{{ pageTitle }}</span>
          <el-button icon="Back" @click="goBack">返回</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
        <el-form-item label="耗材编号" prop="materialCode">
          <el-input v-model="form.materialCode" maxlength="30" placeholder="例如 MAT-A101-001" />
        </el-form-item>
        <el-form-item label="耗材名称" prop="materialName">
          <el-input v-model="form.materialName" maxlength="100" placeholder="请输入耗材名称" />
        </el-form-item>
        <el-form-item label="规格型号" prop="specification">
          <el-input v-model="form.specification" maxlength="100" placeholder="例如 100只/盒、1000μL" />
        </el-form-item>
        <el-form-item label="计量单位" prop="unit">
          <el-input v-model="form.unit" maxlength="20" placeholder="例如 盒、包、根、张" />
        </el-form-item>
        <el-form-item label="所属实验室" prop="roomId">
          <el-select v-model="form.roomId" filterable placeholder="请选择实验室" style="width: 100%">
            <el-option
              v-for="room in roomOptions"
              :key="room.id"
              :label="roomLabel(room)"
              :value="room.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="当前库存" prop="stockQuantity">
          <el-input-number v-model="form.stockQuantity" :min="0" :precision="2" :step="1" :disabled="isEdit" style="width: 220px" />
          <span class="form-hint">{{ isEdit ? "库存需从列表或详情页的“调整库存”变更，以确保生成流水。" : "创建时的初始库存会生成一条初始入库流水。" }}</span>
        </el-form-item>
        <el-form-item label="最低库存" prop="minimumStock">
          <el-input-number v-model="form.minimumStock" :min="0" :precision="2" :step="1" style="width: 220px" />
          <span class="form-hint">库存低于或等于该值时，列表会标记为低库存。</span>
        </el-form-item>
        <el-form-item label="耗材状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in module_material_status" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="可填写保管要求、使用注意事项" />
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" :loading="saving" @click="submitForm">{{ isEdit ? "保存" : "新增" }}</el-button>
        <el-button @click="goBack">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="MaterialDetail">
import { addMaterial, getMaterial, listMaterialRoomOptions, updateMaterial } from "@/api/module/material";

const props = defineProps({
  isEdit: { type: Boolean, default: false }
});
const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const { module_material_status } = proxy.useDict("module_material_status");

const pageBase = "/business/module/material";
const menuPath = "/module/material";
const pageTitle = computed(() => props.isEdit ? "修改耗材档案" : "新增耗材档案");
const loading = ref(false);
const saving = ref(false);
const formRef = ref(null);
const roomOptions = ref([]);
const form = reactive({
  id: null,
  roomId: null,
  materialCode: null,
  materialName: null,
  specification: null,
  unit: null,
  stockQuantity: 0,
  minimumStock: 0,
  status: "1",
  remark: null
});
const rules = {
  roomId: [{ required: true, message: "请选择所属实验室", trigger: "change" }],
  materialCode: [{ required: true, message: "耗材编号不能为空", trigger: "blur" }],
  materialName: [{ required: true, message: "耗材名称不能为空", trigger: "blur" }],
  unit: [{ required: true, message: "计量单位不能为空", trigger: "blur" }],
  stockQuantity: [{ required: true, message: "当前库存不能为空", trigger: "change" }],
  minimumStock: [{ required: true, message: "最低库存不能为空", trigger: "change" }],
  status: [{ required: true, message: "请选择耗材状态", trigger: "change" }]
};

onMounted(() => {
  loadRooms();
  if (props.isEdit) {
    loadDetail();
  }
});

function loadRooms() {
  listMaterialRoomOptions().then(response => {
    roomOptions.value = response.data || [];
  });
}

function loadDetail() {
  loading.value = true;
  getMaterial(route.params.id).then(response => {
    Object.assign(form, response.data || {});
  }).finally(() => {
    loading.value = false;
  });
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return;
    saving.value = true;
    const request = props.isEdit ? updateMaterial : addMaterial;
    request({ ...form }).then(response => {
      proxy.$modal.msgSuccess(props.isEdit ? "修改成功" : "新增成功");
      const id = form.id || response?.data?.id;
      router.push(id ? pageBase + "/view/" + id : menuPath);
    }).finally(() => {
      saving.value = false;
    });
  });
}

function goBack() {
  if (props.isEdit && route.params.id) {
    router.push(pageBase + "/view/" + route.params.id);
    return;
  }
  router.push(menuPath);
}

function roomLabel(room) {
  return [room.roomCode, room.roomName].filter(Boolean).join(" · ");
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.form-footer { margin-top: 20px; }
.form-hint { margin-left: 12px; color: #909399; font-size: 12px; line-height: 1.5; }
</style>
