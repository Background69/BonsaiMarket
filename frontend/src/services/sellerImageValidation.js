// Decode before offering upload; a valid signature alone does not prove a usable image.
export async function decodeSellerImage(file){
 const url=URL.createObjectURL(file),image=new Image();
 try{
  image.src=url;await image.decode();
  if(!image.naturalWidth||!image.naturalHeight)throw new Error('Ảnh không có kích thước hợp lệ.');
  if(image.naturalWidth>6000||image.naturalHeight>6000||image.naturalWidth*image.naturalHeight>20000000)return 'Ảnh vượt 6.000 px mỗi chiều hoặc 20 triệu pixel. Hãy giảm kích thước rồi chọn lại.';
  return '';
 }catch{return 'Tệp ảnh bị hỏng hoặc không giải mã được. Chọn bản ảnh khác; tải lại tệp này sẽ không khắc phục lỗi.';}
 finally{image.src='';URL.revokeObjectURL(url);}
}
