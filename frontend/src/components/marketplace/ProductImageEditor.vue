<template><SellerGalleryManager :product-id="productId" :name="name" @updated="$emit('updated',$event)"/></template>
<script setup>
import SellerGalleryManager from './SellerGalleryManager.vue';
defineProps({productId:{type:Number,required:true},name:{type:String,required:true}});defineEmits(['updated']);
</script>
