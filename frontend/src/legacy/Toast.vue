<template>
  <div v-if="message" class="legacy-toast" role="status">{{ message }}</div>
</template>

<script setup>
import {ref, onMounted, onBeforeUnmount} from 'vue';

const message = ref('');
let timer;

function show(event) {
  message.value = typeof event.detail === 'string' ? event.detail : 'Chức năng đang được phát triển.';
  clearTimeout(timer);
  timer = setTimeout(() => {
    message.value = '';
  }, 3000);
}

onMounted(() => {
  window.addEventListener('bonsai:toast', show);
  window.addEventListener('bonsai:pending', show);
});
onBeforeUnmount(() => {
  window.removeEventListener('bonsai:toast', show);
  window.removeEventListener('bonsai:pending', show);
  clearTimeout(timer);
});
</script>

<style scoped>
.legacy-toast {
  position: fixed;
  top: 88px;
  right: 24px;
  z-index: 9999;
  max-width: 320px;
  padding: 12px 20px;
  border-radius: 12px;
  color: white;
  background: var(--ag-primary-600);
  box-shadow: var(--ag-shadow-lg);
}
</style>
