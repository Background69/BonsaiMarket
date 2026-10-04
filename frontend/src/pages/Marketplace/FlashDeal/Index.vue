<template>
  <MarketplaceLayout>
    <section class="max-w-[1320px] mx-auto px-5 py-8">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-lg sm:text-xl font-bold text-[var(--ag-text-primary)] tracking-tight">Flash Sale</h1>
          <p class="text-sm text-[var(--ag-text-secondary)] mt-1">Khuyến mãi giới hạn, ai đến trước được khóa đơn trước (realtime)</p>
        </div>
        <div class="flex items-center gap-3">
          <span class="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full"
            :class="connected ? 'text-[#15803d] bg-[#16a34a]/10' : 'text-[var(--ag-text-secondary)] bg-[var(--ag-bg)]'">
            <span class="w-2 h-2 rounded-full animate-pulse" :class="connected ? 'bg-[#16a34a]' : 'bg-[var(--ag-neutral-300)]'"></span>
            {{ connected ? 'Đang kết nối' : 'Đang kết nối lại…' }}
          </span>
          <button v-if="canCreate && connected" @click="openCreate"
            class="h-10 px-4 rounded-xl bg-[var(--ag-primary-500)] text-white text-xs font-semibold hover:bg-[var(--ag-primary-600)] transition-all flex items-center gap-1.5">
            <span class="material-symbols-outlined text-sm">bolt</span> Tạo Flash Deal
          </button>
        </div>
      </div>

      <template v-if="deals.length">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          <div v-for="deal in deals" :key="deal.dealId"
            class="bg-white rounded-2xl border border-[var(--ag-border)] overflow-hidden hover:border-[var(--ag-primary-500)]/25 hover:shadow-sm transition-all duration-300 flex flex-col">
            <!-- image -->
            <div class="relative aspect-[4/3] bg-[var(--ag-bg)] overflow-hidden">
              <img v-if="deal.productImage" :src="deal.productImage" :alt="deal.productName" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full flex items-center justify-center">
                <span class="material-symbols-outlined text-5xl text-[var(--ag-neutral-300)]">local_florist</span>
              </div>
              <span class="absolute top-3 left-3 px-2 py-1 rounded-full bg-[var(--ag-danger)] text-white text-[10px] font-bold uppercase tracking-wide shadow">
                -{{ discountPct(deal) }}%
              </span>
              <span v-if="isLocked(deal)" class="absolute top-3 right-3 px-2 py-1 rounded-full bg-[var(--ag-text-primary)]/70 text-white text-[10px] font-bold flex items-center gap-1">
                <span class="material-symbols-outlined text-xs">lock</span> Đã khóa
              </span>
            </div>

            <div class="p-4 flex flex-col flex-1">
              <h3 class="text-sm font-bold text-[var(--ag-text-primary)] leading-snug line-clamp-2">{{ deal.productName || 'Sản phẩm' }}</h3>

              <div class="flex items-baseline gap-2 mt-2">
                <span class="text-lg font-extrabold text-[var(--ag-danger)]">{{ formatPrice(deal.price) }}₫</span>
                <span class="text-xs text-[var(--ag-text-secondary)] line-through">{{ formatPrice(deal.originalPrice) }}₫</span>
              </div>

              <!-- countdown -->
              <div class="mt-3 flex items-center gap-1" v-if="remaining(deal) != null">
                <span class="text-[10px] font-semibold text-[var(--ag-text-secondary)] mr-1">Kết thúc trong</span>
                <span v-for="(u, i) in countdownParts(deal)" :key="i"
                  class="px-1.5 py-0.5 rounded bg-[var(--ag-text-primary)] text-white text-[10px] font-bold min-w-[24px] text-center">{{ u }}</span>
              </div>
              <div v-else class="mt-3 text-[11px] font-semibold text-[var(--ag-text-secondary)]">Đã hết hạn</div>

              <button v-if="!isLocked(deal) && remaining(deal)"
                @click="claimDeal(deal)"
                :disabled="deal.buyerId === String(userId)"
                class="mt-4 h-10 rounded-xl bg-[var(--ag-primary-500)] text-white text-sm font-bold hover:bg-[var(--ag-primary-600)] active:scale-95 transition-all flex items-center justify-center gap-1.5">
                <span class="material-symbols-outlined text-sm">flash_on</span> Mua nhanh & Khóa đơn
              </button>
              <div v-else class="mt-4 h-10 rounded-xl bg-[var(--ag-bg)] text-[var(--ag-text-secondary)] text-sm font-semibold flex items-center justify-center">
                {{ deal.buyerId === String(userId) ? 'Bạn đã khóa đơn' : 'Đơn đã được khóa' }}
              </div>
              <p v-if="deal.buyerId" class="mt-2 text-[10px] text-[var(--ag-text-secondary)] text-center">Khóa bởi người mua #{{ deal.buyerId }}</p>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="bg-white rounded-2xl border border-[var(--ag-border)] p-16 text-center">
        <div class="w-20 h-20 rounded-2xl bg-[var(--ag-bg)] flex items-center justify-center mx-auto mb-4">
          <span class="material-symbols-outlined text-4xl text-[var(--ag-neutral-300)]">flash_on</span>
        </div>
        <p class="text-sm text-[var(--ag-text-secondary)]">Chưa có flash deal nào đang diễn ra. ${canCreate ? 'Hãy tạo một deal để bắt đầu.' : 'Quay lại sau nhé!'}</p>
        <button v-if="canCreate && connected" @click="openCreate"
          class="mt-5 h-10 px-5 rounded-xl bg-[var(--ag-primary-500)] text-white text-sm font-semibold hover:bg-[var(--ag-primary-600)] transition-all">
          Tạo Flash Deal đầu tiên
        </button>
      </div>

      <!-- event toasts -->
      <div v-if="connected" class="fixed bottom-5 right-5 z-50 space-y-2 w-80">
        <div v-for="(ev, i) in events.slice(0, 3)" :key="i" class="bg-[var(--ag-text-primary)] text-white rounded-xl px-4 py-3 text-xs shadow-lg animate-[fadeUp_.3s_ease]">
          {{ ev.text }}
        </div>
      </div>

      <!-- Create deal modal -->
      <div v-if="showCreate" class="fixed inset-0 z-50 flex items-center justify-center bg-[var(--ag-text-primary)]/30 backdrop-blur-sm p-4" @click.self="showCreate = false">
        <div class="bg-white rounded-3xl p-6 w-full max-w-md">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-base font-bold text-[var(--ag-text-primary)]">Tạo Flash Deal mới</h3>
            <button @click="showCreate = false" class="w-8 h-8 rounded-lg hover:bg-[var(--ag-bg)] flex items-center justify-center text-[var(--ag-text-secondary)]">
              <span class="material-symbols-outlined text-lg">close</span>
            </button>
          </div>
          <form @submit.prevent="submitCreate" class="space-y-3">
            <div>
              <label class="text-xs font-semibold text-[var(--ag-text-secondary)]">Tên sản phẩm</label>
              <input v-model="form.productName" required placeholder="VD: Sen đá Thạch Ngọc 5cm"
                class="mt-1 w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8" />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-xs font-semibold text-[var(--ag-text-secondary)]">Giá bán (₫)</label>
                <input v-model.number="form.price" required type="number" min="0"
                  class="mt-1 w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40" />
              </div>
              <div>
                <label class="text-xs font-semibold text-[var(--ag-text-secondary)]">Giá gốc (₫)</label>
                <input v-model.number="form.originalPrice" required type="number" min="0"
                  class="mt-1 w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40" />
              </div>
              <div>
                <label class="text-xs font-semibold text-[var(--ag-text-secondary)]">Số lượng</label>
                <input v-model.number="form.quantity" required type="number" min="1"
                  class="mt-1 w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40" />
              </div>
              <div>
                <label class="text-xs font-semibold text-[var(--ag-text-secondary)]">Thời gian (phút)</label>
                <input v-model.number="form.duration" required type="number" min="1"
                  class="mt-1 w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40" />
              </div>
            </div>
            <div>
              <label class="text-xs font-semibold text-[var(--ag-text-secondary)]">Link ảnh (tuỳ chọn)</label>
              <input v-model="form.productImage" placeholder="https://…"
                class="mt-1 w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40" />
            </div>
            <button type="submit" :disabled="!connected"
              class="w-full h-11 rounded-2xl bg-[var(--ag-primary-500)] text-white text-sm font-semibold hover:bg-[var(--ag-primary-600)] transition-all disabled:opacity-50 disabled:cursor-not-allowed">
              Tạo deal & phát sóng
            </button>
          </form>
        </div>
      </div>
    </section>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { useMarketSocket } from '@agriverse/Composables/useMarketSocket';
import { useAuth } from '@agriverse/Composables/useAuth';

const { user, isSeller, isAdmin, isBuyer } = useAuth();
const userId = computed(() => user.value?.id ?? '');
const canCreate = computed(() => isSeller.value || isAdmin.value);

const { connected, deals, events, connect, disconnect, send, setUserId } = useMarketSocket();
const showCreate = ref(false);
const form = ref({ productName: '', productImage: '', price: 0, originalPrice: 0, quantity: 1, duration: 60 });
const now = ref(Date.now());

let ticker = null;

onMounted(() => {
  setUserId(user.value?.id);
  connect();
  ticker = setInterval(() => { now.value = Date.now(); }, 1000);
});
onBeforeUnmount(() => { clearInterval(ticker); disconnect(); });

const remaining = (deal) => {
  const ends = Number(deal.endsAt);
  if (!ends) return null;
  const d = ends - now.value;
  return d > 0 ? d : null;
};

const countdownParts = (deal) => {
  const ms = remaining(deal);
  if (ms == null) return [];
  const s = Math.floor(ms / 1000);
  const h = String(Math.floor(s / 3600)).padStart(2, '0');
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0');
  const sec = String(s % 60).padStart(2, '0');
  return [h, m, sec];
};

const discountPct = (deal) => {
  if (!deal.originalPrice) return 0;
  return Math.round((1 - deal.price / deal.originalPrice) * 100);
};

const isLocked = (deal) => deal.status === 'locked' || !!deal.buyerId || !!deal.lockedBy;

const claimDeal = (deal) => {
  if (!userId.value) return;
  send({
    eventType: 'FLASH_DEAL',
    action: 'CLAIM',
    from: String(userId.value),
    fromName: user.value?.name || '',
    to: '',
    payload: JSON.stringify({ dealId: deal.dealId }),
  });
};

const openCreate = () => { showCreate.value = true; };

const submitCreate = () => {
  if (!connected.value) return;
  send({
    eventType: 'FLASH_DEAL',
    action: 'CREATE',
    from: String(userId.value),
    fromName: user.value?.name || '',
    to: '',
    payload: JSON.stringify({
      productName: form.value.productName,
      productImage: form.value.productImage,
      price: Number(form.value.price),
      originalPrice: Number(form.value.originalPrice),
      quantity: Number(form.value.quantity),
      duration: Number(form.value.duration * 60), // server tính theo giây
    }),
  });
  showCreate.value = false;
  form.value = { productName: '', productImage: '', price: 0, originalPrice: 0, quantity: 1, duration: 60 };
};

const formatPrice = (n) => Number(n || 0).toLocaleString('vi-VN');
</script>