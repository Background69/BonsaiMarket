<template>
  <SellerLayout><div class="management-page"><header><h1>Thông tin gian hàng</h1><p>Thông tin dưới đây hiển thị công khai trên trang gian hàng.</p></header><p v-if="loading" role="status">Đang tải gian hàng…</p><FormFeedback prefix="store" :error="error" :fields="fields" :success="success"/>
    <button v-if="!loading&&!store" class="button button-outline" @click="load">Thử lại</button>
    <form v-if="store" class="management-form" :aria-busy="busy" @submit.prevent="save"><StoreFields :draft="draft" :fields="fields" :busy="busy||loading" storefront/><div class="management-actions"><button type="submit" class="button button-primary" :disabled="busy||loading||!dirty||conflict">{{ busy?'Đang lưu…':'Lưu gian hàng' }}</button><button type="button" class="button button-outline" :disabled="busy" @click="cancel">Hủy thay đổi</button><RouterLink :to="'/stores/'+store.id" class="text-link">Xem trang công khai</RouterLink></div><button v-if="conflict" type="button" class="button button-outline" :disabled="busy" @click="refresh">Tải phiên bản mới để chỉnh sửa lại</button></form>
    <StoreLocationEditor/>
    <CheckoutPackageEditor/>
  </div></SellerLayout>
</template>
<script setup>
import {ref,onMounted} from 'vue';import SellerLayout from './SellerLayout.vue';import StoreFields from '../../../components/marketplace/StoreFields.vue';import FormFeedback from '../../../components/marketplace/FormFeedback.vue';
import {useManagedForm} from '../../../composables/useManagedForm.js';import {marketplaceRequest as request} from '../../../services/marketplace.js';
import StoreLocationEditor from '../../../components/marketplace/StoreLocationEditor.vue';
import CheckoutPackageEditor from '../../../components/marketplace/CheckoutPackageEditor.vue';
const store=ref(null),loading=ref(true),conflict=ref(false);const {draft,busy,dirty,error,success,fields,reset,fail,submit}=useManagedForm({name:'',description:'',phone:'',address:'',logoUrl:'',bannerUrl:'',aboutShop:'',revision:0},'store');
const values=s=>({name:s.name,description:s.description||'',phone:s.phone||'',address:s.address||'',logoUrl:s.logoUrl||'',bannerUrl:s.bannerUrl||'',aboutShop:s.aboutShop||'',revision:s.revision});
async function load(){loading.value=true;try{store.value=await request('/seller/store');reset(values(store.value));conflict.value=false;}catch(e){await fail(e);}finally{loading.value=false;}}
function cancel(){if(dirty.value&&!window.confirm('Bỏ thay đổi gian hàng chưa lưu?'))return;reset(values(store.value));success.value='';}
function refresh(){if(!dirty.value||window.confirm('Bỏ bản nháp và tải thông tin mới?'))load();}
function save(){submit(async()=>{try{store.value=await request('/seller/store','PUT',{...draft});reset(values(store.value));success.value='Đã lưu thông tin gian hàng.';conflict.value=false;}catch(e){conflict.value=e.status===409;throw e;}});}
onMounted(load);
</script>
