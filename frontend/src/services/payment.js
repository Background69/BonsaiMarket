import { marketplaceRequest } from './marketplace.js';
export const reconcileVnpay = code => marketplaceRequest('/account/payments/vnpay/'+encodeURIComponent(code)+'/reconcile','POST');

export const initPayment = (data) => {
    return marketplaceRequest('/account/payments/init', 'POST', data);
};

export const verifyVnpayCallback = (params) => {
    return marketplaceRequest('/account/payments/vnpay-callback', 'POST', params);
};

export const mockConfirmPayment = (txnCode) => {
    return marketplaceRequest(`/account/payments/mock-confirm/${encodeURIComponent(txnCode)}`, 'POST');
};

export const fetchOrderTransactions = (orderId) => {
    return marketplaceRequest(`/account/payments/order/${encodeURIComponent(orderId)}`);
};

export const fetchBankTransferInfo = (orderId) => {
    return marketplaceRequest(`/account/payments/banking/${encodeURIComponent(orderId)}`);
};
