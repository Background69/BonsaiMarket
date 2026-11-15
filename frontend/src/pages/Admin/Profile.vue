<template>
  <AdminLayout>
    <AdminPageHeader title="Hồ sơ quản trị viên"
                     description="Thông tin tài khoản, bảo mật và phạm vi quyền quản trị trên BonsaiMarket."/>
    <div class="admin-profile-grid">
      <aside class="admin-stack">
        <section class="market-panel admin-profile-identity"><span
            class="admin-avatar">{{ (user?.name || 'Q').slice(0, 1).toUpperCase() }}</span>
          <h2>{{ user?.name || 'Quản trị viên' }}</h2>
          <p>{{ user?.email }}</p><span class="admin-badge">Quản trị viên</span>
          <p class="admin-note">Danh tính và quyền được xác minh bằng phiên đăng nhập tại máy chủ.</p></section>
        <section class="market-panel admin-profile-security">
          <ChangePasswordForm/>
        </section>
      </aside>
      <div class="admin-stack">
        <section class="market-panel"><h2>Thông tin hồ sơ</h2>
          <ProfileEditor/>
        </section>
        <section class="market-panel"><h2>Phạm vi quyền hiện có</h2>
          <dl class="admin-facts">
            <dt>Quản lý sàn</dt>
            <dd>Tài khoản, danh mục, gian hàng và đơn hàng theo contract của từng nghiệp vụ.</dd>
            <dt>Kiểm duyệt</dt>
            <dd>Sản phẩm, cộng đồng và báo cáo có lý do, kiểm tra trạng thái và nhật ký.</dd>
            <dt>Hồ sơ tài chính</dt>
            <dd>Xét duyệt nội bộ; không xác nhận đã chuyển tiền khi thiếu bằng chứng.</dd>
            <dt>Sản phẩm người bán</dt>
            <dd>Không chỉnh sửa giá hoặc tồn kho của gian hàng khác.</dd>
            <dt>Phân quyền</dt>
            <dd>Không tự cấp vai trò hoặc mạo danh người dùng.</dd>
          </dl>
        </section>
        <section class="market-panel"><h2>Cấu hình hệ thống</h2>
          <p>Cấu hình vận hành hiện chưa mở cho quản trị viên. Các thay đổi cần chính sách và phạm vi giá trị được phê
            duyệt riêng.</p></section>
        <section class="market-panel"><h2>Tài khoản liên kết</h2>
          <AccountLinks/>
        </section>
      </div>
    </div>
  </AdminLayout>
</template>
<script setup>
import AdminLayout from '../../layouts/AdminLayout.vue';
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue';
import ProfileEditor from '../../components/marketplace/ProfileEditor.vue';
import ChangePasswordForm from '../../components/auth/ChangePasswordForm.vue';
import AccountLinks from '../../components/auth/AccountLinks.vue';
import {useAuth} from '../../composables/useAuth.js';

const {user} = useAuth();
</script>
<style scoped>
.admin-profile-grid {
  display: grid;
  grid-template-columns:minmax(280px, 1fr) minmax(0, 1.7fr);
  gap: 24px;
  align-items: start
}

.admin-profile-identity {
  text-align: center;
  overflow-wrap: anywhere
}

.admin-profile-identity > .admin-avatar {
  margin: 0 auto 16px;
  width: 72px;
  height: 72px;
  font-size: 28px
}

.admin-profile-security :deep(.account-password) {
  padding: 0;
  margin: 0;
  max-width: none
}

@media (max-width: 1100px) {
  .admin-profile-grid {
    grid-template-columns:minmax(0, 1fr)
  }
}
</style>
