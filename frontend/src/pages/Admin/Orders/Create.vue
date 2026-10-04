<template>
  <AdminLayout>
    <div class="max-w-3xl mx-auto py-8 px-5">
      <h1 class="text-xl font-bold text-[var(--ag-text-primary)] mb-6">Tạo đơn hàng thủ công</h1>
      <form @submit.prevent="submit" class="bg-white rounded-2xl border border-[var(--ag-border)] p-6 space-y-5">
        <div>
          <label class="block text-xs font-semibold text-[var(--ag-text-secondary)] uppercase tracking-[0.08em] mb-1.5">Sản phẩm</label>
          <select v-model="form.product_id" required class="w-full h-12 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8">
            <option value="">Chọn sản phẩm...</option>
            <option v-for="p in products" :key="p.id" :value="p.id">{{ p.name }} ({{ p.store?.name }} - {{ formatPrice(p.price) }}₫)</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-semibold text-[var(--ag-text-secondary)] uppercase tracking-[0.08em] mb-1.5">Khách hàng</label>
          <select v-model="form.buyer_id" required class="w-full h-12 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8">
            <option value="">Chọn khách hàng...</option>
            <option v-for="u in buyers" :key="u.id" :value="u.id">{{ u.name }} ({{ u.email }})</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-semibold text-[var(--ag-text-secondary)] uppercase tracking-[0.08em] mb-1.5">Số lượng</label>
          <input v-model.number="form.quantity" type="number" min="1" required class="w-full h-12 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8">
        </div>
        <div>
          <label class="block text-xs font-semibold text-[var(--ag-text-secondary)] uppercase tracking-[0.08em] mb-1.5">Địa chỉ giao hàng</label>
          <textarea v-model="form.shipping_address" required class="w-full h-24 px-4 py-3 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none resize-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8"></textarea>
        </div>
        <div>
          <label class="block text-xs font-semibold text-[var(--ag-text-secondary)] uppercase tracking-[0.08em] mb-1.5">Ghi chú</label>
          <textarea v-model="form.notes" class="w-full h-20 px-4 py-3 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none resize-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8"></textarea>
        </div>
        <div class="flex gap-3 pt-3">
          <button type="submit" :disabled="loading" class="h-12 px-8 rounded-2xl bg-[var(--ag-primary-500)] text-white text-sm font-semibold hover:bg-[var(--ag-primary-600)] transition-all disabled:opacity-50">
            <span v-if="loading" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin inline-block mr-2" />
            Tạo đơn hàng
          </button>
          <Link :href="route('admin.agriverse.orders.index')" class="h-12 px-8 rounded-2xl border-2 border-[var(--ag-border)] text-[var(--ag-text-secondary)] text-sm font-semibold leading-12 hover:bg-[var(--ag-bg)] transition-all">
            Hủy
          </Link>
        </div>
      </form>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

defineProps({ products: Array, buyers: Array });
const loading = ref(false);
const form = reactive({ product_id: '', buyer_id: '', quantity: 1, shipping_address: '', notes: '' });

function formatPrice(p) { return new Intl.NumberFormat('vi-VN').format(p); }

function submit() {
  loading.value = true;
  router.post(route('admin.agriverse.orders.store'), form, {
    onFinish: () => { loading.value = false; },
  });
}
</script>
