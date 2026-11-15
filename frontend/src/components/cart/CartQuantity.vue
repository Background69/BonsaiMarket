<script setup>
import {computed, ref, watch} from 'vue';
import {MAX_CART_QUANTITY} from '../../services/cart.js';
const props = defineProps({id: Number, name: String, quantity: Number, stock: Number, disabled: Boolean});
const emit = defineEmits(['change']);
const draft = ref(String(props.quantity)), error = ref('');
const maximum=computed(()=>Number.isSafeInteger(props.stock)&&props.stock>=0?Math.min(props.stock,MAX_CART_QUANTITY):0);
watch(() => props.quantity, value => {draft.value = String(value); error.value = '';});
function commit(value, input) {
  if(props.disabled)return;
  const quantity = Number(value), max = maximum.value;
  if (!Number.isSafeInteger(quantity) || quantity < 1 || quantity > max) {
    error.value = max > 0 ? `Nhập số nguyên từ 1 đến ${max}.` : Number.isSafeInteger(props.stock)&&props.stock===0?'Sản phẩm hiện không còn hàng.':'Chưa có thông tin tồn kho hợp lệ. Tải lại giỏ hàng để kiểm tra.';
  } else if (quantity !== props.quantity) {
    error.value = ''; draft.value=String(quantity);emit('change', quantity);
  } else error.value = '';
  // Totals still use the server quantity. Keep the attempted draft for correction after an API error.
  if (input) input.value = draft.value;
}
</script>
<template>
  <div class="cart-quantity">
    <label :for="'cart-qty-'+id">Số lượng<span class="sr-only"> {{ name }}</span></label>
    <div class="cart-stepper">
      <button type="button" :aria-label="'Giảm số lượng '+name" :disabled="disabled||maximum<1||quantity<=1" @click="commit(quantity-1)">−</button>
      <input :id="'cart-qty-'+id" v-model="draft" type="number" inputmode="numeric" min="1" :max="maximum" step="1"
        :disabled="disabled" :aria-invalid="!!error" :aria-describedby="error?'cart-qty-error-'+id:undefined" @change="commit($event.target.value,$event.target)" @keydown.enter.prevent="commit($event.target.value,$event.target)"/>
      <button type="button" :aria-label="'Tăng số lượng '+name" :disabled="disabled||maximum<1||quantity>=maximum" @click="commit(quantity+1)">+</button>
    </div>
    <p v-if="error" :id="'cart-qty-error-'+id" class="cart-line-error" role="alert">{{ error }}</p>
  </div>
</template>
