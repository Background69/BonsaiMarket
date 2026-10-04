<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4"><h1 class="text-base font-bold text-stone-800">Giao dịch</h1></div>
    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <div class="p-3 border-b border-stone-100 flex gap-2">
        <select v-model="statusFilter" @change="filter" class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
          <option value="">Tất cả</option><option value="paid">Đã thanh toán</option><option value="pending">Chờ thanh toán</option><option value="failed">Thất bại</option>
        </select>
        <select v-model="methodFilter" @change="filter" class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
          <option value="">Phương thức</option><option value="transfer">Chuyển khoản</option><option value="cash">Tiền mặt</option>
        </select>
      </div>
      <table class="w-full text-xs">
        <thead><tr class="bg-stone-50 text-stone-500 text-left"><th class="p-3 font-medium">Mã GD</th><th class="p-3 font-medium">Người dùng</th><th class="p-3 font-medium">Số tiền</th><th class="p-3 font-medium">Phương thức</th><th class="p-3 font-medium">Trạng thái</th><th class="p-3 font-medium"></th></tr></thead>
        <tbody>
          <tr v-for="t in transactions.data" :key="t.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 font-mono text-stone-600">{{ t.transaction_id || '#'+t.id }}</td>
            <td class="p-3 text-stone-800">{{ t.user?.name }}</td>
            <td class="p-3 text-stone-700 font-semibold">{{ formatPrice(t.amount) }}₫</td>
            <td class="p-3 text-stone-500">{{ t.payment_method || '—' }}</td>
            <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="t.payment_status === 'paid' ? 'bg-emerald-50 text-emerald-600' : t.payment_status === 'pending' ? 'bg-amber-50 text-amber-600' : 'bg-red-50 text-red-600'">{{ {paid:'Đã TT',pending:'Chờ',failed:'Thất bại'}[t.payment_status] || t.payment_status }}</span></td>
            <td class="p-3 text-right"><Link :href="route('admin.agriverse.transactions.show', t.id)" class="text-emerald-600 hover:text-emerald-800">Chi tiết</Link></td>
          </tr>
        </tbody>
      </table>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
const props = defineProps({ transactions: Object });
const statusFilter = ref(''); const methodFilter = ref('');
function formatPrice(v) { return new Intl.NumberFormat('vi-VN').format(v); }
function filter() { router.get(route('admin.agriverse.transactions.index'), { status: statusFilter.value, method: methodFilter.value }, { preserveState: true }); }
</script>
