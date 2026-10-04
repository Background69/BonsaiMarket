<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Gói đăng ký</h1>
      <Link :href="route('admin.agriverse.plans.create')" class="h-8 px-3 rounded-lg bg-emerald-600 text-white text-xs font-bold leading-8 hover:bg-emerald-700 transition-all">+ Thêm</Link>
    </div>
    <div class="grid grid-cols-6 md:grid-cols-12 gap-3">
      <div v-for="p in plans.data" :key="p.id" class="col-span-6 md:col-span-4">
        <div class="bg-white rounded-xl border border-stone-200 p-4 hover:shadow-sm transition-all">
          <div class="text-sm font-bold text-stone-800">{{ p.name }}</div>
          <div class="text-lg font-bold text-emerald-600 mt-1">{{ formatPrice(p.price_per_month) }}₫<span class="text-xs text-stone-400 font-normal">/tháng</span></div>
          <div class="text-xs text-stone-500 mt-2">3D models: {{ p.limit_3d_models }}</div>
          <div class="flex items-center gap-2 mt-2">
            <span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="p.status === 'active' ? 'bg-emerald-50 text-emerald-700' : 'bg-stone-100 text-stone-600'">{{ p.status === 'active' ? 'Hoạt động' : 'Tắt' }}</span>
          </div>
          <div class="flex gap-2 mt-3 pt-3 border-t border-stone-100">
            <Link :href="route('admin.agriverse.plans.edit', p.id)" class="text-xs text-emerald-600 hover:text-emerald-800">Sửa</Link>
            <button @click="destroy(p.id)" class="text-xs text-red-500 hover:text-red-700">Xóa</button>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ plans: Object });
function formatPrice(v) { return new Intl.NumberFormat('vi-VN').format(v); }
function destroy(id) { if (confirm('Xóa gói này?')) router.delete(route('admin.agriverse.plans.destroy', id)); }
</script>
