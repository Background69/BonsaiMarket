<template>
  <AdminLayout>
    <div class="max-w-3xl mx-auto py-8 px-5 space-y-4">
      <div class="bg-white rounded-2xl border border-[var(--ag-border)] p-6">
        <div class="flex items-center justify-between mb-4">
          <h1 class="text-lg font-bold text-[var(--ag-text-primary)]">Yêu cầu hoàn tiền #{{ refund.id }}</h1>
          <span class="text-xs px-3 py-1 rounded-full font-semibold" :class="statusClass(refund.status)">{{ statusLabel(refund.status) }}</span>
        </div>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
            <div class="text-[var(--ag-text-muted)] text-xs">Sản phẩm</div>
            <div class="font-semibold text-[var(--ag-text-primary)]">{{ refund.order?.product?.name }}</div>
          </div>
          <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
            <div class="text-[var(--ag-text-muted)] text-xs">Người yêu cầu</div>
            <div class="font-semibold text-[var(--ag-text-primary)]">{{ refund.user?.name }}</div>
          </div>
          <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
            <div class="text-[var(--ag-text-muted)] text-xs">Số tiền</div>
            <div class="font-semibold text-[var(--ag-danger)]">{{ formatPrice(refund.amount) }}₫</div>
          </div>
          <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
            <div class="text-[var(--ag-text-muted)] text-xs">Lý do</div>
            <div class="font-semibold text-[var(--ag-text-primary)]">{{ refund.reason }}</div>
          </div>
          <div v-if="refund.description" class="col-span-2 p-3 rounded-xl bg-[var(--ag-bg)]">
            <div class="text-[var(--ag-text-muted)] text-xs">Mô tả</div>
            <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ refund.description }}</div>
          </div>
        </div>

        <div v-if="refund.status === 'pending'" class="mt-6 pt-6 border-t border-[var(--ag-border)]/60 space-y-4">
          <h3 class="text-sm font-bold text-[var(--ag-text-primary)]">Xử lý yêu cầu</h3>
          <textarea v-model="note" placeholder="Ghi chú (bắt buộc nếu từ chối)..."
            class="w-full h-24 px-4 py-3 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none resize-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8" />
          <div class="flex gap-3">
            <button @click="approve" class="h-11 px-6 rounded-2xl bg-[var(--ag-primary-500)] text-white text-sm font-semibold hover:bg-[var(--ag-primary-600)] transition-all">
              Duyệt hoàn tiền
            </button>
            <button @click="reject" class="h-11 px-6 rounded-2xl bg-[var(--ag-danger)] text-white text-sm font-semibold hover:bg-[#A0122E] transition-all">
              Từ chối
            </button>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
import { formatPrice, statusLabel, statusClass } from '@agriverse/utils';

const props = defineProps({ refund: Object });
const note = ref('');

function approve() {
  router.post(route('admin.agriverse.refunds.approve', props.refund.id), { note: note.value }, { preserveState: true });
}
function reject() {
  if (!note.value) { alert('Vui lòng nhập ghi chú khi từ chối.'); return; }
  router.post(route('admin.agriverse.refunds.reject', props.refund.id), { note: note.value }, { preserveState: true });
}
</script>
