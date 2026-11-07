<template>
  <fieldset :disabled="disabled" class="pot-specifications-fields"><legend>Thông số chậu</legend>
    <p class="muted">Chỉ điền thông tin thực tế. Các kích thước cũ được giữ khi đổi kiểu dáng và vẫn hiện để bạn kiểm tra.</p>
    <div class="product-content-field-grid">
      <label :for="prefix+'-pot-material'">Chất liệu<select :id="prefix+'-pot-material'" :value="materialChoice" :aria-invalid="!!fields['potSpecs.material']" :aria-describedby="fields['potSpecs.material']?prefix+'-pot-material-error':undefined" @change="chooseMaterial($event.target.value)"><option value="">Chưa cung cấp</option><option v-for="material in materials" :key="material" :value="material">{{ material }}</option><option value="OTHER">Khác</option></select></label>
      <label v-if="materialChoice==='OTHER'" :for="prefix+'-pot-material-other'">Chất liệu khác<input :id="prefix+'-pot-material-other'" :value="modelValue.material" maxlength="160" @input="set('material',$event.target.value)"/></label>
      <label :for="prefix+'-pot-shape'">Kiểu dáng<select :id="prefix+'-pot-shape'" :value="modelValue.shape||''" @change="set('shape',$event.target.value)"><option value="">Chưa cung cấp</option><option v-for="(label,key) in shapes" :key="key" :value="key">{{ label }}</option></select></label>
      <ManagedField :id="prefix+'-pot-color'" :model-value="modelValue.color||''" label="Màu sắc" :max="100" :error="fields['potSpecs.color']" @update:model-value="set('color',$event)"/>
      <label v-for="field in measurements" :key="field.key" :for="prefix+'-pot-'+field.key">{{ field.label }} ({{ field.unit }})<input :id="prefix+'-pot-'+field.key" :value="modelValue[field.key]??''" type="number" :min="field.min" :max="field.max" :step="field.step" :aria-invalid="!!fields['potSpecs.'+field.key]" :aria-describedby="fields['potSpecs.'+field.key]?prefix+'-pot-'+field.key+'-error':undefined" @input="set(field.key,$event.target.value)"/><span v-if="fields['potSpecs.'+field.key]" :id="prefix+'-pot-'+field.key+'-error'" class="management-field-error">{{ fields['potSpecs.'+field.key] }}</span></label>
      <label :for="prefix+'-pot-drainageStatus'">Lỗ thoát nước<select :id="prefix+'-pot-drainageStatus'" :value="modelValue.drainageStatus||''" :aria-invalid="!!fields['potSpecs.drainageConsistent']" :aria-describedby="fields['potSpecs.drainageConsistent']?prefix+'-pot-drainage-error':undefined" @change="set('drainageStatus',$event.target.value)"><option value="">Chưa cung cấp</option><option value="UNKNOWN">Chưa rõ</option><option value="YES">Có</option><option value="NO">Không</option></select></label>
      <label :for="prefix+'-pot-hasTray'">Kèm khay lót<select :id="prefix+'-pot-hasTray'" :value="modelValue.hasTray==null?'':String(modelValue.hasTray)" @change="set('hasTray',$event.target.value===''?null:$event.target.value==='true')"><option value="">Chưa cung cấp</option><option value="true">Có</option><option value="false">Không</option></select></label>
      <ManagedField :id="prefix+'-pot-suitablePlants'" :model-value="modelValue.suitablePlants||''" label="Phù hợp loại cây" :max="1000" hint="Các loại cây bạn đã xác định phù hợp; ngăn cách bằng dấu phẩy." :error="fields['potSpecs.suitablePlants']" @update:model-value="set('suitablePlants',$event)"/>
      <ManagedField :id="prefix+'-pot-usageInstructions'" :model-value="modelValue.usageInstructions||''" label="Hướng dẫn sử dụng" :max="2000" multiline class="product-content-field-wide" :error="fields['potSpecs.usageInstructions']" @update:model-value="set('usageInstructions',$event)"/>
    </div>
    <p v-if="shapeWarning" class="pot-specifications-warning" role="status">{{ shapeWarning }}</p>
    <p v-if="fields['potSpecs.material']" :id="prefix+'-pot-material-error'" class="management-field-error" role="alert">{{ fields['potSpecs.material'] }}</p>
    <p v-if="fields['potSpecs.drainageConsistent']" :id="prefix+'-pot-drainage-error'" class="management-field-error" role="alert">{{ fields['potSpecs.drainageConsistent'] }}</p>
  </fieldset>
</template>
<script setup>
import {computed,ref} from 'vue';import ManagedField from './ManagedField.vue';
const props=defineProps({modelValue:{type:Object,required:true},prefix:{type:String,required:true},disabled:Boolean,fields:{type:Object,default:()=>({})}}),emit=defineEmits(['update:modelValue']);
const materials=['Gốm','Đất nung','Xi măng','Nhựa','Đá','Gỗ'],shapes={ROUND:'Tròn',RECTANGULAR:'Chữ nhật',SQUARE:'Vuông',OVAL:'Bầu dục',OTHER:'Khác'},custom=ref(false);
const materialChoice=computed(()=>custom.value||props.modelValue.material&&!materials.includes(props.modelValue.material)?'OTHER':props.modelValue.material||'');
function set(key,value){emit('update:modelValue',{...props.modelValue,[key]:value});}
function chooseMaterial(value){custom.value=value==='OTHER';if(value!=='OTHER')set('material',value);}
const dimensionLabels={diameterCm:'Đường kính',lengthCm:'Chiều dài',widthCm:'Chiều rộng',heightCm:'Chiều cao'};
const measurements=computed(()=>{
    const rectangular=['RECTANGULAR','SQUARE'].includes(props.modelValue.shape),keys=rectangular?['lengthCm','widthCm','heightCm','diameterCm']:['diameterCm','heightCm','lengthCm','widthCm'];
    return [...keys.map(key=>({key,label:dimensionLabels[key],unit:'cm',min:0.1,max:500,step:0.1})),{key:'drainageHoleCount',label:'Số lỗ thoát nước',unit:'lỗ',min:0,max:1000,step:1},{key:'weightGrams',label:'Khối lượng',unit:'g',min:1,max:1000000,step:1}];
});
const shapeWarning=computed(()=>props.modelValue.shape==='ROUND'&&(props.modelValue.lengthCm||props.modelValue.widthCm)?'Chậu tròn thường dùng đường kính và chiều cao. Bạn vẫn đang có chiều dài/rộng; kiểm tra và xóa thủ công nếu không áp dụng.':['RECTANGULAR','SQUARE'].includes(props.modelValue.shape)&&props.modelValue.diameterCm?'Chậu chữ nhật/vuông thường dùng dài × rộng × cao. Đường kính cũ vẫn được giữ; kiểm tra trước khi lưu.':'');
</script>
<style scoped>.pot-specifications-fields{min-width:0}.pot-specifications-warning{background:var(--bm-warning-surface);color:var(--bm-warning);padding:var(--bm-space-4);line-height:1.6}.pot-specifications-fields label{min-width:0}</style>
