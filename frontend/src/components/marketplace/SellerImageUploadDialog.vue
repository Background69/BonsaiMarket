<template>
  <dialog ref="dialog" class="seller-image-dialog bm-motion-dialog" :aria-labelledby="id+'-title'" @cancel.prevent="close" @close="closed" @keydown.tab="trapDialogFocus" @dragover.prevent @drop.prevent>
    <header><div><h2 :id="id+'-title'">Hình ảnh sản phẩm</h2><p>{{ name }}</p><div v-if="ready" class="gallery-counters" role="status"><span>Đã lưu <strong>{{ count }}/8</strong></span><span>Chờ tải <strong>{{ pendingCount }}</strong></span><span>Có thể thêm <strong>{{ remaining }}</strong></span></div><p v-else>Chưa xác định số ảnh trên máy chủ.</p></div><button type="button" class="button button-outline" :disabled="busy" @click="close">Đóng</button></header>
    <div class="gallery-dialog-body">
    <p v-if="closeMessage" role="status">{{ closeMessage }}</p><p v-if="loading" role="status">Đang tải gallery…</p>
    <div v-if="error" ref="errorBox" class="gallery-error-summary" role="alert" tabindex="-1"><p>{{ error }}</p><button type="button" class="button button-outline" :disabled="busy" @click="$emit('reload')">Tải lại gallery</button></div>
    <p v-if="message" role="status" aria-atomic="true">{{ message }}</p>
    <div class="gallery-dropzone" :class="{'gallery-dropzone--active':dragDepth>0}" role="group" :aria-labelledby="id+'-drop-title'" :aria-describedby="id+'-drop-help'" @dragenter.prevent="enter" @dragleave.prevent="leave" @dragover.prevent="over" @drop.stop.prevent="drop">
      <svg aria-hidden="true" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.7" viewBox="0 0 24 24"><path d="M12 16V3m-5 5 5-5 5 5M3 14v7h18v-7"/></svg><h3 :id="id+'-drop-title'">Kéo thả hoặc chọn nhiều ảnh</h3>
      <button ref="pickerButton" type="button" class="button button-outline" :disabled="busy||!ready" @click="picker.click()">Chọn ảnh từ máy</button>
      <input ref="picker" type="file" accept="image/jpeg,image/png,image/webp" multiple hidden :disabled="busy||!ready" @change="choose"/>
      <p :id="id+'-drop-help'" class="muted">JPG, PNG, WebP · Tối đa 8 ảnh mỗi sản phẩm · {{ (maxBytes/1048576).toLocaleString('vi-VN',{maximumFractionDigits:2}) }} MB/ảnh. Không nhận SVG.</p>
    </div>
    <slot name="queue"/>
    <div v-if="uploading" class="gallery-overall"><label :for="id+'-overall'">Tiến trình truyền toàn bộ ảnh đã chọn</label><progress :id="id+'-overall'" :value="overall==null?undefined:overall" max="100"/><p>{{ overall===null?'Đang truyền ảnh…':overall+'%' }} · Chỉ ghi “Đã tải lên” khi máy chủ xác nhận lưu.</p></div>
    <slot name="gallery"/>
    <p class="muted gallery-close-help">Đóng cửa sổ giữ hàng đợi trong trang này. Khi đang lưu ảnh, chờ phản hồi trước khi đóng. Reload hoặc rời trang có thể mất các tệp chưa tải.</p>
    </div>
    <footer class="gallery-upload-actions"><button type="button" class="button button-outline" :disabled="busy" @click="$emit('clear-uploaded')">Dọn thông báo đã tải</button><button type="button" class="button button-primary" :disabled="busy||!ready||!pendingCount" @click="$emit('upload')">{{ uploading?'Đang tải ảnh…':'Tải lên tất cả' }}</button></footer>
  </dialog>
</template>
<script setup>
import {nextTick,onBeforeUnmount,ref,useId,watch} from 'vue';import {trapDialogFocus} from '../../utils/dialogFocus.js';
import {lockBodyScroll} from '../../utils/bodyScrollLock.js';
const props=defineProps({name:String,count:Number,remaining:Number,maxBytes:Number,busy:Boolean,loading:Boolean,ready:Boolean,error:String,message:String,pendingCount:Number,uploading:Boolean,overall:Number});
const emit=defineEmits(['files','upload','reload','clear-uploaded','closed']);
const dialog=ref(null),picker=ref(null),pickerButton=ref(null),errorBox=ref(null),dragDepth=ref(0),closeMessage=ref(''),id=useId();let opener,unlock;
async function open(trigger){opener=trigger||document.activeElement;closeMessage.value='';await nextTick();if(!dialog.value.open){unlock=lockBodyScroll();dialog.value.showModal();}}
function close(){if(props.busy){closeMessage.value='Đang xử lý ảnh. Chờ phản hồi hoặc hết thời gian chờ trước khi đóng; hàng đợi sẽ được giữ.';return;}dialog.value.close();}
function closed(){dragDepth.value=0;unlock?.();unlock=undefined;if(opener?.isConnected)opener.focus({preventScroll:true});emit('closed');}
function enter(event){if(event.dataTransfer.types.includes('Files'))dragDepth.value++;}
function leave(){dragDepth.value=Math.max(0,dragDepth.value-1);}
function over(event){if(event.dataTransfer.types.includes('Files'))event.dataTransfer.dropEffect=props.busy?'none':'copy';}
function drop(event){dragDepth.value=0;if(!props.busy&&props.ready&&event.dataTransfer.files.length)emit('files',Array.from(event.dataTransfer.files));}
function choose(event){emit('files',Array.from(event.target.files||[]));event.target.value='';}
watch(()=>props.error,async value=>{if(value&&dialog.value?.open){await nextTick();errorBox.value?.focus();}});
onBeforeUnmount(()=>unlock?.());
async function focusPicker(){await nextTick();pickerButton.value?.focus({preventScroll:true});}
defineExpose({open,focusPicker});
</script>
<style scoped>
.seller-image-dialog{width:min(1080px,calc(100% - 24px));max-width:calc(100% - 24px);height:min(800px,calc(100dvh - 32px));max-height:calc(100dvh - 32px);box-sizing:border-box;padding:0;border:1px solid var(--bm-border);border-radius:var(--bm-radius);color:var(--bm-ink);background:var(--bm-surface);overflow:hidden}.seller-image-dialog[open]{display:grid;grid-template-rows:auto minmax(0,1fr) auto}.seller-image-dialog::backdrop{background:var(--bm-scrim)}.seller-image-dialog header{display:flex;justify-content:space-between;align-items:start;gap:16px;padding:20px 24px;border-bottom:1px solid var(--bm-border)}.seller-image-dialog h2{margin:0;font-size:20px;overflow-wrap:anywhere}.seller-image-dialog p{line-height:1.6;margin:8px 0;font-size:14px}.seller-image-dialog header p{color:var(--bm-muted)}.seller-image-dialog header button{flex-shrink:0}.gallery-counters{display:flex;gap:16px;flex-wrap:wrap;font-size:14px;margin-top:12px;color:var(--bm-muted)}.gallery-counters strong{color:var(--bm-ink);font-variant-numeric:tabular-nums}.gallery-dialog-body{overflow:auto;overscroll-behavior:contain;padding:20px 24px;min-height:0}.gallery-dropzone{display:grid;grid-template-columns:32px minmax(0,1fr) auto;align-items:center;gap:8px 16px;padding:20px;border:1px dashed var(--bm-control-border);border-radius:var(--bm-radius);background:var(--bm-bg);color:var(--bm-forest)}.gallery-dropzone h3{font-size:16px;margin:0}.gallery-dropzone>p{grid-column:2/-1;margin:0;color:var(--bm-muted);font-size:13px}.gallery-dropzone--active{border-color:var(--bm-forest);background:var(--bm-soft)}.gallery-error-summary{padding:16px;color:var(--bm-error);background:var(--bm-error-surface);border-radius:var(--bm-radius-control);margin-block:16px}.gallery-upload-actions{display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;padding:16px 24px;border-top:1px solid var(--bm-border);background:var(--bm-surface)}.gallery-overall{display:grid;gap:8px;margin-block:20px}.gallery-overall progress{width:100%;height:16px;accent-color:var(--bm-forest)}.seller-image-dialog :is(button,a):focus-visible{outline:3px solid var(--bm-focus);outline-offset:3px}.seller-image-dialog button{min-height:44px}.gallery-close-help{color:var(--bm-muted)}@media(max-width:600px){.seller-image-dialog header,.gallery-dialog-body{padding:16px}.seller-image-dialog header{gap:12px}.gallery-dropzone{grid-template-columns:28px minmax(0,1fr);padding:16px;gap:12px}.gallery-dropzone button{grid-column:1/-1}.gallery-dropzone>p{grid-column:1/-1}.gallery-upload-actions{padding:12px 16px}.gallery-upload-actions button{flex:1}.gallery-counters{gap:8px 12px;font-size:13px}}@media(prefers-reduced-motion:reduce){.seller-image-dialog{scroll-behavior:auto}}
</style>
