import {onBeforeUnmount,ref,watch} from 'vue';
import {marketplaceRequest} from '../services/marketplace.js';
export function useAdminResource(path){
  const data=ref(null),loading=ref(true),error=ref(''),loadedAt=ref(null);let sequence=0,controller,disposed=false;
  async function load(){if(disposed)return;const ticket=++sequence;controller?.abort();controller=new AbortController();loading.value=true;error.value='';try{const value=await marketplaceRequest(path(),'GET',undefined,true,{signal:controller.signal});if(ticket===sequence){data.value=value;loadedAt.value=new Date().toISOString();}}catch(e){if(ticket===sequence){data.value=null;error.value=e.status===404?'Không tìm thấy dữ liệu được yêu cầu.':e.message;}}finally{if(ticket===sequence)loading.value=false;}}
  watch(path,load,{immediate:true});onBeforeUnmount(()=>{disposed=true;sequence++;controller?.abort();});return {data,loading,error,load,loadedAt};
}
