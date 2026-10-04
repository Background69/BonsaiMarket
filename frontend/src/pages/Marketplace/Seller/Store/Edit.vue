<template>
  <SellerLayout>
    <div class="max-w-2xl">
      <h1 class="text-2xl font-semibold mb-8" style="color: var(--ag-on-surface); font-family: var(--ag-font-display);">Thông tin cửa hàng</h1>

      <form @submit.prevent="submit" class="space-y-6">
        <div>
          <label class="text-sm font-medium mb-1.5 block">Tên cửa hàng <span class="text-red-500">*</span></label>
          <input v-model="form.name" class="w-full h-11 px-4 rounded-xl border text-sm outline-none focus:border-[var(--ag-primary-500)]" style="border-color: var(--ag-border);">
        </div>

        <div>
          <label class="text-sm font-medium mb-1.5 block">Mô tả</label>
          <textarea v-model="form.description" rows="4" class="w-full px-4 py-3 rounded-xl border text-sm outline-none focus:border-[var(--ag-primary-500)] resize-none" style="border-color: var(--ag-border);"></textarea>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="text-sm font-medium mb-1.5 block">Số điện thoại</label>
            <input v-model="form.phone" class="w-full h-11 px-4 rounded-xl border text-sm outline-none focus:border-[var(--ag-primary-500)]" style="border-color: var(--ag-border);">
          </div>
          <div>
            <label class="text-sm font-medium mb-1.5 block">Logo (URL)</label>
            <input v-model="form.logo" class="w-full h-11 px-4 rounded-xl border text-sm outline-none focus:border-[var(--ag-primary-500)]" style="border-color: var(--ag-border);">
          </div>
        </div>

        <div>
          <label class="text-sm font-medium mb-1.5 block">Địa chỉ</label>
          <input v-model="form.address" class="w-full h-11 px-4 rounded-xl border text-sm outline-none focus:border-[var(--ag-primary-500)]" style="border-color: var(--ag-border);">
        </div>

        <div class="rounded-2xl border p-5" style="border-color: var(--ag-border);">
          <h3 class="text-sm font-semibold mb-4">Thông tin thanh toán</h3>
          <div class="space-y-4">
            <div>
              <label class="text-sm font-medium mb-1.5 block">Ngân hàng</label>
              <input v-model="form.bank_name" class="w-full h-11 px-4 rounded-xl border text-sm outline-none focus:border-[var(--ag-primary-500)]" style="border-color: var(--ag-border);" placeholder="VD: Vietcombank">
            </div>
            <div>
              <label class="text-sm font-medium mb-1.5 block">Chủ tài khoản</label>
              <input v-model="form.bank_account_name" class="w-full h-11 px-4 rounded-xl border text-sm outline-none focus:border-[var(--ag-primary-500)]" style="border-color: var(--ag-border);">
            </div>
            <div>
              <label class="text-sm font-medium mb-1.5 block">Số tài khoản</label>
              <input v-model="form.bank_account_number" class="w-full h-11 px-4 rounded-xl border text-sm outline-none focus:border-[var(--ag-primary-500)]" style="border-color: var(--ag-border);">
            </div>
          </div>
        </div>

        <button type="submit" :disabled="saving" class="px-6 py-2.5 rounded-xl text-sm font-semibold text-white disabled:opacity-50" style="background: var(--ag-primary-500);">
          {{ saving ? 'Đang lưu...' : 'Lưu thay đổi' }}
        </button>
      </form>
    </div>
  </SellerLayout>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { router } from '@inertiajs/vue3'
import SellerLayout from '../SellerLayout.vue'

const props = defineProps({
  store: { type: Object, required: true },
})

const saving = ref(false)

const form = reactive({
  name: props.store.name || '',
  description: props.store.description || '',
  phone: props.store.phone || '',
  logo: props.store.logo || '',
  address: props.store.address || '',
  bank_name: props.store.bank_name || '',
  bank_account_name: props.store.bank_account_name || '',
  bank_account_number: props.store.bank_account_number || '',
})

function submit() {
  saving.value = true
  router.put(route('agriverse.shop.seller.store.update'), form, {
    preserveScroll: true,
    onSuccess: () => { saving.value = false },
    onError: () => { saving.value = false },
  })
}
</script>
