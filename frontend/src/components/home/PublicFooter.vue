<template>
  <footer class="public-footer" :class="{'public-footer--compact':compact}" data-section="footer">
    <div v-if="compact" class="hfs-container hfs-compact-footer">
      <RouterLink aria-label="BonsaiMarket — Trang chủ" class="hfs-compact-brand" to="/"><BonsaiMark/>BonsaiMarket</RouterLink>
      <nav aria-label="Liên kết hỗ trợ tài khoản"><RouterLink to="/products">Sản phẩm</RouterLink><RouterLink to="/categories">Danh mục</RouterLink><RouterLink to="/stores">Gian hàng</RouterLink></nav>
    </div>
    <div v-else class="hfs-container hfs-footer-grid">
      <div class="hfs-footer-brand">
        <RouterLink aria-label="BonsaiMarket — Trang chủ" class="hfs-brand" to="/"><BonsaiMark/><span>BonsaiMarket<small>Nhiều gian hàng · Một đam mê</small></span></RouterLink>
        <p>BonsaiMarket kết nối những người yêu cây cảnh với các gian hàng độc lập tại Việt Nam.</p>
        <FooterSocialIcons/>
      </div>
      <nav aria-label="Khám phá marketplace"><h2>Khám phá</h2>
        <RouterLink to="/products">Tất cả sản phẩm</RouterLink>
        <RouterLink to="/categories">Danh mục cây cảnh</RouterLink>
        <RouterLink to="/stores">Khám phá gian hàng</RouterLink>
        <RouterLink to="/community">Cộng đồng BonsaiMarket</RouterLink>
      </nav>
      <nav aria-label="Tài khoản và bán hàng"><h2>Tài khoản & bán hàng</h2>
        <template v-if="navigationReady">
          <RouterLink :to="user?'/profile':'/login?redirect=%2Fprofile'">Tài khoản của tôi</RouterLink>
          <RouterLink v-if="sellerEntry" :to="sellerEntry.to">{{ sellerEntry.label }}</RouterLink>
        </template>
      </nav>
      <nav v-if="showAI" class="hfs-footer-support" aria-label="Hỗ trợ"><h2>Hỗ trợ</h2>
        <button type="button" @click="openAI"><MarketIcon name="chat"/>Trợ lý cây cảnh</button>
      </nav>
    </div>
    <div class="hfs-footer-bottom">
      <div class="hfs-container hfs-footer-bottom-inner"><span>© {{ year }} BonsaiMarket</span>
        <div class="hfs-locale"><span><svg aria-hidden="true" height="16" viewBox="0 0 30 20" width="24"><path d="M0 0h30v20H0z" fill="#da001b"/><path d="m15 3 1.7 5.2h5.5l-4.4 3.2 1.7 5.2-4.5-3.2-4.5 3.2 1.7-5.2-4.4-3.2h5.5z" fill="#ffde00"/></svg>Tiếng Việt</span><span>VND (₫)</span></div>
      </div>
    </div>
  </footer>
</template>
<script setup>
import {computed} from 'vue';
import {useRoute} from 'vue-router';
import BonsaiMark from './BonsaiMark.vue';
import MarketIcon from '../marketplace/MarketIcon.vue';
import FooterSocialIcons from './FooterSocialIcons.vue';
import {useMarketplaceNavigation} from '../../composables/useMarketplaceNavigation.js';
import {useAuth} from '../../composables/useAuth.js';
const {navigationReady,sellerEntry}=useMarketplaceNavigation(),{user}=useAuth();
defineProps({categories:{type:Array,default:()=>[]},compact:Boolean});
const current=useRoute(),year=new Date().getFullYear();
const showAI=computed(()=>!/^\/(login|register|admin|seller)(\/|$)/.test(current.path));
function openAI(event){window.dispatchEvent(new CustomEvent('agriverse-open-ai-expert',{detail:{opener:event?.currentTarget}}));}
</script>
