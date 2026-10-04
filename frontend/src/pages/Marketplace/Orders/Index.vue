<template>
  <MarketplaceLayout>
    <section class="max-w-[1320px] mx-auto px-5 py-8">
      <h1 class="text-lg sm:text-xl font-bold text-[var(--ag-text-primary)] tracking-tight mb-4 sm:mb-6">Đơn hàng của tôi</h1>

      <template v-if="orders.data?.length">
        <div class="space-y-3">
          <div v-for="order in orders.data" :key="order.id"
            class="bg-white rounded-2xl border border-[var(--ag-border)] p-4 hover:border-[var(--ag-primary-500)]/25 transition-all duration-300">
            <div class="flex items-center gap-4">
              <Link :href="route('agriverse.shop.orders.show', order.id)" class="flex items-center gap-4 flex-1 min-w-0">
                <div class="w-14 h-14 rounded-xl bg-[var(--ag-bg)] flex items-center justify-center shrink-0">
                  <span class="text-xl text-[var(--ag-neutral-300)] font-bold">{{ order.product?.name?.charAt(0)?.toUpperCase() }}</span>
                </div>
                <div class="min-w-0">
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-bold text-[var(--ag-text-primary)] truncate">{{ order.product?.name }}</span>
                    <span class="text-[10px] px-2 py-0.5 rounded-full font-semibold" :class="statusClass(order.status)">
                      {{ statusLabel(order.status) }}
                    </span>
                  </div>
                  <div class="flex items-center gap-4 text-sm text-[var(--ag-text-secondary)] mt-1">
                    <span class="font-medium">x{{ order.quantity }}</span>
                    <span class="font-semibold text-[var(--ag-danger)]">{{ formatPrice(order.total_amount) }}₫</span>
                    <span>{{ formatDate(order.created_at) }}</span>
                  </div>
                </div>
              </Link>
              <div class="flex items-center gap-2 shrink-0">
                <span v-if="order.tracking_number" class="text-[11px] text-[var(--ag-text-secondary)] truncate max-w-[100px]">{{ order.tracking_number }}</span>
                <Link v-if="order.status === 'shipping'" :href="route('agriverse.shop.orders.show', order.id)"
                  class="h-9 px-4 rounded-xl bg-[var(--ag-primary-500)]/10 text-[var(--ag-primary-500)] text-xs font-semibold hover:bg-[var(--ag-primary-500)]/20 transition-all flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-sm">local_shipping</span>
                  Theo dõi
                </Link>
                <button v-if="['pending', 'confirmed'].includes(order.status)" @click.stop="openCancel(order)"
                  class="h-9 px-4 rounded-xl bg-[var(--ag-danger)]/10 text-[var(--ag-danger)] text-xs font-semibold hover:bg-[var(--ag-danger)]/20 transition-all">
                  Hủy
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Cancel modal -->
        <div v-if="cancelTarget" class="fixed inset-0 z-50 flex items-center justify-center bg-[var(--ag-text-primary)]/30 backdrop-blur-sm" @click.self="cancelTarget = null">
          <div class="bg-white rounded-3xl p-6 max-w-md w-full mx-4">
            <h3 class="text-base font-bold text-[var(--ag-text-primary)] mb-3">Hủy đơn hàng</h3>
            <textarea v-model="cancelReason" placeholder="Nhập lý do hủy..."
              class="w-full h-24 px-4 py-3 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none resize-none focus:border-[var(--ag-danger)]/40 focus:ring-4 focus:ring-[var(--ag-danger)]/8 mb-4" />
            <div class="flex gap-3">
              <button @click="handleCancel" class="flex-1 h-11 rounded-2xl bg-[var(--ag-danger)] text-white text-sm font-semibold hover:bg-[var(--ag-danger)]/80 transition-all">Xác nhận hủy</button>
              <button @click="cancelTarget = null" class="flex-1 h-11 rounded-2xl border-2 border-[var(--ag-border)] text-[var(--ag-text-secondary)] text-sm font-semibold hover:bg-[var(--ag-bg)] transition-all">Đóng</button>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="bg-white rounded-2xl border border-[var(--ag-border)] p-16 text-center">
        <div class="w-20 h-20 rounded-2xl bg-[var(--ag-bg)] flex items-center justify-center mx-auto">
          <span class="material-symbols-outlined text-3xl text-[var(--ag-neutral-300)]">receipt_long</span>
        </div>
        <div class="text-base font-bold text-[var(--ag-text-primary)] mt-5">Chưa có đơn hàng nào</div>
        <div class="text-sm text-[var(--ag-text-muted)] mt-1">Hãy mua sắm và đặt hàng đầu tiên.</div>
        <Link :href="route('agriverse.shop.products.index')"
          class="inline-flex items-center gap-2 mt-6 h-11 px-6 rounded-2xl bg-[var(--ag-primary-500)] text-white text-sm font-semibold hover:bg-[var(--ag-primary-600)] transition-all duration-300 active:scale-[0.97]">
          Mua sắm ngay
        </Link>
      </div>
    </section>
  </MarketplaceLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { formatPrice, statusLabel, statusClass } from '@agriverse/utils';
import { useToast } from 'primevue/usetoast';

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

const toast = useToast();
const props = defineProps({ orders: Object });

const cancelTarget = ref(null);
const cancelReason = ref('');

function openCancel(order) {
  cancelTarget.value = order;
  cancelReason.value = '';
}

function handleCancel() {
  if (!cancelReason.value) { toast.add({ severity: 'error', summary: 'Vui lòng nhập lý do hủy', life: 3000 }); return; }
  router.post(route('agriverse.api.orders.cancel', cancelTarget.value.id), { reason: cancelReason.value }, {
    preserveState: true,
    preserveScroll: true,
    onSuccess: () => { cancelTarget.value = null; cancelReason.value = ''; },
    onError: () => toast.add({ severity: 'error', summary: 'Hủy đơn thất bại', life: 3000 }),
  });
}
</script>
