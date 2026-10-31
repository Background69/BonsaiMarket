import { marketplaceRequest } from './marketplace.js';

export const fetchMyOrders = (status = '', page = 0, size = 10, filters = {}) => {
    let url = `/account/orders?page=${page}&size=${size}`;
    if (status && status !== 'ALL') {
        url += `&status=${encodeURIComponent(status)}`;
    }
    for (const key of ['search', 'from', 'to']) if (typeof filters[key] === 'string' && filters[key]) url += `&${key}=${encodeURIComponent(filters[key])}`;
    return marketplaceRequest(url);
};

export const fetchMyOrderDetail = (orderId) => {
    return marketplaceRequest(`/account/orders/${encodeURIComponent(orderId)}`);
};

export const cancelMyOrder = (orderId, reason = '') => {
    return marketplaceRequest(`/account/orders/${encodeURIComponent(orderId)}/cancel`, 'POST', { reason });
};
