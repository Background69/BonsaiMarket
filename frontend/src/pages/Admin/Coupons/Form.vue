<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">{{ coupon ? 'Sửa mã giảm giá' : 'Thêm mã giảm giá' }}</h1>
    </div>
    <form @submit.prevent="submit" class="bg-white rounded-xl border border-stone-200 p-4 max-w-lg">
      <div class="space-y-3">
        <div class="grid grid-cols-2 gap-3">
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Mã</label><input v-model="form.code" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" required></div>
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Tên</label><input v-model="form.name" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" required></div>
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Mô tả</label><textarea v-model="form.description" rows="2" class="w-full px-3 py-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></textarea></div>
        <div class="grid grid-cols-2 gap-3">
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Loại</label><select v-model="form.type" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"><option value="percent">Phần trăm</option><option value="fixed">Cố định</option></select></div>
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Giá trị</label><input v-model.number="form.value" type="number" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" required></div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Đơn hàng tối thiểu</label><input v-model.number="form.min_order_amount" type="number" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Giảm tối đa</label><input v-model.number="form.max_discount" type="number" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Ngày bắt đầu</label><input v-model="form.starts_at" type="date" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Ngày hết hạn</label><input v-model="form.expires_at" type="date" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Giới hạn sử dụng</label><input v-model.number="form.usage_limit" type="number" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Kích hoạt</label><select v-model="form.is_active" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"><option :value="true">Có</option><option :value="false">Không</option></select></div>
      </div>
      <div class="flex gap-2 mt-4 pt-3 border-t border-stone-100">
        <button type="submit" class="h-9 px-4 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all">{{ coupon ? 'Lưu' : 'Tạo' }}</button>
        <Link :href="route('admin.agriverse.coupons.index')" class="h-9 px-4 rounded-lg border border-stone-300 text-stone-600 text-xs font-bold leading-9 hover:bg-stone-50 transition-all">Hủy</Link>
      </div>
    </form>
  </AdminLayout>
</template>

<script setup>
import { reactive } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ coupon: Object });
const form = reactive({
  code: props.coupon?.code || '', name: props.coupon?.name || '', description: props.coupon?.description || '',
  type: props.coupon?.type || 'percent', value: props.coupon?.value || 0,
  min_order_amount: props.coupon?.min_order_amount || null, max_discount: props.coupon?.max_discount || null,
  usage_limit: props.coupon?.usage_limit || null, starts_at: props.coupon?.starts_at || '', expires_at: props.coupon?.expires_at || '',
  is_active: props.coupon?.is_active ?? true,
});

function submit() {
  const r = props.coupon ? route('admin.agriverse.coupons.update', props.coupon.id) : route('admin.agriverse.coupons.store');
  router[props.coupon ? 'put' : 'post'](r, form);
}
</script>
