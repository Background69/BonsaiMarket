<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Cửa hàng</h1>
      <Link :href="route('admin.agriverse.stores.create')" class="h-8 px-3 rounded-lg bg-emerald-600 text-white text-xs font-bold leading-8 hover:bg-emerald-700 transition-all">+ Thêm</Link>
    </div>
    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <div class="p-3 border-b border-stone-100 flex gap-2">
        <input v-model="search" @input="filter" placeholder="Tìm kiếm..." class="h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500 w-52">
        <select v-model="statusFilter" @change="filter" class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
          <option value="">Tất cả</option><option value="active">Hoạt động</option><option value="inactive">Tạm ngưng</option><option value="suspended">Khóa</option>
        </select>
      </div>
      <table class="w-full text-xs">
        <thead><tr class="bg-stone-50 text-stone-500 text-left"><th class="p-3 font-medium">Tên</th><th class="p-3 font-medium">Chủ sở hữu</th><th class="p-3 font-medium">Số SP</th><th class="p-3 font-medium">Trạng thái</th><th class="p-3 font-medium"></th></tr></thead>
        <tbody>
          <tr v-for="s in stores.data" :key="s.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 font-medium text-stone-800">{{ s.name }}</td>
            <td class="p-3 text-stone-500">{{ s.owner?.name || '—' }}</td>
            <td class="p-3 text-stone-600">{{ s.products_count || 0 }}</td>
            <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="s.status === 'active' ? 'bg-emerald-50 text-emerald-600' : s.status === 'inactive' ? 'bg-amber-50 text-amber-600' : 'bg-red-50 text-red-600'">{{ {active:'Hoạt động',inactive:'Tạm ngưng',suspended:'Khóa'}[s.status] || s.status }}</span></td>
            <td class="p-3 text-right"><Link :href="route('admin.agriverse.stores.edit', s.id)" class="text-emerald-600 hover:text-emerald-800 mr-2">Sửa</Link><button @click="destroy(s.id)" class="text-red-500 hover:text-red-700">Xóa</button></td>
          </tr>
        </tbody>
      </table>
      <div class="p-3 border-t border-stone-100 flex items-center justify-between text-xs text-stone-500">
        <span>Trang {{ stores.current_page }}/{{ stores.last_page }}</span>
        <div class="flex gap-1"><Link v-for="link in stores.links" :key="link.label" :href="link.url || '#'" v-html="link.label" class="px-2 py-1 rounded border border-stone-200 hover:bg-emerald-50" :class="{ 'bg-emerald-600 text-white': link.active }" /></div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ stores: Object });
const search = ref(''); const statusFilter = ref('');
function filter() { router.get(route('admin.agriverse.stores.index'), { search: search.value, status: statusFilter.value }, { preserveState: true }); }
function destroy(id) { if (confirm('Xóa cửa hàng này?')) router.delete(route('admin.agriverse.stores.destroy', id)); }
</script>
