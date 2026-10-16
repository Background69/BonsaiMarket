<template>
  <footer class="market-footer public-footer" data-section="footer">
    <div v-if="compact" class="hfs-container hfs-compact-footer">
      <RouterLink class="hfs-compact-brand" to="/">BonsaiMarket</RouterLink>
      <nav aria-label="Liên kết hỗ trợ tài khoản"><RouterLink to="/products">Sản phẩm</RouterLink><RouterLink to="/stores">Gian hàng</RouterLink><RouterLink to="/categories">Danh mục</RouterLink><button v-if="showAI" type="button" @click="openAI">Trợ lý cây cảnh</button></nav>
    </div>
    <div v-else class="hfs-container hfs-footer-grid">
      <div class="hfs-footer-brand">
        <RouterLink aria-label="BonsaiMarket — Trang chủ" class="hfs-brand" to="/">
          <BonsaiMark/>
          <span>BonsaiMarket<small>Nhiều gian hàng · Một đam mê</small></span></RouterLink>
        <p>Nền tảng marketplace dành cho cộng đồng yêu bonsai và cây cảnh tại Việt Nam.</p>
        <FooterSocialIcons/>
      </div>
      <nav aria-label="Marketplace"><h2>Khám phá</h2>
        <RouterLink to="/products">Khám phá sản phẩm</RouterLink>
        <RouterLink to="/stores">Khám phá gian hàng</RouterLink>
        <RouterLink to="/categories">Xem tất cả danh mục</RouterLink>
      </nav>
      <nav aria-label="Tài khoản và gian hàng"><h2>Tài khoản & gian hàng</h2>
        <RouterLink to="/profile">Tài khoản của tôi</RouterLink>
        <RouterLink v-if="sellerEntry" :to="sellerEntry.to">{{ sellerEntry.label }}</RouterLink></nav>
      <nav aria-label="Hỗ trợ"><h2>Hỗ trợ</h2>
        <button v-if="showAI" type="button" @click="openAI">Trợ lý AI cây cảnh</button>
      </nav>
    </div>
    <div class="hfs-footer-bottom">
      <div class="hfs-container hfs-footer-bottom-inner"><span>© {{
          year
        }} BonsaiMarket. Tất cả quyền được bảo lưu.</span>
        <div class="hfs-locale"><span><svg aria-hidden="true" height="16" viewBox="0 0 30 20" width="24"><path
            d="M0 0h30v20H0z" fill="#da001b"/><path d="m15 3 1.7 5.2h5.5l-4.4 3.2 1.7 5.2-4.5-3.2-4.5 3.2 1.7-5.2-4.4-3.2h5.5z"
                                                    fill="#ffde00"/></svg>Tiếng Việt</span><span>VND (₫)</span>
        </div>
      </div>
    </div>
  </footer>
</template>
<script setup>
import {computed} from 'vue';
import {useRoute} from 'vue-router';
import BonsaiMark from './BonsaiMark.vue';
import FooterSocialIcons from './FooterSocialIcons.vue';
import {useMarketplaceNavigation} from '../../composables/useMarketplaceNavigation.js';
const {sellerEntry} = useMarketplaceNavigation();

defineProps({categories: {type: Array, default: () => []},compact:Boolean});
const current = useRoute(), year = new Date().getFullYear();
const showAI = computed(() => !/^\/(login|register|admin|seller)(\/|$)/.test(current.path));

function openAI(event) {
  window.dispatchEvent(new CustomEvent('agriverse-open-ai-expert', {detail:{opener:event?.currentTarget}}))
}
</script>
<style scoped>
.hfs-compact-footer {display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap;padding-block:0 20px;}
.hfs-compact-brand {font-size:20px;font-weight:700;color:inherit;text-decoration:none;}
.hfs-compact-footer nav {display:flex;align-items:center;gap:8px 24px;flex-wrap:wrap;}
.hfs-compact-footer nav a,.hfs-compact-footer nav button {display:inline-flex;align-items:center;min-height:44px;background:none;border:0;padding:0;font:inherit;font-size:14px;color:inherit;text-decoration:none;cursor:pointer;}
.hfs-compact-footer nav :is(a,button):hover {text-decoration:underline;text-underline-offset:4px;}
</style>
