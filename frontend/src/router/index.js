import {createRouter, createWebHistory} from 'vue-router';
import {defineAsyncComponent, defineComponent, h, ref, watch, onBeforeUnmount} from 'vue';
import api from '../services/api.js';
import FeaturePage from '../pages/FeaturePage.vue';
import {restoreSession, useAuth} from '../composables/useAuth.js';
import {productPage, usableImage, adaptProduct} from '../services/catalog.js';

const emptyPage = {data: [], total: 0, links: [], last_page: 1};

function page(importer, load = async () => ({}), initial = {}, detail = false) {
    const View = defineAsyncComponent(importer);
    return defineComponent({
        setup() {
            const props = ref({...initial, isLoading: true, apiError: false});
            const error = ref('');
            const loading = ref(false);
            let requestId = 0;
            const routeName = router.currentRoute.value.name;
            onBeforeUnmount(() => requestId++);
            const localState = () => ['products', 'store-show', 'categories', 'stores', 'admin-products'].includes(router.currentRoute.value.name);
            async function reload() {
                const currentRequest = ++requestId;
                error.value = '';
                loading.value = true;
                props.value = {...props.value, isLoading: true, apiError: false};
                try {
                    const result = await load(router.currentRoute.value);
                    if (currentRequest === requestId) props.value = {
                        ...initial, ...result,
                        isLoading: false,
                        apiError: false
                    };
                } catch (cause) {
                    if (currentRequest === requestId) {
                        error.value = cause.status === 404 ? 'Không tìm thấy dữ liệu.' : cause.status === 400 ? cause.message : 'Không thể tải dữ liệu. Vui lòng thử lại.';
                        props.value = {...initial, ...(localState() ? {store: props.value.store} : {}), isLoading: false, apiError: true, errorMessage: error.value};
                    }
                } finally {
                    if (currentRequest === requestId) loading.value = false;
                }
            }
            watch(() => router.currentRoute.value.fullPath, () => {
                if (router.currentRoute.value.name === routeName) reload();
            }, {immediate: true});
            return () => h('div', [
                loading.value && !localState() ? h('p', {class: 'integration-loading', role: 'status'}, 'Đang tải dữ liệu...') : null,
                error.value && !detail && !localState() ? h('div', {class: 'integration-error', role: 'alert'}, [error.value,
                    h('button', {class: 'button button-outline', onClick: () => router.go(0)}, 'Thử lại')]) : null,
                loading.value && detail && !localState() ? null : error.value && detail && !localState()
                    ? h(FeaturePage, {
                        title: error.value,
                        message: 'Vui lòng quay lại danh sách sản phẩm hoặc cửa hàng.'
                    }) : h(View, {...props.value, onRetry: reload}),
            ]);
        },
    });
}

const routes = [
    {path:'/store-application',name:'store-application',component:()=>import('../pages/Marketplace/SellerApplication.vue')},
    {path:'/admin/categories',name:'admin-categories',component:()=>import('../pages/Admin/Categories.vue')},
    {path:'/admin/store-applications',name:'admin-store-applications',component:page(()=>import('../pages/Admin/CaseQueue.vue'),async()=>({kind:'applications'}),{kind:'applications'})},
    {path:'/admin/reports',name:'admin-reports',component:page(()=>import('../pages/Admin/CaseQueue.vue'),async()=>({kind:'reports'}),{kind:'reports'})},
    {path:'/seller/store',name:'seller-store',component:()=>import('../pages/Marketplace/Seller/StoreSettings.vue')},
    {path:'/admin/statistics',name:'admin-statistics',component:()=>import('../pages/Admin/Statistics.vue')},
    {path:'/admin/users/:id(\\d+)',name:'admin-user-show',component:()=>import('../pages/Admin/Users/Show.vue')},
    {path:'/admin/orders/:id(\\d+)',name:'admin-order-show',component:()=>import('../pages/Admin/Orders/Show.vue')},
    {path:'/seller/orders/:id(\\d+)',name:'seller-order-show',component:()=>import('../pages/Marketplace/Seller/Orders/Show.vue')},
    {path:'/seller/images',name:'seller-images',component:()=>import('../pages/Marketplace/Seller/ProductImages.vue')},
    {
        path: '/', name: 'home', component: () => import('../pages/Marketplace/Home.vue')
    },
    {
        path: '/products',
        name: 'products',
        component: page(() => import('../pages/Marketplace/Products/Index.vue'), async route => {
            const [products, categories, stores] = await Promise.all([productPage(route.query), api.get('/categories'), api.get('/stores')]);
            return {
                products, categories: categories.data, stores: stores.data, filters: route.query
            };
        }, {products: emptyPage, categories: [], stores: [], filters: {}})
    },
    {
        path: '/products/:id',
        name: 'product-show',
        component: page(() => import('../pages/Marketplace/Products/Show.vue'), async route => {
            const response = await api.get(`/products/${encodeURIComponent(route.params.id)}`);
            const product = response.data;
            let relatedProducts = [];
            try {
                const others = await productPage({store: product.store.id, size: 5});
                relatedProducts = others.data.filter(p => p.id !== product.id).slice(0, 4);
            } catch { /* Product detail remains available if related catalog fails. */
            }
            return {product: adaptProduct(product), relatedProducts};
        }, {product: {name: '', price: 0, stock: 0}, relatedProducts: []}, true)
    },
    {
        path: '/categories',
        name: 'categories',
        component: page(() => import('../pages/Marketplace/Categories/Index.vue'), async () => {
            return {categories: (await api.get('/categories')).data};
        }, {categories: []})
    },
    {
        path: '/stores',
        name: 'stores',
        component: page(() => import('../pages/Marketplace/Stores/Index.vue'), async () => {
            const {data} = await api.get('/stores');
            return {stores: {...emptyPage, data: data.map(s => ({...s, logo: usableImage(s.logoUrl), status: s.status?.toLowerCase()})), total: data.length}};
        }, {stores: emptyPage})
    },
    {
        path: '/stores/:id',
        name: 'store-show',
        component: page(() => import('../pages/Marketplace/Stores/Show.vue'), async route => {
            const [response, products] = await Promise.all([api.get(`/stores/${encodeURIComponent(route.params.id)}`), productPage({...route.query, store: route.params.id})]);
            return {
                store: {
                    ...response.data,
                    logo: usableImage(response.data.logoUrl),
                    status: response.data.status?.toLowerCase(),
                }, products
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
        component: page(() => import('../pages/Marketplace/Profile/Index.vue'), async () => ({user: useAuth().user.value, recentOrders: []}), {
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
            return {};
        }, {products: emptyPage})
    },
    {
        path: '/seller/orders',
        name: 'seller-orders',
        component: () => import('../pages/Marketplace/Seller/Orders/Index.vue')
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
        component: page(() => import('../pages/Admin/Products/Index.vue'), async route => {
            const [products, categoryResponse] = await Promise.all([productPage(route.query), api.get('/categories')]);
            return {
                products,
                categories: categoryResponse.data
            };
        }, {products: emptyPage, categories: []})
    },
    {
        path: '/admin/users',
        name: 'admin-users',
        component: () => import('../pages/Admin/Users/Index.vue')
    },
    {
        path: '/admin/orders',
        name: 'admin-orders',
        component: () => import('../pages/Admin/Orders/Index.vue')
    },
    ...['login', 'register', 'verify-email', 'forgot-password'].map(name => ({path: '/' + name, name, component: () => import('../pages/Auth/AuthPage.vue')})),
    {path: '/reset-password', redirect: '/forgot-password'},
    {path: '/oauth/complete', name: 'oauth-complete', component: () => import('../pages/Auth/AuthPage.vue')},
    {path: '/auth/callback', name: 'auth-callback', component: () => import('../pages/Auth/AuthPage.vue')},
    {path: '/forbidden', name: 'forbidden', component: FeaturePage, props: {title: 'Bạn không có quyền truy cập trang này', message: 'Vui lòng dùng tài khoản có quyền phù hợp hoặc quay về trang chủ.'}},
    {
        path: '/coming-soon',
        name: 'coming-soon',
        component: FeaturePage,
        props: {title: 'Khám phá BonsaiMarket', message: 'Tìm sản phẩm và gian hàng từ những người yêu cây cảnh.'}
    },
    {path: '/:pathMatch(.*)*', name:'not-found', component:FeaturePage, props:{title:'Không tìm thấy trang',message:'Đường dẫn có thể đã thay đổi hoặc không tồn tại.',notFound:true}},
];

const legacyDestinations = {
    '/cart': ['Giỏ hàng', 'Trang này chưa nhận thao tác thêm hoặc cập nhật giỏ hàng.'],
    '/checkout': ['Thanh toán', 'Trang này chưa nhận đơn đặt hàng hoặc thông tin thanh toán.'],
    '/orders': ['Đơn mua', 'Trang này chưa cung cấp lịch sử giao dịch.'],
    '/wishlist': ['Sản phẩm yêu thích', 'Trang này chưa lưu danh sách sản phẩm yêu thích.'],
};
for (const route of routes) {
    const recovery = legacyDestinations[route.path];
    if (recovery) {
        route.component = FeaturePage;
        route.props = {title: recovery[0], message: recovery[1],
            ...(route.path.startsWith('/admin') ? {returnTo:'/admin',returnLabel:'Về quản trị sàn'}
                : route.path.startsWith('/seller') ? {returnTo:'/seller/products',returnLabel:'Quản lý tồn kho'} : {})};
    }
}

const router = createRouter({
    history: createWebHistory(),
    routes,
    scrollBehavior: (to, from, saved) => saved || (to.hash ? {el: to.hash, top: 180} : {top: 0})
});
router.beforeEach(async to => {
    const privateRoute = /^\/(?:admin|seller|profile|orders|wishlist|checkout|store-application)(?:\/|$)/.test(to.path);
    if (!privateRoute) return;
    const user = await restoreSession(true);
    if (!user) return {name: 'login', query: {redirect: to.fullPath, notice: 'session-expired'}};
    if (/^\/admin(?:\/|$)/.test(to.path) && user.role !== 'admin') return '/forbidden';
    if (/^\/seller(?:\/|$)/.test(to.path) && !['seller', 'admin'].includes(user.role)) return '/forbidden';
});
window.addEventListener('bonsai:logged-out', () => router.replace('/login'));
window.addEventListener('bonsai:session-expired', () => {
    const current = router.currentRoute.value;
    if (/^\/(?:admin|seller|profile|orders|wishlist|checkout|store-application)(?:\/|$)/.test(current.path))
        router.replace({name: 'login', query: {notice: 'session-expired', redirect: current.fullPath}});
});
export default router;
