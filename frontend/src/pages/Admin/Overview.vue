<template>
  <AdminLayout>
    <AdminPageHeader title="Tổng quan" description="Theo dõi hoạt động và các công việc cần xử lý trên sàn."
                     eyebrow="Không gian quản trị">
      <button type="button" class="button button-outline" :disabled="loading" @click="load">Làm mới dữ liệu</button>
    </AdminPageHeader>
    <form class="management-toolbar" @submit.prevent="apply"><label>Từ ngày<input v-model="from" type="date"
                                                                                  :max="to||undefined"/></label><label>Đến
      ngày<input v-model="to" type="date" :min="from||undefined"/></label>
      <button class="button button-primary" :disabled="loading||!!(from&&to&&from>to)">Xem tổng quan</button>
      <RouterLink class="button button-outline" to="/admin/statistics">Thống kê chi tiết</RouterLink>
    </form>
    <p v-if="loading" role="status">Đang tổng hợp dữ liệu quản trị…</p>
    <div v-else-if="error" class="management-error" role="alert"><p>{{ error }}</p>
      <button class="button button-outline" @click="load">Thử lại</button>
    </div>
    <template v-else-if="data"><p v-if="data.developmentData" class="admin-environment-note">Môi trường phát triển / QA
      · có dữ liệu mẫu; các giá trị dưới đây không đại diện doanh thu thực tế.</p>
      <div class="admin-stat-grid">
        <AdminStatCard label="Đơn hàng trong kỳ" :value="totalOrders" icon="bag" :note="period"/>
        <AdminStatCard label="Giá trị đơn đủ điều kiện" :value="money(data.statistics.totalRevenue)" icon="wallet"
                       note="Tiền hàng của đơn đã giao / hoàn tất"/>
        <AdminStatCard label="Gian hàng đang hoạt động" :value="data.activeStores" icon="store"
                       note="Gian hàng và tài khoản chủ đang hoạt động"/>
        <AdminStatCard label="Tổng tài khoản" :value="data.users" icon="users"
                       :note="data.activeUsers+' tài khoản đang hoạt động'"/>
        <AdminStatCard label="Báo cáo chưa xử lý" :value="data.queue[1].count" icon="info"
                       note="Mới gửi hoặc đang xem xét"/>
        <AdminStatCard label="Hồ sơ hoàn tiền cần xem xét" :value="data.queue[2].count" icon="clock"
                       note="Chưa xác nhận kết quả tài chính"/>
      </div>
      <div class="admin-two-column">
        <section class="market-panel">
          <div class="admin-section-heading">
            <div><h2>Giá trị đơn theo ngày</h2>
              <p>VND · theo ngày đặt hàng tại Việt Nam</p></div>
            <RouterLink to="/admin/statistics">Xem chi tiết</RouterLink>
          </div>
          <RevenueBarChart :days="data.statistics.days"/>
          <p class="admin-note">Giá trị hàng hóa của đơn đã giao hoặc hoàn tất, chưa gồm phí vận chuyển. Không thể suy
            ra tiền đã thu hoặc đã quyết toán.</p></section>
        <section class="market-panel">
          <div class="admin-section-heading"><h2>Công việc cần xử lý</h2>
            <MarketIcon name="list"/>
          </div>
          <div class="admin-queue">
            <RouterLink v-for="item in data.queue" :key="item.link" :to="item.link">
              <MarketIcon name="clock"/>
              <span><strong>{{ item.label }}</strong><small>{{ item.note }}</small></span><b class="admin-queue-count">{{
                item.count
              }}</b>
              <MarketIcon name="chevron"/>
            </RouterLink>
          </div>
          <p class="admin-note">Số lượng toàn sàn, cập nhật khi tải lại dữ liệu.</p></section>
      </div>
      <div class="admin-two-column">
        <section class="market-panel"><h2>Đơn hàng theo trạng thái</h2>
          <div class="admin-status-bars">
            <RouterLink v-for="(count,status) in data.statistics.statusCounts" :key="status"
                        :to="'/admin/orders?status='+status"><span>{{ statusLabel(status) }}</span>
              <meter min="0" :max="Math.max(1,totalOrders)" :value="count"
                     :aria-label="statusLabel(status)+': '+count+' đơn'"/>
              <strong>{{ count }}</strong></RouterLink>
          </div>
        </section>
        <section class="market-panel"><h2>Phân bố giá trị theo danh mục</h2>
          <CategoryPieChart :categories="data.statistics.categories"/>
        </section>
      </div>
      <p v-if="data.emptyOrders" class="admin-environment-note">Có {{ data.emptyOrders }} đơn chưa có dòng sản phẩm. Cần
        đối chiếu nguồn gốc; các đơn vẫn được giữ trong danh sách quản trị.</p></template>
  </AdminLayout>
</template>
<script setup>
import {computed, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import AdminLayout from '../../layouts/AdminLayout.vue';
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue';
import AdminStatCard from '../../components/admin/AdminStatCard.vue';
import RevenueBarChart from '../../components/admin/AdminRevenueChart.vue';
import CategoryPieChart from '../../components/marketplace/CategoryPieChart.vue';
import MarketIcon from '../../components/marketplace/MarketIcon.vue';
import {useAdminResource} from '../../composables/useAdminResource.js';
import {money} from '../../services/adminPresentation.js';
import {statusLabel} from '../../utils.js';

const route = useRoute(), router = useRouter(), from = ref(''), to = ref(''), {
  data,
  loading,
  error,
  load
} = useAdminResource(() => '/admin/overview?' + new URLSearchParams({
  from: route.query.from || '',
  to: route.query.to || ''
}));
const totalOrders = computed(() => Object.values(data.value?.statistics.statusCounts || {}).reduce((a, b) => a + b, 0)),
    period = computed(() => data.value ? data.value.statistics.from + ' → ' + data.value.statistics.to : '');
watch(data, d => {
  if (d) {
    from.value = d.statistics.from;
    to.value = d.statistics.to;
  }
});

function apply() {
  if (from.value && to.value && from.value > to.value) return;
  router.push({path: route.path, query: {from: from.value, to: to.value}});
}
</script>
