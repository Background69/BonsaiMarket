<template>
  <Transition name="compare-slide">
    <div v-if="count > 0" class="compare-bar">
      <div style="max-width: var(--ag-container-max, 1280px); margin: 0 auto; padding: 0 var(--ag-margin-desktop, 64px); display: flex; align-items: center; justify-content: space-between;">
        <div class="flex items-center gap-3">
          <span class="compare-bar-icon">
            <span class="material-symbols-outlined">compare_arrows</span>
          </span>
          <span style="font-family: var(--ag-font-body); font-size: 14px; color: var(--ag-text-primary);">
            <strong>{{ count }}</strong> sản phẩm đang so sánh
          </span>
          <button @click="clear" class="compare-bar-clear">Xoá tất cả</button>
        </div>
        <div class="flex items-center gap-3">
          <button v-if="canCompare" @click="goCompare" class="compare-bar-action">
            So sánh ngay
            <span class="material-symbols-outlined" style="font-size: 16px;">arrow_forward</span>
          </button>
          <button v-else class="compare-bar-hint" disabled>
            Cần thêm {{ 2 - count }} sản phẩm
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import { useCompare } from '@agriverse/Composables/useCompare';

const { compareIds, count, canCompare, clear } = useCompare();

function goCompare() {
  const ids = compareIds.value.join(',');
  router.get(route('agriverse.shop.compare.index', { ids }));
}
</script>

<style scoped>
.compare-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(16px);
  border-top: 1px solid color-mix(in srgb, var(--ag-border) 40%, transparent);
  padding: 12px 0;
  z-index: 50;
  box-shadow: 0 -4px 24px rgba(0,0,0,0.06);
}
.compare-bar-icon {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: rgba(72, 103, 48, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ag-primary-500);
}
.compare-bar-icon .material-symbols-outlined { font-size: 18px; }
.compare-bar-clear {
  background: none;
  border: none;
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
  color: var(--ag-text-muted);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
  transition: color 0.2s;
}
.compare-bar-clear:hover { color: var(--ag-danger); }
.compare-bar-action {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  background: var(--ag-primary-500);
  color: white;
  border: none;
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}
.compare-bar-action:hover { background: var(--ag-primary-600); gap: 10px; }
.compare-bar-hint {
  padding: 10px 24px;
  background: var(--ag-surface-container-highest);
  color: var(--ag-text-muted);
  border: none;
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  cursor: default;
}
.compare-slide-enter-active,
.compare-slide-leave-active { transition: transform 0.3s ease, opacity 0.3s ease; }
.compare-slide-enter-from,
.compare-slide-leave-to { transform: translateY(100%); opacity: 0; }
</style>
