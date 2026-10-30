<template>
  <dialog ref="dialog" class="modal-card order-cancel-dialog" aria-labelledby="cancel-title"
          aria-describedby="cancel-description" :aria-busy="busy || undefined"
          tabindex="-1" @cancel="cancel" @close="closed" @click="backdrop" @keydown.tab="trapFocus">
    <form @submit.prevent="$emit('confirm', reason)">
      <h2 id="cancel-title">Xác nhận hủy đơn hàng</h2>
      <p id="cancel-description">Bạn muốn hủy đơn <strong>#{{ code }}</strong>? Kiểm tra lại đơn trước khi xác nhận.</p>
      <label for="cancel-reason">Lý do hủy <span>(không bắt buộc)</span></label>
      <textarea id="cancel-reason" v-model="reason" rows="3" maxlength="1000" :disabled="busy"
                placeholder="Nhập lý do muốn hủy đơn hàng…"/>
      <p v-if="error" class="cancel-error" role="alert">{{ error }}</p>
      <div class="modal-actions">
        <button type="button" class="button button-outline" autofocus :disabled="busy" @click="dismiss">Giữ đơn hàng</button>
        <button type="submit" class="button btn-danger" :disabled="busy">{{ busy ? 'Đang hủy…' : 'Xác nhận hủy' }}</button>
      </div>
    </form>
  </dialog>
</template>
<script setup>
import {nextTick, ref, watch} from 'vue';
const props=defineProps({open:Boolean,code:String,busy:Boolean,error:String});
const emit=defineEmits(['update:open','confirm']);
const dialog=ref(null),reason=ref('');
let opener;
watch(()=>props.open,async open=>{
  if(open){
    opener=document.activeElement;
    reason.value='';
    await nextTick();
    if(props.open&&dialog.value&&!dialog.value.open)dialog.value.showModal();
  }else dialog.value?.close();
});
function dismiss(){if(!props.busy)dialog.value.close();}
function cancel(event){if(props.busy)event.preventDefault();}
function trapFocus(event){
  const controls=[...dialog.value.querySelectorAll('textarea:not(:disabled),button:not(:disabled)')];
  const first=controls[0],last=controls.at(-1);
  if(!first){event.preventDefault();dialog.value.focus();}
  else if(event.shiftKey&&document.activeElement===first){event.preventDefault();last.focus();}
  else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus();}
}
function closed(){
  emit('update:open',false);
  nextTick(()=>{
    if(opener?.isConnected)opener.focus({preventScroll:true});
    else {const main=document.getElementById('market-main');main?.focus({preventScroll:true});}
  });
}
function backdrop(event){
  if(event.target!==dialog.value)return;
  const r=dialog.value.getBoundingClientRect();
  if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)dismiss();
}
</script>
<style scoped>
.order-cancel-dialog {width:min(480px,calc(100% - 32px));max-height:calc(100dvh - 32px);overflow:auto;overscroll-behavior:contain;padding:24px;border:1px solid var(--bm-border);border-radius:var(--bm-radius);background:var(--bm-surface);color:var(--bm-ink);box-shadow:var(--bm-shadow-dialog);font:var(--bm-type-body)/1.6 var(--bm-font);}
.order-cancel-dialog::backdrop {background:var(--bm-scrim);}
.order-cancel-dialog h2 {margin:0 0 12px;font-size:var(--bm-type-subheading);line-height:1.4;}
.order-cancel-dialog p {margin:0 0 20px;color:var(--bm-muted);}
.order-cancel-dialog label {display:block;margin-bottom:8px;font-size:var(--bm-type-label);font-weight:600;}
.order-cancel-dialog label span {color:var(--bm-muted);font-weight:400;}
.order-cancel-dialog textarea {display:block;width:100%;resize:vertical;min-height:104px;max-height:40dvh;padding:12px;border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);background:var(--bm-surface);color:var(--bm-ink);font:inherit;}
.order-cancel-dialog .cancel-error {margin-top:12px;color:var(--bm-error);}
.modal-actions {display:flex;justify-content:flex-end;flex-wrap:wrap;gap:12px;margin-top:24px;}
.btn-danger {color:var(--bm-surface);background:var(--bm-error);}
@media(max-width:480px){.modal-actions {flex-direction:column;}.modal-actions button {width:100%;}}
</style>
