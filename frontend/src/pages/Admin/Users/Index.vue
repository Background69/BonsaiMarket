<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Người dùng</h1>
    </div>
    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <div class="p-3 border-b border-stone-100 flex gap-2">
        <input v-model="search" @input="filter" placeholder="Tìm kiếm..." class="h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500 w-52">
        <select v-model="roleFilter" @change="filter" class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
          <option value="">Tất cả vai trò</option>
          <option value="admin">Admin</option>
          <option value="seller">Người bán</option>
          <option value="employee">Nhân viên</option>
          <option value="buyer">Người mua</option>
        </select>
        <select v-model="activeFilter" @change="filter" class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
          <option value="">Tất cả trạng thái</option>
          <option value="1">Hoạt động</option>
          <option value="0">Vô hiệu</option>
        </select>
      </div>
      <table class="w-full text-xs">
        <thead><tr class="bg-stone-50 text-stone-500 text-left"><th class="p-3 font-medium">ID</th><th class="p-3 font-medium">Tên</th><th class="p-3 font-medium">Email</th><th class="p-3 font-medium">Điện thoại</th><th class="p-3 font-medium">Vai trò</th><th class="p-3 font-medium">Trạng thái</th><th class="p-3 font-medium">Ngày ĐK</th><th class="p-3 font-medium"></th></tr></thead>
        <tbody>
          <tr v-for="u in users.data" :key="u.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 text-stone-500 font-mono">{{ u.id }}</td>
            <td class="p-3 font-medium text-stone-800 flex items-center gap-2">
              <div class="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold" style="background: var(--ag-primary-500); color: white;">{{ u.name?.charAt(0)?.toUpperCase() }}</div>
              {{ u.name }}
            </td>
            <td class="p-3 text-stone-500">{{ u.email }}</td>
            <td class="p-3 text-stone-500">{{ u.phone || '—' }}</td>
            <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="roleClass(u.role)">{{ roleLabel(u.role) }}</span></td>
            <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="u.is_active ? 'bg-emerald-50 text-emerald-600' : 'bg-red-50 text-red-600'">{{ u.is_active ? 'Hoạt động' : 'Vô hiệu' }}</span></td>
            <td class="p-3 text-stone-500">{{ formatDate(u.created_at) }}</td>
            <td class="p-3 text-right">
              <Link :href="route('admin.agriverse.users.show', u.id)" class="text-emerald-600 hover:text-emerald-800 mr-2">Xem</Link>
              <button @click="toggleActive(u)" class="text-amber-600 hover:text-amber-800 mr-2">{{ u.is_active ? 'Vô hiệu' : 'Kích hoạt' }}</button>
              <button @click="destroy(u.id)" class="text-red-500 hover:text-red-700">Xóa</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="p-3 border-t border-stone-100 flex items-center justify-between text-xs text-stone-500">
        <span>Trang {{ users.current_page }}/{{ users.last_page }}</span>
        <div class="flex gap-1"><Link v-for="link in users.links" :key="link.label" :href="link.url || '#'" v-html="link.label" class="px-2 py-1 rounded border border-stone-200 hover:bg-emerald-50" :class="{ 'bg-emerald-600 text-white border-emerald-600': link.active }" /></div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ users: Object });
const search = ref('');
const roleFilter = ref('');
const activeFilter = ref('');

function formatDate(d) { return d ? new Date(d).toLocaleDateString('vi-VN') : '—'; }
function roleLabel(r) { return { admin: 'Admin', seller: 'Người bán', employee: 'Nhân viên', buyer: 'Người mua' }[r] || r; }
function roleClass(r) { return { admin: 'bg-purple-50 text-purple-600', seller: 'bg-blue-50 text-blue-600', employee: 'bg-amber-50 text-amber-600', buyer: 'bg-emerald-50 text-emerald-600' }[r] || 'bg-stone-100 text-stone-600'; }

function filter() {
  router.get(route('admin.agriverse.users.index'), { search: search.value, role: roleFilter.value, is_active: activeFilter.value }, { preserveState: true });
}

function toggleActive(u) {
  if (confirm(`${u.is_active ? 'Vô hiệu hóa' : 'Kích hoạt'} người dùng "${u.name}"?`)) {
    router.post(route('admin.agriverse.users.toggle-active', u.id));
  }
}

function destroy(id) {
  if (confirm('Xóa người dùng này?')) router.delete(route('admin.agriverse.users.destroy', id));
}
</script>
