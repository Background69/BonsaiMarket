<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Mã giảm giá</h1>
      <Link :href="route('admin.agriverse.coupons.create')" class="h-8 px-3 rounded-lg bg-emerald-600 text-white text-xs font-bold leading-8 hover:bg-emerald-700 transition-all">+ Thêm</Link>
    </div>
    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <div class="p-3 border-b border-stone-100"><input v-model="search" @input="filter" placeholder="Tìm mã..." class="h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500 w-52"></div>
      <table class="w-full text-xs">
        <thead><tr class="bg-stone-50 text-stone-500 text-left"><th class="p-3 font-medium">Mã</th><th class="p-3 font-medium">Loại</th><th class="p-3 font-medium">Giá trị</th><th class="p-3 font-medium">HSD</th><th class="p-3 font-medium">Trạng thái</th><th class="p-3 font-medium"></th></tr></thead>
        <tbody>
          <tr v-for="c in coupons.data" :key="c.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 font-mono font-bold text-stone-800">{{ c.code }}</td>
            <td class="p-3 text-stone-500">{{ c.type === 'percent' ? '%' : 'VNĐ' }}</td>
            <td class="p-3 text-stone-700">{{ c.type === 'percent' ? c.value+'%' : formatPrice(c.value)+'₫' }}</td>
            <td class="p-3 text-stone-500">{{ c.expires_at || '—' }}</td>
            <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full" :class="c.is_active ? 'bg-emerald-50 text-emerald-700' : 'bg-stone-100 text-stone-600'">{{ c.is_active ? 'Hoạt động' : 'Tắt' }}</span></td>
            <td class="p-3 text-right"><Link :href="route('admin.agriverse.coupons.edit', c.id)" class="text-emerald-600 hover:text-emerald-800 mr-2">Sửa</Link><button @click="destroy(c.id)" class="text-red-500 hover:text-red-700">Xóa</button></td>
          </tr>
        </tbody>
      </table>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
const props = defineProps({ coupons: Object });
const search = ref('');
function formatPrice(v) { return new Intl.NumberFormat('vi-VN').format(v); }
function filter() { router.get(route('admin.agriverse.coupons.index'), { search: search.value }, { preserveState: true }); }
function destroy(id) { if (confirm('Xóa mã giảm giá?')) router.delete(route('admin.agriverse.coupons.destroy', id)); }
</script>
