<template>
  <div class="flex items-center w-full">
    <template v-for="(step, i) in steps" :key="step.key">
      <div class="flex items-center gap-1.5 shrink-0">
        <span
            :class="nodeClass(i)"
            class="w-6 h-6 rounded-full flex items-center justify-center border-2 text-[10px] font-bold transition-all">
          {{ i < currentIndex ? '✓' : i + 1 }}
        </span>
        <span :class="i <= currentIndex ? 'text-[var(--ag-text-primary)]' : 'text-[var(--ag-text-secondary)]'"
              class="text-[10px] font-semibold whitespace-nowrap">
          {{ step.label }}
        </span>
      </div>
      <div v-if="i < steps.length - 1"
           :class="i < currentIndex ? 'bg-[#16a34a]' : (i === currentIndex ? 'bg-[var(--ag-primary-500)]/40' : 'bg-[var(--ag-border)]')"
           class="flex-1 h-0.5 mx-2 rounded-full transition-all"></div>
    </template>
  </div>
</template>

<script setup>
import {computed} from 'vue';

const props = defineProps({
  status: {type: String, default: 'PROPOSED'},
});

const steps = [
  {key: 'PROPOSED', label: 'Đề xuất'},
  {key: 'NEGOTIATE', label: 'Thương lượng'},
  {key: 'ACCEPTED', label: 'Đồng ý'},
  {key: 'APPROVED', label: 'Admin duyệt'},
];

const currentIndex = computed(() => {
  switch (props.status) {
    case 'ACCEPTED':
      return 2;
    case 'APPROVED':
      return 3;
    case 'COUNTERED':
    case 'PROPOSED':
    case 'REJECTED':
    case 'DECLINED':
      return 1;
    default:
      return 0;
  }
});

const nodeClass = (i) => {
  if (props.status === 'REJECTED' || props.status === 'DECLINED') {
    if (i < currentIndex.value) return 'border-[#16a34a] bg-[#16a34a] text-white';
    return 'border-[var(--ag-border)] bg-white text-[var(--ag-text-secondary)]';
  }
  if (i < currentIndex.value) return 'border-[#16a34a] bg-[#16a34a] text-white';
  if (i === currentIndex.value) return 'border-[var(--ag-primary-500)] bg-[var(--ag-primary-500)]/10 text-[var(--ag-primary-500)] animate-pulse';
  return 'border-[var(--ag-border)] bg-white text-[var(--ag-text-secondary)]';
};
</script>