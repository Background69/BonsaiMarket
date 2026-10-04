<template>
  <MarketplaceLayout>
    <main class="support">
      <section class="support__hero">
        <h1 class="support__hero-title">Các nhà làm vườn của chúng tôi có thể hỗ trợ bạn hôm nay như thế nào?</h1>
        <p class="support__hero-desc">Kết nối khoa học thực vật với không gian sống của bạn. Khám phá các kênh hỗ trợ chuyên biệt của chúng tôi bên dưới.</p>
      </section>

      <section class="support__search">
        <div class="support__search-wrap" ref="searchWrapRef">
          <span class="material-symbols-outlined support__search-icon">search</span>
          <input type="text" class="support__search-input" placeholder="Tìm kiếm Cơ sở Tri thức về chăm sóc loài, dữ liệu ánh sáng hoặc phòng ngừa bệnh..." v-model="searchQuery" @focus="onSearchFocus" @blur="onSearchBlur" />
        </div>
      </section>

      <section class="support__bento">
        <div class="support__bento-card support__bento-card--specialist">
          <span class="material-symbols-outlined support__bento-icon">urology</span>
          <h2 class="support__bento-title">Tư Vấn Chuyên Gia</h2>
          <p class="support__bento-desc">Đặt lịch chẩn đoán ảo 1-1 với các nhà thực vật học kỳ cựu. Lý tưởng cho việc thích nghi mẫu vật quý hiếm hoặc tối ưu hóa môi trường phức tạp.</p>
          <div class="support__bento-actions">
            <button class="support__btn support__btn--primary" @click="openBooking">Đặt Lịch Gọi</button>
            <span class="support__bento-availability">Có sẵn tiếp theo: Hôm nay, 2:00 CH</span>
          </div>
        </div>

        <div class="support__bento-card support__bento-card--warranty">
          <span class="material-symbols-outlined support__bento-icon support__bento-icon--secondary">verified_user</span>
          <h2 class="support__bento-title">Bảo Hành Sức Khỏe</h2>
          <p class="support__bento-desc">Mọi mẫu vật từ AgriVerse đều được bảo đảm bởi cam kết toàn vẹn sinh học 30 ngày của chúng tôi.</p>
          <button class="support__btn support__btn--outline">Gửi Yêu Cầu</button>
        </div>

        <div class="support__bento-card support__bento-card--stats">
          <h3 class="support__bento-small-title">Sức Khỏe Đội Tàu Toàn Cầu</h3>
          <div class="support__stat">
              <div class="support__stat-head">
                <span class="support__stat-label">Tỷ Lệ Thành Công Trung Bình</span>
                <span class="support__stat-value">{{ stats.successRate }}%</span>
              </div>
              <div class="support__stat-bar">
                <div class="support__stat-fill" :style="{ width: stats.successRate + '%' }"></div>
              </div>
            </div>
            <div class="support__stat">
              <div class="support__stat-head">
                <span class="support__stat-label">Phiếu Hỗ Trợ Đang Hoạt Động</span>
                <span class="support__stat-value">{{ stats.activeTickets }}</span>
              </div>
              <div class="support__stat-bar">
                <div class="support__stat-fill support__stat-fill--secondary" :style="{ width: (stats.activeTickets / 80 * 100) + '%' }"></div>
              </div>
          </div>
        </div>

        <div class="support__bento-card support__bento-card--repository">
          <div class="support__repo-text">
            <h3 class="support__repo-title">Kho Lưu Trữ Khoa Học</h3>
            <p class="support__repo-desc">Cơ sở tri thức của chúng tôi chứa hơn 4.200 bài báo được bình duyệt về làm vườn nhiệt đới và khô hạn, được duy trì bởi phòng thí nghiệm nghiên cứu nội bộ.</p>
          </div>
          <div class="support__repo-stat">
            <div class="support__repo-number">4.2k+</div>
            <div class="support__repo-label">Bài Viết</div>
          </div>
        </div>
      </section>

      <section class="support__contact">
        <div class="support__contact-form">
          <h2 class="support__contact-title">Yêu Cầu Trực Tiếp</h2>
          <form class="support__form" @submit.prevent="handleSubmit">
            <div class="support__form-row">
              <div class="support__form-group">
                <label class="support__form-label">Tên</label>
                <input type="text" class="support__form-input" v-model="form.firstName" />
              </div>
              <div class="support__form-group">
                <label class="support__form-label">Họ</label>
                <input type="text" class="support__form-input" v-model="form.lastName" />
              </div>
            </div>
            <div class="support__form-group">
              <label class="support__form-label">Địa Chỉ Email</label>
              <input type="email" class="support__form-input" v-model="form.email" />
            </div>
            <div class="support__form-group">
              <label class="support__form-label">Chủ Đề</label>
              <select class="support__form-input support__form-select" v-model="form.subject">
                <option>Yêu Cầu Đặt Hàng</option>
                <option>Chăm Sóc Cây Kỹ Thuật</option>
                <option>Đối Tác Bán Buôn</option>
                <option>Yêu Cầu Phát Triển Bền Vững</option>
              </select>
            </div>
            <div class="support__form-group">
              <label class="support__form-label">Tin Nhắn</label>
              <textarea class="support__form-textarea" rows="4" v-model="form.message"></textarea>
            </div>
            <button type="submit" class="support__btn support__btn--primary support__btn--block">Gửi Tin Nhắn</button>
          </form>
        </div>
        <div class="support__contact-image">
          <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuDGkhxzF4nRfPU7LnLEb8765PLJ9O5TgvQZ-Hwch2s0Cn7wOzYMb8xdhiDu7Ymby-EW2snJ0I5akuZIkDj2vJFCFMuivNX-QsMVoenfSQBRcNTN8UgcN3YBLkQZifWCZ40qeh8psxeHfUgqSQ5hbq1wch9cZcots5WUC78kWpndcz6Rk5xR8hydkDcUhgHKDVdBnwiBbA4YqHnFqgOWc5sNlQVUwL4tvOOJL15lf5anZ8VM-ukwlDDvOoUvHRpCnnUQGYWTaaButQo" alt="Nhà kính thực vật" class="support__contact-img" />
          <div class="support__contact-overlay"></div>
          <div class="support__contact-quote">
            <p class="support__quote-text">"Sứ mệnh của chúng tôi là đảm bảo mọi cây trồng chúng tôi gửi đi đều phát triển như một kiệt tác của thiên nhiên trong ngôi nhà mới của nó."</p>
            <p class="support__quote-author">— Tiến sĩ Elena Vance, Trưởng phòng Làm vườn</p>
          </div>
        </div>
      </section>

      <div class="support__concierge" @mouseenter="showTooltip = true" @mouseleave="showTooltip = false">
        <button class="support__concierge-btn" @click="toggleChat">
          <span class="material-symbols-outlined">psychology</span>
        </button>
        <div class="support__concierge-tooltip" :class="{ 'support__concierge-tooltip--visible': showTooltip }">Trợ Lý AI Thực Vật</div>
      </div>

      <div class="support__chat" :class="{ 'support__chat--open': chatOpen }">        <div class="support__chat-header">
          <div class="support__chat-header-info">
            <span class="material-symbols-outlined">auto_awesome</span>
            <span class="support__chat-header-title">Trợ Lý AI AgriVerse</span>
          </div>
          <button class="support__chat-close" @click="toggleChat">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="support__chat-messages" ref="chatRef">
          <div v-for="(msg, i) in chatMessages" :key="i" :class="['support__chat-msg', msg.isBot ? 'support__chat-msg--bot' : 'support__chat-msg--user']">
            <p>{{ msg.text }}</p>
          </div>
        </div>
        <div class="support__chat-input-wrap">
          <input type="text" class="support__chat-input" placeholder="Nhập câu hỏi thực vật của bạn..." v-model="chatInput" @keyup.enter="sendChat" />
          <button class="support__chat-send" @click="sendChat">
            <span class="material-symbols-outlined">send</span>
          </button>
        </div>
      </div>

      <!-- Booking modal -->
      <Transition name="support-modal">
        <div v-if="bookingOpen" class="support-modal-overlay" @click.self="closeBooking">
          <div class="support-modal">
            <div class="support-modal-header">
              <div>
                <h3 class="support-modal-title">Đặt Lịch Tư Vấn Chuyên Gia</h3>
                <p class="support-modal-sub">Chuyên gia AgriVerse sẽ liên hệ lại để xác nhận lịch.</p>
              </div>
              <button type="button" class="support-modal-close" @click="closeBooking">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>

            <div v-if="bookingDone" class="support-modal-done">
              <span class="material-symbols-outlined support-modal-done-icon">check_circle</span>
              <h4 class="support-modal-done-title">Đặt lịch thành công!</h4>
              <p class="support-modal-done-desc">
                Mã tham chiếu: <strong>{{ bookingReference }}</strong>.
                Chúng tôi sẽ liên hệ với bạn trong thời gian sớm nhất.
              </p>
              <button type="button" class="support__btn support__btn--primary" @click="closeBooking">Đóng</button>
            </div>

            <form v-else class="support-modal-form" @submit.prevent="submitBooking">
              <div class="support__form-group">
                <label class="support__form-label">Họ tên *</label>
                <input type="text" class="support__form-input" v-model="booking.name" placeholder="Nguyễn Văn A" required />
              </div>
              <div class="support-modal-row">
                <div class="support__form-group">
                  <label class="support__form-label">Email *</label>
                  <input type="email" class="support__form-input" v-model="booking.email" placeholder="you@example.com" required />
                </div>
                <div class="support__form-group">
                  <label class="support__form-label">Số điện thoại</label>
                  <input type="tel" class="support__form-input" v-model="booking.phone" placeholder="0987654321" />
                </div>
              </div>
              <div class="support__form-group">
                <label class="support__form-label">Chủ đề tư vấn</label>
                <select class="support__form-input support__form-select" v-model="booking.topic">
                  <option value="">Chọn chủ đề</option>
                  <option>Chăm sóc cây cảnh định kỳ</option>
                  <option>Chẩn đoán bệnh & sâu hại</option>
                  <option>Tư vấn phong thủy cây cảnh</option>
                  <option>Tạo dáng & thế bonsai</option>
                  <option>Thích nghi cây mới mua</option>
                  <option>Khác</option>
                </select>
              </div>
              <div class="support-modal-row">
                <div class="support__form-group">
                  <label class="support__form-label">Ngày mong muốn</label>
                  <input type="date" class="support__form-input" v-model="booking.preferred_date" :min="minDate" />
                </div>
                <div class="support__form-group">
                  <label class="support__form-label">Giờ mong muốn</label>
                  <select class="support__form-input support__form-select" v-model="booking.preferred_time">
                    <option value="">Chọn giờ</option>
                    <option>08:00 – 10:00</option>
                    <option>10:00 – 12:00</option>
                    <option>13:30 – 15:30</option>
                    <option>15:30 – 17:30</option>
                  </select>
                </div>
              </div>
              <div class="support__form-group">
                <label class="support__form-label">Mô tả vấn đề</label>
                <textarea class="support__form-textarea" rows="3" v-model="booking.message" placeholder="Mô tả tình trạng cây, môi trường sống, các triệu chứng..."></textarea>
              </div>
              <p v-if="bookingError" class="support-modal-error">{{ bookingError }}</p>
              <button type="submit" class="support__btn support__btn--primary support__btn--block" :disabled="bookingSubmitting">
                <span v-if="bookingSubmitting" class="support-modal-spinner"></span>
                {{ bookingSubmitting ? 'Đang gửi...' : 'Xác nhận đặt lịch' }}
              </button>
            </form>
          </div>
        </div>
      </Transition>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue'

const props = defineProps({
  faqs: { type: Object, default: () => ({}) },
  stats: { type: Object, default: () => ({ successRate: 98.4, activeTickets: 12, repositoryArticles: 4200 }) },
})

const searchQuery = ref('')
const chatOpen = ref(false)
const chatInput = ref('')
const chatRef = ref(null)
const searchWrapRef = ref(null)
const showTooltip = ref(false)

const form = ref({
  firstName: '',
  lastName: '',
  email: '',
  subject: 'Yêu Cầu Đặt Hàng',
  message: ''
})

const bookingOpen = ref(false)
const bookingDone = ref(false)
const bookingSubmitting = ref(false)
const bookingError = ref('')
const bookingReference = ref('')
const booking = ref({
  name: '',
  email: '',
  phone: '',
  topic: '',
  preferred_date: '',
  preferred_time: '',
  message: '',
})

const minDate = new Date().toISOString().split('T')[0]

function openBooking() {
  bookingDone.value = false
  bookingError.value = ''
  bookingOpen.value = true
}

function closeBooking() {
  if (bookingSubmitting.value) return
  bookingOpen.value = false
}

async function submitBooking() {
  if (bookingSubmitting.value) return
  bookingSubmitting.value = true
  bookingError.value = ''
  try {
    const { data } = await window.axios.post(route('agriverse.shop.support.booking'), booking.value)
    if (data?.success) {
      bookingReference.value = data.reference || ''
      bookingDone.value = true
      booking.value = { name: '', email: '', phone: '', topic: '', preferred_date: '', preferred_time: '', message: '' }
    } else {
      bookingError.value = data?.message || 'Đã có lỗi xảy ra, vui lòng thử lại.'
    }
  } catch (e) {
    bookingError.value = e?.response?.data?.message || 'Đã có lỗi xảy ra, vui lòng thử lại.'
  } finally {
    bookingSubmitting.value = false
  }
}

const chatMessages = ref([
  { text: 'Xin chào. Tôi là Trợ lý AI AgriVerse, được đào tạo trên cơ sở dữ liệu làm vườn độc quyền của chúng tôi. Tôi có thể giúp gì cho bộ sưu tập của bạn hôm nay?', isBot: true },
  { text: 'Cây Fiddle Leaf Fig của tôi có đốm nâu trên mép lá.', isBot: false },
  { text: 'Mép lá nâu trên Ficus lyrata thường cho thấy tưới nước không đều hoặc độ ẩm thấp. Bạn có đang sử dụng máy đo độ ẩm không?', isBot: true }
])

function toggleChat() {
  chatOpen.value = !chatOpen.value
}

function sendChat() {
  if (!chatInput.value.trim()) return
  chatMessages.value.push({ text: chatInput.value, isBot: false })
  chatInput.value = ''
  nextTick(() => {
    if (chatRef.value) {
      chatRef.value.scrollTop = chatRef.value.scrollHeight
    }
  })
  setTimeout(() => {
    chatMessages.value.push({ text: 'Cảm ơn câu hỏi của bạn. Chuyên gia sẽ xem xét trường hợp của bạn trong thời gian ngắn.', isBot: true })
    nextTick(() => {
      if (chatRef.value) {
        chatRef.value.scrollTop = chatRef.value.scrollHeight
      }
    })
  }, 1000)
}

function handleSubmit() {
  if (!form.value.email) return
  window.axios?.post(route('agriverse.api.support.ticket'), form.value)
    .then(() => { form.value = { firstName: '', lastName: '', email: '', subject: 'Yêu Cầu Đặt Hàng', message: '' } })
    .catch(() => {})
}

function onSearchFocus() {
  if (searchWrapRef.value) {
    searchWrapRef.value.classList.add('support__search-wrap--focused')
  }
}

function onSearchBlur() {
  if (searchWrapRef.value) {
    searchWrapRef.value.classList.remove('support__search-wrap--focused')
  }
}
</script>

<style scoped>
.support {
  font-family: var(--ag-font-body);
  max-width: 1280px;
  margin: 0 auto;
  padding: 64px 64px;
  color: var(--ag-on-surface);
}

.support__hero {
  text-align: center;
  max-width: 768px;
  margin: 0 auto 80px;
}

.support__hero-title {
  font-family: var(--ag-font-display);
  font-size: 48px;
  font-weight: 500;
  line-height: 56px;
  letter-spacing: -0.02em;
  margin: 0 0 24px;
}

.support__hero-desc {
  font-size: 18px;
  line-height: 28px;
  color: var(--ag-on-surface-variant);
  margin: 0;
}

.support__search {
  margin-bottom: 96px;
}

.support__search-wrap {
  position: relative;
  max-width: 672px;
  margin: 0 auto;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.support__search-wrap--focused {
  transform: scale(1.02);
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);
}

.support__search-icon {
  position: absolute;
  left: 24px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ag-outline);
}

.support__search-input {
  width: 100%;
  padding: 20px 24px 20px 64px;
  background: #fff;
  border: 1px solid color-mix(in srgb, var(--ag-outline) 20%, transparent);
  border-radius: 9999px;
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-on-surface);
  transition: border-color 0.2s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  outline: none;
  font-family: var(--ag-font-body);
}

.support__search-input:focus {
  border-color: var(--ag-primary);
}

.support__bento {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  margin-bottom: 96px;
}

@media (min-width: 768px) {
  .support__bento {
    grid-template-columns: repeat(12, 1fr);
  }
}

.support__bento-card {
  border-radius: 12px;
  padding: 40px;
  transition: all 0.3s;
}

.support__bento-card--specialist {
  background: #fff;
  box-shadow: 0 10px 30px rgba(44,44,44,0.05);
  border: 1px solid color-mix(in srgb, var(--ag-outline) 5%, transparent);
  display: flex;
  flex-direction: column;
}

@media (min-width: 768px) {
  .support__bento-card--specialist {
    grid-column: span 7;
  }
}

.support__bento-card--specialist:hover {
  border-color: color-mix(in srgb, var(--ag-primary) 20%, transparent);
}

.support__bento-card--warranty {
  background: #fff;
  box-shadow: 0 10px 30px rgba(44,44,44,0.05);
  border: 1px solid color-mix(in srgb, var(--ag-outline) 5%, transparent);
  display: flex;
  flex-direction: column;
}

@media (min-width: 768px) {
  .support__bento-card--warranty {
    grid-column: span 5;
  }
}

.support__bento-card--stats {
  background: var(--ag-surface-container-low);
  border: 1px solid color-mix(in srgb, var(--ag-outline) 10%, transparent);
}

@media (min-width: 768px) {
  .support__bento-card--stats {
    grid-column: span 4;
  }
}

.support__bento-card--repository {
  background: var(--ag-inverse-surface);
  color: var(--ag-inverse-on-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
}

@media (min-width: 768px) {
  .support__bento-card--repository {
    grid-column: span 8;
  }
}

.support__bento-icon {
  font-size: 40px;
  color: var(--ag-primary);
  margin-bottom: 24px;
  display: block;
}

.support__bento-icon--secondary {
  color: var(--ag-secondary);
}

.support__bento-title {
  font-family: var(--ag-font-display);
  font-size: 32px;
  font-weight: 500;
  line-height: 40px;
  margin: 0 0 16px;
}

.support__bento-desc {
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-on-surface-variant);
  margin: 0 0 32px;
  flex-grow: 1;
}

.support__bento-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.support__bento-availability {
  font-size: 12px;
  line-height: 16px;
  color: var(--ag-outline);
}

.support__bento-small-title {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--ag-primary);
  margin: 0 0 24px;
}

.support__stat {
  margin-bottom: 16px;
}

.support__stat:last-child {
  margin-bottom: 0;
}

.support__stat-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 8px;
}

.support__stat-label {
  font-size: 12px;
  line-height: 16px;
  color: var(--ag-outline);
}

.support__stat-value {
  font-size: 18px;
  line-height: 28px;
  color: var(--ag-on-surface);
}

.support__stat-bar {
  width: 100%;
  height: 4px;
  background: color-mix(in srgb, var(--ag-outline) 10%, transparent);
  border-radius: 9999px;
  overflow: hidden;
}

.support__stat-fill {
  height: 100%;
  background: var(--ag-primary);
  border-radius: 9999px;
}

.support__stat-fill--secondary {
  background: var(--ag-secondary-fixed);
}

.support__repo-text {
  max-width: 448px;
}

.support__repo-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  line-height: 32px;
  margin: 0 0 8px;
}

.support__repo-desc {
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-surface-variant);
  margin: 0;
  opacity: 0.8;
}

.support__repo-stat {
  text-align: right;
  display: none;
}

@media (min-width: 1024px) {
  .support__repo-stat {
    display: block;
  }
}

.support__repo-number {
  font-family: var(--ag-font-display);
  font-size: 40px;
  font-style: italic;
  color: var(--ag-primary-fixed-dim);
}

.support__repo-label {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  opacity: 0.6;
}

.support__btn {
  padding: 12px 32px;
  border-radius: 9999px;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: var(--ag-font-body);
}

.support__btn--primary {
  background: var(--ag-primary);
  color: var(--ag-on-primary);
}

.support__btn--primary:hover {
  background: var(--ag-on-primary-fixed-variant);
}

.support__btn--outline {
  border: 1px solid var(--ag-on-surface);
  color: var(--ag-on-surface);
  background: transparent;
}

.support__btn--outline:hover {
  background: var(--ag-on-surface);
  color: var(--ag-inverse-on-surface);
}

.support__btn--block {
  width: 100%;
  padding: 16px 0;
  border-radius: 8px;
}

.support__contact {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 50px rgba(44,44,44,0.08);
  display: grid;
  grid-template-columns: 1fr;
}

@media (min-width: 1024px) {
  .support__contact {
    grid-template-columns: 1fr 1fr;
  }
}

.support__contact-form {
  padding: 48px 80px;
}

@media (max-width: 767px) {
  .support__contact-form {
    padding: 48px 32px;
  }
}

.support__contact-title {
  font-family: var(--ag-font-display);
  font-size: 32px;
  font-weight: 500;
  line-height: 40px;
  margin: 0 0 32px;
}

.support__form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.support__form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.support__form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.support__form-label {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  color: var(--ag-outline);
}

.support__form-input {
  border: none;
  border-bottom: 1px solid color-mix(in srgb, var(--ag-outline) 20%, transparent);
  background: transparent;
  padding: 8px 0;
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-on-surface);
  transition: border-color 0.2s;
  outline: none;
  font-family: var(--ag-font-body);
}

.support__form-input:focus {
  border-color: var(--ag-primary);
}

.support__form-select {
  border-bottom: 1px solid color-mix(in srgb, var(--ag-outline) 20%, transparent);
  appearance: none;
  cursor: pointer;
}

.support__form-textarea {
  border: 1px solid color-mix(in srgb, var(--ag-outline) 20%, transparent);
  background: transparent;
  padding: 16px;
  border-radius: 8px;
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-on-surface);
  transition: border-color 0.2s;
  outline: none;
  resize: vertical;
  font-family: var(--ag-font-body);
}

.support__form-textarea:focus {
  border-color: var(--ag-primary);
}

.support__contact-image {
  position: relative;
  min-height: 400px;
  display: none;
}

@media (min-width: 1024px) {
  .support__contact-image {
    display: block;
  }
}

.support__contact-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.support__contact-overlay {
  position: absolute;
  inset: 0;
  background: color-mix(in srgb, var(--ag-primary) 10%, transparent);
  mix-blend-mode: multiply;
}

.support__contact-quote {
  position: absolute;
  bottom: 48px;
  left: 48px;
  right: 48px;
  padding: 32px;
  background: color-mix(in srgb, #fff 90%, transparent);
  backdrop-filter: blur(12px);
  border-radius: 12px;
}

.support__quote-text {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  font-style: italic;
  line-height: 32px;
  color: var(--ag-on-surface);
  margin: 0 0 8px;
}

.support__quote-author {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  color: var(--ag-primary);
  margin: 0;
}

.support__concierge {
  position: fixed;
  bottom: 32px;
  right: 32px;
  z-index: 60;
}

.support__concierge-btn {
  width: 64px;
  height: 64px;
  background: var(--ag-primary);
  color: #fff;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.support__concierge-btn:hover {
  transform: scale(1.05);
}

.support__concierge-btn .material-symbols-outlined {
  font-size: 32px;
}

.support__concierge-tooltip {
  position: absolute;
  bottom: 100%;
  right: 0;
  margin-bottom: 16px;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s;
  background: var(--ag-inverse-surface);
  color: var(--ag-inverse-on-surface);
  padding: 8px 16px;
  border-radius: 8px;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
}

.support__concierge-tooltip--visible {
  opacity: 1;
}

.support__chat {
  position: fixed;
  bottom: 112px;
  right: 32px;
  width: 380px;
  height: 520px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25);
  z-index: 60;
  display: flex;
  flex-direction: column;
  border: 1px solid color-mix(in srgb, var(--ag-outline) 10%, transparent);
  transform: translateY(20px);
  opacity: 0;
  pointer-events: none;
  transition: all 0.3s;
}

.support__chat--open {
  transform: translateY(0);
  opacity: 1;
  pointer-events: all;
}

.support__chat-header {
  padding: 20px;
  border-bottom: 1px solid color-mix(in srgb, var(--ag-outline) 5%, transparent);
  background: var(--ag-primary);
  color: #fff;
  border-radius: 16px 16px 0 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.support__chat-header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.support__chat-header-title {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
}

.support__chat-close {
  background: none;
  border: none;
  color: #fff;
  opacity: 0.6;
  cursor: pointer;
  transition: opacity 0.2s;
}

.support__chat-close:hover {
  opacity: 1;
}

.support__chat-messages {
  flex-grow: 1;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--ag-surface-container-low);
}

.support__chat-messages::-webkit-scrollbar {
  width: 4px;
}

.support__chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.support__chat-messages::-webkit-scrollbar-thumb {
  background: var(--ag-outline-variant);
  border-radius: 10px;
}

.support__chat-msg {
  max-width: 85%;
  padding: 16px;
  border-radius: 12px;
  font-size: 16px;
  line-height: 24px;
}

.support__chat-msg--bot {
  background: var(--ag-surface-container);
  align-self: flex-start;
  border-top-left-radius: 0;
  color: var(--ag-on-surface-variant);
}

.support__chat-msg--user {
  background: var(--ag-primary-container);
  align-self: flex-end;
  border-top-right-radius: 0;
  color: var(--ag-on-primary-container);
}

.support__chat-msg p {
  margin: 0;
}

.support__chat-input-wrap {
  padding: 16px;
  border-top: 1px solid color-mix(in srgb, var(--ag-outline) 5%, transparent);
  background: #fff;
  border-radius: 0 0 16px 16px;
  position: relative;
}

.support__chat-input {
  width: 100%;
  background: var(--ag-surface-container-low);
  border: none;
  border-radius: 9999px;
  padding: 12px 48px 12px 16px;
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-on-surface);
  outline: none;
  font-family: var(--ag-font-body);
}

.support__chat-input:focus {
  box-shadow: 0 0 0 1px var(--ag-primary);
}

.support__chat-send {
  position: absolute;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--ag-primary);
  cursor: pointer;
}

.support-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(28, 28, 28, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.support-modal {
  background: #fff;
  width: 100%;
  max-width: 520px;
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  border-radius: 16px;
  box-shadow: 0 24px 60px -12px rgba(0,0,0,0.3);
  padding: 32px;
}
.support-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}
.support-modal-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  line-height: 32px;
  margin: 0 0 4px;
  color: var(--ag-on-surface);
}
.support-modal-sub {
  font-size: 14px;
  line-height: 20px;
  color: var(--ag-on-surface-variant);
  margin: 0;
}
.support-modal-close {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--ag-outline);
  padding: 4px;
  transition: color 0.2s;
}
.support-modal-close:hover { color: var(--ag-on-surface); }
.support-modal-form { display: flex; flex-direction: column; gap: 20px; }
.support-modal-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 520px) { .support-modal-row { grid-template-columns: 1fr; } }
.support-modal-error {
  font-size: 13px;
  color: var(--ag-error, #b3261e);
  margin: 0;
}
.support-modal-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: support-spin 0.8s linear infinite;
  margin-right: 8px;
  vertical-align: middle;
}
@keyframes support-spin { to { transform: rotate(360deg); } }
.support-modal-done {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 12px;
  padding: 24px 0;
}
.support-modal-done-icon {
  font-size: 56px;
  color: var(--ag-primary);
}
.support-modal-done-title {
  font-family: var(--ag-font-display);
  font-size: 22px;
  font-weight: 500;
  margin: 0;
}
.support-modal-done-desc {
  font-size: 14px;
  line-height: 22px;
  color: var(--ag-on-surface-variant);
  margin: 0 0 8px;
}
.support-modal-enter-active, .support-modal-leave-active { transition: opacity 0.25s ease; }
.support-modal-enter-from, .support-modal-leave-to { opacity: 0; }
.support-modal-enter-active .support-modal, .support-modal-leave-active .support-modal { transition: transform 0.25s ease; }
.support-modal-enter-from .support-modal, .support-modal-leave-to .support-modal { transform: translateY(16px) scale(0.98); }
</style>
