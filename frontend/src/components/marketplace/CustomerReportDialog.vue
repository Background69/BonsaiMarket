<template>
  <div class="customer-report">
    <button type="button" class="button button-outline" @click="open"><MarketIcon name="flag"/> Báo cáo khách hàng</button>
    <dialog ref="dialog" tabindex="-1" aria-labelledby="customer-report-title" :aria-busy="busy" @cancel="cancel" @close="restoreFocus" @keydown.tab="trapDialogFocus">
      <h2 id="customer-report-title">Báo cáo khách hàng</h2>
      <p v-if="loading" role="status">Đang xác minh khách hàng của đơn hàng…</p>
      <p v-if="error" role="alert">{{ error }}</p>
      <p v-if="receipt" role="status">Đã gửi báo cáo #{{ receipt.id }}. {{ receipt.status==='OPEN'?'Báo cáo đang chờ quản trị viên xem xét.':receipt.status==='IN_REVIEW'?'Quản trị viên đang xem xét báo cáo.':'Báo cáo đã được xử lý.' }}</p>
      <form v-if="context?.eligible&&!receipt" @submit.prevent="submit">
        <dl><dt>Khách hàng</dt><dd>{{ context.customerName }}</dd><dt>Đơn hàng liên quan</dt><dd>{{ context.orderCode }}</dd></dl>
        <p class="report-notice">Báo cáo là yêu cầu xem xét, chưa phải kết luận vi phạm. Không đưa mật khẩu, thông tin thanh toán hoặc dữ liệu riêng tư không cần thiết vào mô tả.</p>
        <label for="customer-report-reason">Lý do <span aria-hidden="true">*</span></label>
        <select id="customer-report-reason" v-model="reason" required :disabled="busy||uncertain"><option disabled value="">Chọn lý do</option><option value="HARASSMENT">Hành vi quấy rối</option><option value="PROHIBITED">Giao tiếp hoặc nội dung không phù hợp</option><option value="FRAUD">Nghi ngờ gian lận giao dịch</option><option value="MISLEADING">Thông tin giao dịch sai lệch</option><option value="SPAM">Tin nhắn rác / quảng cáo</option><option value="OTHER">Khác</option></select>
        <label for="customer-report-description">Mô tả {{ reason==='OTHER'?'(bắt buộc)':'(không bắt buộc)' }}</label>
        <textarea id="customer-report-description" v-model="description" rows="5" maxlength="2000" :required="reason==='OTHER'" :disabled="busy||uncertain" aria-describedby="customer-report-count"/>
        <p id="customer-report-count" class="report-notice">{{ description.length }}/2.000 ký tự</p>
        <p v-if="uncertain" role="status">Chưa xác định được kết quả. Thử xác nhận lại dùng đúng mã yêu cầu và nội dung cũ để tránh tạo trùng báo cáo.</p>
        <div class="report-actions"><button type="button" class="button button-outline" :disabled="busy" @click="dialog.close()">Hủy</button><button class="button button-primary" :disabled="busy||!reason">{{ busy?'Đang gửi…':uncertain?'Xác nhận lại yêu cầu':'Gửi báo cáo' }}</button></div>
      </form>
      <p v-else-if="context&&!context.eligible" role="status">Khách hàng trong đơn này không thuộc đối tượng được phép báo cáo.</p>
      <div v-if="!context||receipt||!context.eligible" class="report-actions"><button v-if="error&&!context" class="button button-outline" :disabled="loading" @click="loadContext">Thử lại</button><button class="button button-outline" :disabled="busy" @click="dialog.close()">Đóng</button></div>
    </dialog>
  </div>
</template>
<script setup>
import {ref,nextTick,onBeforeUnmount,watch} from 'vue';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import MarketIcon from './MarketIcon.vue';
import {trapDialogFocus} from '../../utils/dialogFocus.js';
import {lockBodyScroll} from '../../utils/bodyScrollLock.js';
const props=defineProps({orderId:{type:Number,required:true}});
const dialog=ref(null),context=ref(null),loading=ref(false),busy=ref(false),error=ref(''),receipt=ref(null),reason=ref(''),description=ref(''),uncertain=ref(false);
let opener,key,payload,unlock,generation=0;
async function open(event){opener=event.currentTarget;unlock=lockBodyScroll();dialog.value.showModal();await loadContext();await nextTick();dialog.value?.querySelector('select')?.focus();}
async function loadContext(){const ticket=++generation;loading.value=true;error.value='';try{const result=await request('/seller/orders/'+props.orderId+'/customer-report-context');if(ticket===generation)context.value=result;}catch(e){if(ticket===generation)error.value=e.message;}finally{if(ticket===generation)loading.value=false;}}
async function submit(){
  if(busy.value||!context.value?.eligible)return;
  if(reason.value==='OTHER'&&!description.value.trim()){error.value='Vui lòng mô tả lý do khác.';return;}
  key??=crypto.randomUUID();
  if(!uncertain.value)payload={targetType:'USER',targetId:context.value.customerId,contextType:'ORDER',contextId:props.orderId,reason:reason.value,description:description.value,requestKey:key};
  busy.value=true;error.value='';const ticket=generation;
  try{const saved=await request('/account/reports','POST',payload,false);if(ticket===generation){receipt.value=saved;uncertain.value=false;}}
  catch(e){if(ticket===generation){error.value=e.message;uncertain.value=!e.status||e.status>=500;if(!uncertain.value)key=undefined;}}
  finally{if(ticket===generation)busy.value=false;}
}
function cancel(event){if(busy.value)event.preventDefault();}
function restoreFocus(){unlock?.();unlock=undefined;opener?.isConnected&&opener.focus({preventScroll:true});}
watch(()=>props.orderId,()=>{generation++;dialog.value?.close();context.value=null;receipt.value=null;reason.value='';description.value='';error.value='';uncertain.value=false;busy.value=false;key=undefined;payload=undefined;});
onBeforeUnmount(()=>{generation++;dialog.value?.close();unlock?.();});
</script>
<style scoped>
.customer-report dialog{width:min(560px,calc(100% - 32px));max-height:calc(100dvh - 40px);overflow:auto;padding:28px;border:1px solid var(--bm-border);border-radius:16px;background:var(--bm-surface);color:var(--bm-ink);box-shadow:var(--bm-shadow-dialog);font:15px/1.6 var(--bm-font)}.customer-report dialog::backdrop{background:var(--bm-scrim)}.customer-report h2{font-size:24px;margin:0 0 16px;color:var(--bm-forest)}.customer-report label{display:block;font-weight:600;margin:16px 0 8px}.customer-report textarea,.customer-report select{display:block;width:100%;font:inherit;border:1px solid var(--bm-control-border);border-radius:8px;padding:10px;background:white;color:var(--bm-ink)}.customer-report dl{display:grid;grid-template-columns:auto 1fr;gap:8px 20px}.customer-report dd{margin:0;font-weight:600;overflow-wrap:anywhere}.customer-report dt,.report-notice{color:var(--bm-muted)}.report-notice{font-size:13px}.report-actions{display:flex;flex-wrap:wrap;justify-content:flex-end;gap:12px;margin-top:24px}.customer-report [role=alert]{color:var(--bm-error)}.customer-report :focus-visible{outline:3px solid var(--bm-forest);outline-offset:3px}
</style>
