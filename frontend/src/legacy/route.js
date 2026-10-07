// Local URL mapping for the mounted Vue pages. Unknown Laravel names lead to a clear deferred page.
const routes = {
    'agriverse.shop.home': '/',
    'agriverse.shop.products.index': '/products',
    'agriverse.shop.products.show': '/products/:id',
    'agriverse.shop.categories.index': '/categories',
    'agriverse.shop.stores.index': '/stores',
    'agriverse.shop.stores.show': '/stores/:id',
    'agriverse.shop.cart.index': '/cart',
    'agriverse.shop.checkout.index': '/checkout',
    'agriverse.shop.orders.index': '/orders',
    'agriverse.shop.profile.index': '/profile',
    'agriverse.shop.wishlist.index': '/wishlist',
    'agriverse.shop.seller.dashboard': '/seller',
    'agriverse.shop.seller.products.index': '/seller/products',
    'agriverse.shop.seller.orders.index': '/seller/orders',
    'admin.agriverse.dashboard': '/admin',
    'admin.agriverse.products.index': '/admin/products',
    'admin.agriverse.users.index': '/admin/users',
    'admin.agriverse.orders.index': '/admin/orders',
    'login': '/login',
    'register': '/register',
};

export function route(name, params = {}) {
    if (!name) {
        return {
            current(pattern) {
                const prefix = pattern.replace(/\*$/, '');
                return Object.entries(routes).some(([key, path]) => {
                    if (!key.startsWith(prefix)) return false;
                    const pattern = '^' + path.replace(/:[^/]+/g, '[^/]+') + '$';
                    return new RegExp(pattern).test(window.location.pathname);
                });
            }
        };
    }
    const target = routes[name];
    if (!target) return `/coming-soon?feature=${encodeURIComponent(name)}`;
    const values = typeof params === 'object' && params !== null ? {...params} : {id: params};
    const path = target.replace(/:([a-z]+)/g, (_, key) => encodeURIComponent(values[key] ?? ''));
    Object.keys(values).forEach(key => {
        if (target.includes(`:${key}`)) delete values[key];
    });
    const query = new URLSearchParams(Object.entries(values).filter(([, value]) => value !== null && value !== '' && value !== undefined));
    return path + (query.size ? `?${query}` : '');
}

export default route;
