<script setup>
import {ref, computed} from 'vue';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faEnvelope, faLock, faEye, faEyeSlash} from '@fortawesome/free-solid-svg-icons';
const props = defineProps({id: {type: String, required: true}, label: String, modelValue: String,
  type: {type: String, default: 'text'}, placeholder: String, autocomplete: String, error: String, help: String, disabled: Boolean, required: Boolean});
const emit = defineEmits(['update:modelValue']);
const visible = ref(false);
const actualType = computed(() => props.type === 'password' && visible.value ? 'text' : props.type);
const inputPlaceholder = computed(() => props.placeholder ?? (props.type === 'email' ? 'Nhập email của bạn' : props.type === 'password' ? 'Nhập mật khẩu của bạn' : props.id === 'name' ? 'Nhập họ và tên của bạn' : undefined));
</script>
<template>
  <div class="auth-field">
    <label :for="id">{{ label }}</label>
    <div class="auth-input-wrap">
      <FontAwesomeIcon v-if="type==='email'||type==='password'" :icon="type==='email'?faEnvelope:faLock" class="auth-input-icon"/>
      <input :id="id" :name="id" :type="actualType" :value="modelValue" :placeholder="inputPlaceholder" :autocomplete="autocomplete" :disabled="disabled" :required="required"
             :aria-invalid="!!error" :aria-describedby="error?id+'-error':help?id+'-help':undefined" :class="{'has-icon':type==='email'||type==='password','has-toggle':type==='password'}"
             :maxlength="type==='email'?255:type==='password'?72:150" @input="emit('update:modelValue',$event.target.value)"/>
      <button v-if="type==='password'" type="button" class="auth-toggle" :aria-label="visible?'Ẩn mật khẩu':'Hiện mật khẩu'" :aria-pressed="visible" :disabled="disabled" @click="visible=!visible"><FontAwesomeIcon :icon="visible?faEyeSlash:faEye"/></button>
    </div>
    <p v-if="error" :id="id+'-error'" class="auth-field-error">{{ error }}</p>
    <p v-else-if="help" :id="id+'-help'" class="auth-help">{{ help }}</p>
  </div>
</template>
<style>
.auth-field{display:flex;flex-direction:column;gap:7px}.auth-field label{font-size:14px;font-weight:600}.auth-input-wrap{position:relative}.auth-input-wrap input{width:100%;min-height:50px;border:1px solid var(--bm-border);border-radius:var(--bm-radius);background:var(--bm-surface);font:inherit;font-size:16px;color:var(--bm-ink);padding:12px 14px;caret-color:var(--bm-forest)}.auth-input-wrap input.has-icon{padding-left:42px}.auth-input-wrap input.has-toggle{padding-right:54px}.auth-input-wrap input[aria-invalid=true]{border-color:#a13333}.auth-input-wrap input:disabled{opacity:.7}.auth-input-icon{position:absolute;left:15px;top:17px;width:16px;height:16px;color:var(--bm-muted);pointer-events:none}.auth-toggle{position:absolute;right:3px;top:3px;width:44px;height:44px;display:grid;place-items:center;background:none;border:0;border-radius:5px;color:var(--bm-muted);cursor:pointer}.auth-toggle svg{width:18px}.auth-field-error{font-size:13px;line-height:1.4;color:#8b2525;margin:0}
</style>
