<template>
  <SellerLayout>
    <header class="bm-seller-page-header"><div><h1>Tổng quan gian hàng</h1><p>Một nơi để chăm chút sản phẩm và quản lý công việc bán hàng mỗi ngày.</p></div><RouterLink class="button button-primary" to="/seller/products/new"><MarketIcon name="plus"/>Tạo sản phẩm mới</RouterLink></header>
    <div class="bm-seller-dashboard-start"><section><MarketIcon name="store"/><h2>Gian hàng sẵn sàng cho ngày mới</h2><p>Kiểm tra đơn cần xử lý, trả lời người mua và cập nhật sản phẩm từ những tác vụ bên cạnh.</p><RouterLink class="button button-primary" to="/seller/orders">Quản lý đơn hàng<MarketIcon name="arrow"/></RouterLink></section><nav aria-label="Công việc bán hàng"><RouterLink v-for="task in tasks" :key="task.to" :to="task.to"><MarketIcon :name="task.icon"/><span><strong>{{ task.label }}</strong><small>{{ task.description }}</small></span><MarketIcon name="arrow"/></RouterLink></nav></div>
    <h2 class="bm-seller-dashboard-heading">Hoạt động của gian hàng</h2><p class="bm-seller-help">Chọn khoảng ngày để xem dữ liệu thực tế từ đơn hàng.</p>
    <StatisticsPanel endpoint="/seller/statistics"/>
  </SellerLayout>
</template>
<script setup>
import SellerLayout from './SellerLayout.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import StatisticsPanel from '../../../components/marketplace/StatisticsPanel.vue';
const tasks=[{to:'/seller/products',label:'Sản phẩm và tồn kho',description:'Chỉnh sửa, bổ sung ảnh và gửi duyệt',icon:'leaf'},{to:'/messages',label:'Tin nhắn của khách hàng',description:'Mở hộp thư trao đổi riêng tư',icon:'chat'},{to:'/seller/questions',label:'Hỏi đáp sản phẩm',description:'Xem các câu hỏi trên sản phẩm',icon:'list'}];
</script>
<style scoped>
.bm-seller-dashboard-start{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:32px;background:var(--bm-surface);border:1px solid var(--bm-border);border-radius:var(--bm-radius);padding:32px}.bm-seller-dashboard-start section>svg{color:var(--bm-forest);width:32px;height:32px;margin-bottom:20px}.bm-seller-dashboard-start h2{font-size:24px;max-width:24ch}.bm-seller-dashboard-start p{max-width:48ch;color:var(--bm-muted);line-height:1.7;margin-block:16px 24px}.bm-seller-dashboard-start nav{display:grid}.bm-seller-dashboard-start nav a{display:flex;align-items:center;gap:16px;padding:16px 0;text-decoration:none;color:var(--bm-forest);border-bottom:1px solid var(--bm-border)}.bm-seller-dashboard-start nav a:last-child{border:0}.bm-seller-dashboard-start nav span{flex:1}.bm-seller-dashboard-start nav strong{display:block;color:var(--bm-ink);font-size:16px}.bm-seller-dashboard-start nav small{display:block;font-size:14px;color:var(--bm-muted);margin-top:4px}.bm-seller-dashboard-heading{margin-top:40px}@media(max-width:768px){.bm-seller-dashboard-start{grid-template-columns:minmax(0,1fr);padding:24px;gap:16px}}@media(max-width:600px){.bm-seller-dashboard-start{padding:20px}.bm-seller-dashboard-start h2{font-size:20px}}
</style>
