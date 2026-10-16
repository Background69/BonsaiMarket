import api from './api.js';

export function usableImage(url) {
    if (typeof url !== 'string' || !url.trim()) return null;
    const value = url.trim();
    if (/\/(storage|resources)\//i.test(value) || /\.php(?:[?#]|$)/i.test(value)
        || /^https?:\/\/(?:localhost|127\.0\.0\.1)(?::|\/)/i.test(value)) return null;
    return /^(https?:\/\/|\/[^/])/.test(value) ? value : null;
}
export function adaptProduct(product) {
    return {...product, compare_price: product.comparePrice, image: usableImage(product.imageUrl),
        category: product.category?.name || null, store: product.store || null, status: product.status?.toLowerCase()};
}
export async function productPage(query = {}) {
    const allowed = ['search', 'q', 'category', 'categoryId', 'store', 'min_price', 'max_price', 'in_stock', 'sort', 'page', 'size'];
    const params = new URLSearchParams();
    for (const key of allowed) if (query[key] !== undefined && query[key] !== null && query[key] !== '') params.set(key, String(query[key]));
    const {data} = await api.get('/products?' + params);
    return {...data, data: data.content.map(adaptProduct), total: data.totalElements, last_page: data.totalPages};
}
