<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Quản lý nhóm chat</h1>
      <button @click="showCreate = true" class="h-8 px-3 rounded-lg text-xs font-semibold text-white"
        style="background: var(--ag-primary-500);">
        + Tạo nhóm mới
      </button>
    </div>

    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <table class="w-full text-xs">
        <thead>
          <tr class="bg-stone-50 text-stone-500 text-left">
            <th class="p-3 font-medium">ID</th>
            <th class="p-3 font-medium">Tên nhóm</th>
            <th class="p-3 font-medium">Số thành viên</th>
            <th class="p-3 font-medium">Người tạo</th>
            <th class="p-3 font-medium">Ngày tạo</th>
            <th class="p-3 font-medium"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="g in groups.data" :key="g.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 text-stone-500 font-mono">{{ g.id }}</td>
            <td class="p-3 font-medium text-stone-800 flex items-center gap-2">
              <div class="w-6 h-6 rounded-lg flex items-center justify-center text-[12px]"
                style="background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent); color: var(--ag-primary-500);">
                <span class="material-symbols-outlined" style="font-size: 14px;">groups</span>
              </div>
              {{ g.name }}
            </td>
            <td class="p-3 text-stone-500">{{ g.member_count }}</td>
            <td class="p-3 text-stone-500">{{ g.created_by }}</td>
            <td class="p-3 text-stone-500">{{ formatDate(g.created_at) }}</td>
            <td class="p-3 text-right">
              <Link :href="route('admin.agriverse.chat-groups.show', g.id)" class="text-emerald-600 hover:text-emerald-800">Quản lý</Link>
              <button @click="destroy(g.id)" class="text-red-500 hover:text-red-700 ml-3">Xóa</button>
            </td>
          </tr>
          <tr v-if="!groups.data?.length">
            <td colspan="6" class="p-8 text-center text-stone-400">Chưa có nhóm chat nào</td>
          </tr>
        </tbody>
      </table>
      <div v-if="groups.last_page > 1" class="p-3 border-t border-stone-100 flex items-center justify-between text-xs text-stone-500">
        <span>Trang {{ groups.current_page }}/{{ groups.last_page }}</span>
        <div class="flex gap-1">
          <Link v-for="link in groups.links" :key="link.label" :href="link.url || '#'"
            v-html="link.label"
            class="px-2 py-1 rounded border border-stone-200 hover:bg-emerald-50"
            :class="{ 'bg-emerald-600 text-white border-emerald-600': link.active }" />
        </div>
      </div>
    </div>

    <!-- Create Modal -->
    <Teleport to="body">
      <div v-if="showCreate" class="fixed inset-0 z-[9999] flex items-center justify-center p-4"
        style="background: rgba(0,0,0,0.3); backdrop-filter: blur(4px);" @click.self="showCreate = false">
        <div class="bg-white rounded-2xl w-full max-w-sm p-6 shadow-xl">
          <h2 class="text-base font-bold text-stone-800 mb-4">Tạo nhóm chat mới</h2>
          <form @submit.prevent="create">
            <input v-model="newName" placeholder="Tên nhóm..." maxlength="100"
              class="w-full h-10 px-3 rounded-xl border border-stone-300 text-sm outline-none focus:border-emerald-500 mb-4">
            <div class="flex gap-3 justify-end">
              <button type="button" @click="showCreate = false" class="h-9 px-4 rounded-xl text-xs font-semibold text-stone-500 border border-stone-300">Hủy</button>
              <button type="submit" class="h-9 px-4 rounded-xl text-xs font-semibold text-white"
                style="background: var(--ag-primary-500);" :disabled="!newName.trim()">Tạo</button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </AdminLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ groups: Object });

const showCreate = ref(false);
const newName = ref('');

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('vi-VN') : '—';
}

function create() {
  if (!newName.value.trim()) return;
  router.post(route('admin.agriverse.chat-groups.store'), { name: newName.value }, {
    onSuccess: () => { showCreate.value = false; newName.value = ''; }
  });
}

function destroy(id) {
  if (confirm('Xóa nhóm chat này?')) {
    router.delete(route('admin.agriverse.chat-groups.destroy', id));
  }
}
</script>
