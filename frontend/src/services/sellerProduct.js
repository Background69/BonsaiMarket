// Guidance mirrors ProductPublication; submission still rechecks current server policy.
export const productStatusLabels={DRAFT:'Bản nháp',PENDING:'Chờ duyệt',ACTIVE:'Đang hiển thị',REJECTED:'Bị từ chối',ARCHIVED:'Đã lưu trữ'};
export function productRequirements(product,content,gallery){
 return [
  {key:'name',label:'Tên sản phẩm hợp lệ',target:'bm-seller-name',met:!!product?.name?.trim()&&product.name.trim().length<=180},
  {key:'category',label:'Đã chọn danh mục',target:'bm-seller-category',met:Number(product?.categoryId)>0},
  {key:'price',label:'Giá bán hợp lệ',target:'bm-seller-price',met:Number(product?.price)>=1&&Number(product?.price)<=9999999999999.99},
  {key:'content',label:'Đã lưu mô tả',target:'bm-seller-content',met:!!content?.description?.trim()},
  {key:'gallery',label:'Có ảnh sản phẩm đã lưu',target:'bm-seller-gallery',met:!!gallery&&(gallery.images.length>0||!!gallery.imageUrl)},
  {key:'stock',label:'Tồn khả dụng lớn hơn 0',target:'bm-seller-stock',met:Number(product?.stock)>0},
 ];
}
export const inventoryReturnQuery=query=>Object.fromEntries(['page','inventorySearch','inventoryStock','inventoryStatus','inventoryCategory','inventorySort'].filter(key=>query[key]!==undefined).map(key=>[key,query[key]]));
