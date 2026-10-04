<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">{{ plan ? 'Sửa gói đăng ký' : 'Thêm gói đăng ký' }}</h1>
    </div>
    <form @submit.prevent="submit" class="bg-white rounded-xl border border-stone-200 p-4 max-w-lg">
      <div class="space-y-3">
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Tên gói</label><input v-model="form.name" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" required></div>
        <div class="grid grid-cols-2 gap-3">
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Giá / tháng</label><input v-model.number="form.price_per_month" type="number" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" required></div>
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Giới hạn 3D</label><input v-model.number="form.limit_3d_models" type="number" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" required></div>
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Tính năng (mỗi dòng một tính năng)</label><textarea v-model="form.featuresText" rows="3" class="w-full px-3 py-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" placeholder="Hỗ trợ 5 mô hình 3D&#10;Ưu tiên xử lý đơn hàng&#10;Hỗ trợ kỹ thuật 24/7"></textarea></div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Trạng thái</label><select v-model="form.status" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"><option value="active">Hoạt động</option><option value="inactive">Tắt</option></select></div>
      </div>
      <div class="flex gap-2 mt-4 pt-3 border-t border-stone-100">
        <button type="submit" class="h-9 px-4 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all">{{ plan ? 'Lưu' : 'Tạo' }}</button>
        <Link :href="route('admin.agriverse.plans.index')" class="h-9 px-4 rounded-lg border border-stone-300 text-stone-600 text-xs font-bold leading-9 hover:bg-stone-50 transition-all">Hủy</Link>
      </div>
    </form>
  </AdminLayout>
</template>

<script setup>
import { reactive } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ plan: Object });
const form = reactive({
  name: props.plan?.name || '',
  price_per_month: props.plan?.price_per_month || 0,
  limit_3d_models: props.plan?.limit_3d_models || 0,
  status: props.plan?.status || 'active',
  featuresText: Array.isArray(props.plan?.features) ? props.plan.features.join('\n') : '',
});

function submit() {
  const payload = {
    ...form,
    features: form.featuresText ? form.featuresText.split('\n').filter(f => f.trim()) : [],
  };
  const routeName = props.plan ? route('admin.agriverse.plans.update', props.plan.id) : route('admin.agriverse.plans.store');
  router[props.plan ? 'put' : 'post'](routeName, payload);
}
</script>
