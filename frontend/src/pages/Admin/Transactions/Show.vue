<template>
  <AdminLayout>
    <div class="mb-4"><h1 class="text-base font-bold text-stone-800">Chi tiết giao dịch</h1></div>
    <div class="bg-white rounded-xl border border-stone-200 p-4 max-w-lg">
      <div class="text-xs space-y-2">
        <div class="flex"><span class="w-28 text-stone-500">Mã GD:</span><span class="font-mono">{{ transaction.transaction_id || '—' }}</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Người dùng:</span><span>{{ transaction.user?.name }}</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Số tiền:</span><span class="font-semibold">{{ formatPrice(transaction.amount) }}₫</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Phí:</span><span>{{ formatPrice(transaction.commission_fee) }}₫</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Phương thức:</span><span>{{ transaction.payment_method || '—' }}</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Trạng thái:</span><span class="font-semibold" :class="transaction.payment_status === 'paid' ? 'text-emerald-600' : 'text-amber-600'">{{ transaction.payment_status }}</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Sản phẩm:</span><span>{{ transaction.order?.product?.name || '—' }}</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Người mua:</span><span>{{ transaction.order?.buyer?.name || '—' }}</span></div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
const props = defineProps({ transaction: Object });
function formatPrice(v) { return new Intl.NumberFormat('vi-VN').format(v); }
</script>
