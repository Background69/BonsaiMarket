<template>
  <section class="product-information" aria-label="Thông tin chi tiết sản phẩm">
    <div role="tablist" aria-label="Thông tin sản phẩm" class="product-information-tabs" @keydown="move">
      <button v-for="(tab,index) in tabs" :id="'product-tab-'+tab.key" :key="tab.key" :ref="el=>buttons[index]=el" type="button" role="tab"
              :aria-selected="active===tab.key" :aria-controls="panelId(tab.key)" :tabindex="active===tab.key?0:-1" @click="select(tab.key)">
        <MarketIcon :name="tab.icon"/><span>{{ tab.label }}</span>
      </button>
    </div>
    <div v-show="active==='description'" id="product-description" role="tabpanel" aria-labelledby="product-tab-description" tabindex="0" class="product-information-panel">
      <h2 id="product-description-title">Mô tả sản phẩm</h2>
      <SellerText v-if="product.description" :text="product.description" collapsible/>
      <p v-else class="muted">Gian hàng chưa bổ sung mô tả cho sản phẩm này.</p>
    </div>
    <div v-show="active==='specifications'" id="product-specifications" role="tabpanel" aria-labelledby="product-tab-specifications" tabindex="0" class="product-information-panel">
      <h2>Thông tin sản phẩm</h2><p class="muted">Thông số do người bán cung cấp.</p>
      <table class="product-specifications"><caption class="sr-only">Thuộc tính của {{ product.name }}</caption><tbody>
        <tr v-for="row in rows" :key="row.label"><th scope="row">{{ row.label }}</th><td><RouterLink v-if="row.to" :to="row.to">{{ row.value }}</RouterLink><template v-else>{{ row.value }}</template></td></tr>
      </tbody></table>
    </div>
    <div v-show="active==='stores'" id="product-related-stores" role="tabpanel" aria-labelledby="product-tab-stores" tabindex="0" class="product-information-panel">
      <h2>Gian hàng khác{{ related?.category?.name?' có '+related.category.name:'' }}</h2>
      <div v-if="loading" class="related-store-loading" role="status" aria-busy="true"><span/>Đang tìm gian hàng cùng danh mục…</div>
      <div v-else-if="error" role="alert"><p>{{ error }}</p><button type="button" class="button button-outline" @click="load">Thử lại</button></div>
      <div v-else-if="!related?.stores?.length" class="related-store-empty"><p>Chưa có gian hàng khác trong danh mục này.</p><RouterLink v-if="categoryId" :to="{path:'/products',query:{categoryId}}" class="text-link">Khám phá sản phẩm cùng danh mục</RouterLink></div>
      <div v-else class="related-category-stores">
        <article v-for="entry in related.stores" :key="entry.store.id" class="related-category-store">
          <header><CatalogImage :src="usableImage(entry.store.logoUrl)||undefined" :alt="entry.store.name" avatar/><div><h3><RouterLink :to="'/stores/'+entry.store.id">{{ entry.store.name }}</RouterLink></h3><p v-if="entry.store.description" class="related-store-summary">{{ entry.store.description }}</p><p class="muted">{{ entry.matchingProductCount }} sản phẩm cùng danh mục</p></div></header>
          <div class="related-store-products"><ProductCard v-for="p in entry.products" :key="p.id" :product="adaptProduct(p)" streamlined/></div>
          <footer><RouterLink :to="'/stores/'+entry.store.id" class="text-link">Xem gian hàng<MarketIcon name="arrow"/></RouterLink><RouterLink :to="{path:'/stores/'+entry.store.id+'/products',query:{categoryId:related.category.id}}" class="text-link">Xem sản phẩm {{ related.category.name }}</RouterLink></footer>
        </article>
      </div>
    </div>
  </section>
</template>
<script setup>
import {computed,nextTick,onBeforeUnmount,ref,watch} from 'vue';
import api from '../../services/api.js';
import {adaptProduct,usableImage} from '../../services/catalog.js';
import MarketIcon from './MarketIcon.vue';import CatalogImage from './CatalogImage.vue';import SellerText from './SellerText.vue';import ProductCard from '../ProductCard.vue';
const props=defineProps({product:{type:Object,required:true}});
const tabs=[{key:'description',label:'Mô tả',icon:'list'},{key:'specifications',label:'Thông tin sản phẩm',icon:'info'},{key:'stores',label:'Gian hàng liên quan',icon:'store'}];
const active=ref('description'),buttons=[],related=ref(null),loading=ref(false),error=ref('');let generation=0;
const categoryId=computed(()=>props.product.categoryId||related.value?.category?.id);
const panelId=key=>({description:'product-description',specifications:'product-specifications',stores:'product-related-stores'}[key]);
const rows=computed(()=>{
  const p=props.product,s=p.specifications||{},result=[];
  const add=(label,value,to)=>{if(value!==null&&value!==undefined&&String(value).trim()!=='')result.push({label,value,to});};
  add('Danh mục',p.category);add('Tình trạng tồn kho',Number.isInteger(p.stock)&&p.stock>=0?p.stock===0?'Hết hàng':'Còn hàng':'Chưa có thông tin tồn kho');
  if(Number.isInteger(p.stock)&&p.stock>=0)add('Số lượng hiện có',p.stock+' sản phẩm');
  add('Tình trạng sản phẩm',s.condition);add('Tên khoa học',s.scientificName);
  if(typeof s.heightCm==='number')add('Chiều cao cây',s.heightCm+' cm');if(typeof s.potDiameterCm==='number')add('Đường kính chậu',s.potDiameterCm+' cm');
  add('Chất liệu chậu',s.potMaterial);add('Ánh sáng',{INDIRECT:'Ánh sáng gián tiếp',PARTIAL_SHADE:'Bóng râm một phần',SUN:'Nắng trực tiếp'}[s.light]);
  add('Tưới nước',s.watering);add('Giá thể',s.substrate);add('Nguồn gốc do người bán khai báo',s.origin);add('Ghi chú chăm sóc',s.careNotes);
  add('Gian hàng',p.store?.name,Number.isSafeInteger(p.store?.id)&&p.store.id>0?'/stores/'+p.store.id:undefined);add('Khu vực gian hàng',p.store?.address);return result;
});
watch(()=>props.product.id,()=>{generation++;active.value='description';related.value=null;loading.value=false;error.value='';});
onBeforeUnmount(()=>generation++);
async function load(){
  if(loading.value)return;const ticket=++generation,id=props.product.id;loading.value=true;error.value='';
  try{const {data}=await api.get('/products/'+encodeURIComponent(id)+'/related-stores');if(ticket===generation)related.value=data;}
  catch{if(ticket===generation)error.value='Chưa tải được gian hàng liên quan. Vui lòng thử lại.';}
  finally{if(ticket===generation)loading.value=false;}
}
function select(key){active.value=key;if(key==='stores'&&!related.value&&!loading.value)load();}
function move(event){const index=tabs.findIndex(t=>t.key===active.value);let next;
  if(event.key==='ArrowRight')next=(index+1)%tabs.length;else if(event.key==='ArrowLeft')next=(index+tabs.length-1)%tabs.length;else if(event.key==='Home')next=0;else if(event.key==='End')next=tabs.length-1;else return;
  event.preventDefault();select(tabs[next].key);buttons[next]?.focus();
}
async function openDescription(){select('description');await nextTick();const panel=document.getElementById('product-description');panel?.focus({preventScroll:true});panel?.scrollIntoView({block:'start',behavior:'instant'});}
defineExpose({openDescription});
</script>
