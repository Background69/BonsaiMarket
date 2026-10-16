<template>
  <ManagedField :id="prefix+'-name'" v-model="draft.name" label="Tên gian hàng" :max="200" required :disabled="busy" :error="fields.name"/>
  <ManagedField :id="prefix+'-description'" v-model="draft.description" label="Giới thiệu gian hàng" :max="4000" multiline :disabled="busy" :error="fields.description"/>
  <ManagedField :id="prefix+'-phone'" v-model="draft.phone" label="Số điện thoại công khai" type="tel" :max="30" :disabled="busy" :error="fields.phone" hint="Thông tin này sẽ hiển thị công khai khi gian hàng hoạt động."/>
  <ManagedField :id="prefix+'-address'" v-model="draft.address" label="Khu vực / địa chỉ công khai" :max="500" :disabled="busy" :error="fields.address" hint="Có thể chỉ ghi tỉnh/thành phố. Chỉ cung cấp địa chỉ bạn muốn công khai."/>
</template>
<script setup>
import ManagedField from './ManagedField.vue';
defineProps({draft:{type:Object,required:true},prefix:{type:String,default:'store'},busy:Boolean,fields:{type:Object,default:()=>({})}});
</script>
