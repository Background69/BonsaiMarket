import {marketplaceRequest} from './marketplace.js';
import {usePage} from '../legacy/navigation.js';

// Hợp đồng API giỏ hàng (backend cần triển khai khớp với các đường dẫn này).
// Mọi phản hồi thành công trả về CartView:
//   { items: [{ cartItemId, productId, name, imageUrl, storeId, storeName,
//               unitPrice, quantity, lineTotal, stock, available }] }
// Chủ sở hữu giỏ luôn lấy từ phiên đăng nhập, frontend không bao giờ gửi userId.

export const MAX_CART_QUANTITY = 1000000;

export const loadCart = () => marketplaceRequest('/account/cart');

export const addCartItem = (productId, quantity = 1) =>
    marketplaceRequest('/account/cart/items', 'POST', {productId, quantity});

export const updateCartItem = (cartItemId, quantity) =>
    marketplaceRequest('/account/cart/items/' + encodeURIComponent(cartItemId), 'PUT', {quantity});

export const removeCartItem = cartItemId =>
    marketplaceRequest('/account/cart/items/' + encodeURIComponent(cartItemId), 'DELETE');

export const cartItems = cart => (Array.isArray(cart?.items) ? cart.items : []);

// Cập nhật số lượng hiển thị trên biểu tượng giỏ hàng ở header.
export function syncCartCount(cart) {
    const total = cartItems(cart).reduce((sum, item) => sum + (Number(item.quantity) || 0), 0);
    usePage().props.cartCount = total;
    return total;
}