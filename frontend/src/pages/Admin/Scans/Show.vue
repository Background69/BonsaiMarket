<template>
  <AdminLayout>
    <div class="mb-4"><h1 class="text-base font-bold text-stone-800">Chi tiết Scan #{{ job.id }}</h1></div>
    <div class="bg-white rounded-xl border border-stone-200 p-4 max-w-lg">
      <div class="text-xs space-y-2">
        <div class="flex"><span class="w-28 text-stone-500">Cửa hàng:</span><span>{{ job.store?.name }}</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Sản phẩm:</span><span>{{ job.asset?.product?.name || '—' }}</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Trạng thái:</span><span class="font-semibold" :class="job.status === 'completed' ? 'text-emerald-600' : job.status === 'failed' ? 'text-red-600' : 'text-amber-600'">{{ job.status }}</span></div>
        <div class="flex"><span class="w-28 text-stone-500">Ngày tạo:</span><span>{{ job.created_at }}</span></div>
        <div v-if="job.completed_at" class="flex"><span class="w-28 text-stone-500">Hoàn thành:</span><span>{{ job.completed_at }}</span></div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
defineProps({ job: Object });
</script>
