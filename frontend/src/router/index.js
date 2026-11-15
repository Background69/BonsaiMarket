import {createRouter, createWebHistory} from 'vue-router';
import {defineAsyncComponent, defineComponent, h, ref, watch, onBeforeUnmount} from 'vue';
import api from '../services/api.js';
import FeaturePage from '../pages/FeaturePage.vue';
import {restoreSession, useAuth} from '../composables/useAuth.js';
import {productPage, storePage, usableImage, adaptProduct} from '../services/catalog.js';
import ProductDetailLoading from '../components/marketplace/ProductDetailLoading.vue';

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
            const localState = () => ['products', 'store-show', 'store-products', 'categories', 'stores', 'admin-products'].includes(router.currentRoute.value.name);
            async function reload() {
                const currentRequest = ++requestId;
                error.value = '';
                loading.value = true;
                props.value = {...props.value, isLoading: true, apiError: false};
                try {
                    const currentRoute=router.currentRoute.value;
                    const result = await load(currentRoute);
                    const list=result.products || result.stores;
                    if(currentRequest===requestId && Number.isInteger(list?.totalPages) && Number(currentRoute.query.page||0)>Math.max(0,list.totalPages-1)){
                        await router.replace({path:currentRoute.path,query:{...currentRoute.query,page:String(Math.max(0,list.totalPages-1))},hash:currentRoute.hash});return;
                    }
                    if (currentRequest === requestId) props.value = {
                        ...initial, ...result,
                        isLoading: false,
                        apiError: false
                    };
                } catch (cause) {
                    if (currentRequest === requestId) {
                        error.value = cause.status === 404 ? 'Không tìm thấy dữ liệu.'
                            : cause.status === 401 ? 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
                            : cause.status === 403 ? 'Tài khoản của bạn chưa có quyền xem nội dung này.'
                            : cause.status === 400 ? cause.message
                            : cause.status >= 500 ? 'Máy chủ chưa xử lý được yêu cầu. Vui lòng thử lại sau.'
                            : cause.status ? 'Không thể tải dữ liệu. Vui lòng thử lại.' : 'Không thể kết nối máy chủ. Kiểm tra kết nối và thử lại.';
                        props.value = {...initial, ...(localState()&&String(props.value.store?.id)===String(router.currentRoute.value.params.id) ? {store: props.value.store} : {}), isLoading: false, apiError: true, errorMessage: error.value, errorStatus:cause.status, notFound:cause.status===404};
                    }
                } finally {
                    if (currentRequest === requestId) loading.value = false;
                }
            }
            // Reviews/Q&A own their query/hash state. Keep their drafts mounted on the same product.
            watch(() => routeName === 'product-show' ? router.currentRoute.value.path : router.currentRoute.value.fullPath, () => {
                if (router.currentRoute.value.name === routeName) reload();
            }, {immediate: true});
            return () => loading.value && routeName === 'product-show' ? h(ProductDetailLoading) : h('div', [
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
    {path:'/seller/banner-credits',name:'seller-banner-credits',component:()=>import('../pages/Marketplace/Seller/BannerCredits.vue')},
    {path:'/banner-credits',name:'banner-credits',component:()=>import('../pages/Marketplace/BannerCredits.vue')},
    {path:'/wallet',name:'wallet',component:()=>import('../pages/Marketplace/Wallet.vue')},
    {path:'/admin/credits',name:'admin-credits',component:()=>import('../pages/Admin/Credits.vue')},
    {path:'/seller/banner-campaigns',name:'seller-banner-campaigns',component:()=>import('../pages/Marketplace/BannerCampaigns.vue')},
    {path:'/admin/banner-campaigns',name:'admin-banner-campaigns',component:()=>import('../pages/Marketplace/BannerCampaigns.vue')},
    {path:'/seller/products/new',name:'seller-product-create',component:()=>import('../pages/Marketplace/Seller/Products/Edit.vue')},
    {path:'/seller/products/:id(\\d+)/edit',name:'seller-product-edit',component:()=>import('../pages/Marketplace/Seller/Products/Edit.vue')},
    {path:'/admin/profile',name:'admin-profile',component:()=>import('../pages/Admin/Profile.vue')},
    {path:'/admin/notifications',name:'admin-notifications',component:()=>import('../pages/Admin/Notifications.vue')},
    {path:'/notifications',name:'notifications',component:()=>import('../pages/Marketplace/Notifications.vue')},
    {path:'/admin/audit',name:'admin-audit',component:()=>import('../pages/Admin/Audit.vue')},
    {path:'/support-cases/:id(\\d+)?',name:'support-cases',component:()=>import('../pages/Marketplace/SupportCases.vue')},
    {path:'/admin/stores',name:'admin-stores',component:()=>import('../pages/Admin/Stores.vue')},
    {path:'/admin/stores/:id(\\d+)',name:'admin-store-detail',component:()=>import('../pages/Admin/StoreDetail.vue')},
    {path:'/admin/products/:id(\\d+)',name:'admin-product-detail',component:()=>import('../pages/Admin/ProductModeration.vue')},
    {path:'/admin/transactions',name:'admin-transactions',component:()=>import('../pages/Admin/Payments.vue')},
    {path:'/admin/disputes',name:'admin-disputes',component:()=>import('../pages/Admin/Disputes.vue')},
    {path:'/admin/disputes/:id(\\d+)',name:'admin-dispute-detail',component:()=>import('../pages/Admin/DisputeDetail.vue')},
    {path:'/messages/:id(\\d+)?',name:'private-messages',component:()=>import('../pages/Marketplace/Messages.vue')},
    {path:'/admin/refunds',name:'admin-refunds',component:()=>import('../pages/Admin/RefundManagement.vue')},
    {path:'/admin/refunds/:id(\\d+)',name:'admin-refund-detail',component:()=>import('../pages/Admin/RefundDetail.vue')},
    {path:'/admin/community',name:'admin-community',component:()=>import('../pages/Admin/Community.vue')},
    {path:'/admin/ai-assistant',name:'admin-ai-assistant',component:()=>import('../pages/Admin/AiAssistant.vue')},
    {path:'/community',name:'community',component:()=>import('../pages/Marketplace/Community/Index.vue')},
    {path:'/community/create',name:'community-create',component:()=>import('../pages/Marketplace/Community/Edit.vue')},
    {path:'/community/posts/:id/edit',name:'community-edit',component:()=>import('../pages/Marketplace/Community/Edit.vue')},
    {path:'/community/posts/:id',name:'community-post',component:()=>import('../pages/Marketplace/Community/Show.vue')},
    {path:'/store-application',name:'store-application',component:()=>import('../pages/Marketplace/SellerApplication.vue')},
    {path:'/admin/categories',name:'admin-categories',component:()=>import('../pages/Admin/Categories.vue')},
    {path:'/admin/store-applications',name:'admin-store-applications',component:page(()=>import('../pages/Admin/CaseQueue.vue'),async()=>({kind:'applications'}),{kind:'applications'})},
    {path:'/admin/reports',name:'admin-reports',component:page(()=>import('../pages/Admin/CaseQueue.vue'),async()=>({kind:'reports'}),{kind:'reports'})},
    {path:'/seller/store',name:'seller-store',component:()=>import('../pages/Marketplace/Seller/StoreSettings.vue')},
    {path:'/seller/questions',name:'seller-questions',component:()=>import('../pages/Marketplace/Seller/Questions.vue')},
    {path:'/admin/statistics',name:'admin-statistics',component:()=>import('../pages/Admin/Statistics.vue')},
    {path:'/admin/users/:id(\\d+)',name:'admin-user-show',component:()=>import('../pages/Admin/Users/Show.vue')},
    {path:'/admin/orders/:id(\\d+)',name:'admin-order-show',component:()=>import('../pages/Admin/Orders/Show.vue')},
    {path:'/seller/orders/:id(\\d+)',name:'seller-order-show',component:()=>import('../pages/Marketplace/Seller/Orders/Show.vue')},
    {path:'/seller/images',name:'seller-images',component:()=>import('../pages/Marketplace/Seller/ProductImages.vue')},
    {path:'/seller/products/:id/content',name:'seller-product-content',component:()=>import('../pages/Marketplace/Seller/Products/Content.vue')},
    {
        path: '/', name: 'home', component: () => import('../pages/Marketplace/Home.vue')
    },
    {
        path: '/products',
        name: 'products',
        component: page(() => import('../pages/Marketplace/Products/Index.vue'), async route => {
            const [products, categories, stores] = await Promise.allSettled([productPage(route.query), api.get('/categories'), api.get('/stores')]);
            if(products.status==='rejected')throw products.reason;
            return {
                products:products.value,
                categories:categories.status==='fulfilled'?categories.value.data:[],
                stores:stores.status==='fulfilled'?stores.value.data:[],
                categoriesError:categories.status==='rejected', storesError:stores.status==='rejected', filters: route.query
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
                if (Number.isSafeInteger(product.store?.id) && product.store.id > 0) {
                    const others = await productPage({store: product.store.id, size: 5});
                    relatedProducts = others.data.filter(p => p.id !== product.id).slice(0, 4);
                }
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
        component: page(() => import('../pages/Marketplace/Stores/Index.vue'), async route => {
            const [stores,categories]=await Promise.all([storePage(route.query),api.get('/categories')]);
            return {stores,categories:categories.data};
        }, {stores: emptyPage,categories:[]})
    },
    {
        path: '/stores/:id',
        name: 'store-show',
        component: page(() => import('../pages/Marketplace/Stores/Show.vue'), async route => {
            const response=await api.get(`/stores/${encodeURIComponent(route.params.id)}`);
            let products={...emptyPage},previewError=false;
            try{products=await productPage({store:route.params.id,size:4});}catch{previewError=true;}
            return {store:response.data,products,previewError};
        }, {store: {name: ''}, products: emptyPage}, true)
    },
    {
        path:'/stores/:id/products',name:'store-products',
        component:page(()=>import('../pages/Marketplace/Stores/Products.vue'),async route=>{
            const [response,products,categories]=await Promise.all([api.get(`/stores/${encodeURIComponent(route.params.id)}`),productPage({...route.query,store:route.params.id}),api.get('/categories')]);
            return {store:response.data,products,categories:categories.data};
        },{store:{name:''},products:emptyPage,categories:[]},true)
    },
    {
        path: '/cart',
        name: 'cart',
        component: () => import('../pages/Marketplace/Cart/Index.vue')
    },
    {
        path: '/checkout',
        name: 'checkout',
        component: () => import('../pages/Marketplace/Checkout/Index.vue')
    },
    {
        path: '/orders',
        name: 'orders',
        component: () => import('../pages/Marketplace/Orders/Index.vue')
    },
    {
        path: '/orders/:id(\\d+)',
        name: 'order-detail',
        component: () => import('../pages/Marketplace/Orders/Show.vue')
    },
    {
        path: '/payment',
        name: 'payment',
        component: () => import('../pages/Marketplace/Payment/Index.vue')
    },
    {
        path: '/payment/callback',
        name: 'payment-callback',
        component: () => import('../pages/Marketplace/Payment/Callback.vue')
    },
    {
        path: '/payment/banking',
        name: 'payment-banking',
        component: () => import('../pages/Marketplace/Payment/Banking.vue')
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
        component: () => import('../pages/Admin/Overview.vue')
    },
    {
        path: '/admin/products',
        name: 'admin-products',
        component: () => import('../pages/Admin/ProductManagement.vue')
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
    async scrollBehavior(to, from, saved) {
        if(saved)return saved;
        if(to.path==='/profile'&&/^#bm-buyer-account-(wishlist|notifications)$/.test(to.hash)) {
            // Profile is loaded asynchronously; wait for its own section before resolving the anchor.
            if(!document.querySelector(to.hash))await new Promise(resolve=>{
                const observer=new MutationObserver(()=>{if(document.querySelector(to.hash)){observer.disconnect();clearTimeout(timer);resolve();}});
                const timer=setTimeout(()=>{observer.disconnect();resolve();},2000);
                observer.observe(document.getElementById('app'),{childList:true,subtree:true});
            });
            return document.querySelector(to.hash)?{el:to.hash,top:180}:false;
        }
        return to.hash?{el:to.hash,top:180}:{top:0};
    }
});
function requiresSession(path){return /^\/(?:admin|seller|wallet|banner-credits|messages|profile|orders|wishlist|cart|checkout|payment|store-application|support-cases|notifications)(?:\/|$)/.test(path) || /^\/community\/(?:create|posts\/[^/]+\/edit)$/.test(path);}
const privateCommunityFilter=route=>route.path==='/community'&&Boolean(route.query.scope);
router.beforeEach(async to => {
    const privateRoute = requiresSession(to.path)||privateCommunityFilter(to);
    if (!privateRoute) return;
    const user = await restoreSession(true);
    if (!user) return {name: 'login', query: {redirect: to.fullPath, notice: 'session-expired'}};
    if (/^\/admin(?:\/|$)/.test(to.path) && user.role !== 'admin') return '/forbidden';
    if (/^\/seller(?:\/|$)/.test(to.path) && !['seller', 'admin'].includes(user.role)) return '/forbidden';
});
window.addEventListener('bonsai:logged-out', () => router.replace('/login'));
window.addEventListener('bonsai:session-expired', () => {
    const current = router.currentRoute.value;
    if (requiresSession(current.path)||privateCommunityFilter(current))
        router.replace({name: 'login', query: {notice: 'session-expired', redirect: current.fullPath}});
});
export default router;
