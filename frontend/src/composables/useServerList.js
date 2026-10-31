import {computed,onBeforeUnmount,ref,watch} from 'vue';
import {useRoute,useRouter} from 'vue-router';
import {marketplaceRequest} from '../services/marketplace.js';
export function useServerList(endpoint,filters=()=>({})) {
    const router=useRouter(),route=useRoute(),data=ref({content:[],page:0,totalPages:0,totalElements:0}),loading=ref(true),error=ref('');let revision=0;
    const url=computed(()=>endpoint+'?'+new URLSearchParams({page:route.query.page||'0',...filters(route.query)}));
    async function load(){const id=++revision;loading.value=true;error.value='';try{const result=await marketplaceRequest(url.value);if(id===revision){
      const lastPage=Math.max(0,result.totalPages-1);
      if(Number(route.query.page||0)>lastPage){await router.replace({path:route.path,query:{...route.query,page:String(lastPage)},hash:route.hash});return;}
      data.value=result;
    }}catch(e){if(id===revision)error.value=e.message||'Không thể tải danh sách. Vui lòng thử lại.';}finally{if(id===revision)loading.value=false;}}
    watch(url,load,{immediate:true});onBeforeUnmount(()=>revision++);
    return {data,loading,error,load};
}
