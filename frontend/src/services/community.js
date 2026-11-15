import {uiErrorMessage,responseData} from './presentation.js';
import {authRequest} from './api.js';
export const postTypes={QUESTION:'Hỏi đáp chăm cây',SHOWCASE:'Khoe góc xanh',GUIDE:'Chia sẻ kinh nghiệm',PLANT_HELP:'Cứu cây',ANNOUNCEMENT:'Thông báo'};
export const helpStates={OPEN:'Đang cần giúp đỡ',HELPFUL:'Đã có giải pháp hữu ích',RESOLVED:'Tác giả đã xác nhận giải quyết'};
export const roleLabels={CUSTOMER:'Thành viên',SELLER:'Người bán',ADMIN:'Quản trị viên'};
export const careFields={plantName:['Tên hoặc loại cây',180],symptoms:['Triệu chứng',1000],duration:['Thời gian xuất hiện',180],lighting:['Điều kiện ánh sáng',500],watering:['Cách tưới nước',500],substrate:['Giá thể (nếu biết)',500],attemptedCare:['Những biện pháp đã thử',1000]};
export function newCare(){return Object.fromEntries(Object.keys(careFields).map(k=>[k,'']));}
export async function uploadCommunityImage(post,file){
  const csrf=await authRequest('/csrf'),data=new FormData();data.append('file',file.file);
  let response;try{response=await fetch('/api/account/community/posts/'+post.id+'/images?revision='+post.revision+'&requestKey='+file.key,{method:'POST',credentials:'include',headers:{[csrf.headerName]:csrf.token},body:data,signal:AbortSignal.timeout(15000)});}catch{throw new Error('Chưa xác nhận được tải ảnh. Bài viết đã được lưu; giữ ảnh và thử lưu lại để kiểm tra.');}
  const body=await responseData(response);if(!response.ok)throw Object.assign(new Error(uiErrorMessage(response.status,body)),{status:response.status});return body;
}
