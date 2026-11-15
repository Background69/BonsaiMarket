import {computed,onBeforeUnmount,ref} from 'vue';
import {loadSellerGallery,initializeGallery,uploadGalleryImage,reorderGallery,setGalleryPrimary,removeGalleryImage} from '../services/productGallery.js';
import {decodeSellerImage} from '../services/sellerImageValidation.js';

export function useSellerGallery(productId,onUpdated){
    const gallery=ref({productId,images:[],revision:0,maxImages:8,maxBytes:5242880,initialized:false,imageUrl:null});
    const loading=ref(false),saving=ref(false),uploading=ref(false),adding=ref(false),error=ref(''),message=ref(''),queue=ref([]),ready=ref(false);
    const controllers=new Map();let active=true,generation=0;
    const busy=computed(()=>loading.value||saving.value||uploading.value||adding.value);
    const pending=computed(()=>queue.value.filter(item=>['READY','FAILED'].includes(item.status)&&!item.invalid));
    const remaining=computed(()=>Math.max(0,gallery.value.maxImages-gallery.value.images.length-pending.value.length-queue.value.filter(item=>item.status==='UPLOADING').length));
    const overall=computed(()=>{const items=queue.value.filter(item=>!item.invalid),total=items.reduce((sum,item)=>sum+item.size,0);if(!total)return 0;if(items.some(item=>item.status==='UPLOADING'&&item.progress===null))return null;return Math.floor(items.reduce((sum,item)=>sum+item.size*(item.status==='UPLOADED'?1:(item.progress||0)),0)/total*100);});
    const hasDraft=computed(()=>queue.value.some(item=>item.status!=='UPLOADED'));
    function apply(result){if(!active)return;if(!ready.value||result.revision>=gallery.value.revision){gallery.value=result;ready.value=true;onUpdated?.(result);}}
    async function load(initialize=false){
        const ticket=++generation;loading.value=true;error.value='';
        try{let result=await loadSellerGallery(productId);if(initialize&&!result.initialized)result=await initializeGallery(productId);if(ticket===generation)apply(result);}
        catch(cause){if(active&&ticket===generation)error.value=cause.message||'Chưa tải được gallery.';}
        finally{if(active&&ticket===generation)loading.value=false;}
    }
    function revoke(item){if(item.preview){URL.revokeObjectURL(item.preview);item.preview='';}}
    function removeQueue(key){if(busy.value)return;const item=queue.value.find(item=>item.key===key);if(item){revoke(item);queue.value=queue.value.filter(item=>item.key!==key);}}
    function clearUploaded(){queue.value.filter(item=>item.status==='UPLOADED').forEach(revoke);queue.value=queue.value.filter(item=>item.status!=='UPLOADED');}
    async function addFiles(files){
        if(busy.value||!ready.value)return;adding.value=true;error.value='';message.value='';
        try{
            const candidates=Array.from(files);if(candidates.length>32)error.value='Mỗi lượt kiểm tra tối đa 32 tệp; các tệp còn lại chưa được thêm.';
            for(const file of candidates.slice(0,32)){
                if(!active)return;if(queue.value.length>=48){error.value='Dọn các mục không cần thiết trong hàng đợi trước khi chọn thêm.';break;}
                const item={key:crypto.randomUUID(),file,name:file.name,size:file.size,preview:'',uploadedUrl:'',status:'READY',progress:0,error:'',invalid:false,fingerprint:''};
                let reason='';
                if(!file.size)reason='Tệp rỗng. Chọn ảnh có dữ liệu.';
                else if(file.size>gallery.value.maxBytes)reason='Ảnh vượt giới hạn '+(gallery.value.maxBytes/1048576)+' MB. Giảm dung lượng rồi chọn lại.';
                else {
                    const bytes=new Uint8Array(await file.slice(0,12).arrayBuffer());
                    const jpeg=bytes[0]===255&&bytes[1]===216&&bytes[2]===255,png=bytes[0]===137&&bytes[1]===80&&bytes[2]===78&&bytes[3]===71;
                    const webp=String.fromCharCode(...bytes.slice(0,4))==='RIFF'&&String.fromCharCode(...bytes.slice(8,12))==='WEBP';
                    if(!jpeg&&!png&&!webp)reason='Chỉ chọn JPEG, PNG hoặc WebP. Không nhận SVG hoặc tệp khác.';
                    else {reason=await decodeSellerImage(file);if(!reason&&crypto.subtle){item.fingerprint=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',await file.arrayBuffer()))).map(value=>value.toString(16).padStart(2,'0')).join('');if(queue.value.some(old=>old.fingerprint===item.fingerprint)){message.value='Ảnh trùng nội dung trong hàng đợi đã được bỏ qua.';continue;}}}
                }
                if(!active)return;
                if(!reason&&remaining.value===0)reason='Không còn chỗ trống: sản phẩm tối đa 8 ảnh, gồm cả ảnh đang chờ tải.';
                if(reason){item.status='FAILED';item.invalid=true;item.error=reason;item.file=null;}
                else item.preview=URL.createObjectURL(file);
                queue.value.push(item);
            }
        }catch(cause){error.value='Chưa thể đọc tệp vừa chọn. Các ảnh đã có trong hàng đợi được giữ nguyên.';}
        finally{if(active)adding.value=false;}
    }
    async function uploadAll(onlyKey){
        if(busy.value||!ready.value)return;
        const items=pending.value.filter(item=>!onlyKey||item.key===onlyKey);if(!items.length)return;
        uploading.value=true;error.value='';message.value='';let index=0;
        const worker=async()=>{
            while(active&&index<items.length){
                const item=items[index++];item.status='UPLOADING';item.error='';item.progress=0;const controller=new AbortController();controllers.set(item.key,controller);
                try{
                    const result=await uploadGalleryImage(productId,item.file,item.key,progress=>{if(active)item.progress=progress;},controller.signal);
                    if(!active)return;apply(result.gallery);item.status='UPLOADED';item.progress=1;item.uploadedUrl=result.gallery.images.find(image=>image.id===result.imageId)?.thumbnailUrl||'';
                    item.error=result.duplicate?'Ảnh đã có trong gallery; không tạo bản trùng.':result.replayed?'Đã đối chiếu lại yêu cầu tải trước.':'';revoke(item);item.file=null;
                }catch(cause){if(active){item.status=cause.code==='IMAGE_UPLOAD_REMOVED'?'CANCELLED':'FAILED';item.error=cause.message||'Chưa tải được ảnh. Giữ mục này để thử lại.';item.progress=0;if(cause.code==='IMAGE_UPLOAD_REMOVED'||[400,413,415,422].includes(cause.status)){item.invalid=true;item.file=null;revoke(item);}}}
                finally{controllers.delete(item.key);}
            }
        };
        try{await Promise.all([worker(),worker()]);if(active){await load(false);message.value='Đã xử lý hàng đợi. Kiểm tra trạng thái từng ảnh; mục lỗi được giữ để thử lại.';}}
        finally{if(active)uploading.value=false;}
    }
    async function mutate(action,success){
        if(busy.value||!ready.value)return;saving.value=true;error.value='';message.value='';
        try{apply(await action(gallery.value.revision));message.value=success;return true;}
        catch(cause){error.value=cause.message||'Chưa xác nhận được thay đổi. Gallery sẽ được tải lại trước thao tác tiếp theo.';try{apply(await loadSellerGallery(productId));}catch{ready.value=false;error.value+=' Cần tải lại gallery để đối chiếu trạng thái.';}return false;}
        finally{if(active)saving.value=false;}
    }
    function move(from,to){if(from===to||from<0||to<0||from>=gallery.value.images.length||to>=gallery.value.images.length)return;const ids=gallery.value.images.map(image=>image.id),[id]=ids.splice(from,1);ids.splice(to,0,id);return mutate(revision=>reorderGallery(productId,revision,ids),'Đã chuyển ảnh sang vị trí '+(to+1)+' / '+ids.length+'.');}
    function primary(image){return mutate(revision=>setGalleryPrimary(productId,image.id,revision),'Đã đặt ảnh bìa. Thứ tự ảnh được giữ nguyên.');}
    function remove(image){if(!window.confirm(gallery.value.images.length===1?'Xóa ảnh cuối? Sản phẩm sẽ hiển thị hình dự phòng.':image.primary?'Xóa ảnh bìa? Ảnh đầu tiên còn lại sẽ trở thành ảnh bìa.':'Xóa ảnh này khỏi gallery?'))return;return mutate(revision=>removeGalleryImage(productId,image.id,revision),'Đã xóa ảnh khỏi gallery.');}
    function canDiscard(){if(busy.value)return false;return !hasDraft.value||window.confirm('Rời sản phẩm và bỏ các ảnh chưa tải trong hàng đợi? Ảnh đã được máy chủ lưu vẫn được giữ.');}
    onBeforeUnmount(()=>{active=false;generation++;controllers.forEach(controller=>controller.abort());queue.value.forEach(revoke);});
    return {gallery,loading,busy,uploading,adding,error,message,queue,ready,pending,remaining,overall,hasDraft,load,addFiles,removeQueue,clearUploaded,uploadAll,move,primary,remove,canDiscard};
}
