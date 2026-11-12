<template>
  <div class="bm-seller" :class="{'bm-seller-collapsed':collapsed}">
    <a class="skip-link" href="#bm-seller-main">Bỏ qua điều hướng</a>
    <aside id="bm-seller-sidebar" class="bm-seller-sidebar">
      <RouterLink class="market-brand" to="/"><span class="brand-mark"><MarketIcon name="leaf"/></span><span>BonsaiMarket</span></RouterLink>
      <p class="bm-seller-sidebar-label">Kênh người bán</p>
      <nav aria-label="Điều hướng người bán"><div v-for="group in groups" :key="group.label" class="bm-seller-nav-group"><p>{{ group.label }}</p><RouterLink v-for="item in group.items" :key="item.to" :to="item.to" :aria-current="active(item)?'page':undefined" :aria-label="item.label" :title="collapsed?item.label:undefined"><MarketIcon :name="item.icon"/><span>{{ item.label }}</span></RouterLink></div></nav>
      <RouterLink class="bm-seller-back" to="/"><MarketIcon name="arrow"/><span>Về trang mua sắm</span></RouterLink>
    </aside>
    <div class="bm-seller-body">
      <header class="bm-seller-topbar">
        <button class="bm-seller-nav-toggle" type="button" aria-controls="bm-seller-sidebar" :aria-expanded="!collapsed" aria-label="Thu gọn hoặc mở rộng điều hướng" @click="collapse"><MarketIcon name="menu"/></button>
        <button ref="mobileToggle" class="bm-seller-mobile-toggle" type="button" aria-controls="bm-seller-navigation" aria-haspopup="dialog" :aria-expanded="navigationOpen" aria-label="Mở điều hướng người bán" @click="openNavigation"><MarketIcon name="menu"/></button>
        <nav aria-label="Vị trí trong kênh người bán" class="bm-seller-breadcrumb"><RouterLink to="/seller">Kênh người bán</RouterLink><span aria-hidden="true">/</span><strong>{{ pageTitle }}</strong></nav>
        <div class="bm-seller-account"><RouterLink to="/messages" aria-label="Tin nhắn"><MarketIcon name="chat"/><span>Tin nhắn</span></RouterLink><RouterLink to="/profile" aria-label="Tài khoản"><MarketIcon name="user"/><span>Tài khoản</span></RouterLink><button type="button" :disabled="loggingOut" @click="signOut">{{ loggingOut?'Đang thoát…':'Đăng xuất' }}</button></div>
      </header>
      <main id="bm-seller-main" class="bm-seller-main" tabindex="-1"><p v-if="authError" class="bm-seller-alert" role="alert">{{ authError }}</p><slot/></main>
      <footer class="bm-seller-footer">BonsaiMarket · Đồng hành cùng gian hàng độc lập</footer>
    </div>
    <dialog id="bm-seller-navigation" ref="drawer" class="bm-seller-drawer" aria-labelledby="bm-seller-navigation-title" @keydown.tab="trapDialogFocus" @close="closeNavigation" @click="backdrop">
      <header><h2 id="bm-seller-navigation-title">Kênh người bán</h2><button type="button" aria-label="Đóng điều hướng" @click="drawer.close()"><MarketIcon name="close"/></button></header>
      <nav aria-label="Điều hướng người bán trên điện thoại"><div v-for="group in groups" :key="group.label" class="bm-seller-nav-group"><p>{{ group.label }}</p><RouterLink v-for="item in group.items" :key="item.to" :to="item.to" :aria-current="active(item)?'page':undefined" @click="drawer.close()"><MarketIcon :name="item.icon"/><span>{{ item.label }}</span></RouterLink></div><RouterLink to="/" @click="drawer.close()">Về trang mua sắm</RouterLink></nav>
    </dialog>
  </div>
</template>
<script setup>
import {computed,onBeforeUnmount,onMounted,ref,watch} from 'vue';
import {useRoute} from 'vue-router';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import {useAuth} from '../../../composables/useAuth.js';
import {lockBodyScroll} from '../../../utils/bodyScrollLock.js';
import {trapDialogFocus} from '../../../utils/dialogFocus.js';
import '../../../css/seller-center.css';
const route=useRoute(),{logout,user}=useAuth(),collapsed=ref(false),drawer=ref(null),mobileToggle=ref(null),navigationOpen=ref(false),loggingOut=ref(false),authError=ref('');
const groups=[
 {label:'Tổng quan',items:[{to:'/seller',label:'Dashboard',icon:'grid'}]},
 {label:'Quản lý bán hàng',items:[{to:'/seller/orders',label:'Đơn hàng',icon:'bag'},{to:'/seller/products',label:'Sản phẩm và tồn kho',icon:'leaf'},{to:'/seller/questions',label:'Hỏi đáp sản phẩm',icon:'chat'},{to:'/messages',label:'Tin nhắn',icon:'chat'}]},
 {label:'Gian hàng',items:[{to:'/seller/store',label:'Thông tin gian hàng',icon:'store'},{to:'/seller/images',label:'Hình ảnh sản phẩm',icon:'grid'}]},
 {label:'Quảng bá',items:[{to:'/seller/banner-campaigns',label:'Cạnh tranh banner',icon:'chart'},{to:'/seller/banner-credits',label:'Credit quảng bá',icon:'wallet'}]},
];
const active=item=>route.path===item.to||(item.to!=='/seller'&&route.path.startsWith(item.to+'/'));
const pageTitle=computed(()=>route.name==='seller-product-create'?'Tạo sản phẩm':/^seller-product-(edit|content)$/.test(route.name||'')?'Chỉnh sửa sản phẩm':groups.flatMap(g=>g.items).find(active)?.label||'Gian hàng');
const preference=computed(()=>'bm-seller-sidebar:'+user.value?.id);
function collapse(){collapsed.value=!collapsed.value;try{sessionStorage.setItem(preference.value,String(collapsed.value));}catch{/* optional preference */}}
let unlock,media;
function openNavigation(){unlock?.();unlock=lockBodyScroll();drawer.value.showModal();navigationOpen.value=true;}
function closeNavigation(){navigationOpen.value=false;unlock?.();unlock=undefined;mobileToggle.value?.focus({preventScroll:true});}
function backdrop(e){if(e.target!==drawer.value)return;const b=drawer.value.getBoundingClientRect();if(e.clientX<b.left||e.clientX>b.right||e.clientY<b.top||e.clientY>b.bottom)drawer.value.close();}
function desktop(){if(media.matches)drawer.value?.close();}
watch(()=>route.fullPath,()=>drawer.value?.close());
async function signOut(){if(loggingOut.value)return;loggingOut.value=true;authError.value='';try{await logout();}catch(e){authError.value=e.message;}finally{loggingOut.value=false;}}
onMounted(()=>{try{collapsed.value=sessionStorage.getItem(preference.value)==='true';}catch{/* optional preference */}media=matchMedia('(min-width:1024px)');media.addEventListener('change',desktop);});
onBeforeUnmount(()=>{unlock?.();media?.removeEventListener('change',desktop);});
</script>
