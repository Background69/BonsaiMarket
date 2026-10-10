<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Đơn hàng</h1>
    </div>
    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <div class="p-3 border-b border-stone-100 flex gap-2">
        <input v-model="search" aria-label="Tìm kiếm (chưa khả dụng)" class="h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500 w-52"
               disabled
               placeholder="Tìm kiếm..."
               @input="filter">
        <select v-model="statusFilter" aria-label="Bộ lọc (chưa khả dụng)" class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"
                disabled
                @change="filter">
          <option value="">Tất cả</option>
          <option value="pending">Chờ xác nhận</option>
          <option value="confirmed">Đã xác nhận</option>
          <option value="shipping">Đang giao</option>
          <option value="delivered">Đã giao</option>
          <option value="completed">Hoàn thành</option>
          <option value="cancelled">Đã hủy</option>
        </select>
      </div>
      <div v-if="orders.data?.length" aria-label="Bảng dữ liệu, cuộn ngang để xem thêm" class="workspace-table-scroll"
           role="region" tabindex="0">
        <table class="w-full text-xs" style="min-width:700px">
          <thead>
          <tr class="bg-stone-50 text-stone-500 text-left">
            <th class="p-3 font-medium" scope="col">Mã ĐH</th>
            <th class="p-3 font-medium" scope="col">Sản phẩm</th>
            <th class="p-3 font-medium" scope="col">Người mua</th>
            <th class="p-3 font-medium" scope="col">Tổng</th>
            <th class="p-3 font-medium" scope="col">Trạng thái</th>
            <th class="p-3 font-medium" scope="col"><span class="sr-only">Thao t?c</span></th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="o in orders.data" :key="o.id"
              class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 font-mono text-stone-600">{{ o.uuid || '#' + o.id }}</td>
            <td class="p-3 font-medium text-stone-800">{{ o.product?.name }}</td>
            <td class="p-3 text-stone-500">{{ o.buyer?.name }}</td>
            <td class="p-3 text-stone-700">{{ formatPrice(o.total_amount) }}₫</td>
            <td class="p-3"><span :class="statusClass(o.status)"
                                  class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold">{{
                statusLabel(o.status)
              }}</span></td>
            <td class="p-3 text-right">
              <Link :href="route('admin.agriverse.orders.show', o.id)" class="text-emerald-600 hover:text-emerald-800">
                Chi
                tiết
              </Link>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <p v-if="!orders.data?.length" class="workspace-table-empty" role="status">Chưa kết nối API đơn hàng. Không có dữ
        liệu để hiển thị.</p>
      <div v-if="orders.data?.length"
           class="p-3 border-t border-stone-100 flex items-center justify-between text-xs text-stone-500">
        <span>Trang {{ orders.current_page || 1 }}/{{ orders.last_page }}</span>
        <div class="flex gap-1">
          <Link v-for="link in orders.links" :key="link.label" :class="{ 'bg-emerald-600 text-white': link.active }"
                :href="link.url || '#'"
                class="px-2 py-1 rounded border border-stone-200 hover:bg-emerald-50"
          >{{ link.label }}
          </Link>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import {ref} from 'vue';
import {Link, router} from '@inertiajs/vue3';
import {route} from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
import {formatPrice, statusLabel, statusClass} from '@agriverse/utils';

const props = defineProps({orders: Object});
const search = ref('');
const statusFilter = ref('');

function filter() {
  router.get(route('admin.agriverse.orders.index'), {
    search: search.value,
    status: statusFilter.value
  }, {preserveState: true});
}
</script>
