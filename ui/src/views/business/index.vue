<template>
  <component v-if="pageComponent" :is="pageComponent" />
  <el-result
    v-else
    icon="warning"
    title="业务页面不存在"
    :sub-title="`未找到 ${moduleName}/${businessName}/${pageName} 对应的页面文件`"
  />
</template>

<script setup name="BusinessPage">
import { computed, defineAsyncComponent, watchEffect } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const pages = import.meta.glob(["../**/*.vue", "!../business/**"]);

const moduleName = computed(() => String(route.params.moduleName || ""));
const businessName = computed(() => String(route.params.businessName || ""));
const pageName = computed(() => String(route.params.page || ""));
const pageKey = computed(() => `../${moduleName.value}/${businessName.value}/${pageName.value}.vue`);
const pageComponent = computed(() => {
  const loader = pages[pageKey.value];
  return loader ? defineAsyncComponent(loader) : null;
});

watchEffect(() => {
  route.meta.activeMenu = `/${moduleName.value}/${businessName.value}`;
});
</script>
