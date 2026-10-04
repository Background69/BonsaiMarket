<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Hợp đồng</h1>
    </div>
    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <table class="w-full text-xs">
        <thead><tr class="bg-stone-50 text-stone-500 text-left"><th class="p-3 font-medium">Mã HĐ</th><th class="p-3 font-medium">Sản phẩm</th><th class="p-3 font-medium">Người mua</th><th class="p-3 font-medium">Trạng thái</th><th class="p-3 font-medium"></th></tr></thead>
        <tbody>
          <tr v-for="c in contracts.data" :key="c.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 font-mono text-stone-600">{{ c.uuid || '#'+c.id }}</td>
            <td class="p-3 text-stone-800 font-medium">{{ c.order?.product?.name }}</td>
            <td class="p-3 text-stone-500">{{ c.order?.buyer?.name }}</td>
            <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="c.status === 'active' ? 'bg-emerald-50 text-emerald-700' : c.status === 'pending' ? 'bg-amber-50 text-amber-700' : 'bg-stone-100 text-stone-600'">{{ {active:'Hiệu lực',pending:'Chờ ký',expired:'Hết hạn',cancelled:'Hủy'}[c.status] || c.status }}</span></td>
            <td class="p-3 text-right"><Link :href="route('admin.agriverse.contracts.show', c.id)" class="text-emerald-600 hover:text-emerald-800 mr-2">Chi tiết</Link><button @click="destroy(c.id)" class="text-red-500 hover:text-red-700">Xóa</button></td>
          </tr>
        </tbody>
      </table>
      <div class="p-3 border-t border-stone-100 flex items-center justify-between text-xs text-stone-500">
        <span>Trang {{ contracts.current_page }}/{{ contracts.last_page }}</span>
        <div class="flex gap-1"><Link v-for="link in contracts.links" :key="link.label" :href="link.url || '#'" v-html="link.label" class="px-2 py-1 rounded border border-stone-200 hover:bg-emerald-50" :class="{ 'bg-emerald-600 text-white': link.active }" /></div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ contracts: Object });
function destroy(id) { if (confirm('Xóa hợp đồng?')) router.delete(route('admin.agriverse.contracts.destroy', id)); }
</script>
