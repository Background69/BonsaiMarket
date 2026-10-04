<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Báo cáo</h1>
    </div>

    <div class="flex gap-1 mb-4">
      <button v-for="t in tabs" :key="t.key" @click="switchTab(t.key)"
        class="h-8 px-3 rounded-lg text-xs font-bold transition-all"
        :class="tab === t.key ? 'bg-emerald-600 text-white' : 'bg-white border border-stone-200 text-stone-600 hover:bg-stone-50'">
        {{ t.label }}
      </button>
    </div>

    <!-- Overview Tab -->
    <div v-if="tab === 'overview'">
      <div class="grid grid-cols-12 gap-4 mb-4">
        <div v-for="card in statCards" :key="card.label" class="col-span-6 md:col-span-3">
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <div class="text-xs text-stone-500 font-medium">{{ card.label }}</div>
            <div class="text-xl font-bold text-stone-800 mt-1">{{ card.value }}</div>
          </div>
        </div>
      </div>
      <div class="grid grid-cols-12 gap-4">
        <div class="col-span-12 md:col-span-6">
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <h3 class="text-xs font-bold text-stone-700 mb-3">Sản phẩm bán chạy</h3>
            <table class="w-full text-xs">
              <thead><tr class="text-stone-500 text-left"><th class="pb-2 font-medium">Sản phẩm</th><th class="pb-2 font-medium">Đã bán</th></tr></thead>
              <tbody>
                <tr v-for="p in topProducts" :key="p.id" class="border-t border-stone-100">
                  <td class="py-2 text-stone-800">{{ p.name }}</td>
                  <td class="py-2 text-stone-600">{{ p.total_sold || 0 }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="col-span-12 md:col-span-6">
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <h3 class="text-xs font-bold text-stone-700 mb-3">Cửa hàng hàng đầu</h3>
            <table class="w-full text-xs">
              <thead><tr class="text-stone-500 text-left"><th class="pb-2 font-medium">Cửa hàng</th><th class="pb-2 font-medium">SP</th><th class="pb-2 font-medium">Doanh thu</th></tr></thead>
              <tbody>
                <tr v-for="s in topStores" :key="s.id" class="border-t border-stone-100">
                  <td class="py-2 text-stone-800">{{ s.name }}</td>
                  <td class="py-2 text-stone-600">{{ s.products_count || 0 }}</td>
                  <td class="py-2 text-stone-600">{{ formatPrice(s.total_revenue || 0) }}₫</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Revenue Tab -->
    <div v-if="tab === 'revenue'">
      <div class="grid grid-cols-12 gap-4 mb-4">
        <div class="col-span-12 md:col-span-4">
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <div class="text-xs text-stone-500 font-medium">Tổng doanh thu</div>
            <div class="text-xl font-bold text-emerald-600 mt-1">{{ formatPrice(totalRevenue) }}₫</div>
          </div>
        </div>
        <div class="col-span-12 md:col-span-4">
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <div class="text-xs text-stone-500 font-medium">Hoa hồng kiếm được</div>
            <div class="text-xl font-bold text-amber-600 mt-1">{{ formatPrice(totalCommission) }}₫</div>
          </div>
        </div>
        <div class="col-span-12 md:col-span-4">
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <div class="text-xs text-stone-500 font-medium">Thanh toán người bán</div>
            <div class="text-xl font-bold text-blue-600 mt-1">{{ formatPrice(sellerPayouts) }}₫</div>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-12 gap-4 mb-4">
        <div class="col-span-12 md:col-span-8">
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-xs font-bold text-stone-700">Doanh thu theo tháng</h3>
              <button @click="exportRevenue" class="h-7 px-3 rounded-lg bg-stone-100 text-stone-600 text-[10px] font-bold hover:bg-stone-200 transition-all">Xuất Excel</button>
            </div>
            <div class="flex items-end gap-2 h-48">
              <div v-for="(m, i) in monthlyRevenue" :key="i" class="flex-1 flex flex-col items-center gap-1">
                <div class="w-full rounded-t-md transition-all duration-300"
                  :style="{ height: Math.max((m.total / maxMonthly) * 100, 4) + '%', background: 'var(--ag-primary-500)' }"
                  :title="formatPrice(m.total) + '₫'">
                </div>
                <span class="text-[8px] text-stone-400 -rotate-45 origin-left whitespace-nowrap">{{ m.month }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="col-span-12 md:col-span-4">
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <h3 class="text-xs font-bold text-stone-700 mb-3">Người bán hàng đầu</h3>
            <table class="w-full text-xs">
              <thead><tr class="text-stone-500 text-left"><th class="pb-2 font-medium">Người bán</th><th class="pb-2 font-medium">Cửa hàng</th><th class="pb-2 font-medium">Doanh thu</th></tr></thead>
              <tbody>
                <tr v-for="s in topSellers" :key="s.id" class="border-t border-stone-100">
                  <td class="py-2 text-stone-800">{{ s.name }}</td>
                  <td class="py-2 text-stone-500">{{ s.stores_count || 0 }}</td>
                  <td class="py-2 text-stone-600">{{ formatPrice(s.seller_revenue || 0) }}₫</td>
                </tr>
                <tr v-if="!topSellers.length"><td colspan="3" class="py-4 text-center text-stone-400">Chưa có dữ liệu</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-stone-200 p-4">
        <h3 class="text-xs font-bold text-stone-700 mb-3">Chi tiết doanh thu theo tháng</h3>
        <table class="w-full text-xs">
          <thead><tr class="text-stone-500 text-left"><th class="pb-2 font-medium">Tháng</th><th class="pb-2 font-medium">Doanh thu</th><th class="pb-2 font-medium">Hoa hồng</th><th class="pb-2 font-medium">Thanh toán</th></tr></thead>
          <tbody>
            <tr v-for="m in monthlyRevenue" :key="m.month" class="border-t border-stone-100">
              <td class="py-2 text-stone-800 font-medium">{{ m.month }}</td>
              <td class="py-2 text-emerald-600">{{ formatPrice(m.total) }}₫</td>
              <td class="py-2 text-amber-600">{{ formatPrice(m.commission) }}₫</td>
              <td class="py-2 text-blue-600">{{ formatPrice(m.payout) }}₫</td>
            </tr>
            <tr v-if="!monthlyRevenue.length"><td colspan="4" class="py-4 text-center text-stone-400">Chưa có dữ liệu</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed } from 'vue';
import { router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
import { formatPrice } from '@agriverse/utils';

const props = defineProps({
  storesCount: Number, productsCount: Number, ordersCount: Number, usersCount: Number,
  totalRevenue: Number, totalCommission: Number, pendingOrders: Number,
  revenueByMonth: Array, topProducts: Array, topStores: Array,
  monthlyRevenue: Array, sellerPayouts: Number, topSellers: Array, tab: String,
});

const tabs = [
  { key: 'overview', label: 'Tổng quan' },
  { key: 'revenue', label: 'Doanh thu' },
];

const maxMonthly = computed(() => Math.max(...(props.monthlyRevenue || []).map(m => Number(m.total)), 1));

const statCards = computed(() => [
  { label: 'Doanh thu', value: formatPrice(props.totalRevenue || 0) + '₫' },
  { label: 'Tổng SP', value: props.productsCount },
  { label: 'Đơn hàng', value: props.ordersCount },
  { label: 'Đơn chờ', value: props.pendingOrders },
  { label: 'Cửa hàng', value: props.storesCount },
  { label: 'Người dùng', value: props.usersCount },
  { label: 'Hoa hồng', value: formatPrice(props.totalCommission || 0) + '₫' },
]);

function switchTab(key) {
  router.get(route('admin.agriverse.reports.index'), { tab: key }, { preserveState: true });
}

function exportRevenue() {
  // Placeholder — no actual export
  alert('Chức năng đang phát triển');
}
</script>
