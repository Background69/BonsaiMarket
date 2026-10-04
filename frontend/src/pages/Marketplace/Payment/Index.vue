<template>
  <div class="max-w-4xl mx-auto p-6">
    <h1 class="text-2xl font-bold mb-6">Thanh toán</h1>
    <div v-if="order" class="bg-white rounded-lg shadow p-6 mb-6">
      <h2 class="text-lg font-semibold mb-4">Đơn hàng #{{ order.code }}</h2>
      <p class="text-gray-600">Tổng tiền: <strong>{{ formatPrice(order.total) }}</strong></p>
    </div>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div v-for="method in paymentMethods" :key="method.id"
        class="border rounded-lg p-4 cursor-pointer hover:border-emerald-500 transition"
        @click="selectMethod(method)">
        <h3 class="font-medium">{{ method.name }}</h3>
        <p class="text-sm text-gray-500">{{ method.description }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  order: Object,
  paymentMethods: Array,
  transaction: Object,
})

function formatPrice(price) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
}

function selectMethod(method) {
  window.location.href = route('payment.process', { method: method.id })
}
</script>
