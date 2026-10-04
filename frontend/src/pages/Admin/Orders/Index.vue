<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Đơn hàng</h1>
    </div>
    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <div class="p-3 border-b border-stone-100 flex gap-2">
        <input v-model="search" @input="filter" placeholder="Tìm kiếm..." class="h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500 w-52">
        <select v-model="statusFilter" @change="filter" class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
          <option value="">Tất cả</option>
          <option value="pending">Chờ xác nhận</option>
          <option value="confirmed">Đã xác nhận</option>
          <option value="shipping">Đang giao</option>
          <option value="delivered">Đã giao</option>
          <option value="completed">Hoàn thành</option>
          <option value="cancelled">Đã hủy</option>
        </select>
      </div>
      <table class="w-full text-xs">
        <thead><tr class="bg-stone-50 text-stone-500 text-left"><th class="p-3 font-medium">Mã ĐH</th><th class="p-3 font-medium">Sản phẩm</th><th class="p-3 font-medium">Người mua</th><th class="p-3 font-medium">Tổng</th><th class="p-3 font-medium">Trạng thái</th><th class="p-3 font-medium"></th></tr></thead>
        <tbody>
          <tr v-for="o in orders.data" :key="o.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 font-mono text-stone-600">{{ o.uuid || '#'+o.id }}</td>
            <td class="p-3 font-medium text-stone-800">{{ o.product?.name }}</td>
            <td class="p-3 text-stone-500">{{ o.buyer?.name }}</td>
            <td class="p-3 text-stone-700">{{ formatPrice(o.total_amount) }}₫</td>
            <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="statusClass(o.status)">{{ statusLabel(o.status) }}</span></td>
            <td class="p-3 text-right"><Link :href="route('admin.agriverse.orders.show', o.id)" class="text-emerald-600 hover:text-emerald-800">Chi tiết</Link></td>
          </tr>
        </tbody>
      </table>
      <div class="p-3 border-t border-stone-100 flex items-center justify-between text-xs text-stone-500">
        <span>Trang {{ orders.current_page }}/{{ orders.last_page }}</span>
        <div class="flex gap-1"><Link v-for="link in orders.links" :key="link.label" :href="link.url || '#'" v-html="link.label" class="px-2 py-1 rounded border border-stone-200 hover:bg-emerald-50" :class="{ 'bg-emerald-600 text-white': link.active }" /></div>
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

const props = defineProps({ orders: Object });
const search = ref('');
const statusFilter = ref('');

function filter() { router.get(route('admin.agriverse.orders.index'), { search: search.value, status: statusFilter.value }, { preserveState: true }); }
</script>
