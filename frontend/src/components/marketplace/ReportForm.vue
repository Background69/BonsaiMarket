<template>
  <details class="management-report-entry"><summary>Báo cáo {{ targetType==='PRODUCT'?'sản phẩm':'gian hàng' }}</summary>
    <p v-if="!user"><RouterLink :to="{path:'/login',query:{redirect:route.fullPath}}">Đăng nhập</RouterLink> để gửi báo cáo.</p>
    <form v-else class="management-form management-page" :aria-busy="busy" @submit.prevent="save"><p>Báo cáo được gửi riêng cho quản trị viên. Việc gửi báo cáo không tự động ẩn sản phẩm hay khóa gian hàng.</p>
      <FormFeedback :prefix="prefix" :error="error" :fields="fields" :success="success"/>
      <template v-if="!sent"><label class="management-field" :for="prefix+'-reason'">Lý do<select :id="prefix+'-reason'" v-model="draft.reason" required :disabled="busy"><option value="MISLEADING">Thông tin sai lệch</option><option value="PROHIBITED">Nội dung không phù hợp</option><option value="FRAUD">Dấu hiệu lừa đảo</option><option value="OTHER">Lý do khác</option></select></label>
        <ManagedField :id="prefix+'-description'" v-model="draft.description" label="Mô tả" :max="2000" multiline :required="draft.reason==='OTHER'" :disabled="busy" :error="fields.description" hint="Không đưa mật khẩu, mã OTP hoặc thông tin thanh toán vào báo cáo."/>
        <button class="button button-outline" :disabled="busy">{{ busy?'Đang gửi…':'Gửi báo cáo' }}</button></template>
    </form>
  </details>
</template>
<script setup>
import {ref} from 'vue';import {useRoute} from 'vue-router';import {useAuth} from '../../composables/useAuth.js';import {useManagedForm} from '../../composables/useManagedForm.js';import {marketplaceRequest as request} from '../../services/marketplace.js';import FormFeedback from './FormFeedback.vue';import ManagedField from './ManagedField.vue';
const props=defineProps({targetType:{type:String,required:true},targetId:{type:Number,required:true}}),route=useRoute(),{user}=useAuth(),sent=ref(false),prefix='report-'+props.targetType.toLowerCase();
const {draft,busy,error,success,fields,reset,submit}=useManagedForm({reason:'MISLEADING',description:''},prefix);
function save(){submit(async()=>{await request('/account/reports','POST',{...draft,targetType:props.targetType,targetId:props.targetId});reset({reason:'MISLEADING',description:''});sent.value=true;success.value='Báo cáo đã được ghi nhận để quản trị viên xem xét.';});}
</script>
