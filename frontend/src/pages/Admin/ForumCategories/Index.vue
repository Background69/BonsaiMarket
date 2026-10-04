<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Danh mục diễn đàn</h1>
      <button @click="showForm = true; form = { name: '', description: '' }"
        class="h-8 px-3 rounded-lg text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700 transition-colors">
        + Thêm danh mục
      </button>
    </div>

    <!-- Create/Edit Modal -->
    <Transition name="fade">
      <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="showForm = false">
        <div class="bg-white rounded-2xl p-6 w-full max-w-md mx-4 shadow-xl">
          <h2 class="text-sm font-bold text-stone-800 mb-4">{{ editing ? 'Sửa danh mục' : 'Thêm danh mục' }}</h2>
          <form @submit.prevent="submit">
            <div class="space-y-3">
              <div>
                <label class="text-xs font-semibold text-stone-600 block mb-1">Tên danh mục</label>
                <input v-model="form.name" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" placeholder="VD: Cây cảnh bonsai" required />
              </div>
              <div>
                <label class="text-xs font-semibold text-stone-600 block mb-1">Mô tả</label>
                <textarea v-model="form.description" class="w-full px-3 py-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500 resize-none" rows="2" placeholder="Mô tả ngắn..."></textarea>
              </div>
              <div v-if="editing" class="flex items-center gap-2">
                <input id="is_active" type="checkbox" v-model="form.is_active" class="rounded border-stone-300" />
                <label for="is_active" class="text-xs text-stone-600">Kích hoạt</label>
              </div>
            </div>
            <div class="flex justify-end gap-2 mt-5">
              <button type="button" @click="showForm = false" class="h-8 px-4 rounded-lg text-xs font-semibold text-stone-600 border border-stone-300 hover:bg-stone-50">Hủy</button>
              <button type="submit" class="h-8 px-4 rounded-lg text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700" :disabled="!form.name.trim()">{{ editing ? 'Cập nhật' : 'Thêm' }}</button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- Categories Table -->
    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <table class="w-full text-xs">
        <thead><tr class="bg-stone-50 text-stone-500 text-left"><th class="p-3 font-medium">ID</th><th class="p-3 font-medium">Tên</th><th class="p-3 font-medium">Slug</th><th class="p-3 font-medium">Mô tả</th><th class="p-3 font-medium">Bài viết</th><th class="p-3 font-medium">Trạng thái</th><th class="p-3 font-medium"></th></tr></thead>
        <tbody>
          <tr v-for="c in categories" :key="c.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 text-stone-500 font-mono">{{ c.id }}</td>
            <td class="p-3 font-medium text-stone-800">{{ c.name }}</td>
            <td class="p-3 text-stone-400 font-mono">{{ c.slug }}</td>
            <td class="p-3 text-stone-500 max-w-[200px] truncate">{{ c.description || '—' }}</td>
            <td class="p-3 text-stone-500">{{ c.posts_count }}</td>
            <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="c.is_active ? 'bg-emerald-50 text-emerald-700' : 'bg-stone-100 text-stone-500'">{{ c.is_active ? 'Hoạt động' : 'Ẩn' }}</span></td>
            <td class="p-3 text-right whitespace-nowrap">
              <button @click="edit(c)" class="text-sky-600 hover:text-sky-800 mr-2">Sửa</button>
              <button @click="destroy(c.id)" class="text-red-500 hover:text-red-700">Xóa</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!categories.length" class="p-6 text-center text-xs text-stone-400">
        Chưa có danh mục nào. Hãy thêm danh mục đầu tiên.
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref } from 'vue';
import { router, usePage } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import axios from 'axios';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ categories: Array });

const showForm = ref(false);
const editing = ref(false);
const form = ref({ name: '', description: '', is_active: true });

function edit(c) {
  editing.value = true;
  form.value = { name: c.name, description: c.description, is_active: c.is_active };
  showForm.value = true;
  // store the id for update
  form.value._id = c.id;
}

function resetForm() {
  form.value = { name: '', description: '', is_active: true };
  editing.value = false;
  showForm.value = false;
}

async function submit() {
  if (!form.value.name.trim()) return;
  try {
    if (editing.value) {
      await axios.put(route('admin.agriverse.forum-categories.update', form.value._id), {
        name: form.value.name,
        description: form.value.description,
        is_active: form.value.is_active,
      });
    } else {
      await axios.post(route('admin.agriverse.forum-categories.store'), {
        name: form.value.name,
        description: form.value.description,
      });
    }
    router.reload({ only: ['categories'] });
    resetForm();
  } catch {
    // validation error
  }
}

function destroy(id) {
  if (!confirm('Xóa danh mục này?')) return;
  router.delete(route('admin.agriverse.forum-categories.destroy', id));
}
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
