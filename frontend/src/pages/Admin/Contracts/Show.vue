<template>
  <AdminLayout>
    <div class="mb-4">
      <h1 class="text-base font-bold text-stone-800">Hợp đồng #{{ contract.uuid || contract.id }}</h1>
    </div>
    <div class="bg-white rounded-xl border border-stone-200 p-4 max-w-2xl">
      <div class="text-xs space-y-2">
        <div class="flex"><span class="w-28 text-stone-500">Sản phẩm:</span><span class="text-stone-800 font-medium">{{ contract.order?.product?.name }}</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Người mua:</span><span>{{ contract.order?.buyer?.name }}</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Người bán:</span><span>{{ contract.order?.seller?.name }}</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Trạng thái:</span><span class="font-semibold" :class="contract.status === 'active' ? 'text-emerald-600' : 'text-amber-600'">{{ contract.status }}</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Ngày tạo:</span><span>{{ contract.created_at }}</span></div>
        <div v-if="contract.signed_at" class="flex"><span class="w-28 text-stone-500">Ngày ký:</span><span>{{ contract.signed_at }}</span></div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
defineProps({ contract: Object });
</script>
