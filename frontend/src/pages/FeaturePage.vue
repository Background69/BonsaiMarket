<template>
  <MarketplaceLayout>
    <main class="market-container market-page buyer-page">
      <div class="breadcrumbs"><RouterLink to="/">Trang chủ</RouterLink><span>/</span><span>{{ title }}</span></div>
      <section class="market-panel account-feature">
        <MarketIcon :name="authPage?'user':'leaf'"/>
        <p class="eyebrow">{{ authPage?'TÀI KHOẢN BONSAIMARKET':'BONSAIMARKET' }}</p>
        <h1>{{ title }}</h1>
        <p>{{ message }}</p>
        <p v-if="authPage" class="development-note">Đăng nhập, đăng ký và xác nhận email chưa kết nối backend. Hiện có thể khám phá sản phẩm và gian hàng mà không cần tài khoản.</p>
        <div class="button-row"><RouterLink to="/products" class="button button-primary">Khám phá sản phẩm</RouterLink><RouterLink to="/stores" class="button button-outline">Khám phá gian hàng</RouterLink></div>
        <RouterLink v-if="authPage" :to="current.path==='/login'?'/register':'/login'" class="text-link">{{ current.path==='/login'?'Chưa có tài khoản? Xem trang đăng ký':'Đã có tài khoản? Xem trang đăng nhập' }}</RouterLink>
      </section>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import MarketplaceLayout from '../layouts/MarketplaceLayout.vue';
import MarketIcon from '../components/marketplace/MarketIcon.vue';
import {computed} from 'vue';
import {useRoute} from 'vue-router';
const current=useRoute();
const authPage=computed(()=>['/login','/register'].includes(current.path));

defineProps({
  title: {type: String, default: 'BonsaiMarket'},
  message: {type: String, default: 'Chức năng đang được phát triển.'},
});
</script>

<style scoped>
.feature-page {
  min-height: 55vh;
  padding: 160px 24px 80px;
  text-align: center;
  color: var(--ag-text-primary);
}

.feature-page .material-symbols-outlined {
  font-size: 56px;
  color: var(--ag-primary-500);
}

.feature-page h1 {
  font: 500 2.5rem var(--ag-font-display);
  margin: 16px 0;
}

.feature-page p {
  color: var(--ag-text-secondary);
  margin-bottom: 24px;
}

.feature-page a {
  display: inline-block;
  padding: 12px 24px;
  color: white;
  background: var(--ag-primary-500);
  border-radius: 24px;
}
</style>
