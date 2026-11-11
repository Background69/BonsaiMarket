import {reactive,watch,onBeforeUnmount} from 'vue';
import {useAuth} from './useAuth.js';
import {accountProfile,accountAddresses,accountOrderSummary,accountUnreadCount} from '../services/buyerAccount.js';
export function useBuyerAccount(){
    const {user}=useAuth();let generation=0;
    const resources={profile:reactive({data:null,loading:false,error:''}),addresses:reactive({data:null,loading:false,error:''}),orders:reactive({data:null,loading:false,error:''}),notifications:reactive({data:null,loading:false,error:''})};
    const readers={profile:accountProfile,addresses:accountAddresses,orders:accountOrderSummary,notifications:accountUnreadCount};
    const revisions={profile:0,addresses:0,orders:0,notifications:0};
    async function load(key,refresh=false){
        const resource=resources[key];if((resource.loading&&!refresh)||!user.value?.id)return;
        const ticket=generation,version=++revisions[key],actor=user.value.id;resource.loading=true;resource.error='';
        const current=()=>ticket===generation&&version===revisions[key]&&user.value?.id===actor;
        try{const data=await readers[key]();if(current())resource.data=data;}
        catch(e){if(current()){resource.data=null;resource.error=e.message||'Chưa tải được thông tin. Vui lòng thử lại.';}}
        finally{if(current())resource.loading=false;}
    }
    function acceptProfile(value){if(value.email!==user.value?.email)return;revisions.profile++;Object.assign(resources.profile,{data:value,loading:false,error:''});}
    watch(()=>user.value?.id,()=>{generation++;for(const resource of Object.values(resources))Object.assign(resource,{data:null,loading:false,error:''});if(user.value?.id)Promise.allSettled(Object.keys(resources).map(load));},{immediate:true,flush:'sync'});
    onBeforeUnmount(()=>generation++);
    return {...resources,load,acceptProfile};
}
