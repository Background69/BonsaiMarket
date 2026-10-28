import {computed,ref,watch} from 'vue';
import {useAuth} from './useAuth.js';
import {sellerDestination} from '../navigation/marketplace.js';
import {marketplaceRequest} from '../services/marketplace.js';

// Gộp lần đọc hồ sơ khi cùng danh tính được server khôi phục.
// Danh tính mới được đọc lại; không lưu vai trò hoặc trạng thái hồ sơ lâu dài.
const applicationReads=new WeakMap();

export function useMarketplaceNavigation() {
    const {user, state} = useAuth();
    const navigationReady = computed(() => state.ready && !state.unavailable);
    const application=ref(null);
    watch(()=>navigationReady.value?user.value:null,(person,previous,onCleanup)=>{
        let active=true;onCleanup(()=>active=false);application.value=null;
        if(!person||!['buyer','customer'].includes(person.role))return;
        if(!applicationReads.has(person))applicationReads.set(person,marketplaceRequest('/account/seller-application').catch(()=>null));
        applicationReads.get(person).then(value=>{if(active)application.value=value;});
    },{immediate:true});
    const sellerEntry = computed(() => {
        if(!navigationReady.value)return null;
        const destination=sellerDestination(user.value);
        return ['PENDING','REJECTED'].includes(application.value?.status)
            ? {...destination,label:'Hồ sơ mở gian hàng'}:destination;
    });
    const navigationUnavailable=computed(()=>state.ready&&state.unavailable);
    return {navigationReady, navigationUnavailable, sellerEntry};
}
