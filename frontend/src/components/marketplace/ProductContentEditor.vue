<template>
  <section class="product-content-editor" :aria-labelledby="prefix+'-heading'">
    <h2 :id="prefix+'-heading'">Mô tả và thông số<span v-if="saved?.name||name"> · {{ saved?.name||name }}</span></h2>
    <p v-if="loading" role="status">Đang tải nội dung sản phẩm…</p>
    <FormFeedback :prefix="prefix" :error="error" :fields="feedbackFields" :success="success"/>
    <button v-if="!loading&&!loaded" class="button button-outline" @click="load">Thử lại</button>
    <form v-if="loaded" class="management-form" :aria-busy="busy" @submit.prevent="save">
      <ManagedField :id="prefix+'-description'" v-model="draft.description" label="Mô tả đầy đủ" multiline :max="16000" :disabled="busy" :error="fields.description" hint="Giữ nội dung của bạn theo từng đoạn. Dùng ## Tiêu đề hoặc - cho danh sách; không nhập HTML."/>
      <fieldset :disabled="busy"><legend>Thông số tùy chọn</legend><p class="muted">Chỉ cung cấp thông tin bạn biết. Để trống các thuộc tính không áp dụng.</p>
        <ManagedField v-for="field in textFields" :id="prefix+'-'+field.key" :key="field.key" v-model="draft[field.key]" :label="field.label" :max="field.max" :multiline="field.multiline" :error="fields['specifications.'+field.key]"/>
        <div class="product-content-measurements"><label :for="prefix+'-heightCm'">Chiều cao cây (cm)<input :id="prefix+'-heightCm'" v-model="draft.heightCm" type="number" min="0.1" max="2000" step="0.1" :aria-invalid="fields['specifications.heightCm']?true:undefined" :aria-describedby="fields['specifications.heightCm']?prefix+'-height-error':undefined"/><span v-if="fields['specifications.heightCm']" :id="prefix+'-height-error'" class="management-field-error">{{ fields['specifications.heightCm'] }}</span></label><label :for="prefix+'-potDiameterCm'">Đường kính chậu (cm)<input :id="prefix+'-potDiameterCm'" v-model="draft.potDiameterCm" type="number" min="0.1" max="500" step="0.1" :aria-invalid="fields['specifications.potDiameterCm']?true:undefined" :aria-describedby="fields['specifications.potDiameterCm']?prefix+'-pot-error':undefined"/><span v-if="fields['specifications.potDiameterCm']" :id="prefix+'-pot-error'" class="management-field-error">{{ fields['specifications.potDiameterCm'] }}</span></label></div>
        <label :for="prefix+'-light'">Ánh sáng<select :id="prefix+'-light'" v-model="draft.light"><option value="">Chưa cung cấp</option><option value="INDIRECT">Ánh sáng gián tiếp</option><option value="PARTIAL_SHADE">Bóng râm một phần</option><option value="SUN">Nắng trực tiếp</option></select></label>
      </fieldset>
      <div class="management-actions"><button type="submit" class="button button-primary" :disabled="busy||!dirty||conflict">{{ busy?'Đang lưu…':'Lưu nội dung' }}</button><button class="button button-outline" type="button" :disabled="busy" @click="cancel">Hủy thay đổi</button></div>
      <button v-if="conflict" class="button button-outline" type="button" :disabled="busy" @click="refresh">Tải nội dung mới để chỉnh sửa lại</button>
    </form>
  </section>
</template>
<script setup>
import {computed,onMounted,ref} from 'vue';import {useManagedForm} from '../../composables/useManagedForm.js';import {marketplaceRequest as request} from '../../services/marketplace.js';import ManagedField from './ManagedField.vue';import FormFeedback from './FormFeedback.vue';
const props=defineProps({productId:{type:Number,required:true},name:String});
const prefix='product-content-'+props.productId,loaded=ref(false),loading=ref(true),conflict=ref(false),saved=ref(null);
const textFields=[{key:'scientificName',label:'Tên khoa học',max:160},{key:'condition',label:'Tình trạng sản phẩm',max:160},{key:'potMaterial',label:'Chất liệu chậu',max:160},{key:'watering',label:'Hướng dẫn tưới nước',max:1000,multiline:true},{key:'substrate',label:'Giá thể',max:300},{key:'origin',label:'Nguồn gốc do bạn khai báo',max:200},{key:'careNotes',label:'Ghi chú chăm sóc',max:2000,multiline:true}];
const keys=[...textFields.map(f=>f.key),'heightCm','potDiameterCm','light'];
const {draft,busy,dirty,error,success,fields,reset,fail,submit}=useManagedForm({description:'',revision:0,...Object.fromEntries(keys.map(k=>[k,'']))},prefix);
const feedbackFields=computed(()=>Object.fromEntries(Object.entries(fields.value).map(([key,value])=>[key.replace(/^specifications\./,''),value])));
const values=result=>({description:result.description||'',revision:result.revision,...Object.fromEntries(keys.map(k=>[k,result.specifications?.[k]==null?'':String(result.specifications[k])]))});
async function load(){loading.value=true;try{saved.value=await request('/seller/products/'+props.productId+'/content');reset(values(saved.value));loaded.value=true;conflict.value=false;}catch(e){await fail(e);}finally{loading.value=false;}}
function cancel(){if(dirty.value&&!window.confirm('Bỏ nội dung chưa lưu?'))return;reset(values(saved.value));success.value='';}
function refresh(){if(!dirty.value||window.confirm('Bỏ bản nháp và tải nội dung mới?'))load();}
function save(){submit(async()=>{try{
  const specifications=Object.fromEntries(keys.map(k=>[k,draft[k]===''?null:['heightCm','potDiameterCm'].includes(k)?Number(draft[k]):draft[k]]));
  saved.value=await request('/seller/products/'+props.productId+'/content','PUT',{description:draft.description,specifications,revision:draft.revision});reset(values(saved.value));success.value='Đã lưu mô tả và thông số.';
}catch(e){conflict.value=e.status===409;throw e;}});}
onMounted(load);
</script>
