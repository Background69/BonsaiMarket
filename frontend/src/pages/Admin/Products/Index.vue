<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Sản phẩm</h1>
      <span class="text-xs text-stone-500">Chỉ xem · sản phẩm công khai</span>
    </div>
    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <div class="p-3 border-b border-stone-100 flex flex-wrap items-center gap-2">
        <input aria-label="Tìm sản phẩm trong catalog" v-model="search" class="h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500 w-52"
               placeholder="Tìm kiếm...">
        <select aria-label="Lọc danh mục sản phẩm" v-model="categoryFilter"
                class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
          <option value="">Tất cả danh mục</option>
          <option v-for="c in categories" :key="c.id" :value="c.name">{{ c.name }}</option>
        </select>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-xs" style="min-width: 700px;">
          <thead>
          <tr class="bg-stone-50 text-stone-500 text-left">
            <th class="p-3 font-medium">ID</th>
            <th class="p-3 font-medium">Tên</th>
            <th class="p-3 font-medium">Danh mục</th>
            <th class="p-3 font-medium">Cửa hàng</th>
            <th class="p-3 font-medium">Giá</th>
            <th class="p-3 font-medium">Tồn kho</th>
            <th class="p-3 font-medium">Trạng thái</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="p in filteredProducts" :key="p.id"
              class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 text-stone-500">{{ p.id }}</td>
            <td class="p-3 font-medium text-stone-800"><RouterLink :to="'/products/'+p.id">{{ p.name }}</RouterLink></td>
            <td class="p-3 text-stone-500">{{ p.category || '—' }}</td>
            <td class="p-3 text-stone-500"><RouterLink v-if="p.store?.id" :to="'/stores/'+p.store.id">{{ p.store.name }}</RouterLink><span v-else>—</span></td>
            <td class="p-3 text-stone-700">{{ formatPrice(p.price) }}₫</td>
            <td class="p-3 text-stone-700">{{ p.stock ?? '—' }}</td>
            <td class="p-3"><span :style="statusBadgeStyle(p.status)"
                                  class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold">{{ statusLabel(p.status) }}</span></td>
          </tr>
          <tr v-if="!filteredProducts.length && !isLoading && !apiError">
            <td class="p-8 text-center text-stone-500" colspan="7">Chưa có sản phẩm.</td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="p-3 border-t border-stone-100 text-xs text-stone-500">{{ filteredProducts.length }} sản phẩm công
        khai
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import {ref, computed} from 'vue';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({products: Object, categories: Array, isLoading: Boolean, apiError: Boolean});
const search = ref('');
const categoryFilter = ref('');
const filteredProducts = computed(() => (props.products?.data || []).filter(p =>
    (!search.value || p.name?.toLocaleLowerCase().includes(search.value.toLocaleLowerCase())) &&
    (!categoryFilter.value || p.category === categoryFilter.value)
));

function formatPrice(v) {
  return new Intl.NumberFormat('vi-VN').format(v);
}

function statusLabel(s) {
  return {
    pending_review: 'Chờ duyệt',
    active: 'Đã duyệt',
    rejected: 'Từ chối',
    published: 'Đã duyệt',
    draft: 'Nháp',
    archived: 'Lưu trữ'
  }[s] || s;
}

function statusBadgeStyle(s) {
  const map = {
    pending_review: 'background: #fef3c7; color: #765419;',
    active: 'background: #dcfce7; color: #104e35;',
    rejected: 'background: #fef2f2; color: #a2382d;',
    published: 'background: #d1fae5; color: #059669;',
    draft: 'background: #fef3c7; color: #d97706;',
    archived: 'background: #f5f5f4; color: #78716c;',
  };
  return map[s] || 'background: #f5f5f4; color: #78716c;';
}

</script>
