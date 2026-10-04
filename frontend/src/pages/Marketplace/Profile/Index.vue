<template>
  <MarketplaceLayout>
    <main class="profile">
      <section class="profile-hero">
        <div class="profile-hero-inner">
          <div class="profile-portrait">
            <div class="profile-portrait-img">
              <img v-if="user.avatar" :src="user.avatar" :alt="user.name" />
              <span v-else class="profile-portrait-letter">{{ user.name?.charAt(0)?.toUpperCase() || 'U' }}</span>
            </div>
          </div>
          <div class="profile-hero-info">
            <div class="profile-hero-heading">
              <h1 class="profile-hero-name">{{ user.name || 'Người dùng' }}</h1>
              <span v-if="user.seller_verified_at" class="profile-hero-verified">Đã xác thực</span>
              <span v-if="isSeller" class="profile-hero-badge-seller">Người bán</span>
            </div>
            <p v-if="user.bio" class="profile-hero-quote">"{{ user.bio }}"</p>
            <div class="profile-hero-meta">
              <div class="profile-hero-meta-item">
                <span class="material-symbols-outlined">email</span>
                <span>{{ user.email }}</span>
              </div>
              <div v-if="user.phone" class="profile-hero-meta-item">
                <span class="material-symbols-outlined">phone</span>
                <span>{{ user.phone }}</span>
              </div>
              <div class="profile-hero-meta-item">
                <span class="material-symbols-outlined">calendar_today</span>
                <span>Thành viên từ {{ memberSince }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Seller Store Section -->
      <section v-if="sellerStore" class="profile-seller-section">
        <div class="profile-seller-inner">
          <div class="profile-seller-header">
            <h2 class="profile-seller-title">Cửa hàng của tôi</h2>
            <Link :href="route('agriverse.shop.seller.dashboard')" class="profile-seller-link">
              <span class="material-symbols-outlined">dashboard</span>
              Bảng điều khiển
            </Link>
          </div>
          <div class="profile-seller-body">
            <div class="profile-seller-info">
              <div class="profile-seller-avatar">
                <span class="profile-seller-letter">{{ sellerStore.name?.charAt(0)?.toUpperCase() || 'S' }}</span>
              </div>
              <div>
                <h3 class="profile-seller-name">{{ sellerStore.name }}</h3>
                <p class="profile-seller-desc">{{ sellerStore.description || 'Chưa có mô tả' }}</p>
                <div class="profile-seller-meta">
                  <span class="profile-seller-stat">
                    <strong>{{ sellerStore.products_count || 0 }}</strong> sản phẩm
                  </span>
                  <span class="profile-seller-stat">
                    <strong>{{ totalOrdersReceived }}</strong> đơn hàng
                  </span>
                  <span v-if="sellerStore.status === 'active'" class="profile-seller-badge profile-seller-badge-active">
                    Đang hoạt động
                  </span>
                </div>
              </div>
            </div>
            <div class="profile-seller-actions">
              <Link :href="route('agriverse.shop.seller.dashboard')" class="profile-seller-btn">
                <span class="material-symbols-outlined">dashboard</span>
                Bảng điều khiển
              </Link>
              <Link :href="route('agriverse.shop.seller.orders.index')" class="profile-seller-btn profile-seller-btn-secondary">
                <span class="material-symbols-outlined">receipt_long</span>
                Đơn hàng
              </Link>
            </div>
          </div>
        </div>
      </section>

      <!-- Quick Actions -->
      <section class="profile-actions">
        <div class="profile-actions-grid">
          <Link :href="route('agriverse.shop.account.settings')" class="profile-action-card">
            <span class="material-symbols-outlined profile-action-icon">settings</span>
            <div>
              <h3 class="profile-action-title">Cài đặt tài khoản</h3>
              <p class="profile-action-desc">Thông tin cá nhân, mật khẩu, thông báo</p>
            </div>
            <span class="material-symbols-outlined profile-action-arrow">chevron_right</span>
          </Link>
          <Link :href="route('agriverse.shop.addresses.index')" class="profile-action-card">
            <span class="material-symbols-outlined profile-action-icon">location_on</span>
            <div>
              <h3 class="profile-action-title">Địa chỉ giao hàng</h3>
              <p class="profile-action-desc">Quản lý địa chỉ nhận hàng</p>
            </div>
            <span class="material-symbols-outlined profile-action-arrow">chevron_right</span>
          </Link>
          <Link :href="route('agriverse.shop.orders.index')" class="profile-action-card">
            <span class="material-symbols-outlined profile-action-icon">receipt_long</span>
            <div>
              <h3 class="profile-action-title">Đơn hàng của tôi</h3>
              <p class="profile-action-desc">Xem lịch sử và trạng thái đơn hàng</p>
            </div>
            <span class="material-symbols-outlined profile-action-arrow">chevron_right</span>
          </Link>
          <Link :href="route('agriverse.shop.wishlist.index')" class="profile-action-card">
            <span class="material-symbols-outlined profile-action-icon">favorite</span>
            <div>
              <h3 class="profile-action-title">Yêu thích</h3>
              <p class="profile-action-desc">Sản phẩm bạn đã lưu</p>
            </div>
            <span class="material-symbols-outlined profile-action-arrow">chevron_right</span>
          </Link>
        </div>
      </section>

      <!-- Order History -->
      <section v-if="recentOrders.length" class="profile-history-section">
        <div class="profile-history-header">
          <h2 class="profile-history-title">Đơn hàng gần đây</h2>
          <Link :href="route('agriverse.shop.orders.index')" class="profile-history-link">Xem tất cả</Link>
        </div>
        <div class="profile-table-wrap">
          <table class="profile-table">
            <thead>
              <tr>
                <th>Mã đơn</th>
                <th>Sản phẩm</th>
                <th>Tổng</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in recentOrders" :key="order.id">
                <td class="profile-table-id">#{{ order.id }}</td>
                <td class="profile-table-name">{{ order.product?.name || '—' }}</td>
                <td class="profile-table-value">{{ formatPrice(order.total_amount) }}₫</td>
                <td>
                  <span class="profile-status" :class="'profile-status-' + order.status">
                    {{ statusLabel(order.status) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { computed } from 'vue';
import { Link } from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';

const props = defineProps({
  user: { type: Object, required: true },
  sellerStore: { type: Object, default: null },
  totalOrdersReceived: { type: Number, default: 0 },
  recentOrders: { type: Array, default: () => [] },
});

const isSeller = computed(() => props.user?.role === 'seller');

const memberSince = computed(() => {
  const d = props.user?.created_at;
  if (!d) return '2024';
  return new Date(d).getFullYear();
});

function formatPrice(price) {
  return new Intl.NumberFormat('vi-VN').format(price || 0);
}

function statusLabel(status) {
  const map = {
    pending: 'Chờ xác nhận',
    confirmed: 'Đã xác nhận',
    shipped: 'Đang giao',
    delivered: 'Đã giao',
    completed: 'Hoàn thành',
    cancelled: 'Đã hủy',
  };
  return map[status] || status;
}
</script>

<style scoped>
.profile {
  max-width: var(--ag-container-max, 1280px);
  margin: 0 auto;
  padding: 0 64px 80px;
}
@media (max-width: 768px) { .profile { padding: 0 16px 60px; } }

.profile-hero {
  background: linear-gradient(135deg, var(--ag-primary-800), var(--ag-primary-600));
  border-radius: 0 0 24px 24px;
  padding: 60px 64px 48px;
  margin-bottom: 40px;
}
@media (max-width: 768px) { .profile-hero { padding: 48px 20px 36px; } }
.profile-hero-inner { display: flex; gap: 32px; align-items: center; flex-wrap: wrap; }
.profile-portrait-img {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(255,255,255,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.profile-portrait-img img { width: 100%; height: 100%; object-fit: cover; }
.profile-portrait-letter {
  font-family: var(--ag-font-display);
  font-size: 40px;
  font-weight: 500;
  color: white;
}
.profile-hero-name {
  font-family: var(--ag-font-display);
  font-size: 28px;
  font-weight: 500;
  color: white;
  margin-bottom: 4px;
}
.profile-hero-verified, .profile-hero-badge-seller {
  display: inline-block;
  font-family: var(--ag-font-body);
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 9999px;
  margin-left: 8px;
  vertical-align: middle;
}
.profile-hero-verified { background: rgba(255,255,255,0.2); color: white; }
.profile-hero-badge-seller { background: rgba(255,255,255,0.15); color: rgba(255,255,255,0.9); }
.profile-hero-quote {
  font-family: var(--ag-font-display);
  font-size: 14px;
  font-style: italic;
  color: rgba(255,255,255,0.7);
  margin: 8px 0 12px;
  max-width: 400px;
}
.profile-hero-meta { display: flex; flex-wrap: wrap; gap: 16px; }
.profile-hero-meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  color: rgba(255,255,255,0.6);
}
.profile-hero-meta-item .material-symbols-outlined { font-size: 16px; }

.profile-actions { margin-bottom: 48px; }
.profile-actions-grid { display: flex; flex-direction: column; gap: 8px; }
.profile-action-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--ag-bg-card, white);
  border: 1px solid color-mix(in srgb, var(--ag-border) 30%, transparent);
  border-radius: 12px;
  text-decoration: none;
  transition: all 0.2s;
}
.profile-action-card:hover {
  border-color: var(--ag-primary-500);
  box-shadow: 0 2px 8px color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
}
.profile-action-icon { font-size: 22px; color: var(--ag-primary-500); }
.profile-action-title {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-text-primary);
}
.profile-action-desc {
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-text-muted);
  margin-top: 2px;
}
.profile-action-arrow {
  margin-left: auto;
  font-size: 20px;
  color: var(--ag-text-muted);
}

.profile-history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.profile-history-title {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ag-text-primary);
}
.profile-history-link {
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  color: var(--ag-primary-500);
  text-decoration: none;
}
.profile-history-link:hover { text-decoration: underline; }
.profile-table-wrap {
  background: var(--ag-bg-card, white);
  border: 1px solid color-mix(in srgb, var(--ag-border) 30%, transparent);
  border-radius: 12px;
  overflow: hidden;
}
.profile-table { width: 100%; border-collapse: collapse; }
.profile-table th {
  text-align: left;
  padding: 12px 16px;
  font-family: var(--ag-font-body);
  font-size: 11px;
  font-weight: 600;
  color: var(--ag-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 50%, transparent);
}
.profile-table td {
  padding: 12px 16px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  color: var(--ag-text-primary);
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 30%, transparent);
}
.profile-table tr:last-child td { border-bottom: none; }
.profile-table-id { color: var(--ag-text-muted); font-family: monospace; }
.profile-table-name { font-weight: 500; }
.profile-table-value { font-weight: 600; }
.profile-status {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 9999px;
}
.profile-status-pending { background: color-mix(in srgb, #D97706 10%, transparent); color: #D97706; }
.profile-status-confirmed { background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent); color: var(--ag-primary-500); }
.profile-status-shipped { background: color-mix(in srgb, #2563EB 10%, transparent); color: #2563EB; }
.profile-status-delivered { background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent); color: var(--ag-primary-500); }
.profile-status-completed { background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent); color: var(--ag-primary-500); }
.profile-status-cancelled { background: color-mix(in srgb, #DC2626 10%, transparent); color: #DC2626; }

.profile-seller-section { margin-bottom: 40px; }
.profile-seller-inner {
  background: var(--ag-bg-card, white);
  border: 1px solid color-mix(in srgb, var(--ag-border) 30%, transparent);
  border-radius: 16px;
  padding: 28px;
}
.profile-seller-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.profile-seller-title {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ag-text-primary);
}
.profile-seller-link {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  color: var(--ag-primary-500);
  text-decoration: none;
}
.profile-seller-body { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.profile-seller-avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--ag-primary-500);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.profile-seller-letter {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  color: white;
}
.profile-seller-name {
  font-family: var(--ag-font-body);
  font-size: 16px;
  font-weight: 600;
  color: var(--ag-text-primary);
}
.profile-seller-desc {
  font-family: var(--ag-font-body);
  font-size: 13px;
  color: var(--ag-text-muted);
  margin-top: 2px;
}
.profile-seller-meta { display: flex; gap: 16px; margin-top: 8px; }
.profile-seller-stat {
  font-family: var(--ag-font-body);
  font-size: 13px;
  color: var(--ag-text-secondary);
}
.profile-seller-stat strong { color: var(--ag-text-primary); }
.profile-seller-badge-active {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 9999px;
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  color: var(--ag-primary-500);
}
.profile-seller-actions { display: flex; gap: 8px; margin-left: auto; }
.profile-seller-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
  background: var(--ag-primary-500);
  color: white;
}
.profile-seller-btn:hover { background: var(--ag-primary-600); }
.profile-seller-btn-secondary {
  background: transparent;
  color: var(--ag-text-secondary);
  border: 1px solid var(--ag-border);
}
.profile-seller-btn-secondary:hover {
  border-color: var(--ag-primary-500);
  color: var(--ag-primary-500);
}
</style>
