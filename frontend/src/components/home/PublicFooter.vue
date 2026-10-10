<template>
  <footer class="market-footer public-footer" data-section="footer">
    <div class="hfs-container hfs-footer-grid">
      <div class="hfs-footer-brand">
        <RouterLink aria-label="BonsaiMarket — Trang chủ" class="hfs-brand" to="/">
          <BonsaiMark/>
          <span>BonsaiMarket<small>Nhiều gian hàng · Một đam mê</small></span></RouterLink>
        <p>Nền tảng marketplace dành cho cộng đồng yêu bonsai và cây cảnh tại Việt Nam.</p>
        <FooterSocialIcons/>
      </div>
      <nav aria-label="Marketplace"><h2>Marketplace</h2>
        <RouterLink to="/">Trang chủ BonsaiMarket</RouterLink>
        <RouterLink to="/products">Khám phá sản phẩm</RouterLink>
        <RouterLink to="/stores">Khám phá gian hàng</RouterLink>
      </nav>
      <nav aria-label="Danh mục"><h2>Danh mục</h2>
        <RouterLink v-for="category in categories.slice(0,5)" :key="category.id"
                    :to="{path:'/products',query:{category:category.slug}}">{{ category.name }}
        </RouterLink>
        <RouterLink to="/categories">Xem tất cả danh mục</RouterLink>
      </nav>
      <nav aria-label="Dành cho người mua"><h2>Dành cho người mua</h2>
        <RouterLink to="/profile">Tài khoản của tôi</RouterLink>
        <RouterLink title="Giỏ hàng đang phát triển" to="/cart">Giỏ hàng</RouterLink>
        <RouterLink title="Đơn mua đang phát triển" to="/orders">Đơn mua</RouterLink>
        <span class="hfs-footer-pending">Mua hàng đang phát triển</span></nav>
      <nav aria-label="Dành cho người bán"><h2>Dành cho người bán</h2>
        <RouterLink to="/seller">Kênh người bán</RouterLink>
        <RouterLink to="/seller/products">Quản lý sản phẩm</RouterLink>
        <RouterLink title="Quản lý đơn hàng đang phát triển" to="/seller/orders">Quản lý đơn hàng</RouterLink>
        <span class="hfs-footer-pending">Mở gian hàng sắp ra mắt</span></nav>
      <nav aria-label="Hỗ trợ"><h2>Hỗ trợ</h2>
        <button v-if="showAI" type="button" @click="openAI">Trợ lý AI cây cảnh</button>
        <RouterLink to="/stores">Tìm gian hàng</RouterLink>
        <RouterLink to="/products">Tìm sản phẩm</RouterLink>
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

defineProps({categories: {type: Array, default: () => []}});
const current = useRoute(), year = new Date().getFullYear();
const showAI = computed(() => !/^\/(login|register|admin|seller)(\/|$)/.test(current.path));

function openAI() {
  window.dispatchEvent(new CustomEvent('agriverse-open-ai-expert'))
}
</script>
