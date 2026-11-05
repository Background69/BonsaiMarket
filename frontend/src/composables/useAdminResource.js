import {onBeforeUnmount,ref,watch} from 'vue';
import {marketplaceRequest} from '../services/marketplace.js';
export function useAdminResource(path){
  const data=ref(null),loading=ref(true),error=ref('');let sequence=0;
  async function load(){const ticket=++sequence;loading.value=true;error.value='';try{const value=await marketplaceRequest(path());if(ticket===sequence)data.value=value;}catch(e){if(ticket===sequence){data.value=null;error.value=e.status===404?'Không tìm thấy dữ liệu được yêu cầu.':e.message;}}finally{if(ticket===sequence)loading.value=false;}}
  watch(path,load,{immediate:true});onBeforeUnmount(()=>sequence++);return {data,loading,error,load};
}
