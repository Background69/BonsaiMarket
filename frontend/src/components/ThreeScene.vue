<template>
  <div ref="containerRef" class="relative w-full h-full min-h-[300px] rounded-2xl overflow-hidden bg-stone-50">
    <canvas ref="canvasRef" class="w-full h-full block" />

    <!-- Loading -->
    <div v-if="loading" class="absolute inset-0 flex flex-col items-center justify-center bg-stone-50/80 backdrop-blur-sm">
      <div class="w-8 h-8 rounded-full border-2 border-emerald-600 border-t-transparent animate-spin mb-2" />
      <span class="text-xs text-emerald-700">Đang tải mô hình 3D...</span>
    </div>

    <!-- Progress -->
    <div v-if="progress > 0 && progress < 100" class="absolute bottom-4 left-4 right-4">
      <div class="h-1 bg-stone-200 rounded-full overflow-hidden">
        <div class="h-full bg-emerald-500 rounded-full transition-transform duration-300 origin-left" :style="{ transform: 'scaleX(' + (progress / 100) + ')' }" />
      </div>
    </div>

    <!-- Error -->
    <div v-if="error" class="absolute inset-0 flex flex-col items-center justify-center bg-stone-50/80">
      <span class="material-symbols-outlined text-3xl text-stone-300 mb-2">broken_image</span>
      <span class="text-xs text-emerald-700">Không thể tải mô hình</span>
    </div>

    <!-- Controls Overlay -->
    <div v-if="!loading && !error" class="absolute bottom-3 right-3 flex gap-1">
      <button @click="resetCamera" class="w-7 h-7 rounded-lg bg-white/80 backdrop-blur-sm shadow-sm flex items-center justify-center text-stone-500 hover:text-emerald-700 hover:bg-white transition-all" title="Reset camera">
        <span class="material-symbols-outlined text-sm">center_focus_strong</span>
      </button>
      <button @click="toggleAutoRotate" class="w-7 h-7 rounded-lg bg-white/80 backdrop-blur-sm shadow-sm flex items-center justify-center transition-all" :class="autoRotateEnabled ? 'text-emerald-600 bg-emerald-50' : 'text-stone-500 hover:text-emerald-700 hover:bg-white'" title="Tự động xoay">
        <span class="material-symbols-outlined text-sm">360</span>
      </button>
      <button @click="toggleFullscreen" class="w-7 h-7 rounded-lg bg-white/80 backdrop-blur-sm shadow-sm flex items-center justify-center text-stone-500 hover:text-emerald-700 hover:bg-white transition-all" title="Toàn màn hình">
        <span class="material-symbols-outlined text-sm">fullscreen</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useThreeScene } from '@agriverse/Composables/useThreeScene';
import { useGLTFLoader } from '@agriverse/Composables/useGLTFLoader';

const props = defineProps({
  modelUrl: { type: String, default: '' },
  autoRotate: { type: Boolean, default: true },
  backgroundColor: { type: String, default: '#f5f5f4' },
});

const containerRef = ref(null);
const canvasRef = ref(null);
const loading = ref(true);
const error = ref(false);
const progress = ref(0);
const autoRotateEnabled = ref(props.autoRotate);

const { init, resize, addModel, clearModel, resetCamera, toggleAutoRotate, dispose } = useThreeScene(canvasRef, {
  backgroundColor: 0xf5f5f4,
  autoRotate: props.autoRotate,
});

function toggleFullscreen() {
  if (!containerRef.value) return;
  if (document.fullscreenElement) {
    document.exitFullscreen();
  } else {
    containerRef.value.requestFullscreen();
  }
}

async function loadModel() {
  if (!props.modelUrl) {
    loading.value = false;
    error.value = false;
    return;
  }

  loading.value = true;
  error.value = false;
  progress.value = 0;

  clearModel();

  try {
    const { loadModel } = useGLTFLoader();
    const model = await loadModel(props.modelUrl, (p) => { progress.value = p; });
    addModel(model);
    progress.value = 100;
  } catch (e) {
    console.error('3D load error:', e);
    error.value = true;
    loading.value = false;
  }
}

let resizeObserver = null;

onMounted(() => {
  init();
  loadModel();
  resizeObserver = new ResizeObserver(() => resize());
  if (containerRef.value) resizeObserver.observe(containerRef.value);
});

onUnmounted(() => {
  dispose();
  if (resizeObserver) resizeObserver.disconnect();
});

watch(() => props.modelUrl, () => { loadModel(); });
</script>
