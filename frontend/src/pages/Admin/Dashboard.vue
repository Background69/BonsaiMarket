<template>
  <AdminLayout>
    <div class="grid grid-cols-12 gap-4 mb-4">
      <div v-for="card in statCards" :key="card.label" class="col-span-6 md:col-span-3">
        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <div class="flex items-center justify-between">
            <span class="text-xs text-stone-500 font-medium">{{ card.label }}</span>
            <span class="material-symbols-outlined text-lg" :class="card.iconClass">{{ card.icon }}</span>
          </div>
          <div class="text-2xl font-bold text-stone-800 mt-1">{{ card.value }}</div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-12 gap-4">
      <div class="col-span-12 md:col-span-8">
        <div class="bg-white rounded-xl border border-stone-200 p-4 mb-4">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-xs font-bold text-stone-700">Doanh thu 7 ngày qua</h3>
            <span class="text-xs text-stone-400">VNĐ</span>
          </div>
          <div class="flex items-end gap-2 h-32">
            <div v-for="(val, i) in chartData" :key="i" class="flex-1 flex flex-col items-center gap-1">
              <div class="w-full h-full min-h-[4px] rounded-t-md bg-[var(--ag-primary-500)] transition-transform duration-300 origin-bottom" :style="{ transform: 'scaleY(' + Math.max((val / maxChart), 0.04) + ')' }" :title="formatPrice(val) + '₫'"></div>
              <span class="text-[9px] text-stone-400">{{ chartLabels[i] }}</span>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-xs font-bold text-stone-700">Đơn hàng gần đây</h3>
            <Link :href="route('admin.agriverse.orders.index')" class="text-[10px] text-emerald-600 hover:underline">Xem tất cả</Link>
          </div>
          <table class="w-full text-xs">
            <thead><tr class="text-stone-500 text-left"><th class="pb-2 font-medium">SP</th><th class="pb-2 font-medium">Người mua</th><th class="pb-2 font-medium">Tổng</th><th class="pb-2 font-medium">Trạng thái</th></tr></thead>
            <tbody>
              <tr v-for="o in recentOrders" :key="o.id" class="border-t border-stone-100">
                <td class="py-2 text-stone-800">{{ o.product?.name || '—' }}</td>
                <td class="py-2 text-stone-500">{{ o.buyer?.name || '—' }}</td>
                <td class="py-2 text-stone-700">{{ formatPrice(o.total_amount) }}₫</td>
                <td class="py-2"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="statusClass(o.status)">{{ statusLabel(o.status) }}</span></td>
              </tr>
              <tr v-if="!recentOrders.length"><td colspan="4" class="py-4 text-center text-stone-400">Chưa có đơn hàng</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="col-span-12 md:col-span-4 space-y-4">
        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <h3 class="text-xs font-bold text-stone-700 mb-3">Sản phẩm bán chạy</h3>
          <div v-for="p in topProducts" :key="p.id" class="flex items-center justify-between py-2 border-t border-stone-100 first:border-t-0">
            <span class="text-xs text-stone-800 truncate flex-1">{{ p.name }}</span>
            <span class="text-[10px] text-stone-500 ml-2">Đã bán: {{ p.total_sold || 0 }}</span>
          </div>
          <div v-if="!topProducts.length" class="text-xs text-stone-400 py-4 text-center">Chưa có dữ liệu</div>
        </div>

        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <h3 class="text-xs font-bold text-stone-700 mb-3">Đơn hàng theo trạng thái</h3>
          <div v-for="(total, status) in ordersByStatus" :key="status" class="flex items-center justify-between py-1.5 border-t border-stone-100 first:border-t-0">
            <span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="statusClass(status)">{{ statusLabel(status) }}</span>
            <span class="text-xs font-bold text-stone-700">{{ total }}</span>
          </div>
          <div v-if="!Object.keys(ordersByStatus).length" class="text-xs text-stone-400 py-4 text-center">Chưa có dữ liệu</div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed } from 'vue';
import { Link } from '@inertiajs/vue3';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
import { formatPrice, statusLabel, statusClass } from '@agriverse/utils';

const props = defineProps({
  storesCount: Number, productsCount: Number, usersCount: Number, ordersCount: Number,
  revenueThisMonth: Number, recentOrders: Array, topProducts: Array,
  chartLabels: Array, chartData: Array, ordersByStatus: Object,
});

const maxChart = computed(() => Math.max(...props.chartData, 1));

const statCards = computed(() => [
  { label: 'Người dùng', value: props.usersCount, icon: 'people', iconClass: 'text-amber-600' },
  { label: 'Sản phẩm', value: props.productsCount, icon: 'inventory_2', iconClass: 'text-blue-600' },
  { label: 'Đơn hàng', value: props.ordersCount, icon: 'receipt_long', iconClass: 'text-violet-600' },
  { label: 'Doanh thu tháng', value: formatPrice(props.revenueThisMonth) + '₫', icon: 'payments', iconClass: 'text-emerald-600' },
]);
</script>
