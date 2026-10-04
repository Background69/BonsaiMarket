<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Banner</h1>
      <button @click="openCreate" class="h-8 px-3 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all">+ Thêm</button>
    </div>

    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <table class="w-full text-xs">
        <thead><tr class="bg-stone-50 text-stone-500 text-left"><th class="p-3 font-medium w-8">#</th><th class="p-3 font-medium">Hình ảnh</th><th class="p-3 font-medium">Tiêu đề</th><th class="p-3 font-medium">Link</th><th class="p-3 font-medium">Thứ tự</th><th class="p-3 font-medium">Kích hoạt</th><th class="p-3 font-medium">Hiệu lực</th><th class="p-3 font-medium"></th></tr></thead>
        <tbody>
          <tr v-for="(b, i) in banners" :key="b.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors" draggable="true" @dragstart="onDragStart($event, i)" @dragover.prevent @drop="onDrop($event, i)">
            <td class="p-3 text-stone-400 cursor-grab"><span class="material-symbols-outlined text-sm">drag_indicator</span></td>
            <td class="p-3"><img :src="b.image_url" class="w-16 h-10 object-cover rounded-lg border border-stone-200" @error="e => e.target.style.display = 'none'"></td>
            <td class="p-3 font-medium text-stone-800">{{ b.title || '—' }}</td>
            <td class="p-3 text-stone-500 max-w-[120px] truncate">{{ b.link_url || '—' }}</td>
            <td class="p-3 text-stone-500">{{ b.sort_order }}</td>
            <td class="p-3"><button @click="toggleActive(b)" class="text-xs px-2 py-1 rounded-full font-semibold" :class="b.is_active ? 'bg-emerald-50 text-emerald-700' : 'bg-stone-100 text-stone-600'">{{ b.is_active ? 'Bật' : 'Tắt' }}</button></td>
            <td class="p-3 text-[10px] text-stone-500">{{ formatDateRange(b.starts_at, b.expires_at) }}</td>
            <td class="p-3 text-right">
              <button @click="openEdit(b)" class="text-emerald-600 hover:text-emerald-800 mr-2">Sửa</button>
              <button @click="destroy(b.id)" class="text-red-500 hover:text-red-700">Xóa</button>
            </td>
          </tr>
          <tr v-if="!banners.length"><td colspan="8" class="p-6 text-center text-stone-400">Chưa có banner nào</td></tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(0,0,0,0.4);">
      <div class="bg-white rounded-xl w-full max-w-lg mx-4 p-5 shadow-xl">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-bold text-stone-800">{{ editing ? 'Sửa banner' : 'Thêm banner' }}</h3>
          <button @click="closeModal" class="text-stone-400 hover:text-stone-600"><span class="material-symbols-outlined text-lg">close</span></button>
        </div>
        <form @submit.prevent="submit" class="space-y-3">
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Tiêu đề</label><input v-model="form.title" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
          <div><label class="block text-xs font-medium text-stone-600 mb-1">URL hình ảnh <span class="text-red-500">*</span></label><input v-model="form.image_url" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" required></div>
          <div v-if="form.image_url" class="flex gap-2 items-center"><img :src="form.image_url" class="w-20 h-12 object-cover rounded border border-stone-200" @error="e => e.target.style.display = 'none'"><span class="text-[10px] text-stone-400">Preview</span></div>
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Link</label><input v-model="form.link_url" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Mô tả</label><textarea v-model="form.description" rows="2" class="w-full px-3 py-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></textarea></div>
          <div class="grid grid-cols-2 gap-3">
            <div><label class="block text-xs font-medium text-stone-600 mb-1">Thứ tự</label><input v-model.number="form.sort_order" type="number" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
            <div><label class="block text-xs font-medium text-stone-600 mb-1">Kích hoạt</label><select v-model="form.is_active" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"><option :value="true">Có</option><option :value="false">Không</option></select></div>
          </div>
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Ngày bắt đầu</label><input v-model="form.starts_at" type="datetime-local" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Ngày kết thúc</label><input v-model="form.expires_at" type="datetime-local" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
          <div class="flex gap-2 pt-3 border-t border-stone-100">
            <button type="submit" class="h-8 px-3 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all">{{ editing ? 'Cập nhật' : 'Thêm' }}</button>
            <button type="button" @click="closeModal" class="h-8 px-3 rounded-lg border border-stone-300 text-stone-600 text-xs font-bold hover:bg-stone-50 transition-all">Hủy</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ banners: Array });
const showModal = ref(false);
const editing = ref(false);
const editingId = ref(null);
const dragIndex = ref(null);

const defaultForm = { title: '', image_url: '', link_url: '', description: '', sort_order: 0, is_active: true, starts_at: '', expires_at: '' };
const form = reactive({ ...defaultForm });

function formatDateRange(s, e) {
  const fmt = d => d ? new Date(d).toLocaleDateString('vi-VN') : '—';
  return `${fmt(s)} → ${fmt(e)}`;
}

function openCreate() { editing.value = false; editingId.value = null; Object.assign(form, defaultForm); showModal.value = true; }
function openEdit(b) {
  editing.value = true; editingId.value = b.id;
  form.title = b.title || '';
  form.image_url = b.image_url;
  form.link_url = b.link_url || '';
  form.description = b.description || '';
  form.sort_order = b.sort_order;
  form.is_active = b.is_active;
  form.starts_at = b.starts_at ? b.starts_at.slice(0, 16) : '';
  form.expires_at = b.expires_at ? b.expires_at.slice(0, 16) : '';
  showModal.value = true;
}
function closeModal() { showModal.value = false; }

function submit() {
  const payload = { ...form };
  payload.starts_at = payload.starts_at || null;
  payload.expires_at = payload.expires_at || null;
  if (editing.value) {
    router.put(route('admin.agriverse.banners.update', editingId.value), { ...payload }, { onSuccess: () => closeModal() });
  } else {
    router.post(route('admin.agriverse.banners.store'), payload, { onSuccess: () => closeModal() });
  }
}

function toggleActive(b) {
  router.put(route('admin.agriverse.banners.update', b.id), { ...b, is_active: !b.is_active });
}

function destroy(id) {
  if (confirm('Xóa banner này?')) router.delete(route('admin.agriverse.banners.destroy', id));
}

function onDragStart(e, i) { dragIndex.value = i; e.dataTransfer.effectAllowed = 'move'; }
function onDrop(e, i) {
  if (dragIndex.value === null || dragIndex.value === i) return;
  const items = [...props.banners];
  const [moved] = items.splice(dragIndex.value, 1);
  items.splice(i, 0, moved);
  const payload = items.map((item, idx) => ({ id: item.id, sort_order: idx }));
  router.post(route('admin.agriverse.banners.reorder'), { items: payload });
  dragIndex.value = null;
}
</script>
