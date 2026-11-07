<template>
  <div class="description-editor">
    <label :for="id">Mô tả đầy đủ</label>
    <p :id="id+'-help'" class="muted">{{ format==='MARKDOWN'?'Định dạng giới hạn: đậm, nghiêng, H2/H3, danh sách và liên kết HTTP/HTTPS. HTML và bảng hiển thị dạng chữ.':'Nội dung cũ được giữ nguyên. Bạn có thể bật định dạng để dùng thanh công cụ.' }}</p>
    <button v-if="format!=='MARKDOWN'" class="button button-outline" type="button" :disabled="disabled" @click="enableFormatting">Bật định dạng mô tả</button>
    <div v-else class="description-toolbar" role="group" aria-label="Định dạng phần văn bản đang chọn">
      <button type="button" :disabled="disabled" @click="wrap('**','**')"><strong>Đậm</strong></button><button type="button" :disabled="disabled" @click="wrap('*','*')"><em>Nghiêng</em></button>
      <button type="button" :disabled="disabled" @click="prefixLines('## ')">H2</button><button type="button" :disabled="disabled" @click="prefixLines('### ')">H3</button>
      <button type="button" :disabled="disabled" @click="prefixLines('- ')">Danh sách •</button><button type="button" :disabled="disabled" @click="prefixLines('1. ')">Danh sách 1.</button>
      <button type="button" :disabled="disabled" @click="linkOpen=!linkOpen">Chèn liên kết</button>
    </div>
    <div v-if="linkOpen" class="description-link"><label :for="id+'-link'">Địa chỉ liên kết<input :id="id+'-link'" v-model="linkUrl" type="url" maxlength="2000" :disabled="disabled" placeholder="https://…"/></label><button class="button button-outline" type="button" :disabled="disabled" @click="insertLink">Chèn vào vị trí đang chọn</button><p v-if="linkError" role="alert">{{ linkError }}</p></div>
    <textarea :id="id" ref="input" :value="modelValue" maxlength="16000" rows="12" :disabled="disabled" :aria-invalid="!!error" :aria-describedby="id+'-help '+id+'-count'+(error?' '+id+'-error':'')" @input="$emit('update:modelValue',$event.target.value)" @select="rememberSelection" @keyup="rememberSelection" @click="rememberSelection"/>
    <p :id="id+'-count'" class="muted">{{ modelValue.length }}/16.000 ký tự</p><p v-if="error" :id="id+'-error'" class="management-field-error" role="alert">{{ error }}</p>
    <div class="management-actions"><button class="button button-outline" type="button" :aria-expanded="preview" :aria-controls="id+'-preview'" @click="preview=!preview">{{ preview?'Ẩn xem trước':'Xem trước' }}</button><button v-if="pot" class="button button-outline" type="button" :disabled="disabled" @click="applyTemplate">Sử dụng mẫu mô tả chậu cây</button></div>
    <p v-if="templateDraft" class="muted" role="status">Mẫu có nội dung hướng dẫn trong ngoặc vuông. Thay bằng thông tin thực tế trước khi lưu.</p>
    <section v-if="preview" :id="id+'-preview'" class="description-preview" aria-label="Xem trước bản nháp mô tả"><SellerText :text="modelValue" :format="format"/></section>
  </div>
</template>
<script setup>
import {nextTick,ref} from 'vue';import SellerText from './SellerText.vue';import {sellerLink} from '../../services/sellerText.js';
const props=defineProps({id:{type:String,required:true},modelValue:{type:String,default:''},format:{type:String,default:'PLAIN'},disabled:Boolean,error:String,pot:Boolean});
const emit=defineEmits(['update:modelValue','update:format']);
const input=ref(null),preview=ref(false),linkOpen=ref(false),linkUrl=ref(''),linkError=ref(''),templateDraft=ref(false);let selection=[0,0];
function rememberSelection(){selection=[input.value.selectionStart,input.value.selectionEnd];}
function enableFormatting(){if(props.modelValue&&!window.confirm('Bật định dạng giới hạn cho nội dung hiện có? Các ký hiệu như ** hoặc ## có thể được hiển thị thành định dạng.'))return;emit('update:format','MARKDOWN');}
async function replace(start,end,text,selectStart=start,selectEnd=start+text.length){if(props.disabled||props.modelValue.length-(end-start)+text.length>16000)return;emit('update:modelValue',props.modelValue.slice(0,start)+text+props.modelValue.slice(end));await nextTick();input.value?.focus();input.value?.setSelectionRange(selectStart,selectEnd);rememberSelection();}
function wrap(before,after){const [start,end]=selection,text=props.modelValue.slice(start,end)||'Nội dung';replace(start,end,before+text+after,start+before.length,start+before.length+text.length);}
function prefixLines(prefix){const [start,end]=selection,from=props.modelValue.lastIndexOf('\n',start-1)+1,next=props.modelValue.indexOf('\n',end),to=next<0?props.modelValue.length:next;const text=props.modelValue.slice(from,to).split('\n').map((line,i)=>(prefix==='1. '?`${i+1}. `:prefix)+line.replace(/^\s*(?:#{1,3}\s+|[-*]\s+|\d+\.\s+)/,'')).join('\n');replace(from,to,text);}
function insertLink(){const url=sellerLink(linkUrl.value);if(!url||/[()]/.test(url)){linkError.value='Nhập liên kết HTTP/HTTPS hợp lệ, không có tài khoản, khoảng trắng hoặc ngoặc tròn.';return;}linkError.value='';wrap('[',']('+url+')');linkOpen.value=false;}
function applyTemplate(){if(props.disabled||props.modelValue.trim()&&!window.confirm('Thay nội dung đang soạn bằng mẫu? Bản mô tả hiện tại sẽ được thay trong bản nháp.'))return;emit('update:format','MARKDOWN');emit('update:modelValue','## Giới thiệu sản phẩm\n\n[Mô tả thiết kế, chất liệu và điểm nổi bật]\n\n## Đặc điểm nổi bật\n\n- [Đặc điểm thực tế 1]\n- [Đặc điểm thực tế 2]\n- [Đặc điểm thực tế 3]\n\n## Cách sử dụng và bảo quản\n\n[Hướng dẫn vệ sinh, sử dụng và bảo quản]\n\n## Đóng gói và lưu ý khi nhận hàng\n\n[Thông tin đóng gói thực tế]');templateDraft.value=true;}
</script>
<style scoped>
.description-editor{display:grid;gap:var(--bm-space-3);min-width:0}.description-toolbar{display:flex;flex-wrap:wrap;gap:var(--bm-space-2)}.description-toolbar button{min-height:44px;padding:8px 12px;border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);background:var(--bm-surface);color:var(--bm-forest);font:inherit}.description-preview{padding:var(--bm-space-5);border:1px solid var(--bm-border);border-radius:var(--bm-radius);background:var(--bm-surface)}.description-link{display:grid;gap:var(--bm-space-3)}button:focus-visible{outline:var(--bm-focus-width) solid var(--bm-focus);outline-offset:var(--bm-focus-offset)}textarea{width:100%;box-sizing:border-box;min-width:0;resize:vertical}
</style>
