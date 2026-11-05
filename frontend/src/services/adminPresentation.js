export const productStatuses={DRAFT:'Bản nháp',PENDING:'Chờ xét duyệt',ACTIVE:'Đang hiển thị',REJECTED:'Đã hạn chế',ARCHIVED:'Đã gỡ / lưu trữ'};
export const storeStatuses={ACTIVE:'Đang hoạt động',INACTIVE:'Đã đình chỉ'};
export const caseStatuses={OPEN:'Mới gửi',IN_REVIEW:'Đang xem xét',RESOLVED:'Đã ghi nhận kết luận',CLOSED:'Đã đóng'};
export const refundStatuses={REQUESTED:'Chờ xét duyệt',APPROVED:'Đã duyệt nội bộ',DENIED:'Đã từ chối',PROCESSING:'Đang xử lý',AWAITING_EXTERNAL:'Chờ xác minh bên ngoài',UNKNOWN:'Chưa rõ kết quả',FAILED:'Xử lý thất bại'};
export const dateTime=v=>v?new Intl.DateTimeFormat('vi-VN',{dateStyle:'short',timeStyle:'short'}).format(new Date(/(?:Z|[+-]\d\d:\d\d)$/.test(v)?v:v+'Z')):'Chưa ghi nhận';
export const money=v=>new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND',maximumFractionDigits:0}).format(v??0);
