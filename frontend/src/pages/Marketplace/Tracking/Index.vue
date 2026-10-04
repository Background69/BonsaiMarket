<template>
  <MarketplaceLayout>
    <section class="max-w-[1320px] mx-auto px-5 py-8">
      <div class="mb-8">
        <h1 class="text-xl font-bold text-[var(--ag-text-primary)] tracking-tight">Theo dõi vận chuyển</h1>
        <p class="text-sm text-[var(--ag-text-muted)] mt-1">Nhập mã đơn hàng hoặc mã vận đơn để tra cứu.</p>
      </div>

      <div class="bg-white rounded-2xl border border-[var(--ag-border)] p-6 mb-8">
        <form @submit.prevent="lookup" class="flex gap-3">
          <div class="flex-1 relative">
            <input v-model="code" placeholder="Nhập mã đơn hàng hoặc mã vận đơn..."
              class="w-full h-12 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8 transition-all" />
            <div v-if="recentOrders.length && !code" class="absolute top-full left-0 right-0 mt-1 bg-white rounded-xl border border-[var(--ag-border)] shadow-lg z-10 overflow-hidden">
              <div class="px-3 py-2 text-xs font-semibold text-[var(--ag-text-muted)] uppercase tracking-wider">Đơn hàng gần đây</div>
              <button v-for="o in recentOrders" :key="o.id" type="button" @click="selectRecentOrder(o)"
                class="w-full flex items-center gap-3 px-3 py-2.5 text-sm text-left hover:bg-[var(--ag-bg)] transition-all">
                <span class="material-symbols-outlined text-base text-[var(--ag-text-muted)]">receipt</span>
                <span class="font-semibold text-[var(--ag-text-primary)]">#{{ o.id }}</span>
                <span class="text-[var(--ag-text-muted)] truncate flex-1">{{ o.product?.name }}</span>
                <span class="text-xs font-semibold px-2 py-0.5 rounded-full" :class="statusClass(o.status)">{{ statusLabel(o.status) }}</span>
              </button>
            </div>
          </div>
          <button type="submit" :disabled="loading || !code"
            class="h-12 px-6 rounded-2xl bg-[var(--ag-primary-500)] text-white text-sm font-semibold hover:bg-[var(--ag-primary-600)] transition-all disabled:opacity-50 flex items-center gap-2">
            <span v-if="loading" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            <span class="material-symbols-outlined text-lg">search</span>
            Tra cứu
          </button>
        </form>
      </div>

      <div v-if="error" class="bg-white rounded-2xl border border-[var(--ag-border)] p-16 text-center">
        <div class="w-20 h-20 rounded-2xl bg-[var(--ag-danger)]/10 flex items-center justify-center mx-auto">
          <span class="material-symbols-outlined text-4xl text-[var(--ag-danger)]">search_off</span>
        </div>
        <div class="text-base font-bold text-[var(--ag-text-secondary)] mt-5">{{ error }}</div>
      </div>

      <div v-if="order" class="grid grid-cols-12 gap-6">
        <div class="col-span-12 md:col-span-8 space-y-6">

          <!-- Order info -->
          <div class="bg-white rounded-2xl border border-[var(--ag-border)] p-5">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-base font-bold text-[var(--ag-text-primary)]">Đơn hàng #{{ order.id }}</h2>
              <span class="text-xs px-3 py-1 rounded-full font-semibold" :class="statusClass(order.status)">
                {{ statusLabel(order.status) }}
              </span>
            </div>
            <div class="flex items-start gap-4 p-4 bg-[var(--ag-bg)] rounded-2xl">
              <div class="w-14 h-14 rounded-xl bg-white flex items-center justify-center shrink-0 shadow-sm">
                <span class="text-xl font-bold text-[var(--ag-neutral-300)]">{{ order.product?.name?.charAt(0)?.toUpperCase() }}</span>
              </div>
              <div>
                <div class="text-sm font-bold text-[var(--ag-text-primary)]">{{ order.product?.name }}</div>
                <div class="flex items-center gap-4 text-sm text-[var(--ag-text-secondary)] mt-1">
                  <span>Số lượng: <strong class="text-[var(--ag-text-primary)]">x{{ order.quantity }}</strong></span>
                  <span>Đơn giá: <strong class="text-[var(--ag-text-primary)]">{{ formatPrice(order.unit_price) }}₫</strong></span>
                </div>
              </div>
            </div>
          </div>

          <!-- Shipping info -->
          <div class="bg-white rounded-2xl border border-[var(--ag-border)] p-5">
            <h2 class="text-sm font-bold text-[var(--ag-text-primary)] mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-lg text-[var(--ag-primary-500)]">local_shipping</span>
              Thông tin vận chuyển
            </h2>
            <div class="grid grid-cols-2 gap-3 text-sm">
              <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                <span class="text-[var(--ag-text-muted)]">Đơn vị vận chuyển</span>
                <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ order.shipping_method || '—' }}</div>
              </div>
              <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                <span class="text-[var(--ag-text-muted)]">Phí vận chuyển</span>
                <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ formatPrice(order.shipping_fee) }}₫</div>
              </div>
              <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                <span class="text-[var(--ag-text-muted)]">Mã vận đơn</span>
                <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ order.tracking_number || '—' }}</div>
              </div>
              <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                <span class="text-[var(--ag-text-muted)]">Dự kiến giao</span>
                <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ order.estimated_delivery || '—' }}</div>
              </div>
              <div class="col-span-2 p-3 rounded-xl bg-[var(--ag-bg)]">
                <span class="text-[var(--ag-text-muted)]">Địa chỉ giao</span>
                <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ order.shipping_address }}</div>
              </div>
            </div>
            <a v-if="order.tracking_url" :href="order.tracking_url" target="_blank"
              class="mt-4 flex items-center justify-center gap-2 h-11 rounded-2xl bg-[var(--ag-primary-500)]/10 text-[var(--ag-primary-500)] text-sm font-semibold hover:bg-[var(--ag-primary-500)]/20 transition-all">
              <span class="material-symbols-outlined text-lg">open_in_new</span>
              Theo dõi trên GHN
            </a>
          </div>

          <!-- Status timeline -->
          <div class="bg-white rounded-2xl border border-[var(--ag-border)] p-5">
            <h2 class="text-sm font-bold text-[var(--ag-text-primary)] mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-lg text-[var(--ag-primary-500)]">timeline</span>
              Lịch sử trạng thái
            </h2>
            <div class="space-y-3">
              <div v-for="(s, i) in statuses" :key="s.id || i" class="flex gap-3">
                <div class="flex flex-col items-center">
                  <div class="w-3 h-3 rounded-full border-2"
                    :class="i === 0 ? 'bg-[var(--ag-primary-500)] border-[var(--ag-primary-500)]' : 'bg-white border-[var(--ag-neutral-300)]'">
                  </div>
                  <div v-if="i < statuses.length - 1" class="w-0.5 flex-1 bg-[var(--ag-border)] mt-1"></div>
                </div>
                <div class="pb-3">
                  <div class="text-sm font-semibold text-[var(--ag-text-primary)]">{{ statusLabel(s.status) }}</div>
                  <div class="text-xs text-[var(--ag-text-muted)] mt-0.5">{{ s.created_at }}</div>
                  <div v-if="s.note" class="text-xs text-[var(--ag-text-secondary)] mt-0.5 bg-[var(--ag-bg)] px-2 py-1 rounded-lg">{{ s.note }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- GHN tracking detail -->
          <div v-if="ghnTracking.order_code" class="bg-white rounded-2xl border border-[var(--ag-border)] p-5">
            <h2 class="text-sm font-bold text-[var(--ag-text-primary)] mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-lg text-[var(--ag-primary-500)]">package_2</span>
              Chi tiết vận chuyển GHN
            </h2>
            <div class="space-y-3">
              <div class="grid grid-cols-2 gap-3 text-sm">
                <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                  <span class="text-[var(--ag-text-muted)]">Mã vận đơn GHN</span>
                  <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ ghnTracking.order_code }}</div>
                </div>
                <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                  <span class="text-[var(--ag-text-muted)]">Trạng thái GHN</span>
                  <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ ghnTracking.status || '—' }}</div>
                </div>
                <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                  <span class="text-[var(--ag-text-muted)]">Người gửi</span>
                  <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ ghnTracking.from_name || '—' }}</div>
                </div>
                <div class="p-3 rounded-xl bg-[var(--ag-bg)]">
                  <span class="text-[var(--ag-text-muted)]">Người nhận</span>
                  <div class="font-semibold text-[var(--ag-text-primary)] mt-0.5">{{ ghnTracking.to_name || '—' }}</div>
                </div>
              </div>

              <div v-if="ghnTracking.log?.length" class="mt-4">
                <h3 class="text-xs font-semibold text-[var(--ag-text-muted)] uppercase tracking-wider mb-3">Lịch trình vận chuyển</h3>
                <div class="space-y-3">
                  <div v-for="(log, i) in ghnTracking.log" :key="i" class="flex gap-3">
                    <div class="flex flex-col items-center">
                      <div class="w-2.5 h-2.5 rounded-full"
                        :class="i === 0 ? 'bg-[var(--ag-primary-500)]' : 'bg-[var(--ag-neutral-300)]'">
                      </div>
                      <div v-if="i < ghnTracking.log.length - 1" class="w-0.5 flex-1 bg-[var(--ag-border)] mt-1"></div>
                    </div>
                    <div class="pb-2">
                      <div class="text-xs font-medium text-[var(--ag-text-primary)]">{{ log.status }}</div>
                      <div class="text-[11px] text-[var(--ag-text-muted)]">{{ log.updated_date }}</div>
                      <div v-if="log.location" class="text-[11px] text-[var(--ag-text-secondary)]">{{ log.location }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="ghnTracking.expected_delivery_time" class="mt-3 p-3 rounded-xl bg-[var(--ag-primary-500)]/8">
                <div class="text-xs text-[var(--ag-text-muted)]">Thời gian giao dự kiến (GHN)</div>
                <div class="text-sm font-semibold text-[var(--ag-primary-500)]">{{ ghnTracking.expected_delivery_time }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-span-12 md:col-span-4">
          <div class="bg-white rounded-2xl border border-[var(--ag-border)] p-5 sticky top-24 space-y-4">
            <h2 class="text-base font-bold text-[var(--ag-text-primary)]">Chi tiết thanh toán</h2>
            <div class="space-y-2 text-sm">
              <div class="flex justify-between text-[var(--ag-text-secondary)]">
                <span>Tạm tính</span>
                <span>{{ formatPrice(order.total_price) }}₫</span>
              </div>
              <div v-if="order.discount_amount > 0" class="flex justify-between text-[var(--ag-primary-500)] font-medium">
                <span>Giảm giá</span>
                <span>-{{ formatPrice(order.discount_amount) }}₫</span>
              </div>
              <div class="flex justify-between text-[var(--ag-text-secondary)]">
                <span>Phí giao dịch</span>
                <span>{{ formatPrice(order.commission_fee) }}₫</span>
              </div>
              <div class="h-px bg-[var(--ag-border)]/60 my-2"></div>
              <div class="flex justify-between text-base">
                <span class="font-bold text-[var(--ag-text-primary)]">Thành tiền</span>
                <span class="font-black text-[var(--ag-danger)]">{{ formatPrice(order.total_amount) }}₫</span>
              </div>
            </div>

            <Link :href="route('agriverse.shop.orders.show', order.id)"
              class="block w-full h-10 rounded-2xl border-2 border-[var(--ag-border)] text-[var(--ag-text-secondary)] text-sm font-semibold leading-10 text-center hover:bg-[var(--ag-bg)] transition-all">
              Xem chi tiết đơn hàng
            </Link>
          </div>
        </div>
      </div>
    </section>
  </MarketplaceLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link } from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { formatPrice, statusLabel, statusClass } from '@agriverse/utils';
import { useToast } from 'primevue/usetoast';

const toast = useToast();
const code = ref('');
const loading = ref(false);
const error = ref('');
const order = ref(null);
const ghnTracking = ref({});
const statuses = ref([]);

const props = defineProps({
  recentOrders: { type: Array, default: () => [] },
});

function selectRecentOrder(o) {
  code.value = String(o.id);
  lookup();
}

async function lookup() {
  if (!code.value) return;
  loading.value = true;
  error.value = '';
  order.value = null;
  ghnTracking.value = {};
  statuses.value = [];

  try {
    const { data } = await window.axios.post(route('agriverse.api.tracking.lookup'), { code: code.value });
    order.value = data.order;
    statuses.value = data.order.statuses || [];
    ghnTracking.value = data.ghn_tracking || {};
  } catch (e) {
    error.value = e.response?.data?.error || 'Không tìm thấy đơn hàng.';
    toast.add({ severity: 'error', summary: error.value, life: 3000 });
  } finally {
    loading.value = false;
  }
}
</script>
