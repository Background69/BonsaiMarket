<template>
  <fieldset class="community-image-picker"><legend>Hình ảnh (tùy chọn)</legend><p id="community-image-hint">Tối đa 4 ảnh JPEG/PNG, 5 MB mỗi ảnh. Ảnh được tải sau khi nội dung bài viết được lưu.</p>
    <label for="community-image-files">Chọn ảnh cây của bạn</label><input id="community-image-files" type="file" accept="image/jpeg,image/png" multiple :disabled="disabled" aria-describedby="community-image-hint" @change="pick"/>
    <p v-if="error" role="alert">{{ error }}</p><ul v-if="modelValue.length" class="community-image-previews"><li v-for="(image,i) in modelValue" :key="image.key"><img :src="image.url" :alt="'Ảnh đã chọn '+(i+1)"/><button type="button" class="button button-outline" :disabled="disabled" :aria-label="'Bỏ ảnh đã chọn '+(i+1)" @click="remove(i)">Bỏ ảnh</button></li></ul>
  </fieldset>
</template>
<script setup>
import {ref,onBeforeUnmount} from 'vue';const props=defineProps({modelValue:{type:Array,default:()=>[]},existing:{type:Number,default:0},disabled:Boolean}),emit=defineEmits(['update:modelValue']),error=ref('');const urls=new Set();
function pick(event){error.value='';const list=[...props.modelValue];for(const file of event.target.files){if(list.length+props.existing>=4){error.value='Mỗi bài tối đa 4 ảnh.';break;}if(!['image/png','image/jpeg'].includes(file.type)||file.size>5242880||!file.size){error.value='Chọn ảnh JPEG/PNG có dữ liệu, không quá 5 MB.';continue;}const url=URL.createObjectURL(file);urls.add(url);list.push({file,url,key:crypto.randomUUID()});}event.target.value='';emit('update:modelValue',list);}
function remove(i){const list=[...props.modelValue];URL.revokeObjectURL(list[i].url);urls.delete(list[i].url);list.splice(i,1);emit('update:modelValue',list);}onBeforeUnmount(()=>urls.forEach(url=>URL.revokeObjectURL(url)));
</script>
