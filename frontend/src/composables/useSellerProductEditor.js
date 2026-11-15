import {computed,nextTick,onBeforeUnmount,onMounted,reactive,ref} from 'vue';
import {onBeforeRouteLeave,useRoute,useRouter} from 'vue-router';
import {marketplaceRequest as request} from '../services/marketplace.js';
import {inventoryReturnQuery,productRequirements} from '../services/sellerProduct.js';

export function useSellerProductEditor(){
 const route=useRoute(),router=useRouter(),id=Number(route.params.id),creating=!route.params.id;
 const form=reactive({name:'',price:'',categoryId:'',stock:0}),product=ref({}),categories=ref([]),content=ref(null),gallery=ref(null),stock=ref(null),history=ref([]);
 const loading=ref(true),loaded=ref(false),busy=ref(false),error=ref(''),success=ref(''),fields=ref({}),saved=ref(''),conflict=ref(false),historyError=ref(''),lastSaved=ref(null);
 const contentEditor=ref(null),basicForm=ref(null),errorSummary=ref(null),preview=ref(null),galleryManager=ref(null),contentState=ref({dirty:false,busy:false,loaded:false}),galleryState=ref({busy:false,draft:false}),stockEditing=ref(false),stockSaving=ref(false);
 const basicDirty=computed(()=>loaded.value&&JSON.stringify(form)!==saved.value),dirty=computed(()=>basicDirty.value||contentState.value.dirty),locked=computed(()=>busy.value||contentState.value.busy||galleryState.value.busy||stockSaving.value);
 const category=computed(()=>categories.value.find(c=>Number(c.id)===Number(form.categoryId)));
 const back=computed(()=>({path:'/seller/products',query:inventoryReturnQuery(route.query)}));
 const requirements=computed(()=>productRequirements(product.value,content.value,gallery.value));
 const canSubmit=computed(()=>loaded.value&&!creating&&!locked.value&&!dirty.value&&!stockEditing.value&&!galleryState.value.draft&&!conflict.value&&contentState.value.loaded&&requirements.value.every(r=>r.met)&&['DRAFT','REJECTED'].includes(product.value.status));
 const requestKey=crypto.randomUUID();let active=true,createBody=null,unknownCreate=false;
 async function showError(e){error.value=e.message||'Chưa lưu được sản phẩm. Dữ liệu đang nhập được giữ lại.';fields.value=e.fields||{};if(e.status===409)conflict.value=true;await nextTick();errorSummary.value?.focus();}
 function values(p){Object.assign(form,{name:p.name,price:p.price,categoryId:p.categoryId,stock:p.stock});saved.value=JSON.stringify(form);}
 async function load(){
  if(locked.value)return;
  loading.value=true;error.value='';loaded.value=false;conflict.value=false;
  try{
   const list=await request('/categories');if(!active)return;categories.value=list;
   if(!creating){
    const [p,c,s]=await Promise.all([request('/seller/products/'+id),request('/seller/products/'+id+'/content'),request('/seller/inventory/'+id)]);if(!active)return;
    product.value=p;content.value=c;stock.value=s;values(p);
    if(p.revision!==c.revision)throw new Error('Sản phẩm vừa thay đổi trong lúc tải. Tải lại để đối chiếu phiên bản.');
    await loadHistory();
   }else saved.value=JSON.stringify(form);
   loaded.value=true;
  }catch(e){if(active)await showError(e);}finally{if(active)loading.value=false;}
 }
 async function loadHistory(){historyError.value='';try{const value=await request('/seller/products/'+id+'/moderation');if(active)history.value=value;}catch(e){if(active)historyError.value=e.message;}}
 function contentSaved(value,meta){content.value=value;if(meta?.updated){product.value={...product.value,revision:value.revision};lastSaved.value=new Date().toISOString();}else if(value.revision!==product.value.revision){conflict.value=true;error.value='Phiên bản nội dung đã thay đổi. Tải lại sản phẩm trước khi lưu thông tin cơ bản.';}}
 async function save(){
  if(locked.value||stockEditing.value||conflict.value||!loaded.value)return;
  if(!basicForm.value?.reportValidity())return;
  busy.value=true;error.value='';success.value='';fields.value={};
  try{
   if(creating){
    if(!createBody)createBody={name:form.name.trim(),price:Number(form.price),categoryId:Number(form.categoryId),stock:Number(form.stock),requestKey};
    else if(unknownCreate&&JSON.stringify({...createBody,name:form.name.trim(),price:Number(form.price),categoryId:Number(form.categoryId),stock:Number(form.stock)})!==JSON.stringify(createBody))throw new Error('Yêu cầu tạo trước chưa rõ kết quả. Giữ nguyên thông tin để đối chiếu cùng mã yêu cầu trước khi chỉnh tiếp.');
    const value=await request('/seller/products','POST',createBody);saved.value=JSON.stringify(form);busy.value=false;
    await router.replace({path:'/seller/products/'+value.id+'/edit',query:inventoryReturnQuery(route.query)});return;
   }
   if(basicDirty.value){
    if(Number(form.categoryId)!==Number(product.value.categoryId)&&!window.confirm('Đổi danh mục? Toàn bộ metadata đã lưu được giữ lại. Kiểm tra thông số phù hợp trước khi gửi duyệt.'))return;
    const previous=product.value.revision;
    const value=await request('/seller/products/'+id,'PUT',{name:form.name.trim(),price:Number(form.price),categoryId:Number(form.categoryId),revision:previous});
    product.value=value;values(value);lastSaved.value=new Date().toISOString();
    if(!contentEditor.value?.rebase(previous,value.revision))throw new Error('Đã lưu thông tin cơ bản. Nội dung đang dùng phiên bản khác; đối chiếu trước khi lưu tiếp.');
    content.value={...content.value,revision:value.revision};
   }
   if(contentState.value.dirty&&!await contentEditor.value?.save())throw new Error('Thông tin cơ bản đã được giữ. Mô tả hoặc thông số chưa lưu được; kiểm tra lỗi tại phần nội dung.');
   success.value='Đã lưu sản phẩm. Ảnh và tồn kho được lưu bằng thao tác riêng ngay trong trang này.';
  }catch(e){if(creating){unknownCreate=!e.status||e.status>=500;if(!unknownCreate&&e.code!=='REQUEST_CONFLICT')createBody=null;}await showError(e);}finally{busy.value=false;}
 }
 async function submit(){
  if(!canSubmit.value)return;
  if(!window.confirm('Gửi sản phẩm đã lưu để quản trị viên xét duyệt? Sản phẩm chỉ hiển thị công khai sau khi được duyệt.'))return;
  busy.value=true;error.value='';success.value='';
  try{const previous=product.value.revision;product.value=await request('/seller/products/'+id+'/submit','POST',{revision:previous});if(!contentEditor.value?.rebase(previous,product.value.revision)){conflict.value=true;throw new Error('Đã gửi duyệt nhưng phiên bản trình sửa chưa đồng bộ. Đối chiếu dữ liệu máy chủ trước khi sửa tiếp.');}content.value={...content.value,revision:product.value.revision};success.value='Đã gửi duyệt. Theo dõi kết quả tại đây.';await loadHistory();}catch(e){await showError(e);}finally{busy.value=false;}
 }
 async function reload(){if(locked.value)return;if((dirty.value||stockEditing.value)&&!window.confirm('Bỏ các trường và tồn kho chưa lưu để tải lại dữ liệu máy chủ?'))return;if(galleryManager.value&&!galleryManager.value.canDiscard())return;await load();}
 function stockUpdated(value){stock.value=value;product.value={...product.value,stock:value.stock};form.stock=value.stock;saved.value=JSON.stringify({...JSON.parse(saved.value),stock:value.stock});}
 function jump(target){const e=document.getElementById(target);e?.scrollIntoView({block:'start',behavior:'instant'});e?.focus({preventScroll:true});}
 function openPreview(event){if(dirty.value||!content.value||!gallery.value)return;preview.value?.open(event.currentTarget);}
 function beforeUnload(e){if(dirty.value||locked.value||stockEditing.value){e.preventDefault();e.returnValue='';}}
 onBeforeRouteLeave(to=>['login','forbidden'].includes(to.name)||(!locked.value&&(!(basicDirty.value||stockEditing.value)||window.confirm('Rời sản phẩm và bỏ thông tin cơ bản hoặc tồn kho chưa lưu?'))));
 onMounted(()=>{load();window.addEventListener('beforeunload',beforeUnload);});onBeforeUnmount(()=>{active=false;window.removeEventListener('beforeunload',beforeUnload);});
 return {id,creating,form,product,categories,content,gallery,stock,history,loading,loaded,busy,error,success,fields,conflict,historyError,lastSaved,contentEditor,basicForm,errorSummary,preview,galleryManager,contentState,galleryState,stockEditing,stockSaving,basicDirty,dirty,locked,category,back,requirements,canSubmit,save,submit,reload,load,loadHistory,contentSaved,stockUpdated,jump,openPreview};
}
