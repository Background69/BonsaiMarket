<template>
  <div class="management-field"><label :for="id">{{ label }}<span v-if="required"> (bắt buộc)</span></label>
    <textarea v-if="multiline" :id="id" :value="modelValue" :maxlength="max" :required="required" :disabled="disabled" :aria-invalid="error?true:undefined" :aria-describedby="description" rows="4" @input="$emit('update:modelValue',$event.target.value)"/>
    <input v-else :id="id" :value="modelValue" :type="type" :maxlength="max" :required="required" :disabled="disabled" :autocomplete="autocomplete" :aria-invalid="error?true:undefined" :aria-describedby="description" @input="$emit('update:modelValue',$event.target.value)"/>
    <p v-if="hint" :id="id+'-hint'" class="muted">{{ hint }}</p><p v-if="error" :id="id+'-error'" class="management-field-error">{{ error }}</p>
  </div>
</template>
<script setup>
import {computed} from 'vue';
const props=defineProps({id:{type:String,required:true},label:String,modelValue:String,max:Number,type:{type:String,default:'text'},autocomplete:String,multiline:Boolean,required:Boolean,disabled:Boolean,error:String,hint:String});
defineEmits(['update:modelValue']);
const description=computed(()=>[props.hint?props.id+'-hint':'',props.error?props.id+'-error':''].filter(Boolean).join(' ')||undefined);
</script>
