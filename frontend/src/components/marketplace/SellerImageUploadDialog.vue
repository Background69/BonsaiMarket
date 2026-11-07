<template>
  <dialog ref="dialog" class="seller-image-dialog" :aria-labelledby="id+'-title'" @cancel.prevent="close" @close="closed" @keydown.tab="trapDialogFocus" @dragover.prevent @drop.prevent>
    <header><div><h2 :id="id+'-title'">Hình ảnh · {{ name }}</h2><p v-if="ready">{{ count }} / 8 ảnh đã lưu. Còn chỗ cho {{ remaining }} ảnh đang chọn thêm.</p><p v-else>Chưa xác định số ảnh trên máy chủ.</p></div><button type="button" class="button button-outline" :disabled="busy" @click="close">Đóng</button></header>
    <p v-if="closeMessage" role="status">{{ closeMessage }}</p><p v-if="loading" role="status">Đang tải gallery…</p>
    <div v-if="error" ref="errorBox" class="gallery-error-summary" role="alert" tabindex="-1"><p>{{ error }}</p><button type="button" class="button button-outline" :disabled="busy" @click="$emit('reload')">Tải lại gallery</button></div>
    <p v-if="message" role="status" aria-atomic="true">{{ message }}</p>
    <div class="gallery-dropzone" :class="{'gallery-dropzone--active':dragDepth>0}" role="group" :aria-labelledby="id+'-drop-title'" :aria-describedby="id+'-drop-help'" @dragenter.prevent="enter" @dragleave.prevent="leave" @dragover.prevent="over" @drop.stop.prevent="drop">
      <h3 :id="id+'-drop-title'">Kéo và thả ảnh vào đây</h3><p>Hoặc chọn nhiều ảnh từ thiết bị của bạn</p>
      <button ref="pickerButton" type="button" class="button button-outline" :disabled="busy||!ready" @click="picker.click()">Chọn ảnh từ máy</button>
      <input ref="picker" type="file" accept="image/jpeg,image/png,image/webp" multiple hidden :disabled="busy||!ready" @change="choose"/>
      <p :id="id+'-drop-help'" class="muted">JPG, PNG, WebP · Tối đa 8 ảnh mỗi sản phẩm · {{ (maxBytes/1048576).toLocaleString('vi-VN',{maximumFractionDigits:2}) }} MB/ảnh. Không nhận SVG.</p>
    </div>
    <slot name="queue"/>
    <div class="gallery-upload-actions"><button type="button" class="button button-primary" :disabled="busy||!ready||!pendingCount" @click="$emit('upload')">{{ uploading?'Đang tải ảnh…':'Tải lên tất cả' }}</button><button type="button" class="button button-outline" :disabled="busy" @click="$emit('clear-uploaded')">Dọn các mục đã tải xong</button></div>
    <div v-if="uploading" class="gallery-overall"><label :for="id+'-overall'">Tiến trình truyền toàn bộ ảnh đã chọn</label><progress :id="id+'-overall'" :value="overall==null?undefined:overall" max="100"/><p>{{ overall===null?'Đang truyền ảnh…':overall+'%' }} · Chỉ ghi “Đã tải lên” khi máy chủ xác nhận lưu.</p></div>
    <slot name="gallery"/>
    <p class="muted">Đóng cửa sổ giữ hàng đợi trong trang này. Khi đang truyền/lưu, chờ yêu cầu kết thúc (thời gian chờ truyền tối đa 60 giây) rồi đóng. Reload hoặc rời trang có thể mất các tệp chưa tải.</p>
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
.seller-image-dialog{width:min(1000px,calc(100% - 24px));max-width:calc(100% - 24px);max-height:92dvh;box-sizing:border-box;padding:var(--bm-space-6);border:1px solid var(--bm-border);border-radius:var(--bm-radius);color:var(--bm-ink);background:var(--bm-surface);overflow:auto;overscroll-behavior:contain}.seller-image-dialog::backdrop{background:rgb(15 30 22 / .65)}.seller-image-dialog header{display:flex;justify-content:space-between;align-items:start;gap:16px}.seller-image-dialog h2{margin:0;font-size:var(--bm-type-subheading);overflow-wrap:anywhere}.seller-image-dialog p{line-height:1.6}.seller-image-dialog header button{flex-shrink:0}.gallery-dropzone{padding:clamp(24px,5vw,48px);margin-block:var(--bm-space-5);border:2px dashed var(--bm-control-border);border-radius:var(--bm-radius);text-align:center;background:var(--bm-bg)}.gallery-dropzone h3{font-size:var(--bm-type-subheading);margin-top:0}.gallery-dropzone--active{border-color:var(--bm-forest);background:var(--bm-soft)}.gallery-error-summary{padding:var(--bm-space-4);color:var(--bm-error);background:var(--bm-error-surface);border-radius:var(--bm-radius-control);margin-block:var(--bm-space-4)}.gallery-upload-actions{display:flex;gap:12px;flex-wrap:wrap;margin-block:20px}.gallery-overall{display:grid;gap:8px;margin-block:20px}.gallery-overall progress{width:100%;height:20px}.seller-image-dialog :is(button,a):focus-visible{outline:var(--bm-focus-width) solid var(--bm-focus);outline-offset:var(--bm-focus-offset)}.seller-image-dialog button{min-height:44px}@media(max-width:600px){.seller-image-dialog{padding:var(--bm-space-4)}.seller-image-dialog header{flex-wrap:wrap}.gallery-dropzone{padding:24px 12px}.gallery-upload-actions button{flex:1 1 100%}}@media(prefers-reduced-motion:reduce){.seller-image-dialog{scroll-behavior:auto}}
</style>
