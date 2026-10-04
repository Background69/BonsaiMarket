<template>
  <MarketplaceLayout>
    <section class="max-w-[1320px] mx-auto px-5 py-8">
      <div class="flex items-center justify-between mb-6">
        <div>
          <Link :href="route('agriverse.shop.orders.show', contract.order?.id)"
            class="text-xs text-[var(--ag-primary-500)] font-semibold hover:underline mb-1 inline-flex items-center gap-1">
            <span class="material-symbols-outlined text-sm">arrow_back</span>
            Về đơn hàng
          </Link>
          <h1 class="text-xl font-bold text-[var(--ag-text-primary)] tracking-tight">Hợp đồng #{{ contract.uuid || contract.id }}</h1>
        </div>
        <div class="flex gap-2">
          <button @click="printContract"
            class="flex items-center gap-1.5 px-4 py-2 rounded-xl border border-[var(--ag-border)] text-sm font-semibold text-[var(--ag-text-secondary)] hover:bg-[var(--ag-bg)] transition-all">
            <span class="material-symbols-outlined text-base">print</span>
            In
          </button>
          <span class="inline-flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-full"
            :class="statusClass">
            <span class="w-1.5 h-1.5 rounded-full" :class="statusDotClass" />
            {{ statusLabel }}
          </span>
        </div>
      </div>

      <div class="grid grid-cols-12 gap-6">
        <div class="col-span-12 md:col-span-8 space-y-6">
          <div class="bg-white rounded-2xl border border-[var(--ag-border)] p-6">
            <div class="flex items-center gap-3 mb-5 pb-4 border-b border-[var(--ag-border)]/60">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-[var(--ag-primary-500)] to-[var(--ag-primary-600)] text-white flex items-center justify-center font-bold text-sm shadow-sm">
                <span class="material-symbols-outlined text-lg">contract</span>
              </div>
              <div>
                <div class="text-sm font-bold text-[var(--ag-text-primary)]">Thông tin hợp đồng</div>
                <div class="text-xs text-[var(--ag-text-muted)]">Chi tiết thỏa thuận mua bán</div>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4 text-sm">
              <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                <div class="text-[var(--ag-text-muted)] text-xs">Sản phẩm</div>
                <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ contract.order?.product?.name }}</div>
              </div>
              <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                <div class="text-[var(--ag-text-muted)] text-xs">Giá trị</div>
                <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ formatPrice(contract.total_amount || contract.order?.total_amount) }}</div>
              </div>
              <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                <div class="text-[var(--ag-text-muted)] text-xs">Người mua</div>
                <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ contract.order?.buyer?.name }}</div>
              </div>
              <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                <div class="text-[var(--ag-text-muted)] text-xs">Người bán</div>
                <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ contract.order?.seller?.name }}</div>
              </div>
              <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                <div class="text-[var(--ag-text-muted)] text-xs">Ngày tạo</div>
                <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ formatDate(contract.created_at) }}</div>
              </div>
              <div v-if="contract.signed_at" class="p-3 rounded-xl bg-[var(--ag-bg)]">
                <div class="text-[var(--ag-text-muted)] text-xs">Ngày ký</div>
                <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ formatDate(contract.signed_at) }}</div>
              </div>
            </div>
          </div>

          <div v-if="contract.terms" class="bg-white rounded-2xl border border-[var(--ag-border)] p-6">
            <h2 class="text-sm font-bold text-[var(--ag-text-primary)] mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-lg text-[var(--ag-primary-500)]">description</span>
              Điều khoản hợp đồng
            </h2>
            <div class="prose prose-sm max-w-none text-[var(--ag-text-secondary)]" v-html="contract.terms" />
          </div>

          <div v-if="statuses.length" class="bg-white rounded-2xl border border-[var(--ag-border)] p-6">
            <h2 class="text-sm font-bold text-[var(--ag-text-primary)] mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-lg text-[var(--ag-primary-500)]">timeline</span>
              Lịch sử trạng thái
            </h2>
            <div class="space-y-3">
              <div v-for="(s, i) in statuses" :key="i" class="flex gap-3">
                <div class="flex flex-col items-center">
                  <div class="w-3 h-3 rounded-full border-2"
                    :class="i === 0 ? 'bg-[var(--ag-primary-500)] border-[var(--ag-primary-500)]' : 'bg-white border-[var(--ag-neutral-300)]'">
                  </div>
                  <div v-if="i < statuses.length - 1" class="w-0.5 flex-1 bg-[var(--ag-border)] mt-1"></div>
                </div>
                <div class="pb-3">
                  <div class="text-sm font-semibold text-[var(--ag-text-primary)]">{{ s.label || s.status }}</div>
                  <div class="text-xs text-[var(--ag-text-muted)] mt-0.5">{{ formatDate(s.created_at) }}</div>
                  <div v-if="s.note" class="text-xs text-[var(--ag-text-secondary)] mt-0.5 bg-[var(--ag-bg)] px-2 py-1 rounded-lg">{{ s.note }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-span-12 md:col-span-4 space-y-4">
          <div class="bg-white rounded-2xl border border-[var(--ag-border)] p-5">
            <h2 class="text-sm font-bold text-[var(--ag-text-primary)] mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-lg text-[var(--ag-primary-500)]">signature</span>
              Chữ ký
            </h2>
            <div class="space-y-3 text-sm">
              <div class="flex items-center justify-between p-3 rounded-xl bg-[var(--ag-bg)]">
                <span class="text-[var(--ag-text-secondary)]">Người mua</span>
                <span class="font-semibold flex items-center gap-1"
                  :class="contract.buyer_signed ? 'text-[var(--ag-primary-500)]' : 'text-[var(--ag-text-muted)]'">
                  <span class="material-symbols-outlined text-base">{{ contract.buyer_signed ? 'check_circle' : 'hourglass_empty' }}</span>
                  {{ contract.buyer_signed ? 'Đã ký' : 'Chờ ký' }}
                </span>
              </div>
              <div class="flex items-center justify-between p-3 rounded-xl bg-[var(--ag-bg)]">
                <span class="text-[var(--ag-text-secondary)]">Người bán</span>
                <span class="font-semibold flex items-center gap-1"
                  :class="contract.seller_signed ? 'text-[var(--ag-primary-500)]' : 'text-[var(--ag-text-muted)]'">
                  <span class="material-symbols-outlined text-base">{{ contract.seller_signed ? 'check_circle' : 'hourglass_empty' }}</span>
                  {{ contract.seller_signed ? 'Đã ký' : 'Chờ ký' }}
                </span>
              </div>
            </div>
          </div>

          <div class="bg-white rounded-2xl border border-[var(--ag-border)] p-5">
            <h2 class="text-sm font-bold text-[var(--ag-text-primary)] mb-4">Đơn hàng liên quan</h2>
            <div class="space-y-2">
              <div class="flex items-center gap-3 p-3 rounded-xl bg-[var(--ag-bg)]">
                <div class="w-10 h-10 rounded-lg bg-white flex items-center justify-center shadow-sm text-lg font-bold text-[var(--ag-neutral-300)]">
                  {{ contract.order?.product?.name?.charAt(0)?.toUpperCase() || '?' }}
                </div>
                <div class="flex-1 min-w-0">
                  <div class="text-sm font-semibold text-[var(--ag-text-primary)] truncate">#{{ contract.order?.id }}</div>
                  <div class="text-xs text-[var(--ag-text-muted)]">{{ contract.order?.product?.name }}</div>
                </div>
              </div>
              <Link :href="route('agriverse.shop.orders.show', contract.order?.id)"
                class="flex items-center justify-center h-10 rounded-2xl border-2 border-[var(--ag-border)] text-sm font-semibold text-[var(--ag-text-secondary)] hover:bg-[var(--ag-bg)] transition-all gap-1">
                <span class="material-symbols-outlined text-base">open_in_new</span>
                Xem đơn hàng
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  </MarketplaceLayout>
</template>

<script setup>
import { computed } from 'vue';
import { Link } from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';

const props = defineProps({ contract: Object });

const statusClass = computed(() =>
  props.contract?.status === 'active' || props.contract?.status === 'signed'
    ? 'bg-[var(--ag-primary-500)]/10 text-[var(--ag-primary-500)]'
    : props.contract?.status === 'cancelled'
    ? 'bg-[var(--ag-danger)]/10 text-[var(--ag-danger)]'
    : 'bg-[var(--ag-warning)]/10 text-[var(--ag-warning)]'
);

const statusDotClass = computed(() =>
  props.contract?.status === 'active' || props.contract?.status === 'signed'
    ? 'bg-[var(--ag-primary-500)]'
    : props.contract?.status === 'cancelled'
    ? 'bg-[var(--ag-danger)]'
    : 'bg-[var(--ag-warning)]'
);

const statusLabel = computed(() => {
  const map = { active: 'Đang hiệu lực', signed: 'Đã ký', pending: 'Chờ xử lý', cancelled: 'Đã hủy', expired: 'Hết hạn' };
  return map[props.contract?.status] || props.contract?.status || 'Không xác định';
});

const statuses = computed(() => props.contract?.statuses || []);

function formatPrice(price) {
  if (price == null) return '—';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
}

function formatDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('vi-VN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function printContract() {
  window.print();
}
</script>
