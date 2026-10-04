<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">{{ store ? 'Sửa cửa hàng' : 'Thêm cửa hàng' }}</h1>
    </div>
    <form @submit.prevent="submit" class="bg-white rounded-xl border border-stone-200 p-4 max-w-lg">
      <div class="space-y-3">
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Tên cửa hàng</label><input v-model="form.name" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" required></div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Mô tả</label><textarea v-model="form.description" rows="3" class="w-full px-3 py-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></textarea></div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Logo URL</label><input v-model="form.logo" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Trạng thái</label><select v-model="form.status" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"><option value="active">Hoạt động</option><option value="inactive">Tạm ngưng</option><option value="suspended">Khóa</option></select></div>
      </div>
      <div class="flex gap-2 mt-4 pt-3 border-t border-stone-100">
        <button type="submit" class="h-9 px-4 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all">{{ store ? 'Lưu' : 'Tạo' }}</button>
        <Link :href="route('admin.agriverse.stores.index')" class="h-9 px-4 rounded-lg border border-stone-300 text-stone-600 text-xs font-bold leading-9 hover:bg-stone-50 transition-all">Hủy</Link>
      </div>
    </form>
  </AdminLayout>
</template>

<script setup>
import { reactive } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ store: Object });
const form = reactive({ name: props.store?.name || '', description: props.store?.description || '', logo: props.store?.logo || '', status: props.store?.status || 'active' });

function submit() {
  const routeName = props.store ? route('admin.agriverse.stores.update', props.store.id) : route('admin.agriverse.stores.store');
  const method = props.store ? 'put' : 'post';
  router[method](routeName, form);
}
</script>
