<template>
  <header ref="headerRoot" class="public-header" @keydown.esc="closeWithEscape">
    <div class="hfs-container hfs-main">
      <RouterLink aria-label="BonsaiMarket — Trang chủ" class="hfs-brand" to="/">
        <BonsaiMark/><span>BonsaiMarket<small>Nhiều gian hàng · Một đam mê</small></span>
      </RouterLink>
      <form id="marketplace-search-form" class="hfs-search" role="search" aria-label="Tìm sản phẩm toàn sàn" @submit.prevent="search">
        <MarketIcon name="search"/>
        <label class="sr-only" for="marketplace-search">Tìm kiếm sản phẩm trên BonsaiMarket</label>
        <input id="marketplace-search" ref="searchInput" v-model="term" type="search" maxlength="160" enterkeyhint="search" placeholder="Tìm bonsai, sen đá, chậu cây, phụ kiện..."/>
        <button v-if="term" class="hfs-search-clear" type="button" aria-label="Xóa từ khóa tìm kiếm" @click="clearSearch"><MarketIcon name="close"/></button>
        <button class="hfs-search-submit" aria-label="Tìm kiếm sản phẩm" type="submit"><MarketIcon name="search"/><span>Tìm kiếm</span></button>
      </form>
      <div class="hfs-actions">
        <RouterLink class="hfs-cart" to="/cart" aria-label="Mở giỏ hàng"><MarketIcon name="cart"/><span v-if="cartCount > 0" class="hfs-cart-count" aria-hidden="true">{{ cartCount }}</span><span class="sr-only" role="status" aria-live="polite" aria-atomic="true">Giỏ hàng có {{ cartCount }} sản phẩm</span></RouterLink>
        <details v-if="navigationReady && authenticated" ref="accountMenu" class="hfs-account" @toggle="accountOpen = $event.target.open">
          <summary aria-label="Mở menu tài khoản" aria-controls="hfs-account-panel" :aria-expanded="accountOpen" @click="closeNavigation">
            <MarketIcon name="user"/><span :title="user?.name">{{ user?.name || 'Tài khoản' }}<small>Tài khoản của tôi</small></span><MarketIcon name="chevron"/>
          </summary>
          <nav id="hfs-account-panel" aria-label="Tài khoản">
            <p class="hfs-account-name">{{ user?.name }}</p>
            <RouterLink to="/profile">Tài khoản của tôi</RouterLink>
            <RouterLink to="/orders">Đơn hàng của tôi</RouterLink>
            <RouterLink to="/cart">Giỏ hàng</RouterLink>
            <RouterLink v-if="sellerEntry" :to="sellerEntry.to">{{ sellerEntry.label }}</RouterLink>
            <button type="button" @click="logout">Đăng xuất</button>
          </nav>
        </details>
        <div v-else-if="navigationReady" class="hfs-guest">
          <RouterLink aria-label="Đăng nhập" class="hfs-user-icon" to="/login"><MarketIcon name="user"/><span>Đăng nhập</span></RouterLink>
          <RouterLink class="hfs-register" to="/register">Đăng ký</RouterLink>
        </div>
        <span v-else class="hfs-session-state" role="status" :aria-label="navigationUnavailable?'Chưa tải được tài khoản':'Đang kiểm tra tài khoản'"><MarketIcon name="user"/><span>{{ navigationUnavailable?'Chưa tải được tài khoản':'Đang kiểm tra tài khoản' }}</span></span>
        <button ref="mobileTrigger" :aria-expanded="mobileOpen" :aria-label="mobileOpen ? 'Đóng menu điều hướng' : 'Mở menu điều hướng'" aria-controls="hfs-mobile-nav" class="hfs-menu-toggle" type="button" @click="toggleMobile"><MarketIcon :name="mobileOpen ? 'close' : 'menu'"/></button>
      </div>
    </div>
    <div class="hfs-nav-band">
      <nav id="hfs-mobile-nav" ref="mobilePanel" aria-label="Điều hướng marketplace" class="hfs-container hfs-nav" :class="{'is-open': mobileOpen}">
        <RouterLink to="/products"><MarketIcon name="grid"/>Sản phẩm</RouterLink>
        <div ref="categoryRoot" class="hfs-category-nav">
          <RouterLink to="/categories"><MarketIcon name="list"/>Danh mục</RouterLink>
          <button ref="categoryTrigger" :aria-expanded="categoryOpen" aria-controls="hfs-category-panel" aria-label="Mở danh mục sản phẩm" type="button" @click="toggleCategories" @keydown.down.prevent="openCategories"><MarketIcon name="chevron"/></button>
          <div v-if="categoryOpen" id="hfs-category-panel" ref="categoryPanel" class="hfs-category-panel" @keydown="categoryKeys">
            <p v-if="categoriesLoading" role="status">Đang tải danh mục…</p>
            <div v-else-if="categoriesError" class="hfs-category-state" role="status"><p>Chưa tải được danh mục.</p><button type="button" @click="$emit('retry-categories')">Thử lại</button></div>
            <template v-else>
              <RouterLink v-for="category in activeCategories" :key="category.id" :to="{path:'/products',query:{categoryId:category.id}}">{{ category.name }}</RouterLink>
              <p v-if="!activeCategories.length">Chưa có danh mục để khám phá.</p>
            </template>
            <RouterLink class="hfs-all-categories" to="/categories">Xem tất cả danh mục<MarketIcon name="arrow"/></RouterLink>
          </div>
        </div>
        <RouterLink to="/stores"><MarketIcon name="store"/>Gian hàng</RouterLink>
        <RouterLink to="/community">Cộng đồng</RouterLink>
        <RouterLink v-if="navigationReady" class="hfs-mobile-account" :to="authenticated?'/profile':'/login'"><MarketIcon name="user"/>{{ authenticated?'Tài khoản của tôi':'Đăng nhập' }}</RouterLink>
        <button v-if="showAI" class="hfs-ai-entry" type="button" @click="openAI"><MarketIcon name="chat"/>Trợ lý cây cảnh</button>
        <RouterLink v-if="sellerEntry" :to="sellerEntry.to" class="hfs-seller"><MarketIcon name="store"/>{{ sellerEntry.label }}<MarketIcon name="arrow"/></RouterLink>
      </nav>
    </div>
  </header>
</template>
<script setup>
import {ref, watch, computed, nextTick, onMounted, onBeforeUnmount} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import MarketIcon from '../marketplace/MarketIcon.vue';
import BonsaiMark from './BonsaiMark.vue';
import {useMarketplaceNavigation} from '../../composables/useMarketplaceNavigation.js';
const {navigationReady, navigationUnavailable, sellerEntry} = useMarketplaceNavigation();
const props = defineProps({
  authenticated:Boolean, seller:Boolean, user:Object, userNav:Array,
  cartCount:{type:Number,default:0}, categories:{type:Array,default:()=>[]},
  categoriesLoading:Boolean, categoriesError:Boolean,
});
const emit=defineEmits(['login','register','logout','retry-categories']);
const router=useRouter(), current=useRoute();
const term=ref(String(current.query.search||current.query.q||'')), searchInput=ref(null);
const headerRoot=ref(null), categoryRoot=ref(null), categoryPanel=ref(null), categoryTrigger=ref(null);
const accountMenu=ref(null), accountOpen=ref(false), mobileTrigger=ref(null), mobilePanel=ref(null);
const categoryOpen=ref(false), mobileOpen=ref(false);
// API trả danh mục đang hoạt động; vẫn loại các mục không hoạt động được truyền vào.
const activeCategories=computed(()=>props.categories.filter(c=>Number.isSafeInteger(c.id)&&c.id>0&&c.active!==false&&c.isActive!==false&&(!c.status||c.status==='ACTIVE')));
const showAI=computed(()=>!/^\/(login|register|admin|seller)(\/|$)/.test(current.path));
watch(()=>[current.query.search,current.query.q],([search,q])=>term.value=String(search||q||''));
watch(()=>current.fullPath,()=>{
  const focused=headerRoot.value?.contains(document.activeElement);
  closeNavigation();closeAccount();
  if(focused)nextTick(()=>document.getElementById('market-main')?.focus({preventScroll:true}));
});
watch(navigationReady,ready=>{if(!ready)closeAccount();});
function search(){
  const query=current.path==='/products'?{...current.query}:{};
  delete query.q;delete query.page;
  const keyword=term.value.trim().replace(/\s+/g,' ');
  if(keyword)query.search=keyword;else delete query.search;
  router.push({path:'/products',query});
}
function clearSearch(){term.value='';searchInput.value?.focus();}
function closeAccount(){if(accountMenu.value)accountMenu.value.open=false;accountOpen.value=false;}
function closeNavigation(){categoryOpen.value=false;mobileOpen.value=false;}
function logout(){closeAccount();emit('logout');}
function openAI(event){closeNavigation();window.dispatchEvent(new CustomEvent('agriverse-open-ai-expert',{detail:{opener:event?.currentTarget}}));}
function toggleCategories(){closeAccount();categoryOpen.value=!categoryOpen.value;}
async function openCategories(){closeAccount();categoryOpen.value=true;await nextTick();categoryPanel.value?.querySelector('a,button')?.focus();}
function categoryKeys(event){
  if(!['ArrowDown','ArrowUp','Home','End'].includes(event.key))return;
  const links=[...categoryPanel.value.querySelectorAll('a,button')],index=links.indexOf(document.activeElement);
  const next=event.key==='Home'?0:event.key==='End'?links.length-1:(index+(event.key==='ArrowDown'?1:-1)+links.length)%links.length;
  links[next]?.focus();event.preventDefault();
}
async function toggleMobile(){
  closeAccount();categoryOpen.value=false;mobileOpen.value=!mobileOpen.value;
  if(mobileOpen.value){await nextTick();mobilePanel.value?.querySelector('a')?.focus();}
}
function closeWithEscape(event){
  if(categoryOpen.value){categoryOpen.value=false;categoryTrigger.value?.focus();}
  else if(accountMenu.value?.open){closeAccount();accountMenu.value?.querySelector('summary')?.focus();}
  else if(mobileOpen.value){mobileOpen.value=false;mobileTrigger.value?.focus();}
  else return;
  event.preventDefault();event.stopPropagation();
}
function onOutside(event){
  if(!categoryRoot.value?.contains(event.target))categoryOpen.value=false;
  if(!accountMenu.value?.contains(event.target))closeAccount();
  if(!headerRoot.value?.contains(event.target)&&!event.target.closest?.('.ai-expert-panel'))mobileOpen.value=false;
}
function onFocusOutside(event){
  if(!categoryRoot.value?.contains(event.target))categoryOpen.value=false;
  if(!accountMenu.value?.contains(event.target))closeAccount();
  if(!headerRoot.value?.contains(event.target)&&!event.target.closest?.('.ai-expert-panel'))mobileOpen.value=false;
}
let desktopMedia;
function onBreakpoint(){
  const focused=mobilePanel.value?.contains(document.activeElement);
  closeNavigation();closeAccount();
  if(focused&&!desktopMedia.matches)mobileTrigger.value?.focus();
}
onMounted(()=>{
  document.addEventListener('click',onOutside);document.addEventListener('focusin',onFocusOutside);
  desktopMedia=window.matchMedia('(min-width: 1024px)');desktopMedia.addEventListener('change',onBreakpoint);
});
onBeforeUnmount(()=>{
  document.removeEventListener('click',onOutside);document.removeEventListener('focusin',onFocusOutside);
  desktopMedia?.removeEventListener('change',onBreakpoint);
});
</script>
