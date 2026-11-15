<script setup>
import {computed} from 'vue';import {useRoute} from 'vue-router';import AccountIcon from './AccountIcon.vue';
const props=defineProps({wallet:Boolean});
const route=useRoute();
const items=[{key:'overview',label:'Tổng quan',icon:'home',to:'/profile'},{key:'orders',label:'Đơn mua',icon:'bag',to:'/orders'},{key:'addresses',label:'Địa chỉ nhận hàng',icon:'pin',to:'/profile?section=addresses'},{key:'wishlist',label:'Sản phẩm yêu thích',icon:'heart',to:'/profile#bm-buyer-account-wishlist'},{key:'notifications',label:'Thông báo & hỗ trợ',icon:'bell',to:'/profile#bm-buyer-account-notifications'},{key:'security',label:'Bảo mật & liên kết',icon:'shield',to:'/profile?section=security'}];
const links=computed(()=>props.wallet?[...items.slice(0,4),{key:"messages",label:"Tin nhắn",icon:"chat",to:"/messages"},{key:"wallet",label:"Ví BonsaiMarket",icon:"wallet",to:"/wallet"},...items.slice(4)]:items);
const active=computed(()=>props.wallet?"wallet":route.hash.includes('wishlist')?'wishlist':route.hash.includes('notifications')?'notifications':['addresses','security'].includes(route.query.section)?route.query.section:'overview');
</script>
<template><aside class="bm-buyer-account-sidebar"><nav aria-label="Quản lý tài khoản"><RouterLink v-for="item in links" :key="item.key" :to="item.to" :aria-current="active===item.key?'page':'false'"><AccountIcon :name="item.icon"/><span>{{ item.label }}</span></RouterLink></nav><img class="bm-buyer-account-sidebar-leaves" src="/images/account/botanical.svg" alt="" aria-hidden="true"/></aside></template>
