import {marketplaceRequest} from './marketplace.js';

export const loadCheckout = () => marketplaceRequest('/account/checkout');
export const createQuote = body => marketplaceRequest('/account/checkout/quote', 'POST', body);
export const placeOrder = quoteId => marketplaceRequest('/account/checkout/orders', 'POST', {quoteId});
export const loadQuote = id => marketplaceRequest('/account/checkout/quotes/' + encodeURIComponent(id));
export const loadReceipt = id => marketplaceRequest('/account/checkout/receipts/' + encodeURIComponent(id));
