<template>
  <MarketplaceLayout>
    <main class="max-w-[1280px] mx-auto px-5 sm:px-16 py-20 min-h-screen">
      <div class="mb-10">
        <h1 class="text-2xl sm:text-3xl font-semibold mb-2" style="color: var(--ag-text-primary); font-family: var(--ag-font-display);">Affiliate</h1>
        <p class="text-sm" style="color: var(--ag-text-secondary);">Kiếm hoa hồng từ việc giới thiệu sản phẩm.</p>
      </div>

      <!-- Referral link -->
      <div class="rounded-2xl border p-5 mb-8" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
        <div class="text-xs font-semibold uppercase tracking-wider mb-2" style="color: var(--ag-text-secondary);">Link giới thiệu của bạn</div>
        <div class="flex gap-2">
          <input :value="referralLink" readonly class="flex-1 h-11 px-3 rounded-xl text-sm outline-none bg-[var(--ag-bg)]" style="border: 1px solid var(--ag-border); color: var(--ag-text-primary); font-family: monospace;" @click="$event.target.select()" />
          <button @click="copyLink" class="h-11 px-5 rounded-xl text-sm font-semibold transition-all" style="background: var(--ag-primary-500); color: white;">
            <span v-if="copied" class="flex items-center gap-1">
              <span class="material-symbols-outlined text-base">check</span> Đã copy
            </span>
            <span v-else class="flex items-center gap-1">
              <span class="material-symbols-outlined text-base">content_copy</span> Copy
            </span>
          </button>
        </div>
      </div>

      <!-- Stats -->
      <div class="grid grid-cols-3 gap-4 mb-8">
        <div class="rounded-2xl border p-5" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
          <div class="text-xs font-semibold uppercase tracking-wider mb-1" style="color: var(--ag-text-muted);">Tổng hoa hồng</div>
          <div class="text-2xl font-semibold" style="color: var(--ag-primary-500); font-family: var(--ag-font-display);">{{ formatPrice(total_earnings) }}</div>
        </div>
        <div class="rounded-2xl border p-5" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
          <div class="text-xs font-semibold uppercase tracking-wider mb-1" style="color: var(--ag-text-muted);">Đã rút</div>
          <div class="text-2xl font-semibold" style="color: var(--ag-text-primary); font-family: var(--ag-font-display);">{{ formatPrice(paid_commission) }}</div>
        </div>
        <div class="rounded-2xl border p-5" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
          <div class="text-xs font-semibold uppercase tracking-wider mb-1" style="color: var(--ag-text-muted);">Có thể rút</div>
          <div class="text-2xl font-semibold" style="color: var(--ag-warning); font-family: var(--ag-font-display);">{{ formatPrice(pending_commission) }}</div>
        </div>
      </div>

      <!-- Referral stats -->
      <div class="grid grid-cols-3 gap-4 mb-8">
        <div class="rounded-2xl border p-4" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
          <div class="text-xs text-[var(--ag-text-muted)]">Tổng lượt giới thiệu</div>
          <div class="text-xl font-bold mt-1" style="color: var(--ag-text-primary);">{{ total_referrals }}</div>
        </div>
        <div class="rounded-2xl border p-4" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
          <div class="text-xs text-[var(--ag-text-muted)]">Tỷ lệ chuyển đổi</div>
          <div class="text-xl font-bold mt-1" style="color: var(--ag-text-primary);">{{ conversion_rate }}%</div>
        </div>
        <div class="rounded-2xl border p-4" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
          <div class="text-xs text-[var(--ag-text-muted)]">Hoa hồng</div>
          <div class="text-xl font-bold mt-1" style="color: var(--ag-primary-500);">{{ affiliate?.commission_rate || 0 }}%</div>
        </div>
      </div>

      <!-- Commission history -->
      <div class="rounded-2xl border mb-8" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
        <div class="px-5 py-4" style="border-bottom: 1px solid var(--ag-border);">
          <h2 class="text-sm font-semibold" style="color: var(--ag-text-primary);">Lịch sử hoa hồng</h2>
        </div>
        <div v-if="recent_commissions.length" class="divide-y" style="border-color: var(--ag-border);">
          <div v-for="c in recent_commissions" :key="c.id" class="flex items-center justify-between px-5 py-3 text-sm">
            <span style="color: var(--ag-text-muted);">{{ formatDate(c.created_at) }}</span>
            <span style="color: var(--ag-text-secondary);">Đơn #{{ c.order_id }}</span>
            <span class="font-semibold" style="color: var(--ag-primary-500);">{{ formatPrice(c.commission_amount) }}</span>
            <span class="text-xs font-semibold px-2 py-0.5 rounded-full" :style="{ background: c.status === 'paid' ? 'rgba(72,103,48,0.1)' : 'rgba(217,119,6,0.1)', color: c.status === 'paid' ? 'var(--ag-primary-500)' : 'var(--ag-warning)' }">{{ statusLabel(c.status) }}</span>
          </div>
        </div>
        <div v-else class="px-5 py-8 text-center text-sm" style="color: var(--ag-text-muted);">Chưa có giao dịch nào.</div>
      </div>

      <!-- Recent referrals -->
      <div class="rounded-2xl border" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
        <div class="px-5 py-4" style="border-bottom: 1px solid var(--ag-border);">
          <h2 class="text-sm font-semibold" style="color: var(--ag-text-primary);">Lượt giới thiệu gần đây</h2>
        </div>
        <div v-if="recent_referrals.length" class="divide-y" style="border-color: var(--ag-border);">
          <div v-for="r in recent_referrals" :key="r.id" class="flex items-center justify-between px-5 py-3 text-sm">
            <span style="color: var(--ag-text-secondary);">{{ r.referred_user?.name || 'Người dùng #' + r.referred_user_id }}</span>
            <span style="color: var(--ag-text-muted);">{{ formatDate(r.created_at) }}</span>
            <span class="text-xs font-semibold px-2 py-0.5 rounded-full" :style="{ background: r.status === 'completed' ? 'rgba(72,103,48,0.1)' : 'rgba(217,119,6,0.1)', color: r.status === 'completed' ? 'var(--ag-primary-500)' : 'var(--ag-warning)' }">{{ statusLabel(r.status) }}</span>
          </div>
        </div>
        <div v-else class="px-5 py-8 text-center text-sm" style="color: var(--ag-text-muted);">Chưa có lượt giới thiệu nào.</div>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref } from 'vue';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { useToast } from 'primevue/usetoast';

const toast = useToast();
const copied = ref(false);

const props = defineProps({
  affiliate: { type: Object, default: null },
  total_earnings: { type: Number, default: 0 },
  pending_commission: { type: Number, default: 0 },
  paid_commission: { type: Number, default: 0 },
  total_referrals: { type: Number, default: 0 },
  conversion_rate: { type: Number, default: 0 },
  referral_link: { type: String, default: '' },
  recent_referrals: { type: Array, default: () => [] },
  recent_commissions: { type: Array, default: () => [] },
});

function formatPrice(price) {
  if (price == null) return '0₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
}

function formatDate(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString('vi-VN', { year: 'numeric', month: 'long', day: 'numeric' });
}

function statusLabel(status) {
  const map = { paid: 'Đã thanh toán', pending: 'Chờ xử lý', completed: 'Hoàn thành', cancelled: 'Đã hủy' };
  return map[status] || status;
}

async function copyLink() {
  if (!props.referral_link) return;
  try {
    await navigator.clipboard.writeText(props.referral_link);
    copied.value = true;
    toast.add({ severity: 'success', summary: 'Đã copy link giới thiệu', life: 2000 });
    setTimeout(() => { copied.value = false; }, 2000);
  } catch {
    toast.add({ severity: 'error', summary: 'Không thể copy', life: 2000 });
  }
}
</script>
