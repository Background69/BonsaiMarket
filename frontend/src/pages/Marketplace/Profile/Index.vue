<script setup>
import {computed,watch} from 'vue';
import {useRoute} from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import AccountLinks from '../../../components/auth/AccountLinks.vue';
import ChangePasswordForm from '../../../components/auth/ChangePasswordForm.vue';
import AddressManagement from '../../../components/marketplace/AddressManagement.vue';
import ProfileEditor from '../../../components/marketplace/ProfileEditor.vue';
import BuyerAccountSidebar from '../../../components/account/BuyerAccountSidebar.vue';
import BuyerProfileSummary from '../../../components/account/BuyerProfileSummary.vue';
import BuyerOrderOverview from '../../../components/account/BuyerOrderOverview.vue';
import BuyerAccountDetails from '../../../components/account/BuyerAccountDetails.vue';
import BuyerAccountExtras from '../../../components/account/BuyerAccountExtras.vue';
import {useBuyerAccount} from '../../../composables/useBuyerAccount.js';
import {useAuth} from '../../../composables/useAuth.js';
import '../../../css/buyer-account.css';
defineProps({user:{type:Object,required:true},sellerStore:{type:Object,default:null}});
const route=useRoute(),account=useBuyerAccount(),{user:actor}=useAuth();
const section=computed(()=>['edit','addresses','security'].includes(route.query.section)?route.query.section:'overview');
watch(section,(value,previous)=>{if(value==='overview'&&previous==='addresses')account.load('addresses',true);});
function saved(profile){account.acceptProfile(profile);}
</script>
<template><MarketplaceLayout><main class="bm-buyer-account">
  <header class="bm-buyer-account-hero"><div><h1>Tài khoản của bạn</h1><p>Quản lý thông tin tài khoản, đơn mua, địa chỉ nhận hàng và bảo mật.</p></div></header>
  <div class="bm-buyer-account-layout"><BuyerAccountSidebar/><div class="bm-buyer-account-content account-content">
    <div v-show="section==='overview'" class="bm-buyer-account-dashboard">
      <BuyerProfileSummary :profile="account.profile" :addresses="account.addresses" @retry="account.load('profile')"/>
      <BuyerOrderOverview :resource="account.orders" @retry="account.load('orders')"/>
      <BuyerAccountDetails :profile="account.profile" :addresses="account.addresses" @retry-addresses="account.load('addresses')" @retry-profile="account.load('profile')"/>
      <BuyerAccountExtras :notifications="account.notifications" @retry="account.load('notifications')"/>
      <div v-if="actor?.role==='buyer'||actor?.role==='seller'||sellerStore" class="bm-buyer-account-seller-entry"><RouterLink v-if="actor?.role==='buyer'" to="/store-application">Bạn muốn bán cây? Đăng ký mở gian hàng <span aria-hidden="true">→</span></RouterLink><RouterLink v-else to="/seller">{{ sellerStore?.name||'Gian hàng của bạn' }} · Mở kênh người bán <span aria-hidden="true">→</span></RouterLink></div>
    </div>
    <section v-if="section==='edit'" :key="'edit-'+actor?.id" class="bm-buyer-account-card bm-buyer-account-editor"><h2>Chỉnh sửa hồ sơ</h2><ProfileEditor initial-editing @saved="saved"/><RouterLink class="bm-buyer-account-text-link" to="/profile">Về tổng quan tài khoản</RouterLink></section>
    <section v-if="section==='addresses'" :key="'addresses-'+actor?.id" class="bm-buyer-account-card bm-buyer-account-editor"><AddressManagement/><RouterLink class="bm-buyer-account-text-link" to="/profile">Về tổng quan tài khoản</RouterLink></section>
    <section v-if="section==='security'" :key="'security-'+actor?.id" class="bm-buyer-account-card bm-buyer-account-editor"><h2>Bảo mật & liên kết</h2><p>Quản lý mật khẩu và liên kết qua quy trình xác minh hiện có.</p><ChangePasswordForm/><AccountLinks/><RouterLink class="bm-buyer-account-text-link" to="/profile">Về tổng quan tài khoản</RouterLink></section>
  </div></div>
</main></MarketplaceLayout></template>
