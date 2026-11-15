<template>
  <section class="product-content-editor" :aria-labelledby="prefix+'-heading'">
    <h2 :id="prefix+'-heading'">Mô tả và thông số</h2>
    <p v-if="loading" role="status">Đang tải nội dung sản phẩm…</p>
    <FormFeedback :prefix="prefix" :error="error" :fields="feedbackFields" :success="success"/>
    <button v-if="!loading&&!loaded" class="button button-outline" @click="load">Thử lại</button>
    <form v-if="loaded" class="management-form" :aria-busy="busy" @submit.prevent="save">
      <p v-if="dirty" class="product-content-draft" role="status">Có thay đổi chưa lưu. Nội dung đang nhập được giữ lại nếu lưu chưa thành công.</p>
      <DescriptionEditor :id="prefix+'-description'" v-model="draft.description" v-model:format="draft.descriptionFormat" :disabled="busy||disabled" :error="fields.description" :pot="draft.potEnabled"/>
      <label v-if="!saved?.potCategory&&!saved?.potSpecs" class="checkbox-label"><input v-model="draft.potEnabled" type="checkbox" :disabled="busy"/>Sản phẩm này là chậu cây rời (không phải cây kèm chậu hoặc phụ kiện)</label>
      <p v-if="draft.potEnabled&&categoryType&&categoryType!=='POT'" class="muted">Thông số chậu đã khai báo vẫn được giữ khi đổi danh mục. Chỉ sửa hoặc xóa từng giá trị sau khi kiểm tra.</p>
      <PotSpecificationsFields v-if="draft.potEnabled" v-model="draft.potSpecs" :prefix="prefix" :disabled="busy||disabled" :fields="fields"/>
      <fieldset v-if="!draft.potEnabled" :disabled="busy"><legend>Thông số tùy chọn</legend><p class="muted">Chỉ cung cấp thông tin bạn biết. Để trống các thuộc tính không áp dụng.</p>
        <div class="product-content-field-grid"><ManagedField v-for="field in visibleFields" :id="prefix+'-'+field.key" :key="field.key" v-model="draft[field.key]" :label="field.label" :max="field.max" :multiline="field.multiline" :class="{'product-content-field-wide':field.multiline}" :error="fields['specifications.'+field.key]"/></div>
        <div v-if="plantFields" class="product-content-measurements"><label :for="prefix+'-heightCm'">Chiều cao cây (cm)<input :id="prefix+'-heightCm'" v-model="draft.heightCm" type="number" min="0.1" max="2000" step="0.1" :aria-invalid="fields['specifications.heightCm']?true:undefined" :aria-describedby="fields['specifications.heightCm']?prefix+'-height-error':undefined"/><span v-if="fields['specifications.heightCm']" :id="prefix+'-height-error'" class="management-field-error">{{ fields['specifications.heightCm'] }}</span></label><label :for="prefix+'-potDiameterCm'">Đường kính chậu kèm cây (cm)<input :id="prefix+'-potDiameterCm'" v-model="draft.potDiameterCm" type="number" min="0.1" max="500" step="0.1" :aria-invalid="fields['specifications.potDiameterCm']?true:undefined" :aria-describedby="fields['specifications.potDiameterCm']?prefix+'-pot-error':undefined"/><span v-if="fields['specifications.potDiameterCm']" :id="prefix+'-pot-error'" class="management-field-error">{{ fields['specifications.potDiameterCm'] }}</span></label></div>
        <label v-if="plantFields" :for="prefix+'-light'">Ánh sáng<select :id="prefix+'-light'" v-model="draft.light"><option value="">Chưa cung cấp</option><option value="INDIRECT">Ánh sáng gián tiếp</option><option value="PARTIAL_SHADE">Bóng râm một phần</option><option value="SUN">Nắng trực tiếp</option></select></label>
      </fieldset>
      <div class="management-actions"><button type="submit" class="button button-primary" :disabled="busy||!dirty||conflict">{{ busy?'Đang lưu…':'Lưu nội dung' }}</button><button class="button button-outline" type="button" :disabled="busy" @click="cancel">Hủy thay đổi</button></div>
      <button v-if="conflict" class="button button-outline" type="button" :disabled="busy" @click="refresh">Tải nội dung mới để chỉnh sửa lại</button>
    </form>
  </section>
</template>
<script setup>
import {computed,onMounted,ref,watch} from 'vue';import {useManagedForm} from '../../composables/useManagedForm.js';import {marketplaceRequest as request} from '../../services/marketplace.js';import ManagedField from './ManagedField.vue';import FormFeedback from './FormFeedback.vue';
import DescriptionEditor from './DescriptionEditor.vue';import PotSpecificationsFields from './PotSpecificationsFields.vue';
const props=defineProps({productId:{type:Number,required:true},name:String,initialContent:Object,categoryType:String,disabled:Boolean}),emit=defineEmits(['state','saved']);
const prefix='product-content-'+props.productId,loaded=ref(false),loading=ref(true),conflict=ref(false),saved=ref(null);
const textFields=[{key:'scientificName',label:'Tên khoa học',max:160},{key:'condition',label:'Tình trạng sản phẩm',max:160},{key:'potMaterial',label:'Chất liệu chậu',max:160},{key:'watering',label:'Hướng dẫn tưới nước',max:1000,multiline:true},{key:'substrate',label:'Giá thể',max:300},{key:'origin',label:'Nguồn gốc do bạn khai báo',max:200},{key:'careNotes',label:'Ghi chú chăm sóc',max:2000,multiline:true}];
const keys=[...textFields.map(f=>f.key),'heightCm','potDiameterCm','light'];
const plantFields=computed(()=>!props.categoryType||props.categoryType==='PLANT');
const visibleFields=computed(()=>plantFields.value?textFields:textFields.filter(f=>['condition','origin'].includes(f.key)));
const {draft,busy,dirty,error,success,fields,reset,fail,submit}=useManagedForm({description:'',descriptionFormat:'PLAIN',revision:0,potEnabled:false,potSpecs:{},...Object.fromEntries(keys.map(k=>[k,'']))},prefix);
const feedbackFields=computed(()=>Object.fromEntries(Object.entries(fields.value).map(([key,value])=>[key==='potSpecs.drainageConsistent'?'pot-drainageStatus':key.replace(/^specifications\./,'').replace(/^potSpecs\./,'pot-'),value])));
const values=result=>({description:result.description||'',descriptionFormat:result.descriptionFormat||'PLAIN',revision:result.revision,potEnabled:!!(result.potCategory||result.potSpecs),potSpecs:{...(result.potSpecs||{})},...Object.fromEntries(keys.map(k=>[k,result.specifications?.[k]==null?'':String(result.specifications[k])]))});
async function load(initial){loading.value=true;try{saved.value=initial?.productId===props.productId?initial:await request('/seller/products/'+props.productId+'/content');reset(values(saved.value));loaded.value=true;conflict.value=false;emit('saved',saved.value);}catch(e){await fail(e);}finally{loading.value=false;}}
function cancel(){if(dirty.value&&!window.confirm('Bỏ nội dung chưa lưu?'))return;reset(values(saved.value));success.value='';}
function refresh(){if(!dirty.value||window.confirm('Bỏ bản nháp và tải nội dung mới?'))load();}
async function save(){if(!loaded.value||conflict.value||busy.value)return false;if(!dirty.value)return true;await submit(async()=>{try{
  const specifications=Object.fromEntries(keys.map(k=>[k,draft[k]===''?null:['heightCm','potDiameterCm'].includes(k)?Number(draft[k]):draft[k]]));
  let potSpecs=null;
  if(draft.potEnabled&&(saved.value.potSpecs||Object.values(draft.potSpecs).some(value=>value!==null&&value!==undefined&&value!==''))){
    potSpecs={...draft.potSpecs};
    if(!saved.value.potSpecs&&!potSpecs.material?.trim())throw Object.assign(new Error('Cung cấp chất liệu cho thông số chậu mới.'),{fields:{'potSpecs.material':'Nhập chất liệu thực tế của chậu.'}});
    for(const key of ['diameterCm','lengthCm','widthCm','heightCm','drainageHoleCount','weightGrams']){
      const value=potSpecs[key];
      if(value==null||value==='')potSpecs[key]=null;
      else if(!Number.isFinite(Number(value)))throw Object.assign(new Error('Kiểm tra thông số dạng số.'),{fields:{['potSpecs.'+key]:'Nhập số hợp lệ.'}});
      else potSpecs[key]=Number(value);
    }
    for(const key of ['material','shape','color','drainageStatus','suitablePlants','usageInstructions'])potSpecs[key]=potSpecs[key]?.trim()||null;
    potSpecs.hasTray??=null;
  }
  if(draft.descriptionFormat==='MARKDOWN'&&/\[(?:Mô tả thiết kế, chất liệu và điểm nổi bật|Đặc điểm thực tế [123]|Hướng dẫn vệ sinh, sử dụng và bảo quản|Thông tin đóng gói thực tế)\]/.test(draft.description))throw Object.assign(new Error('Thay nội dung hướng dẫn trong mẫu bằng thông tin sản phẩm thực tế trước khi lưu.'),{fields:{description:'Mẫu vẫn còn placeholder chưa điền.'}});
  saved.value=await request('/seller/products/'+props.productId+'/content','PUT',{description:draft.description,descriptionFormat:draft.descriptionFormat,specifications,potSpecs,revision:draft.revision});reset(values(saved.value));success.value='Đã lưu mô tả và thông số.';emit('saved',saved.value,{updated:true});
}catch(e){conflict.value=e.status===409;throw e;}});return !error.value;}
// Only rebase after this Editor's own identity write. Concurrent versions remain conflicts.
function rebase(from,to){if(draft.revision!==from)return false;const before=JSON.parse(JSON.stringify(draft));saved.value={...saved.value,revision:to};reset(values(saved.value));Object.assign(draft,{...before,revision:to});return true;}
watch([dirty,busy,loaded,conflict],()=>emit('state',{dirty:dirty.value,busy:busy.value,loaded:loaded.value,conflict:conflict.value}),{immediate:true});
watch(()=>props.categoryType,value=>{if(value==='POT'&&loaded.value)draft.potEnabled=true;});
onMounted(()=>load(props.initialContent));
defineExpose({save,rebase});
</script>
