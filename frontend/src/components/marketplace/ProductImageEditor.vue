<template>
  <section class="management-image-editor management-page"><h2 id="product-image-heading" tabindex="-1">Ảnh chính của {{ name }}</h2><p>Chọn ảnh JPEG hoặc PNG, tối đa {{ (maxBytes/1048576).toLocaleString('vi-VN',{maximumFractionDigits:2}) }} MB.</p>
    <p v-if="loading" role="status">Đang tải ảnh hiện tại…</p><FormFeedback prefix="image" :error="error" :success="success"/>
    <template v-if="!loading&&ready"><img v-if="preview" :src="preview" class="management-image-preview" alt="Bản xem trước ảnh bạn vừa chọn"/><CatalogImage v-else :src="usableImage(current)" :alt="name" class="management-image-preview"/>
      <form class="management-form" :aria-busy="busy" @submit.prevent="upload"><label class="management-field" for="product-image-file">Chọn ảnh mới<input id="product-image-file" ref="input" type="file" accept="image/jpeg,image/png" :disabled="busy" @change="choose"/></label><p v-if="busy" role="status">{{ progress<100?'Đang tải ảnh lên… '+progress+'%':'Đang xử lý và lưu ảnh…' }}</p><progress v-if="busy" :value="progress" max="100" aria-label="Tiến trình tải ảnh"/><div class="management-actions"><button class="button button-primary" :disabled="busy||!file">{{ busy?'Đang lưu…':current?'Thay ảnh chính':'Tải ảnh lên' }}</button><button type="button" class="button button-outline" :disabled="busy||!file" @click="clear">Bỏ ảnh đã chọn</button><button v-if="current" type="button" class="button button-outline" :disabled="busy" @click="remove">Xóa ảnh chính</button></div></form>
    </template><button class="button button-outline" :disabled="busy" @click="reload">Tải lại ảnh hiện tại</button>
  </section>
</template>
<script setup>
import {ref,onMounted,onBeforeUnmount} from 'vue';import CatalogImage from './CatalogImage.vue';import FormFeedback from './FormFeedback.vue';import {useManagedForm} from '../../composables/useManagedForm.js';import {marketplaceRequest as request} from '../../services/marketplace.js';import {usableImage} from '../../services/catalog.js';import {uploadProductImage} from '../../services/productImage.js';
const props=defineProps({productId:{type:Number,required:true},name:{type:String,required:true}}),current=ref(null),file=ref(null),preview=ref(''),input=ref(null),progress=ref(0),maxBytes=ref(5242880),loading=ref(true),ready=ref(false);
const emit=defineEmits(['updated']);
const {draft,busy,error,success,reset,fail,submit}=useManagedForm({selected:false},'image');
function clear(){if(preview.value)URL.revokeObjectURL(preview.value);preview.value='';file.value=null;if(input.value)input.value.value='';reset({selected:false});}
async function choose(event){const chosen=event.target.files?.[0];clear();success.value='';if(!chosen)return;if(chosen.size>maxBytes.value||!['image/jpeg','image/png'].includes(chosen.type)){await fail(new Error('Chọn ảnh JPEG/PNG không vượt quá giới hạn dung lượng hiển thị.'));return;}file.value=chosen;preview.value=URL.createObjectURL(chosen);draft.selected=true;}
async function load(){loading.value=true;ready.value=false;try{const result=await request('/seller/products/'+props.productId+'/image');current.value=result.imageUrl;maxBytes.value=result.maxBytes||5242880;ready.value=true;}catch(e){await fail(e);}finally{loading.value=false;}}
function reload(){if(!file.value||window.confirm('Bỏ ảnh đang chọn và tải lại?')){clear();success.value='';load();}}
function upload(){if(!file.value)return;submit(async()=>{progress.value=0;const result=await uploadProductImage(props.productId,file.value,p=>progress.value=p);current.value=result.imageUrl;clear();emit('updated',result);success.value='Đã lưu ảnh chính.';});}
function remove(){if(!window.confirm('Xóa ảnh chính của sản phẩm này?'))return;submit(async()=>{const result=await request('/seller/products/'+props.productId+'/image','DELETE');current.value=result.imageUrl;clear();emit('updated',result);success.value='Đã xóa ảnh chính.';});}
onMounted(load);onBeforeUnmount(()=>{if(preview.value)URL.revokeObjectURL(preview.value);});
</script>
