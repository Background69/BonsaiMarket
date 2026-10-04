<template>
  <MarketplaceLayout>
    <main class="max-w-[1280px] mx-auto px-5 sm:px-16 py-20 min-h-screen">
      <div class="mb-12 max-w-[600px]">
        <h1 class="text-3xl sm:text-4xl font-semibold tracking-tight mb-4" style="color: var(--ag-text-primary); font-family: var(--ag-font-display);">Thông báo</h1>
        <p class="text-lg text-[var(--ag-text-secondary)]">Cập nhật đơn hàng và hoạt động mới nhất.</p>
      </div>

      <div class="flex items-center justify-between mb-8">
        <div class="flex items-center gap-2 text-sm text-[var(--ag-text-muted)]">
          <span class="font-semibold text-[var(--ag-text-primary)]">{{ unreadCount }}</span> thông báo chưa đọc
        </div>
        <button v-if="unreadCount > 0" @click="markAllRead"
          class="flex items-center gap-2 px-4 py-2 rounded-full border border-[var(--ag-border)] text-sm font-semibold text-[var(--ag-text-secondary)] hover:bg-[var(--ag-bg)] transition-all">
          <span class="material-symbols-outlined text-base">done_all</span>
          Đánh dấu tất cả đã đọc
        </button>
      </div>

      <div v-if="!notifications?.data?.length" class="flex flex-col items-center justify-center py-24 text-center">
        <div class="w-16 h-16 rounded-full bg-[var(--ag-primary-500)]/10 flex items-center justify-center mb-4">
          <span class="material-symbols-outlined text-3xl text-[var(--ag-primary-500)]">notifications_none</span>
        </div>
        <h3 class="text-lg font-semibold text-[var(--ag-text-primary)] mb-2">Chưa có thông báo</h3>
        <p class="text-sm text-[var(--ag-text-secondary)]">Bạn sẽ nhận được thông báo khi có cập nhật về đơn hàng.</p>
      </div>

      <div v-else class="space-y-2">
        <div v-for="notif in notifications.data" :key="notif.id"
          @click="handleClick(notif)"
          class="flex items-start gap-4 p-4 rounded-2xl cursor-pointer transition-all"
          :class="notif.read_at ? 'bg-white border border-[var(--ag-border)]/60' : 'bg-[var(--ag-primary-500)]/5 border border-[var(--ag-primary-500)]/10'">
          <div class="w-10 h-10 rounded-full flex items-center justify-center shrink-0"
            :class="notif.read_at ? 'bg-[var(--ag-bg)]' : 'bg-[var(--ag-primary-500)]/10'">
            <span class="material-symbols-outlined text-xl"
              :class="notif.read_at ? 'text-[var(--ag-text-muted)]' : 'text-[var(--ag-primary-500)]'">
              {{ notif.data?.type === 'new_order' ? 'store' : notif.data?.type === 'order_status' ? 'receipt_long' : 'notifications' }}
            </span>
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-semibold" :class="notif.read_at ? 'text-[var(--ag-text-primary)]' : 'text-[var(--ag-text-primary)]'">
              {{ notif.data?.message || '' }}
            </p>
            <p class="text-xs text-[var(--ag-text-muted)] mt-1">{{ formatDate(notif.created_at) }}</p>
          </div>
          <div v-if="!notif.read_at" class="w-2 h-2 rounded-full bg-[var(--ag-primary-500)] shrink-0 mt-2"></div>
        </div>
      </div>

      <div v-if="notifications.total > notifications.per_page" class="flex justify-center mt-8 gap-2">
        <button v-for="link in notifications.links" :key="link.label"
          v-html="link.label"
          @click="link.url && router.get(link.url)"
          class="px-3 py-1.5 rounded-lg text-sm font-semibold transition-all"
          :class="link.active ? 'bg-[var(--ag-primary-500)] text-white' : 'text-[var(--ag-text-secondary)] hover:bg-[var(--ag-bg)]'"
          :disabled="!link.url">
        </button>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { computed } from 'vue';
import { router } from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';

const props = defineProps({
  notifications: { type: Object, default: () => ({ data: [], total: 0, per_page: 20, links: [] }) },
});

const unreadCount = computed(() => props.notifications.data?.filter(n => !n.read_at).length || 0);

function handleClick(notif) {
  markAsRead(notif);
  const orderId = notif.data?.order_id;
  if (orderId) {
    router.get(route('agriverse.shop.orders.show', orderId));
  }
}

async function markAsRead(notif) {
  if (notif.read_at) return;
  try {
    await window.axios.put(route('api.notifications.read', notif.id));
    notif.read_at = new Date().toISOString();
  } catch {}
}

async function markAllRead() {
  try {
    await window.axios.put(route('api.notifications.read-all'));
    props.notifications.data.forEach(n => {
      if (!n.read_at) n.read_at = new Date().toISOString();
    });
  } catch {}
}

function formatDate(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString('vi-VN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}
</script>
