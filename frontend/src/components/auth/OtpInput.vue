<script setup>
import {ref} from 'vue';
defineProps({modelValue: {type: String, default: ''}, error: String, disabled: Boolean});
const emit = defineEmits(['update:modelValue']);
const inputs = ref([]);
function write(index, raw, value) {
  const numeric = raw.replace(/[^0-9]/g, '');
  if (numeric.length >= 6) index = 0;
  const digits = numeric.slice(0, 6 - index);
  const chars = value.padEnd(6, ' ').split('');
  if (!digits) chars[index] = ' ';
  else [...digits].forEach((digit, offset) => {chars[index + offset] = digit;});
  emit('update:modelValue', chars.join('').trimEnd());
  if (digits) inputs.value[Math.min(index + digits.length, 5)]?.focus();
}
function backspace(event, index, value) {
  if (event.key === 'Backspace' && !value[index]?.trim() && index > 0) {event.preventDefault(); inputs.value[index - 1]?.focus();}
  if (event.key === 'ArrowLeft' && index > 0) {event.preventDefault(); inputs.value[index - 1]?.focus();}
  if (event.key === 'ArrowRight' && index < 5) {event.preventDefault(); inputs.value[index + 1]?.focus();}
}
</script>
<template>
  <fieldset class="auth-otp" :disabled="disabled" :aria-describedby="error?'otp-error':'otp-help'">
    <legend>Mã xác minh 6 chữ số</legend>
    <div class="auth-otp-digits"><input v-for="(_,index) in 6" :key="index" :ref="element=>inputs[index]=element" :aria-label="'Chữ số '+(index+1)" :aria-invalid="!!error" :aria-describedby="error?'otp-error':'otp-help'" :class="{filled:!!modelValue[index]?.trim()}" :value="modelValue[index]?.trim()||''"
      type="text" inputmode="numeric" :autocomplete="index===0?'one-time-code':'off'" maxlength="6" @focus="$event.target.select()"
      @input="write(index,$event.target.value,modelValue)" @paste.prevent="write(index,$event.clipboardData.getData('text'),modelValue)"
      @keydown="backspace($event,index,modelValue)"/></div>
    <p v-if="error" id="otp-error" class="auth-field-error" role="alert">{{ error }}</p><p v-else id="otp-help" class="auth-help">Sử dụng mã mới nhất. Mã có hiệu lực trong 10 phút.</p>
  </fieldset>
</template>
<style>
.auth-otp{padding:0;border:0;margin:0;min-width:0}.auth-otp legend{font-size:14px;font-weight:600;margin-bottom:10px}.auth-otp-digits{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:9px;margin-bottom:10px}.auth-otp-digits input{width:100%;height:56px;background:var(--bm-surface);border:1px solid var(--bm-border);border-radius:var(--bm-radius);text-align:center;font:inherit;font-size:24px;font-weight:600;font-variant-numeric:tabular-nums;caret-color:var(--bm-forest)}.auth-otp-digits input[aria-invalid=true]{border-color:#a13333}.auth-otp-digits input:disabled{opacity:.7}@media(max-width:380px){.auth-otp-digits{gap:6px}.auth-otp-digits input{height:50px;font-size:22px}}
</style>
