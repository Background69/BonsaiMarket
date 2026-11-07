import {authRequest} from './api.js';
import {marketplaceRequest} from './marketplace.js';
import {uiErrorMessage,unreadableResponseError} from './presentation.js';

const base=id=>'/seller/products/'+encodeURIComponent(id)+'/gallery';
export const loadSellerGallery=id=>marketplaceRequest(base(id));
export const initializeGallery=id=>marketplaceRequest(base(id)+'/initialize','POST');
export const loadProductGallery=id=>marketplaceRequest('/products/'+encodeURIComponent(id)+'/gallery');
export const reorderGallery=(id,revision,imageIds)=>marketplaceRequest(base(id)+'/order','PUT',{revision,imageIds});
export const setGalleryPrimary=(id,imageId,revision)=>marketplaceRequest(base(id)+'/'+encodeURIComponent(imageId)+'/primary','PUT',{revision});
export const removeGalleryImage=(id,imageId,revision)=>marketplaceRequest(base(id)+'/'+encodeURIComponent(imageId)+'?revision='+revision,'DELETE');

// One request per file gives real per-file progress. The UI coordinates a batch with concurrency=2.
export async function uploadGalleryImage(id,file,requestKey,onProgress,signal){
    const csrf=await authRequest('/csrf');
    if(signal?.aborted)throw Object.assign(new Error('Đã dừng chờ tải ảnh; kiểm tra gallery trước khi gửi yêu cầu khác.'),{code:'UPLOAD_OUTCOME_UNKNOWN'});
    return new Promise((resolve,reject)=>{
        const xhr=new XMLHttpRequest(),form=new FormData();form.append('file',file);
        let settled=false;
        const finish=(error,result)=>{if(settled)return;settled=true;signal?.removeEventListener('abort',abort);error?reject(error):resolve(result);};
        const unknown=()=>finish(Object.assign(new Error('Chưa xác định được kết quả tải ảnh. Thử lại đúng mục này để đối chiếu cùng mã yêu cầu.'),{code:'UPLOAD_OUTCOME_UNKNOWN'}));
        const abort=()=>xhr.abort();
        xhr.open('POST','/api'+base(id)+'?requestKey='+encodeURIComponent(requestKey));xhr.withCredentials=true;xhr.timeout=60000;
        xhr.setRequestHeader(csrf.headerName,csrf.token);xhr.setRequestHeader('Accept','application/json');
        xhr.upload.onprogress=event=>onProgress(event.lengthComputable?event.loaded/event.total:null);
        xhr.onerror=xhr.ontimeout=xhr.onabort=unknown;
        xhr.onload=()=>{
            let data;try{data=JSON.parse(xhr.responseText);}catch{finish(xhr.status>=200&&xhr.status<300?unreadableResponseError():new Error('Chưa đọc được phản hồi tải ảnh. Giữ mục này để đối chiếu.'));return;}
            if(xhr.status>=200&&xhr.status<300){if(!data?.gallery||!data.imageId){finish(unreadableResponseError());return;}finish(null,data);}
            else {if(xhr.status===401)window.dispatchEvent(new CustomEvent('bonsai:session-expired'));finish(Object.assign(new Error(uiErrorMessage(xhr.status,data)),{status:xhr.status,code:data?.code}));}
        };
        signal?.addEventListener('abort',abort,{once:true});xhr.send(form);
    });
}
