<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Danh mục</h1>
    </div>
    <div class="grid grid-cols-12 gap-4">
      <div class="col-span-12 md:col-span-5">
        <form @submit.prevent="submit" class="bg-white rounded-xl border border-stone-200 p-4">
          <h3 class="text-xs font-bold text-stone-700 mb-3">{{ editing ? 'Sửa danh mục' : 'Thêm danh mục' }}</h3>
          <div class="space-y-3">
            <div><label class="block text-xs font-medium text-stone-600 mb-1">Tên</label><input v-model="form.name" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" required></div>
            <div><label class="block text-xs font-medium text-stone-600 mb-1">Mô tả</label><textarea v-model="form.description" rows="2" class="w-full px-3 py-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></textarea></div>
            <div><label class="block text-xs font-medium text-stone-600 mb-1">Danh mục cha</label><select v-model="form.parent_id" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"><option :value="null">— Không —</option><option v-for="p in parents" :key="p.id" :value="p.id">{{ p.name }}</option></select></div>
            <div class="grid grid-cols-2 gap-3">
              <div><label class="block text-xs font-medium text-stone-600 mb-1">Thứ tự</label><input v-model.number="form.sort_order" type="number" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
              <div><label class="block text-xs font-medium text-stone-600 mb-1">Kích hoạt</label><select v-model="form.is_active" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"><option :value="true">Có</option><option :value="false">Không</option></select></div>
            </div>
          </div>
          <div class="flex gap-2 mt-3 pt-3 border-t border-stone-100">
            <button type="submit" class="h-8 px-3 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all">{{ editing ? 'Cập nhật' : 'Thêm' }}</button>
            <button v-if="editing" type="button" @click="cancelEdit" class="h-8 px-3 rounded-lg border border-stone-300 text-stone-600 text-xs font-bold hover:bg-stone-50 transition-all">Hủy</button>
          </div>
        </form>
      </div>
      <div class="col-span-12 md:col-span-7">
        <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
          <table class="w-full text-xs">
            <thead><tr class="bg-stone-50 text-stone-500 text-left"><th class="p-3 font-medium">Tên</th><th class="p-3 font-medium">Số SP</th><th class="p-3 font-medium">Thứ tự</th><th class="p-3 font-medium">Trạng thái</th><th class="p-3 font-medium"></th></tr></thead>
            <tbody>
              <tr v-for="c in categories.data" :key="c.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
                <td class="p-3 font-medium text-stone-800">{{ c.name }}<span v-if="c.parent" class="text-stone-400 ml-1">({{ c.parent.name }})</span></td>
                <td class="p-3 text-stone-500">{{ c.products_count || 0 }}</td>
                <td class="p-3 text-stone-500">{{ c.sort_order || 0 }}</td>
                <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full" :class="c.is_active ? 'bg-emerald-50 text-emerald-700' : 'bg-stone-100 text-stone-600'">{{ c.is_active ? 'Hoạt động' : 'Ẩn' }}</span></td>
                <td class="p-3 text-right"><button @click="edit(c)" class="text-emerald-600 hover:text-emerald-800 mr-2">Sửa</button><button @click="destroy(c.id)" class="text-red-500 hover:text-red-700">Xóa</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ categories: Object, parents: Array });
const editing = ref(false);
const editingId = ref(null);

const form = reactive({ name: '', description: '', parent_id: null, sort_order: 0, is_active: true });

function edit(c) {
  editing.value = true; editingId.value = c.id;
  form.name = c.name; form.description = c.description || ''; form.parent_id = c.parent_id; form.sort_order = c.sort_order || 0; form.is_active = c.is_active;
}
function cancelEdit() { editing.value = false; editingId.value = null; form.name = ''; form.description = ''; form.parent_id = null; form.sort_order = 0; form.is_active = true; }

function submit() {
  if (editing.value) {
    router.put(route('admin.agriverse.categories.update', editingId.value), { ...form });
  } else {
    router.post(route('admin.agriverse.categories.store'), form);
  }
}
function destroy(id) { if (confirm('Xóa danh mục này?')) router.delete(route('admin.agriverse.categories.destroy', id)); }
</script>
