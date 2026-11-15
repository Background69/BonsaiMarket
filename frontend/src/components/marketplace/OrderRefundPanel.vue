<template>
  <section class="market-panel refund-panel" aria-labelledby="refund-title" :aria-busy="loading||busy">
    <h2 id="refund-title">Hồ sơ hoàn tiền</h2>
    <p>Hủy đơn và hoàn tiền là hai quy trình riêng. Hồ sơ nội bộ chưa xác nhận tiền đã về tài khoản.</p>
    <p v-if="loading" role="status">Đang tải hồ sơ…</p>
    <div v-else-if="loadError" role="alert"><p>{{ loadError }}</p><button type="button" class="button button-outline" @click="load">Thử lại</button></div>
    <template v-else-if="data">
      <div v-if="data.sandboxWalletRefund" role="status"><strong>{{ data.sandboxWalletRefund.status==='COMPLETED'?'Đã hoàn tiền mô phỏng vào ví':'Hoàn tiền mô phỏng đang xử lý' }}</strong><p>{{ money(data.sandboxWalletRefund.amount) }} · Giao dịch {{ data.sandboxWalletRefund.id }}</p><RouterLink to="/wallet">Xem sổ giao dịch ví thử nghiệm</RouterLink><p>Không có hoàn tiền thật hoặc hoàn về ngân hàng.</p></div>
      <template v-if="data.refund">
        <p><strong>{{ labelFor(labels,data.refund.status,'Tiến độ cần đối chiếu') }}</strong> · {{ money(data.refund.amount) }} · {{ paymentMethodLabel(data.refund.method) }}</p>
        <p>Lý do: {{ data.refund.reason }}</p>
        <ol><li v-for="(event,i) in data.refund.history" :key="i"><strong>{{ labelFor(labels,event.status,'Tiến độ cần đối chiếu') }}</strong> · <time :datetime="event.createdAt+'Z'">{{ date(event.createdAt) }}</time><p>{{ event.reason }}</p></li></ol>
      </template>
      <p v-else-if="!data.canRequest">{{ data.blocker || 'Chưa có hồ sơ hoàn tiền.' }}</p>
      <form v-if="data.canRequest || (admin && actions.length)" @submit.prevent="prepare">
        <template v-if="admin && data.refund"><label for="refund-action">Cập nhật hồ sơ</label><select id="refund-action" v-model="target" :disabled="busy" required><option value="" disabled>Chọn thao tác</option><option v-for="action in actions" :key="action" :value="action">{{ labels[action] }}</option></select></template>
        <label for="refund-reason">{{ data.canRequest?'Lý do yêu cầu xem xét':'Lý do xử lý' }}</label><textarea id="refund-reason" v-model="reason" required maxlength="1000" rows="3" :disabled="busy"/>
        <template v-if="data.canRequest"><label for="refund-amount">Số tiền đề nghị (VND, để trống để xem xét toàn bộ)</label><input id="refund-amount" v-model="amount" type="number" min="1" step="1" inputmode="numeric" :disabled="busy" aria-describedby="refund-policy"/><p id="refund-policy">Hoàn một phần chỉ được xem xét sau trả hàng đã kiểm tra. Số tiền và nguồn thanh toán được kiểm tra trên máy chủ.</p></template>
        <p v-if="error" role="alert" class="management-error">{{ error }}</p>
        <button class="button button-outline" :disabled="busy || !reason.trim()">{{ data.canRequest?'Yêu cầu xem xét hoàn tiền':'Kiểm tra trước khi cập nhật' }}</button>
      </form>
      <p v-if="success" role="status">{{ success }}</p>
      <button type="button" class="button button-outline" :disabled="busy || loading" @click="load">Cập nhật tiến độ hồ sơ</button>
    </template>
    <dialog ref="dialog" aria-labelledby="refund-confirm-title" tabindex="-1" @keydown.tab="trapDialogFocus" @cancel="busy && $event.preventDefault()" @close="opener?.focus()">
      <h2 id="refund-confirm-title">Xác nhận thao tác hoàn tiền?</h2><p>{{ target==='EXECUTE'?'Gửi lệnh hoàn theo phương thức gốc. VNPAY chỉ dùng Sandbox; ví chỉ ghi sổ tiền mô phỏng. Nếu kết quả chưa rõ, cần đối soát và không gửi lại lệnh.':(data?.canRequest?'Lập yêu cầu xem xét':labels[target])+'. Thao tác này chỉ cập nhật hồ sơ.' }}</p>
      <p v-if="error" role="alert">{{ error }}</p><div class="management-actions"><button type="button" class="button button-outline" autofocus :disabled="busy" @click="dialog.close()">Quay lại</button><button type="button" class="button button-primary" :disabled="busy" @click="submit">{{ busy?'Đang lưu…':'Xác nhận' }}</button></div>
    </dialog>
  </section>
</template>
<script setup>
import {labelFor,paymentMethodLabel} from '../../services/presentation.js';

import {computed,onBeforeUnmount,ref,watch} from 'vue';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import {useAuth} from '../../composables/useAuth.js';
import {formatPrice} from '../../utils.js';
import {trapDialogFocus} from '../../utils/dialogFocus.js';
const props=defineProps({endpoint:String,orderId:[Number,String]}),{user}=useAuth();
const data=ref(null),loading=ref(false),busy=ref(false),loadError=ref(''),error=ref(''),reason=ref(''),target=ref(''),success=ref(''),dialog=ref(null);let generation=0,opener;
const amount=ref('');
const admin=computed(()=>props.endpoint==='/admin/orders');
const labels={REQUESTED:'Chờ xét duyệt',APPROVED:'Đã duyệt hồ sơ',DENIED:'Từ chối yêu cầu',PROCESSING:'Đang xử lý nội bộ',AWAITING_EXTERNAL:'Chờ xác minh bên ngoài',UNKNOWN:'Chưa rõ kết quả · cần đối soát',FAILED:'Xử lý thất bại · cần đối soát',SUCCEEDED:'Đã xác minh hoàn tiền',RECONCILED:'Đã đối soát'};
labels.EXECUTE='Thực thi hoàn theo phương thức gốc';
labels.RECONCILE='Đối soát kết quả hoàn VNPAY';
const actions=computed(()=>({REQUESTED:['APPROVED','DENIED'],APPROVED:['EXECUTE','PROCESSING'],PROCESSING:['RECONCILE','AWAITING_EXTERNAL','UNKNOWN','FAILED'],AWAITING_EXTERNAL:['RECONCILE','UNKNOWN','FAILED'],UNKNOWN:['RECONCILE']}[data.value?.refund?.status]||[]));
const money=v=>formatPrice(v)+' ₫',date=v=>new Date(v+'Z').toLocaleString('vi-VN');
async function load(){const ticket=++generation;loading.value=true;loadError.value='';try{const result=await request(props.endpoint+'/'+props.orderId+'/refund');if(ticket===generation)data.value=result;}catch(e){if(ticket===generation)loadError.value=e.message;}finally{if(ticket===generation)loading.value=false;}}
function prepare(){if(busy.value)return;if(data.value.canRequest&&amount.value!==''&&(!Number.isSafeInteger(Number(amount.value))||Number(amount.value)<1)){error.value='Nhập số tiền nguyên VND dương hợp lệ.';return;}opener=document.activeElement;dialog.value.showModal();}
async function submit(){if(busy.value)return;const ticket=generation;busy.value=true;error.value='';try{const value=data.value.canRequest?await request(props.endpoint+'/'+props.orderId+'/refund','POST',{reason:reason.value,amount:amount.value===''?null:Number(amount.value)},false):['EXECUTE','RECONCILE'].includes(target.value)?await request('/admin/refunds/'+data.value.refund.id+(target.value==='EXECUTE'?'/execute':'/reconcile'),'POST',{revision:data.value.refund.revision},false):await request('/admin/refunds/'+data.value.refund.id+'/status','POST',{status:target.value,reason:reason.value,revision:data.value.refund.revision},false);if(ticket!==generation)return;data.value=data.value.canRequest?value:{...data.value,refund:value};reason.value='';target.value='';success.value=data.value.refund?.status==='SUCCEEDED'?'Đã xác minh kết quả trong môi trường thử nghiệm. Xem lịch sử và phương thức hoàn.':'Đã lưu hồ sơ. Chưa xác nhận hoàn tiền.';dialog.value.close();}catch(e){if(ticket===generation)error.value=e.message;}finally{if(ticket===generation)busy.value=false;}}
watch([()=>props.orderId,()=>props.endpoint,()=>user.value?.id],()=>{dialog.value?.close();data.value=null;amount.value='';reason.value='';error.value='';success.value='';target.value='';busy.value=false;load();},{immediate:true});onBeforeUnmount(()=>generation++);
</script>
<style scoped>
.refund-panel{margin-block:24px;overflow-wrap:anywhere}.refund-panel>p,.refund-panel ol{max-width:75ch}.refund-panel h2{font-size:var(--bm-type-section)}.refund-panel form{display:grid;gap:12px;max-width:640px}.refund-panel label{font-weight:600}.refund-panel textarea,.refund-panel select,.refund-panel input{width:100%;padding:12px;border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);color:var(--bm-ink);background:var(--bm-surface);font:inherit}.refund-panel li{margin-block:16px}.refund-panel dialog{width:min(520px,calc(100% - 32px));max-height:calc(100dvh - 32px);overflow:auto;padding:24px;border:1px solid var(--bm-border);border-radius:var(--bm-radius);color:var(--bm-ink);background:var(--bm-surface);box-shadow:var(--bm-shadow-dialog)}.refund-panel dialog::backdrop{background:var(--bm-scrim)}.refund-panel :focus-visible{outline:3px solid var(--bm-forest);outline-offset:3px}
</style>
