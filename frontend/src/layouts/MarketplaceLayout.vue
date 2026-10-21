<template>
  <div class="marketplace-app marketplace-assistant-inline"><a class="skip-link" href="#market-main">Bỏ qua điều hướng</a>
    <PublicHeader :authenticated="isAuthenticated" :cart-count="cartCount" :categories="navigationCategories" :seller="isSeller"
                  :categories-loading="categoriesLoading || navigationCategoriesLoading" :categories-error="categoriesError || navigationCategoriesError" @retry-categories="retryCategories"
                  :user="user" @login="goToLogin" @logout="handleLogout"
                  @register="goToRegister"/>
    <div id="market-main" class="market-content" tabindex="-1">
      <p v-if="authError" class="integration-error" role="alert">{{ authError }}</p>
      <slot/>
    </div>
    <AIExpertChat v-if="showAiChat"/>
    <PublicFooter v-if="!hideFooter" :categories="navigationCategories" :compact="currentRoute.path==='/profile'"/>
    <Toast/>
  </div>
</template>
<script setup>
import {ref, computed, onMounted, onBeforeUnmount} from 'vue';
import {useRoute} from 'vue-router';
import {usePage} from '@inertiajs/vue3';
import {useAuth} from '../composables/useAuth.js';
import Toast from 'primevue/toast';
import AIExpertChat from '../components/AIExpertChat.vue';
import PublicHeader from '../components/home/PublicHeader.vue';
import PublicFooter from '../components/home/PublicFooter.vue';
import {apiGet} from '../services/api.js';
const props=defineProps({hideFooter:{type:Boolean,default:false},categories:{type:Array,default:()=>[]},loadNavigationCategories:{type:Boolean,default:true},categoriesLoading:Boolean,categoriesError:Boolean});
const emit=defineEmits(['retry-categories']);
const currentRoute=useRoute(),page=usePage();
const {user,isAuthenticated,logout,syncFromPageProps}=useAuth();
const isSeller=computed(()=>page.props.auth?.user?.role==='seller');
const cartCount=computed(()=>page.props.cartCount??0);
const showAiChat=computed(()=>!/^\/(admin|seller|login|register)(\/|$)/.test(currentRoute.path));
const loadedNavigationCategories=ref([]),authError=ref('');
const navigationCategoriesLoading=ref(false),navigationCategoriesError=ref(false);
const navigationCategories=computed(()=>props.categories.length?props.categories:loadedNavigationCategories.value);
let disposed=false;
function onAuthError(event){authError.value=event.detail;}
onMounted(async()=>{
  window.addEventListener('bonsai:auth-error',onAuthError);
  syncFromPageProps();
  if(props.loadNavigationCategories&&!props.categories.length)loadCategories();
});
async function loadCategories(){
  if(navigationCategoriesLoading.value)return;
  navigationCategoriesLoading.value=true;navigationCategoriesError.value=false;
  try{const categories=await apiGet('/categories');if(!Array.isArray(categories))throw new Error('Invalid categories');if(!disposed)loadedNavigationCategories.value=categories;}
  catch{if(!disposed)navigationCategoriesError.value=true;}
  finally{if(!disposed)navigationCategoriesLoading.value=false;}
}
function retryCategories(){if(props.loadNavigationCategories)loadCategories();else emit('retry-categories');}
onBeforeUnmount(()=>{disposed=true;window.removeEventListener('bonsai:auth-error',onAuthError);});
function goToLogin(){window.location.href='/login';}
function goToRegister(){window.location.href='/register';}
async function handleLogout(){try{await logout();}catch(error){onAuthError({detail:error.message});}}
</script>
