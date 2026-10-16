import {authRequest} from './api.js';
export async function uploadProductImage(id,file,progress) {
    const csrf=await authRequest('/csrf');
    return new Promise((resolve,reject)=>{
        const xhr=new XMLHttpRequest(),form=new FormData();form.append('file',file);
        xhr.open('POST','/api/seller/products/'+encodeURIComponent(id)+'/image');xhr.withCredentials=true;xhr.timeout=60000;
        xhr.setRequestHeader(csrf.headerName,csrf.token);xhr.setRequestHeader('Accept','application/json');
        xhr.upload.onprogress=e=>{if(e.lengthComputable)progress(Math.round(e.loaded/e.total*100));};
        xhr.onload=()=>{let data;try{data=JSON.parse(xhr.responseText);}catch{data={};}
            if(xhr.status>=200&&xhr.status<300)resolve(data);else{if(xhr.status===401)window.dispatchEvent(new CustomEvent('bonsai:session-expired'));reject(Object.assign(new Error(data.message||'Không thể lưu ảnh. Vui lòng thử lại.'),{status:xhr.status,code:data.code,fields:data.fields||{}}));}};
        xhr.onerror=xhr.ontimeout=()=>reject(new Error('Chưa nhận được kết quả lưu ảnh. Tải lại ảnh hiện tại để kiểm tra trước khi thử lại.'));xhr.send(form);
    });
}
