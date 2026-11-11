import {marketplaceRequest as request} from './marketplace.js';
import {unreadableResponseError} from './presentation.js';
const count=value=>Number.isSafeInteger(value)&&value>=0;
export async function accountProfile(){
    const value=await request('/account/profile');
    if(!value||typeof value.name!=='string'||typeof value.email!=='string'||typeof value.emailVerified!=='boolean')throw unreadableResponseError();
    return value;
}
export async function accountAddresses(){
    const value=await request('/account/addresses');
    if(!Array.isArray(value)||!value.every(row=>row&&Number.isSafeInteger(row.id)&&row.id>0&&typeof row.recipientName==='string'&&typeof row.phone==='string'&&typeof row.isDefault==='boolean'))throw unreadableResponseError();
    return value;
}
export async function accountOrderSummary(){
    const value=await request('/account/orders/summary');
    if(!value||!['pendingCount','shippingCount','completedCount','reviewEligibleCount'].every(key=>count(value[key])))throw unreadableResponseError();
    return value;
}
export async function accountUnreadCount(){
    const value=await request('/account/notifications/count');
    if(!value||!count(value.unread))throw unreadableResponseError();
    return value.unread;
}
