import { createRouter, createWebHistory } from 'vue-router'
import FoundationPage from '@agriverse/pages/FoundationPage.vue'

const routes = [
  { path: '/', name: 'home', component: FoundationPage, props: { title: 'BonsaiMarket', message: 'Nền tảng cửa hàng cây cảnh đang được xây dựng.' } },
  { path: '/products', name: 'products', component: FoundationPage, props: { title: 'Sản phẩm', message: 'Danh mục sản phẩm sẽ được kết nối với REST API ở giai đoạn sau.' } },
  { path: '/login', name: 'login', component: FoundationPage, props: { title: 'Đăng nhập', message: 'Chức năng đăng nhập chưa được triển khai.' } },
  { path: '/register', name: 'register', component: FoundationPage, props: { title: 'Đăng ký', message: 'Chức năng đăng ký chưa được triển khai.' } },
  { path: '/admin', name: 'admin', component: FoundationPage, props: { title: 'Quản trị', message: 'Trang quản trị chưa được triển khai.' } },
  { path: '/seller', name: 'seller', component: FoundationPage, props: { title: 'Người bán', message: 'Trang người bán chưa được triển khai.' } },
]

export default createRouter({ history: createWebHistory(), routes })
