<template>
  <div class="max-w-2xl mx-auto p-6">
    <h1 class="text-2xl font-bold mb-6">Chuyển khoản ngân hàng</h1>
    <div class="bg-white rounded-lg shadow p-6 space-y-4">
      <div v-for="(value, key) in bankInfo" :key="key" class="flex justify-between border-b pb-2">
        <span class="text-gray-600">{{ key }}</span>
        <span class="font-medium">{{ value }}</span>
      </div>
      <p class="text-sm text-gray-500 mt-4">Vui lòng chuyển khoản đúng số tiền và nội dung để được xác nhận tự động.</p>
      <button class="bg-emerald-600 text-white px-6 py-2 rounded-lg hover:bg-emerald-700"
        @click="confirmPayment">
        Đã chuyển khoản
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  order: Object,
  bankInfo: Object,
})

function confirmPayment() {
  window.location.href = route('payment.confirm')
}
</script>
