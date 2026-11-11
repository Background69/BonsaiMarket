<template>
  <section class="product-information" aria-label="Thông tin chi tiết sản phẩm">
    <section id="product-description" aria-labelledby="product-description-title" class="bm-product-detail-card detail-description" :class="{'has-highlights':highlights.length,'description-no-image':!usableImage(product.image)}" tabindex="-1">
      <CatalogImage v-if="usableImage(product.image)" :src="usableImage(product.image)" :alt="product.name" fit="cover"/>
      <div><h2 id="product-description-title" tabindex="-1"><MarketIcon name="leaf"/> Giới thiệu sản phẩm</h2><SellerText v-if="product.description" :text="product.description" :format="product.descriptionFormat" collapsible/><p v-else class="muted">Gian hàng chưa bổ sung mô tả cho sản phẩm này.</p></div>
      <aside v-if="highlights.length" class="detail-highlights"><h3><MarketIcon name="sprout"/> Thông tin nổi bật</h3><p v-for="row in highlights" :key="row.label"><MarketIcon name="check"/><span>{{ row.label }}: {{ row.value }}</span></p></aside>
    </section>
    <section id="product-specifications" aria-labelledby="product-specifications-title" class="bm-product-detail-card" tabindex="-1">
      <h2 id="product-specifications-title" tabindex="-1"><MarketIcon name="info"/> Thông số kỹ thuật</h2>
      <p v-if="(product.potCategory||product.potSpecs)&&!potRows.length" class="muted">Gian hàng chưa bổ sung thông số chậu có cấu trúc.</p>
      <dl class="detail-specifications"><div v-for="row in rows" :key="row.label"><dt><MarketIcon name="leaf"/>{{ row.label }}</dt><dd><RouterLink v-if="row.to" :to="row.to">{{ row.value }}</RouterLink><template v-else>{{ row.value }}</template></dd></div></dl>
      <p class="detail-specification-note">Thông số do người bán cung cấp.</p>
    </section>
    <details class="detail-related-stores" @toggle="event=>{if(event.target.open&&!related&&!loading)load()}"><summary>Khám phá gian hàng liên quan</summary><div id="product-related-stores" class="bm-product-detail-card">
      <h2>Gian hàng khác{{ related?.category?.name?' có '+related.category.name:'' }}</h2>
      <div v-if="loading" class="related-store-loading" role="status" aria-busy="true"><span/>Đang tìm gian hàng cùng danh mục…</div>
      <div v-else-if="error" role="alert"><p>{{ error }}</p><button type="button" class="button button-outline" @click="load">Thử lại</button></div>
      <div v-else-if="!related?.stores?.length" class="related-store-empty"><p>Chưa có gian hàng khác trong danh mục này.</p><RouterLink v-if="categoryId" :to="{path:'/products',query:{categoryId}}" class="text-link">Khám phá sản phẩm cùng danh mục</RouterLink></div>
      <div v-else class="related-category-stores">
        <article v-for="entry in related.stores" :key="entry.store.id" class="related-category-store">
          <header><CatalogImage :src="usableImage(entry.store.logoUrl)||undefined" :alt="entry.store.name" avatar/><div><h3><RouterLink :to="'/stores/'+entry.store.id">{{ entry.store.name }}</RouterLink></h3><p v-if="entry.store.description" class="related-store-summary">{{ entry.store.description }}</p><p class="muted">{{ entry.matchingProductCount }} sản phẩm cùng danh mục</p></div></header>
          <div class="related-store-products"><ProductCard v-for="p in entry.products" :key="p.id" :product="adaptProduct(p)" streamlined actual-only/></div>
          <footer><RouterLink :to="'/stores/'+entry.store.id" class="text-link">Xem gian hàng<MarketIcon name="arrow"/></RouterLink><RouterLink :to="{path:'/stores/'+entry.store.id+'/products',query:{categoryId:related.category.id}}" class="text-link">Xem sản phẩm {{ related.category.name }}</RouterLink></footer>
        </article>
      </div>
    </div></details>
  </section>
</template>
<script setup>
import {computed,nextTick,onBeforeUnmount,ref,watch} from 'vue';
import api from '../../services/api.js';
import {adaptProduct,usableImage} from '../../services/catalog.js';
import {potSpecificationRows} from '../../services/potSpecifications.js';
import MarketIcon from './MarketIcon.vue';import CatalogImage from './CatalogImage.vue';import SellerText from './SellerText.vue';import ProductCard from '../ProductCard.vue';
const props=defineProps({product:{type:Object,required:true}});
const potRows=computed(()=>potSpecificationRows(props.product.potSpecs));
const highlights=computed(()=>rows.value.filter(row=>!row.to&&!['Danh mục','Tình trạng tồn kho','Số lượng hiện có','Khu vực gian hàng'].includes(row.label)).slice(0,4));
const related=ref(null),loading=ref(false),error=ref('');let generation=0;
const categoryId=computed(()=>props.product.categoryId||related.value?.category?.id);
const rows=computed(()=>{
  const p=props.product,s=p.specifications||{},result=[];
  const add=(label,value,to)=>{if(value!==null&&value!==undefined&&String(value).trim()!=='')result.push({label,value,to});};
  add('Danh mục',p.category);add('Tình trạng tồn kho',Number.isInteger(p.stock)&&p.stock>=0?p.stock===0?'Hết hàng':'Còn hàng':'Chưa có thông tin tồn kho');
  if(Number.isInteger(p.stock)&&p.stock>=0)add('Số lượng hiện có',p.stock+' sản phẩm');
  add('Tình trạng sản phẩm',s.condition);
  if(p.potSpecs)result.push(...potRows.value);
  else {
    if(!p.potCategory){
      add('Tên khoa học',s.scientificName);if(typeof s.heightCm==='number')add('Chiều cao cây',s.heightCm+' cm');
      add('Ánh sáng',{INDIRECT:'Ánh sáng gián tiếp',PARTIAL_SHADE:'Bóng râm một phần',SUN:'Nắng trực tiếp'}[s.light]);add('Tưới nước',s.watering);add('Giá thể',s.substrate);add('Ghi chú chăm sóc',s.careNotes);
    }
    if(typeof s.potDiameterCm==='number')add('Đường kính chậu',s.potDiameterCm+' cm');add('Chất liệu chậu',s.potMaterial);
  }
  add('Nguồn gốc do người bán khai báo',s.origin);
  add('Gian hàng',p.store?.name,Number.isSafeInteger(p.store?.id)&&p.store.id>0?'/stores/'+p.store.id:undefined);add('Khu vực gian hàng',p.store?.address);return result;
});
watch(()=>props.product.id,()=>{generation++;related.value=null;loading.value=false;error.value='';});
onBeforeUnmount(()=>generation++);
async function load(){
  if(loading.value)return;const ticket=++generation,id=props.product.id;loading.value=true;error.value='';
  try{const {data}=await api.get('/products/'+encodeURIComponent(id)+'/related-stores');if(ticket===generation)related.value=data;}
  catch{if(ticket===generation)error.value='Chưa tải được gian hàng liên quan. Vui lòng thử lại.';}
  finally{if(ticket===generation)loading.value=false;}
}
async function openDescription(){await nextTick();const panel=document.getElementById('product-description-title');panel?.focus({preventScroll:true});panel?.scrollIntoView({block:'start',behavior:'instant'});}
defineExpose({openDescription});
</script>

<style scoped>
.detail-description{display:grid;grid-template-columns:190px minmax(0,1fr);gap:24px;align-items:start}.detail-description.has-highlights{grid-template-columns:190px minmax(0,1.6fr) minmax(0,1fr)}.detail-description.description-no-image{grid-template-columns:minmax(0,1fr)}.detail-description.description-no-image.has-highlights{grid-template-columns:minmax(0,1.6fr) minmax(0,1fr)}.detail-description>.catalog-image{width:100%;aspect-ratio:1;border-radius:14px;overflow:hidden}.product-information h2{margin:0 0 16px}.product-information h2 svg{width:22px;margin-right:8px}.detail-description :deep(.seller-text){font-size:14px;line-height:1.65}.detail-highlights{background:#f0f5e9;border-radius:16px;padding:20px;font-size:13px}.detail-highlights h3{font-size:15px;color:var(--bm-forest);margin:0 0 12px}.detail-highlights p{display:flex;gap:8px;margin:10px 0}.detail-highlights p svg{width:18px;flex-shrink:0;color:var(--bm-forest)}.detail-specifications{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:4px 24px;margin:0}.detail-specifications>div{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1.1fr);gap:12px;padding:10px 12px;border-radius:8px;background:#f7f9f6;font-size:14px;overflow-wrap:anywhere}.detail-specifications>div:nth-child(4n+3),.detail-specifications>div:nth-child(4n+4){background:#fcfdfb}.detail-specifications dt{font-weight:600;display:flex;align-items:start;gap:12px}.detail-specifications dt svg{color:var(--bm-forest);width:18px}.detail-specifications dd{margin:0;white-space:pre-wrap}.detail-specification-note{font-size:12px;color:var(--bm-muted);margin:12px 0 0}.detail-related-stores{font-size:13px;color:var(--bm-muted);margin-bottom:20px}.detail-related-stores summary{cursor:pointer;width:fit-content;padding:8px}.related-category-stores{display:grid;gap:20px}.related-category-store header{display:flex;gap:16px}.related-category-store header>.catalog-image{width:56px}.related-store-products{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:16px}.related-category-store footer{display:flex;gap:20px}.related-store-summary{font-size:13px}
@media(max-width:1100px){.detail-description,.detail-description.has-highlights{grid-template-columns:140px minmax(0,1fr)}.detail-highlights{grid-column:1/-1;display:flex;flex-wrap:wrap;gap:8px 20px}.detail-highlights h3{flex-basis:100%}.detail-highlights p{margin:0}}@media(max-width:767px){.detail-description,.detail-description.has-highlights,.detail-description.description-no-image.has-highlights{grid-template-columns:minmax(0,1fr);gap:16px}.detail-description>.catalog-image{max-width:180px}.detail-specifications{grid-template-columns:minmax(0,1fr)}.detail-specifications>div{grid-template-columns:minmax(0,1fr) minmax(0,1fr)}.related-store-products{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
