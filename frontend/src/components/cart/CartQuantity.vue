<script setup>
import {ref, watch} from 'vue';
import {MAX_CART_QUANTITY} from '../../services/cart.js';
const props = defineProps({id: Number, name: String, quantity: Number, stock: Number, disabled: Boolean});
const emit = defineEmits(['change']);
const draft = ref(String(props.quantity)), error = ref('');
watch(() => props.quantity, value => {draft.value = String(value); error.value = '';});
function commit(value, input) {
  if(props.disabled)return;
  const quantity = Number(value), max = Math.min(props.stock ?? 0, MAX_CART_QUANTITY);
  if (!Number.isSafeInteger(quantity) || quantity < 1 || quantity > max) {
    error.value = max > 0 ? `Nhập số nguyên từ 1 đến ${max}.` : 'Sản phẩm hiện không còn hàng.';
  } else if (quantity !== props.quantity) {
    error.value = ''; emit('change', quantity);
  } else error.value = '';
  // Keep the server value until the mutation succeeds, including rejected edits.
  draft.value = String(props.quantity); if (input) input.value = draft.value;
}
</script>
<template>
  <div class="cart-quantity">
    <label :for="'cart-qty-'+id">Số lượng<span class="sr-only"> {{ name }}</span></label>
    <div class="cart-stepper">
      <button type="button" :aria-label="'Giảm số lượng '+name" :disabled="disabled||quantity<=1" @click="commit(quantity-1)">−</button>
      <input :id="'cart-qty-'+id" v-model="draft" type="number" inputmode="numeric" min="1" :max="Math.min(stock??0,MAX_CART_QUANTITY)" step="1"
        :disabled="disabled" :aria-invalid="!!error" :aria-describedby="error?'cart-qty-error-'+id:undefined" @change="commit($event.target.value,$event.target)" @keydown.enter.prevent="commit($event.target.value,$event.target)"/>
      <button type="button" :aria-label="'Tăng số lượng '+name" :disabled="disabled||quantity>=Math.min(stock??0,MAX_CART_QUANTITY)" @click="commit(quantity+1)">+</button>
    </div>
    <p v-if="error" :id="'cart-qty-error-'+id" class="cart-line-error" role="alert">{{ error }}</p>
  </div>
</template>
