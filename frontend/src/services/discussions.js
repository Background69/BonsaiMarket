export const topics={BONSAI:'Bonsai',SUCCULENTS:'Sen đá',CARE:'Chăm sóc cây',OTHER:'Chuyện góc vườn'};
export const date=value=>value?new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium',timeZone:'Asia/Ho_Chi_Minh'}).format(new Date(value+'Z')):'';
export function preview(body){const text=String(body||'').replace(/\s+/g,' ');if(text.length<=180)return text;return text.slice(0,180).replace(/\s+\S*$/,'')+'…';}
