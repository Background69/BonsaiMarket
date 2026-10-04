<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4"><h1 class="text-base font-bold text-stone-800">Yêu cầu Scan 3D</h1></div>
    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <div class="p-3 border-b border-stone-100">
        <select v-model="statusFilter" @change="filter" class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
          <option value="">Tất cả</option><option value="pending">Chờ xử lý</option><option value="processing">Đang xử lý</option><option value="completed">Hoàn thành</option><option value="failed">Thất bại</option>
        </select>
      </div>
      <table class="w-full text-xs">
        <thead><tr class="bg-stone-50 text-stone-500 text-left"><th class="p-3 font-medium">ID</th><th class="p-3 font-medium">Cửa hàng</th><th class="p-3 font-medium">Trạng thái</th><th class="p-3 font-medium">Ngày tạo</th><th class="p-3 font-medium"></th></tr></thead>
        <tbody>
          <tr v-for="j in jobs.data" :key="j.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 font-mono text-stone-600">#{{ j.id }}</td>
            <td class="p-3 text-stone-800">{{ j.store?.name }}</td>
            <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="statusClass(j.status)">{{ statusLabel(j.status) }}</span></td>
            <td class="p-3 text-stone-500">{{ j.created_at }}</td>
            <td class="p-3 text-right"><Link :href="route('admin.agriverse.scans.show', j.id)" class="text-emerald-600 hover:text-emerald-800 mr-2">Chi tiết</Link><button @click="destroy(j.id)" class="text-red-500 hover:text-red-700">Xóa</button></td>
          </tr>
        </tbody>
      </table>
      <div class="p-3 border-t border-stone-100 flex items-center justify-between text-xs text-stone-500">
        <span>Trang {{ jobs.current_page }}/{{ jobs.last_page }}</span>
        <div class="flex gap-1"><Link v-for="link in jobs.links" :key="link.label" :href="link.url || '#'" v-html="link.label" class="px-2 py-1 rounded border border-stone-200 hover:bg-emerald-50" :class="{ 'bg-emerald-600 text-white': link.active }" /></div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
const props = defineProps({ jobs: Object });
const statusFilter = ref('');
function statusLabel(s) { return { pending: 'Chờ xử lý', processing: 'Đang xử lý', completed: 'Hoàn thành', failed: 'Thất bại' }[s] || s; }
function statusClass(s) { return { pending: 'bg-amber-50 text-amber-600', processing: 'bg-blue-50 text-blue-600', completed: 'bg-emerald-50 text-emerald-600', failed: 'bg-red-50 text-red-600' }[s] || ''; }
function filter() { router.get(route('admin.agriverse.scans.index'), { status: statusFilter.value }, { preserveState: true }); }
function destroy(id) { if (confirm('Xóa?')) router.delete(route('admin.agriverse.scans.destroy', id)); }
</script>
