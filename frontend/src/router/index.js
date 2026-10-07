import {createRouter, createWebHistory} from 'vue-router';
import {defineAsyncComponent, defineComponent, h, ref, watch} from 'vue';
import api from '../services/api.js';
import FeaturePage from '../pages/FeaturePage.vue';

const emptyPage = {data: [], total: 0, links: [], last_page: 1};

function usableImage(url) {
    if (typeof url !== 'string' || !url.trim()) return null;
    const value = url.trim();
    if (/\/(storage|resources)\//i.test(value) || /\.php(?:[?#]|$)/i.test(value)
        || /^https?:\/\/(?:localhost|127\.0\.0\.1)(?::|\/)/i.test(value)) return null;
    return /^(https?:\/\/|\/[^/])/.test(value) ? value : null;
}

function adaptProduct(product) {
    return {
        ...product,
        compare_price: product.comparePrice,
        image: usableImage(product.imageUrl),
        category: product.category?.name || null,
        store: product.store || null,
        status: product.status?.toLowerCase() || 'active',
    };
}

async function catalog() {
    const [categoryResponse, productResponse, storeResponse] = await Promise.all([
        api.get('/categories'), api.get('/products'), api.get('/stores'),
    ]);
    const products = productResponse.data.map(adaptProduct);
    const categories = categoryResponse.data.map(category => ({
        ...category,
        products_count: products.filter(product => product.category === category.name).length,
    }));
    const stores = storeResponse.data.map(store => ({
        ...store,
        logo: usableImage(store.logoUrl), status: store.status?.toLowerCase(),
        products_count: products.filter(product => product.store?.id === store.id).length,
    }));
    return {products, categories, stores};
}

function page(importer, load = async () => ({}), initial = {}, detail = false) {
    const View = defineAsyncComponent(importer);
    return defineComponent({
        setup() {
            const props = ref({...initial, isLoading: true, apiError: false});
            const error = ref('');
            const loading = ref(false);
            let requestId = 0;
            watch(() => router.currentRoute.value.fullPath, async () => {
                const currentRequest = ++requestId;
                error.value = '';
                loading.value = true;
                try {
                    const result = await load(router.currentRoute.value);
                    if (currentRequest === requestId) props.value = {
                        ...initial, ...result,
                        isLoading: false,
                        apiError: false
                    };
                } catch (cause) {
                    if (currentRequest === requestId) {
                        error.value = cause.status === 404 ? 'Không tìm thấy dữ liệu.' : 'Không thể tải dữ liệu. Vui lòng thử lại.';
                        props.value = {...initial, isLoading: false, apiError: true};
                    }
                } finally {
                    if (currentRequest === requestId) loading.value = false;
                }
            }, {immediate: true});
            return () => h('div', [
                loading.value ? h('p', {class: 'integration-loading', role: 'status'}, 'Đang tải dữ liệu...') : null,
                error.value && !detail ? h('p', {class: 'integration-error', role: 'alert'}, error.value) : null,
                /^(\/admin|\/seller|\/profile)/.test(router.currentRoute.value.path)
                    ? h('p', {class: 'integration-demo'}, /\/products$/.test(router.currentRoute.value.path)
                        ? 'Trang chỉ đọc catalog công khai. Chưa có lọc theo người bán hoặc quyền quản trị.'
                        : 'Giao diện mẫu. Chức năng đang được phát triển.') : null,
                loading.value && detail ? null : error.value && detail
                    ? h(FeaturePage, {
                        title: error.value,
                        message: 'Vui lòng quay lại danh sách sản phẩm hoặc cửa hàng.'
                    }) : h(View, props.value),
            ]);
        },
    });
}

const routes = [
    {
        path: '/', name: 'home', component: page(() => import('../pages/Marketplace/Home.vue'), async () => {
            const data = await catalog();
            return {categories: data.categories, featuredProducts: data.products.slice(0, 12), stores: data.stores};
        }, {categories: [], featuredProducts: [], stores: []})
    },
    {
        path: '/products',
        name: 'products',
        component: page(() => import('../pages/Marketplace/Products/Index.vue'), async route => {
            const data = await catalog();
            let products = data.products;
            const q = route.query;
            if (q.search) products = products.filter(p => p.name.toLocaleLowerCase().includes(String(q.search).toLocaleLowerCase()));
            if (q.category) products = products.filter(p => p.category === data.categories.find(c => c.slug === q.category)?.name);
            if (q.store) products = products.filter(p => String(p.store?.id) === String(q.store));
            if (q.min_price) products = products.filter(p => p.price >= Number(q.min_price));
            if (q.max_price) products = products.filter(p => p.price <= Number(q.max_price));
            if (q.in_stock) products = products.filter(p => p.stock > 0);
            if (q.sort === 'price_asc') products.sort((a, b) => a.price - b.price);
            if (q.sort === 'price_desc') products.sort((a, b) => b.price - a.price);
            if (q.sort === 'name_asc') products.sort((a, b) => a.name.localeCompare(b.name));
            return {
                products: {...emptyPage, data: products, total: products.length},
                categories: data.categories,
                stores: data.stores,
                filters: q
            };
        }, {products: emptyPage, categories: [], stores: [], filters: {}})
    },
    {
        path: '/products/:id',
        name: 'product-show',
        component: page(() => import('../pages/Marketplace/Products/Show.vue'), async route => {
            const response = await api.get(`/products/${encodeURIComponent(route.params.id)}`);
            const product = response.data;
            return {product: adaptProduct(product), relatedProducts: []};
        }, {product: {name: '', price: 0, stock: 0}, relatedProducts: []}, true)
    },
    {
        path: '/categories',
        name: 'categories',
        component: page(() => import('../pages/Marketplace/Categories/Index.vue'), async () => {
            const data = await catalog();
            return {categories: data.categories};
        }, {categories: []})
    },
    {
        path: '/stores',
        name: 'stores',
        component: page(() => import('../pages/Marketplace/Stores/Index.vue'), async () => {
            const data = await catalog();
            return {stores: {...emptyPage, data: data.stores, total: data.stores.length}};
        }, {stores: emptyPage})
    },
    {
        path: '/stores/:id',
        name: 'store-show',
        component: page(() => import('../pages/Marketplace/Stores/Show.vue'), async route => {
            const data = await catalog();
            const response = await api.get(`/stores/${encodeURIComponent(route.params.id)}`);
            const products = data.products.filter(p => p.store?.id === response.data.id);
            return {
                store: {
                    ...response.data,
                    logo: usableImage(response.data.logoUrl),
                    status: response.data.status?.toLowerCase(),
                    products_count: products.length
                }, products: {...emptyPage, data: products}
            };
        }, {store: {name: ''}, products: emptyPage}, true)
    },
    {
        path: '/cart',
        name: 'cart',
        component: page(() => import('../pages/Marketplace/Cart/Index.vue'), undefined, {cartItems: []})
    },
    {
        path: '/checkout',
        name: 'checkout',
        component: page(() => import('../pages/Marketplace/Checkout/Index.vue'), undefined, {
            cartItems: [],
            addresses: [],
            provinces: []
        })
    },
    {
        path: '/orders',
        name: 'orders',
        component: page(() => import('../pages/Marketplace/Orders/Index.vue'), undefined, {orders: emptyPage})
    },
    {
        path: '/profile',
        name: 'profile',
        component: page(() => import('../pages/Marketplace/Profile/Index.vue'), undefined, {
            user: {
                name: 'Khách',
                role: 'buyer'
            }, recentOrders: []
        })
    },
    {
        path: '/wishlist',
        name: 'wishlist',
        component: page(() => import('../pages/Marketplace/Wishlist/Index.vue'), undefined, {
            wishlistItems: emptyPage,
            recommendations: []
        })
    },
    {
        path: '/seller',
        name: 'seller',
        component: page(() => import('../pages/Marketplace/Seller/Dashboard.vue'), undefined, {
            store: {},
            stats: {total_products: 0, total_orders: 0, pending_orders: 0, revenue: 0},
            recentOrders: [],
            lowStockProducts: [],
            topProducts: []
        })
    },
    {
        path: '/seller/products',
        name: 'seller-products',
        component: page(() => import('../pages/Marketplace/Seller/Products/Index.vue'), async () => {
            const response = await api.get('/products');
            const products = response.data.map(adaptProduct);
            return {products: {...emptyPage, data: products, total: products.length}};
        }, {products: emptyPage})
    },
    {
        path: '/seller/orders',
        name: 'seller-orders',
        component: page(() => import('../pages/Marketplace/Seller/Orders/Index.vue'), undefined, {orders: emptyPage})
    },
    {
        path: '/admin',
        name: 'admin',
        component: page(() => import('../pages/Admin/Dashboard.vue'), undefined, {
            storesCount: 0,
            productsCount: 0,
            usersCount: 0,
            ordersCount: 0,
            revenueThisMonth: 0,
            recentOrders: [],
            topProducts: [],
            chartLabels: [],
            chartData: [],
            ordersByStatus: {}
        })
    },
    {
        path: '/admin/products',
        name: 'admin-products',
        component: page(() => import('../pages/Admin/Products/Index.vue'), async () => {
            const [productResponse, categoryResponse] = await Promise.all([api.get('/products'), api.get('/categories')]);
            const products = productResponse.data.map(adaptProduct);
            return {
                products: {...emptyPage, data: products, total: products.length},
                categories: categoryResponse.data
            };
        }, {products: emptyPage, categories: []})
    },
    {
        path: '/admin/users',
        name: 'admin-users',
        component: page(() => import('../pages/Admin/Users/Index.vue'), undefined, {users: emptyPage})
    },
    {
        path: '/admin/orders',
        name: 'admin-orders',
        component: page(() => import('../pages/Admin/Orders/Index.vue'), undefined, {orders: emptyPage})
    },
    {path: '/login', name: 'login', component: FeaturePage, props: {title: 'Đăng nhập'}},
    {path: '/register', name: 'register', component: FeaturePage, props: {title: 'Đăng ký'}},
    {
        path: '/coming-soon',
        name: 'coming-soon',
        component: FeaturePage,
        props: {title: 'Chức năng đang được phát triển'}
    },
    {path: '/:pathMatch(.*)*', redirect: '/coming-soon'},
];

const router = createRouter({history: createWebHistory(), routes, scrollBehavior: () => ({top: 0})});
export default router;
