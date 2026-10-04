<template>
  <div class="space-y-2">
    <div class="flex items-center justify-between">
      <span class="text-xs font-semibold text-stone-700">Mô hình 3D</span>
      <span class="text-[10px] text-stone-400">Kéo để xoay 360°</span>
    </div>
    <ThreeScene :model-url="modelUrl" :auto-rotate="true" />
  </div>
</template>

<script setup>
import { defineAsyncComponent } from 'vue';

const ThreeScene = defineAsyncComponent(() => import('@agriverse/Components/ThreeScene.vue'));

defineProps({
  modelUrl: { type: String, default: '' },
});
</script>
