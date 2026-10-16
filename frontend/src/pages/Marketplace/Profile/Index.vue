<template>
  <MarketplaceLayout><main class="market-container market-page buyer-page marketplace-surface">
    <header class="market-page-heading"><h1>Tài khoản của bạn</h1><p>Thông tin tài khoản, địa chỉ nhận hàng và bảo mật.</p></header>
    <div class="account-layout"><nav class="account-nav" aria-label="Quản lý tài khoản"><RouterLink v-for="item in sections" :key="item.key" :to="item.key==='overview'?'/profile':{path:'/profile',query:{section:item.key}}" :aria-current="section===item.key?'page':undefined">{{ item.label }}</RouterLink></nav>
      <div class="account-content">
        <section v-if="section==='overview'" class="account-overview"><h2>Thông tin tài khoản</h2><ProfileEditor/>
          <RouterLink class="button button-primary" to="/profile?section=addresses">Quản lý địa chỉ nhận hàng</RouterLink>
          <RouterLink v-if="user.role==='buyer'" class="text-link account-seller-entry" to="/store-application">Đăng ký mở gian hàng</RouterLink>
          <div v-if="sellerStore || user.role==='seller'" class="account-secondary"><h3>Gian hàng của bạn</h3><p v-if="sellerStore">{{ sellerStore.name }}</p><RouterLink to="/seller" class="text-link">Mở kênh người bán</RouterLink></div>
          <div class="account-secondary"><h3>Khám phá marketplace</h3><RouterLink class="text-link" to="/products">Tiếp tục khám phá sản phẩm</RouterLink></div>
        </section>
        <AddressManagement v-if="addressesVisited" v-show="section==='addresses'"/>
        <section v-if="section==='security'" class="account-security"><h2>Bảo mật và liên kết</h2><RouterLink to="/forgot-password" class="text-link">Đặt lại mật khẩu qua email</RouterLink><AccountLinks/></section>
      </div>
    </div>
  </main></MarketplaceLayout>
</template>
<script setup>
import {computed,ref,watch} from 'vue';
import {useRoute} from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import AccountLinks from '../../../components/auth/AccountLinks.vue';
import AddressManagement from '../../../components/marketplace/AddressManagement.vue';
import ProfileEditor from '../../../components/marketplace/ProfileEditor.vue';
defineProps({user:{type:Object,required:true},sellerStore:{type:Object,default:null}});
const route=useRoute(),sections=[{key:'overview',label:'Thông tin tài khoản'},{key:'addresses',label:'Địa chỉ nhận hàng'},{key:'security',label:'Bảo mật và liên kết'}];
const section=computed(()=>sections.some(s=>s.key===route.query.section)?route.query.section:'overview'),addressesVisited=ref(false);
watch(section,s=>{if(s==='addresses')addressesVisited.value=true;},{immediate:true});
</script>
