<template>
  <div class="inventory-editor">
    <button v-if="!editing" ref="editButton" class="button button-outline" :disabled="disabled" :aria-label="'Chỉnh tồn kho '+product.name" @click="start">Chỉnh tồn kho</button>
    <form v-else class="inventory-form" novalidate @submit.prevent="save"><label :for="'stock-'+product.productId">Tồn kho mới</label><div class="stock-controls"><input :id="'stock-'+product.productId" ref="input" v-model="value" type="number" inputmode="numeric" min="0" max="1000000" step="1" :disabled="saving || conflict" :aria-invalid="!!error" :aria-describedby="error?'stock-error-'+product.productId:'stock-hint-'+product.productId"/><button v-if="!conflict" class="button button-primary" :disabled="saving || !dirty" type="submit">{{ saving?'Đang lưu…':'Lưu tồn' }}</button><button class="button button-outline" :disabled="saving" type="button" @click="cancel">Hủy</button></div><small :id="'stock-hint-'+product.productId">{{ dirty?'Chưa lưu · ':'' }}Số nguyên từ 0 đến 1.000.000.</small>
      <div v-if="error" :id="'stock-error-'+product.productId" class="marketplace-error" role="alert"><p>{{ error }}</p><template v-if="conflict"><p v-if="latest!==null">Tồn kho mới từ máy chủ: <strong>{{ latest }}</strong>. Bạn đã nhập: {{ value }}.</p><button class="button button-outline" type="button" :disabled="checking" @click="reload">{{ checking?'Đang kiểm tra…':'Tải tồn kho mới' }}</button><button v-if="latest!==null" class="button button-outline" type="button" @click="reenter">Nhập lại từ tồn mới</button></template></div>
    </form><p v-if="notice" class="marketplace-success" role="status">{{ notice }}</p>
  </div>
</template>
<script setup>
import {ref,computed,nextTick,watch} from 'vue';
import {marketplaceRequest as request} from '../../services/marketplace.js';
const props=defineProps({product:{type:Object,required:true},page:{type:Number,default:0},disabled:Boolean}),emit=defineEmits(['updated','editing','saving']);
const editing=ref(false),value=ref(''),expected=ref(props.product.stock),saving=ref(false),error=ref(''),notice=ref(''),conflict=ref(false),latest=ref(null),checking=ref(false),input=ref(null),editButton=ref(null);
const dirty=computed(()=>String(value.value)!==String(expected.value));
watch(editing,v=>emit('editing',v));
async function start(){expected.value=props.product.stock;value.value=String(props.product.stock);error.value='';notice.value='';conflict.value=false;latest.value=null;editing.value=true;await nextTick();input.value?.focus();input.value?.select();}
async function cancel(){if(saving.value)return;if(dirty.value&&!window.confirm('Bỏ thay đổi tồn kho chưa lưu?'))return;editing.value=false;error.value='';conflict.value=false;await nextTick();editButton.value?.focus();}
async function save(){if(saving.value||conflict.value||!dirty.value)return;error.value='';notice.value='';const stock=Number(value.value);if(value.value===''||!Number.isInteger(stock)||stock<0||stock>1000000){error.value='Nhập số nguyên từ 0 đến 1.000.000.';input.value?.focus();return;}saving.value=true;emit('saving',true);
  try{const updated=await request('/seller/inventory/'+props.product.productId,'PUT',{stock,expectedStock:expected.value});emit('updated',updated);editing.value=false;notice.value='Đã lưu tồn kho.';await nextTick();editButton.value?.focus();}
  catch(e){error.value=e.status===403?'Bạn không có quyền cập nhật sản phẩm này.':e.message||'Không thể lưu tồn kho. Kiểm tra dữ liệu trước khi thử lại.';if(e.code==='STOCK_CHANGED'||!e.status){conflict.value=true;error.value=e.code==='STOCK_CHANGED'?'Tồn kho đã thay đổi. Kiểm tra giá trị mới trước khi nhập lại.':'Chưa xác định được kết quả lưu. Tải tồn kho mới để kiểm tra trước khi nhập lại.';await reload();}}
  finally{saving.value=false;emit('saving',false);}}
async function reload(){if(checking.value)return;checking.value=true;try{const page=await request('/seller/inventory?'+new URLSearchParams({page:String(props.page),size:'20'}));const found=page.content.find(p=>p.productId===props.product.productId);if(!found){latest.value=null;error.value='Sản phẩm không còn ở trang này. Hủy chỉnh sửa và tải lại danh sách.';}else{latest.value=found.stock;emit('updated',found);}}catch(e){latest.value=null;error.value=e.message||'Không thể kiểm tra tồn kho. Thử tải lại.';}finally{checking.value=false;}}
async function reenter(){expected.value=latest.value;value.value=String(latest.value);conflict.value=false;error.value='';await nextTick();input.value?.focus();input.value?.select();}
</script>
