<template>
  <MarketplaceLayout>
    <main>
      <section class="relative w-full overflow-hidden product-hero-section">
        <!-- 3D Model Viewer -->
        <div v-if="product.model_3d_url" class="absolute inset-0 z-0">
          <Model3DViewer :src="product.model_3d_url"/>
        </div>
        <!-- Product Image -->
        <div v-else-if="product.image" class="absolute inset-0 z-0">
          <img :alt="product.name" :src="product.image" class="w-full h-full object-cover"
               style="object-position: center 30%;"
               @error="product.image = null"/>
        </div>
        <!-- Fallback: letter only -->
        <div v-else class="absolute inset-0 z-0"
             style="background: linear-gradient(135deg, var(--ag-primary-600), var(--ag-primary-600)); display: flex; align-items: center; justify-content: center;">
          <span style="font-family: var(--ag-font-display); font-size: 120px; color: rgba(255,255,255,0.1);">{{
              product.name.charAt(0).toUpperCase()
            }}</span>
        </div>
        <div class="absolute inset-0"
             style="background: linear-gradient(to bottom, rgba(252, 249, 248, 0) 0%, rgba(252, 249, 248, 0.3) 40%, rgba(252, 249, 248, 1) 100%); z-index: 1;"></div>
        <div class="absolute bottom-0 left-0 w-full z-10 product-hero-content"
             style="padding: 0 var(--ag-margin-mobile, 20px) 48px;">
          <div
              style="max-width: var(--ag-container-max, 1280px); margin: 0 auto; display: flex; flex-direction: column; gap: 24px;">
            <div style="flex: 1;">
              <div v-if="product.stock < 1 || product.passport"
                   style="display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap;">
                <span v-if="product.stock < 1" class="tag tag-secondary">Hết hàng</span>
                <span v-if="product.passport" class="tag tag-nft">
                  <span class="material-symbols-outlined text-[15px]"
                        style="color: var(--ag-accent-600);">verified_user</span>
                  Hộ chiếu NFT đã cấp
                </span>
              </div>
              <h1 class="title-lg">{{ product.name }}</h1>
              <div class="flex items-center gap-3 mb-2">
                <p v-if="product.category" class="subtitle">{{ product.category }}</p>
                <Link v-if="product.store?.id" :href="route('agriverse.shop.stores.show', product.store.id)"
                      class="text-sm" style="color: var(--ag-primary-600);">{{ product.store.name }}
                </Link>
                <div v-if="product.seller"
                     class="flex items-center gap-1.5 bg-white/60 backdrop-blur-md px-3 py-1 rounded-full border border-white/40 shadow-sm text-sm font-semibold text-[var(--ag-primary-600)]">
                  <span class="material-symbols-outlined text-[16px]">person</span>
                  Người bán: {{ product.seller.name }}
                </div>
              </div>
            </div>
            <div class="pricing-card">
              <div class="pricing-header">
                <span class="pricing-label">Giá bán</span>
                <div class="pricing-main">
                  <span class="pricing-current">{{ formatPrice(product.price) }}<span
                      style="font-size: 14px; text-decoration: underline;">₫</span></span>
                  <span v-if="product.compare_price" class="pricing-old">{{
                      formatPrice(product.compare_price)
                    }}₫</span>
                  <span v-if="product.compare_price && product.compare_price > product.price"
                        class="pricing-badge">-{{ discountPercent }}%</span>
                </div>
              </div>
              <p class="pricing-note">{{ product.stock > 0 ? `Còn ${product.stock} sản phẩm` : 'Tạm hết hàng' }}</p>
              <div class="pricing-actions">
                <div class="qty-selector">
                  <button class="qty-btn" @click="decrementQty">−</button>
                  <span class="qty-value">{{ quantity }}</span>
                  <button class="qty-btn" @click="incrementQty">+</button>
                </div>
                <button :title="product.wishlisted ? 'Bỏ yêu thích' : 'Thêm yêu thích'"
                        class="wishlist-btn"
                        @click="toggleWishlist">
                  <span :class="product.wishlisted ? 'text-[var(--ag-danger)]' : ''"
                        :style="`font-variation-settings: 'FILL' ${product.wishlisted ? 1 : 0}`"
                        class="material-symbols-outlined">
                    favorite
                  </span>
                </button>
                <button :title="isComparing ? 'Bỏ so sánh' : 'Thêm so sánh'"
                        class="compare-btn"
                        @click="toggleCompare">
                  <span :class="isComparing ? 'text-[var(--ag-primary-500)]' : ''"
                        :style="`font-variation-settings: 'FILL' ${isComparing ? 1 : 0}`"
                        class="material-symbols-outlined">
                    compare_arrows
                  </span>
                </button>
                <button v-if="product.seller && isAuthenticated" class="chat-btn"
                        title="Nhắn tin với người bán"
                        @click="chatWithSeller">
                  <span class="material-symbols-outlined" style="font-size: 20px;">chat</span>
                </button>
                <button class="add-btn" @click="addToCart">
                  <span class="material-symbols-outlined" style="font-size: 20px;">add_shopping_cart</span>
                  Thêm vào giỏ
                </button>
                <button v-if="canOffer" class="chat-btn" title="Đề xuất giá cho người bán"
                        @click="goOffer">
                  <span class="material-symbols-outlined" style="font-size: 20px;">handshake</span>
                </button>
                <button class="buy-now-btn" @click="buyNow">
                  Mua ngay
                </button>
              </div>
              <button class="ar-btn" @click="openARViewer">
                <span class="material-symbols-outlined" style="font-size: 20px;">view_in_ar</span>
                Xem trong không gian của bạn
              </button>
            </div>
          </div>
        </div>
      </section>

      <section class="content-section">
        <div class="content-grid">
          <div class="content-main">
            <h2 class="section-title">Thông tin cây trồng</h2>

            <!-- Identity card -->
            <div v-if="identitySpecs.length" class="identity-grid">
              <div v-for="spec in identitySpecs" :key="spec.label" class="identity-card">
                <span class="material-symbols-outlined identity-icon">{{ spec.icon }}</span>
                <div>
                  <span class="identity-label">{{ spec.label }}</span>
                  <span class="identity-value">{{ spec.value }}</span>
                </div>
              </div>
            </div>

            <!-- Care guide cards -->
            <div v-if="careSpecs.length" class="care-grid">
              <h3 class="care-title">Hướng dẫn chăm sóc</h3>
              <div class="care-cards">
                <div v-for="spec in careSpecs" :key="spec.label" class="care-card">
                  <span class="material-symbols-outlined care-icon">{{ spec.icon }}</span>
                  <span class="care-label">{{ spec.label }}</span>
                  <span class="care-value">{{ spec.value }}</span>
                </div>
              </div>
            </div>

            <!-- Feng shui card -->
            <div v-if="fengshui" class="fengshui-card">
              <span class="material-symbols-outlined fengshui-icon">spa</span>
              <div>
                <span class="fengshui-label">Ý nghĩa phong thủy</span>
                <span class="fengshui-value">{{ fengshui }}</span>
              </div>
            </div>

            <!-- Description (chỉ hiện khi không parse được card) -->
            <div v-if="!identitySpecs.length && !careSpecs.length && cleanDescription" class="narrative-text">
              {{ cleanDescription }}
            </div>
            <div v-else-if="!identitySpecs.length && !careSpecs.length && !cleanDescription" class="narrative-text">
              <p>Được tôn vinh bởi những tán lá hình vĩ cầm ấn tượng, đây là tác phẩm trung tâm hoàn hảo cho không gian
                nội thất hiện đại. Các mẫu vật của chúng tôi được canh tác trong 18 tháng dưới ánh sáng tự nhiên cường
                độ cao để đảm bảo sự phát triển thân cây khỏe mạnh và sắc tố xanh ngọc bích đậm đà.</p>
              <p>Mỗi cây được chọn lọc thủ công bởi các nhà làm vườn bậc thầy của chúng tôi và được vận chuyển trong
                thùng Eco-Crate độc quyền, đảm bảo cây đến nơi trong tình trạng nguyên vẹn với không chất thải nhựa.</p>
            </div>
          </div>

          <aside class="content-sidebar sidebar-sticky">
            <div v-if="product.growth_stats && Object.keys(product.growth_stats).length" class="stats-card">
              <h3 class="sidebar-title">Chỉ số phát triển</h3>
              <div class="stats-list">
                <div v-for="(stat, key) in product.growth_stats" :key="key" class="stat-item">
                  <div class="stat-header">
                    <span class="stat-label">{{ key }}</span>
                    <span class="stat-value">{{ stat.level }}</span>
                  </div>
                  <div class="stat-bar">
                    <div :style="{ transform: 'scaleX(' + (stat.percent / 100) + ')' }" class="stat-fill"></div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="stats-card">
              <h3 class="sidebar-title">Chỉ số phát triển</h3>
              <div class="stats-list">
                <div class="stat-item">
                  <div class="stat-header">
                    <span class="stat-label">Độ cứng cáp</span>
                    <span class="stat-value">Cao</span>
                  </div>
                  <div class="stat-bar">
                    <div class="stat-fill" style="transform: scaleX(0.85);"></div>
                  </div>
                </div>
                <div class="stat-item">
                  <div class="stat-header">
                    <span class="stat-label">Tốc độ tăng trưởng</span>
                    <span class="stat-value">Trung bình</span>
                  </div>
                  <div class="stat-bar">
                    <div class="stat-fill" style="transform: scaleX(0.55);"></div>
                  </div>
                </div>
                <div class="stat-item">
                  <div class="stat-header">
                    <span class="stat-label">Lọc không khí</span>
                    <span class="stat-value">Xuất sắc</span>
                  </div>
                  <div class="stat-bar">
                    <div class="stat-fill" style="transform: scaleX(0.95);"></div>
                  </div>
                </div>
              </div>
            </div>
            <div class="concierge-card">
              <span class="material-symbols-outlined"
                    style="font-size: 32px; color: var(--ag-secondary-500);">verified</span>
              <div>
                <h4 class="concierge-title">Tư vấn chăm sóc đi kèm</h4>
                <p class="concierge-desc">Liên hệ với chuyên gia cây trồng 24/7 qua tin nhắn trong 90 ngày đầu. Chúng
                  tôi luôn sẵn sàng đảm bảo cây của bạn phát triển tốt.</p>
              </div>
            </div>
          </aside>
        </div>
      </section>

      <!-- Digital Passport -->
      <section class="content-section">
        <div class="max-w-[1280px] mx-auto">
          <div class="flex items-start justify-between gap-6 mb-4 flex-wrap">
            <div>
              <h2 class="section-title">Hộ Chiếu Thực Vật Số</h2>
              <p class="text-sm text-[var(--ag-text-secondary)]">Lịch sử truy xuất nguồn gốc và vòng đời của cây cảnh
                này.</p>
            </div>
            <div v-if="product.passport" class="passport-status passport-status-approved">
              <span class="passport-status-dot"></span>
              Đã cấp hộ chiếu
            </div>
          </div>

          <!-- NFT Hộ chiếu đã duyệt -->
          <div v-if="product.passport" class="passport-card">
            <div class="passport-card-head">
              <div class="passport-card-title">
                <span class="material-symbols-outlined" style="color: var(--ag-accent-600);">workspace_premium</span>
                <div>
                  <h3 class="text-lg font-bold text-[var(--ag-text-primary)]">{{ product.passport.name }}</h3>
                  <span class="font-mono text-xs text-[var(--ag-text-muted)]">{{ product.passport.code }}</span>
                </div>
              </div>
              <span v-if="product.passport.is_minted && product.passport.token_id" class="passport-token">
                <span class="material-symbols-outlined text-[15px]">diamond</span>
                Token #{{ product.passport.token_id }}
              </span>
            </div>

            <div v-if="product.passport.image_url" class="passport-card-img">
              <img :alt="product.passport.name" :src="product.passport.image_url"/>
            </div>

            <div class="passport-grid">
              <div v-if="product.passport.species" class="passport-fact">
                <span>Loài</span><strong>{{ product.passport.species }}</strong></div>
              <div v-if="product.passport.variety" class="passport-fact">
                <span>Giống</span><strong>{{ product.passport.variety }}</strong></div>
              <div v-if="product.passport.age_years != null" class="passport-fact">
                <span>Tuổi</span><strong>{{ product.passport.age_years }} năm</strong></div>
              <div v-if="product.passport.height_cm != null" class="passport-fact">
                <span>Chiều cao</span><strong>{{ product.passport.height_cm }} cm</strong></div>
              <div v-if="product.passport.trunk_cm != null" class="passport-fact">
                <span>Đường kính thân</span><strong>{{ product.passport.trunk_cm }} cm</strong></div>
              <div v-if="product.passport.origin" class="passport-fact">
                <span>Nguồn gốc</span><strong>{{ product.passport.origin }}</strong></div>
              <div v-if="product.passport.owner_name" class="passport-fact">
                <span>Chủ sở hữu</span><strong>{{ product.passport.owner_name }}</strong></div>
              <div v-if="product.passport.approved_at" class="passport-fact">
                <span>Ngày cấp</span><strong>{{ product.passport.approved_at }}</strong></div>
            </div>

            <p v-if="product.passport.description" class="passport-desc">{{ product.passport.description }}</p>

            <div class="passport-links">
              <a :href="product.passport.lookup_url" class="ag-btn ag-btn-sm ag-btn-primary passport-lookup-btn" rel="noopener"
                 target="_blank">
                <span class="material-symbols-outlined text-[16px]">qr_code_scanner</span>
                Tra cứu hộ chiếu
                <span class="material-symbols-outlined passport-open-icon">open_in_new</span>
              </a>
              <a v-if="product.passport.ipfs_gateway_uri" :href="product.passport.ipfs_gateway_uri" class="ag-btn ag-btn-outline ag-btn-sm"
                 rel="noopener" target="_blank">
                <span class="material-symbols-outlined text-[15px]">cloud</span>
                Metadata IPFS
              </a>
              <a v-if="product.passport.tx_explorer_url" :href="product.passport.tx_explorer_url" class="ag-btn ag-btn-outline ag-btn-sm"
                 rel="noopener" target="_blank">
                <span class="material-symbols-outlined text-[15px]">link</span>
                Giao dịch trên chuỗi
              </a>
            </div>

            <!-- QR tra cứu công khai -->
            <div class="passport-qr-wrap">
              <div class="passport-qr-box">
                <canvas ref="passportQrCanvas" class="passport-qr-canvas"></canvas>
                <p class="passport-qr-hint">
                  <span class="material-symbols-outlined" style="font-size:13px">qr_code_scanner</span>
                  Quét mã QR để xem hồ sơ sinh trưởng & chủ sở hữu
                </p>
              </div>
            </div>
          </div>

          <!-- Lịch sử chăm sóc hộ chiếu -->
          <div v-if="product.passport && passportCareLogs.length" class="passport-care-history mt-6">
            <div class="passport-care-head">
              <h3 class="text-base font-bold text-[var(--ag-text-primary)]">Lịch sử chăm sóc &nbsp;vòng đời</h3>
              <span class="passport-care-count">{{ passportCareLogs.length }} sự kiện</span>
            </div>
            <div class="space-y-4 passport-timeline">
              <div v-for="(log, i) in passportCareLogs" :key="log.id || i" class="flex gap-4">
                <div class="flex flex-col items-center">
                  <div :class="i === 0 ? 'bg-[var(--ag-primary-500)] text-white' : 'bg-[var(--ag-bg)] text-[var(--ag-text-muted)] border border-[var(--ag-border)]'"
                       class="w-10 h-10 rounded-full flex items-center justify-center">
                    <span class="material-symbols-outlined text-sm">{{ careLogIcon(log.event_type) }}</span>
                  </div>
                  <div v-if="i < passportCareLogs.length - 1" class="w-0.5 flex-1 bg-[var(--ag-border)] mt-1"></div>
                </div>
                <div class="pb-6 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="text-base font-semibold text-[var(--ag-text-primary)]">{{ log.title }}</span>
                    <span class="passport-care-badge">{{ careLogLabel(log.event_type) }}</span>
                  </div>
                  <div class="text-sm text-[var(--ag-text-muted)] mt-0.5">{{ log.event_date }}</div>
                  <p v-if="log.description" class="text-sm text-[var(--ag-text-secondary)] mt-1">{{
                      log.description
                    }}</p>
                  <img v-if="log.image_url" :alt="log.title" :src="log.image_url" class="passport-care-img mt-2"
                       loading="lazy"/>
                </div>
              </div>
            </div>
          </div>

          <!-- Timeline logs -->
          <div v-if="passportLogs.length" class="space-y-4 passport-timeline mt-6">
            <div v-for="(log, i) in passportLogs" :key="log.id || i" class="flex gap-4">
              <div class="flex flex-col items-center">
                <div :class="i === 0 ? 'bg-[var(--ag-primary-500)] text-white' : 'bg-[var(--ag-bg)] text-[var(--ag-text-muted)] border border-[var(--ag-border)]'"
                     class="w-10 h-10 rounded-full flex items-center justify-center">
                  <span class="material-symbols-outlined text-sm">{{ passportIcon(log.action) }}</span>
                </div>
                <div v-if="i < passportLogs.length - 1" class="w-0.5 flex-1 bg-[var(--ag-border)] mt-1"></div>
              </div>
              <div class="pb-6">
                <div class="text-base font-semibold text-[var(--ag-text-primary)]">{{ passportLabel(log.action) }}</div>
                <div class="text-sm text-[var(--ag-text-muted)] mt-0.5">{{ log.created_at }}</div>
                <div v-if="log.performer" class="text-sm text-[var(--ag-text-secondary)] mt-0.5">Bởi:
                  {{ log.performer.name }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="!product.passport && !passportLogs.length" class="content-section">
        <div class="max-w-[1280px] mx-auto">
          <h2 class="section-title">Hộ Chiếu Thực Vật Số</h2>
          <div class="passport-empty">
            <span class="material-symbols-outlined passport-empty-icon">fingerprint</span>
            <p class="passport-empty-text">Chưa có dữ liệu truy xuất cho cây này.</p>
          </div>
        </div>
      </section>

      <section class="reviews-section">
        <div
            style="max-width: var(--ag-container-max, 1280px); margin: 0 auto; padding: 0 var(--ag-margin-desktop, 64px);">
          <div class="reviews-header">
            <div>
              <h2 class="reviews-title">Cảm nhận của người sưu tầm</h2>
              <div v-if="reviews.length" class="rating-row">
                <div class="stars">
                  <span v-for="i in 5" :key="i" :class="i <= Math.round(averageRating) ? 'star-filled' : 'star-empty'"
                        class="material-symbols-outlined star-icon">star</span>
                </div>
                <span class="rating-text">{{ averageRating }} trung bình ({{ reviews.length }} đánh giá)</span>
              </div>
            </div>
          </div>
          <div v-if="reviews.length" class="reviews-grid">
            <div v-for="review in reviews" :key="review.id" class="review-card">
              <div class="review-header">
                <span class="review-author">{{ review.user?.name || 'Ẩn danh' }}</span>
                <span class="review-badge">Đã mua hàng</span>
              </div>
              <p v-if="review.comment" class="review-quote">"{{ review.comment }}"</p>
            </div>
          </div>
          <div v-else class="reviews-empty">
            <span class="material-symbols-outlined reviews-empty-icon">rate_review</span>
            <p class="reviews-empty-text">Chưa có đánh giá nào. Hãy là người sưu tầm đầu tiên chia sẻ cảm nhận sau khi
              nhận cây.</p>
          </div>
        </div>
      </section>

      <!-- Related Products -->
      <section v-if="relatedProducts.length" class="related-section">
        <div
            style="max-width: var(--ag-container-max, 1280px); margin: 0 auto; padding: 0 var(--ag-margin-desktop, 64px);">
          <div class="related-header">
            <h2 class="section-title">Có thể bạn cũng thích</h2>
            <Link :href="route('agriverse.shop.products.index')" class="related-view-all">
              Xem tất cả
              <span class="material-symbols-outlined" style="font-size: 18px;">arrow_forward</span>
            </Link>
          </div>
          <div class="carousel-wrap">
            <button :disabled="carouselAtStart" class="carousel-btn carousel-btn-prev" @click="scrollCarousel(-1)">
              <span class="material-symbols-outlined">chevron_left</span>
            </button>
            <div ref="carouselRef" class="carousel-track" @scroll="onCarouselScroll">
              <div v-for="rp in relatedProducts" :key="rp.id" class="carousel-item">
                <ProductCard :product="rp"/>
              </div>
            </div>
            <button :disabled="carouselAtEnd" class="carousel-btn carousel-btn-next" @click="scrollCarousel(1)">
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
      </section>

      <!-- Chat with seller -->
      <section v-if="product.seller" class="seller-chat-section">
        <div
            style="max-width: var(--ag-container-max, 1280px); margin: 0 auto; padding: 0 var(--ag-margin-desktop, 64px);">
          <div class="seller-chat-card">
            <div class="seller-chat-header">
              <div class="seller-chat-avatar">
                <span class="material-symbols-outlined">eco</span>
              </div>
              <div>
                <h2 class="seller-chat-title">Trò chuyện với {{ product.seller.name }}</h2>
                <p class="seller-chat-sub">Đặt câu hỏi về cây này — người bán sẽ phản hồi nhanh nhất có thể.</p>
              </div>
            </div>
            <div class="quick-replies">
              <button v-for="reply in quickReplies" :key="reply" class="quick-reply-btn" @click="chatWithSeller(reply)">
                <span class="material-symbols-outlined" style="font-size: 16px;">chat_bubble_outline</span>
                {{ reply }}
              </button>
            </div>
            <button class="seller-chat-cta" @click="chatWithSeller()">
              <span class="material-symbols-outlined" style="font-size: 20px;">forum</span>
              Mở hội thoại
            </button>
          </div>
        </div>
      </section>

      <div :class="{ 'sticky-bar-visible': showStickyBar }" class="sticky-bar">
        <div
            style="max-width: var(--ag-container-max, 1280px); margin: 0 auto; padding: 0 var(--ag-margin-desktop, 64px); display: flex; justify-content: space-between; align-items: center;">
          <div style="display: flex; align-items: center; gap: 16px;">
            <div
                style="width: 48px; height: 48px; border-radius: 50%; background: linear-gradient(135deg, var(--ag-primary-600), var(--ag-primary-500)); display: flex; align-items: center; justify-content: center;">
              <span style="font-family: var(--ag-font-display); font-size: 20px; color: rgba(255,255,255,0.3);">{{
                  product.name.charAt(0).toUpperCase()
                }}</span>
            </div>
            <div>
              <h3 class="sticky-name">{{ product.name }}</h3>
              <p v-if="product.price" class="sticky-price">{{ formatPrice(product.price) }}₫</p>
            </div>
          </div>
          <div style="display: flex; gap: 10px;">
            <button class="sticky-ar" @click="openARViewer">
              <span class="material-symbols-outlined" style="font-size: 18px;">view_in_ar</span>
            </button>
            <button class="sticky-add-cart" @click="addToCart">Thêm vào giỏ</button>
            <button class="sticky-buy" @click="buyNow">Mua ngay</button>
          </div>
        </div>
      </div>
    </main>

    <!-- AR Modal -->
    <div v-if="arModalOpen" class="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
         @click.self="arModalOpen = false">
      <div class="bg-white rounded-3xl w-full max-w-lg max-h-[90vh] overflow-y-auto shadow-2xl" @click.stop>
        <div class="flex items-center justify-between p-5 border-b border-stone-100">
          <h3 class="text-sm font-bold text-stone-800">Xem trong không gian của bạn</h3>
          <button class="w-8 h-8 rounded-full hover:bg-stone-100 flex items-center justify-center transition-colors"
                  @click="arModalOpen = false">
            <span class="material-symbols-outlined text-lg">close</span>
          </button>
        </div>
        <div class="p-5">
          <ARViewer :model-url="arModelUrl"/>
        </div>
      </div>
    </div>
  </MarketplaceLayout>
</template>

<script setup>
import {ref, computed, onMounted, nextTick, onBeforeUnmount} from 'vue';
import {Link, router, usePage} from '@inertiajs/vue3';
import {route} from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import ProductCard from '@agriverse/Components/ProductCard.vue';
import {parsePlantDescription} from '@agriverse/utils';
import {useToast} from 'primevue/usetoast';
import {useChat} from '@agriverse/Composables/useChat';
import {useCompare} from '@agriverse/Composables/useCompare';
// Advanced 3D/AR views remain in the repository and will be activated later.
const ARViewer = null;
const Model3DViewer = null;

const toast = useToast();
const page = usePage();
const {openPanel} = useChat();
const {isComparing, toggle: toggleCompareId} = useCompare();
const isAuthenticated = computed(() => !!page.props.auth?.user);

const props = defineProps({
  product: {type: Object, default: () => ({})},
  relatedProducts: {type: Array, default: () => []},
});

const carouselRef = ref(null);
const carouselAtStart = ref(true);
const carouselAtEnd = ref(false);

function scrollCarousel(dir) {
  if (!carouselRef.value) return;
  const scrollAmount = carouselRef.value.querySelector('.carousel-item')?.offsetWidth + 24 || 320;
  carouselRef.value.scrollBy({left: dir * scrollAmount, behavior: 'smooth'});
}

function onCarouselScroll() {
  if (!carouselRef.value) return;
  const el = carouselRef.value;
  carouselAtStart.value = el.scrollLeft <= 4;
  carouselAtEnd.value = el.scrollLeft + el.clientWidth >= el.scrollWidth - 4;
}

const quantity = ref(1);
const arModalOpen = ref(false);
const arModelUrl = computed(() => props.product?.model_3d_url || '');
const reviews = computed(() => props.product?.reviews || []);
const averageRating = computed(() => {
  const r = reviews.value;
  if (!r.length) return 0;
  return (r.reduce((s, r) => s + (r.rating || 0), 0) / r.length).toFixed(1);
});

const discountPercent = computed(() => {
  if (!props.product?.compare_price) return 0;
  return Math.round((1 - props.product.price / props.product.compare_price) * 100);
});

const specIconMap = {
  'Tên khoa học': 'biotech',
  'Họ thực vật': 'category',
  'Nguồn gốc': 'public',
  'Ánh sáng': 'light_mode',
  'Tưới nước': 'water_drop',
  'Phân bón': 'nutrition',
  'Nhiệt độ tối thiểu': 'thermostat',
  'Độ pH đất': 'science',
  'Vị trí': 'home',
};
const identityKeys = ['Tên khoa học', 'Họ thực vật', 'Nguồn gốc'];

function parseSpecs() {
  const desc = props.product?.description || '';
  const parsed = parsePlantDescription(desc);

  if (parsed.identity.length || parsed.care.length) {
    return parsed;
  }

  const specs = props.product?.technical_specs;
  if (specs && typeof specs === 'object') {
    for (const [key, val] of Object.entries(specs)) {
      if (!val) continue;
      const icon = specIconMap[key] || 'info';
      const item = {label: key, value: val, icon};
      if (identityKeys.includes(key)) {
        parsed.identity.push(item);
      } else {
        parsed.care.push(item);
      }
    }
    const meta = props.product?.metadata;
    if (meta?.origin) parsed.identity.push({label: 'Nguồn gốc', value: meta.origin, icon: 'public'});
    if (meta?.fertilizing_guide) parsed.care.push({
      label: 'Phân bón',
      value: meta.fertilizing_guide,
      icon: 'nutrition'
    });
    if (meta?.meaning_fengshui) parsed.fengshui = meta.meaning_fengshui;
  }
  return parsed;
}

const identitySpecs = computed(() => parseSpecs().identity);
const careSpecs = computed(() => parseSpecs().care);
const fengshui = computed(() => parseSpecs().fengshui);
const cleanDescription = computed(() => {
  const parsed = parseSpecs();
  return parsed.cleanDesc || props.product?.description || '';
});

const passportLogs = computed(() => props.product?.passport_logs || []);
const passportCareLogs = computed(() => props.product?.passport?.logs || []);

const showStickyBar = ref(false);
let scrollHandler = null;

const passportQrCanvas = ref(null);
const passportQrUrl = computed(() => props.product?.passport?.lookup_url || '');

function renderPassportQr() {
  if (!passportQrCanvas.value || !passportQrUrl.value) return;
  if (typeof window.QRCode === 'undefined') {
    const s = document.createElement('script');
    s.src = 'https://cdn.jsdelivr.net/npm/qrcode/build/qrcode.min.js';
    s.onload = () => renderPassportQr();
    s.onerror = () => {
    };
    document.head.appendChild(s);
    return;
  }
  window.QRCode.toCanvas(
      passportQrCanvas.value,
      passportQrUrl.value,
      {width: 132, height: 132, margin: 1, color: {dark: '#1f2937', light: '#ffffff'}},
      (err) => {
        if (err) console.error('QR render failed', err);
      }
  );
}

onMounted(() => {
  scrollHandler = () => {
    showStickyBar.value = window.scrollY > 800;
  };
  window.addEventListener('scroll', scrollHandler, {passive: true});
  nextTick(() => {
    onCarouselScroll();
    renderPassportQr();
  });
});

onBeforeUnmount(() => {
  if (scrollHandler) window.removeEventListener('scroll', scrollHandler);
});

function formatPrice(price) {
  return new Intl.NumberFormat('vi-VN').format(price || 0);
}

function incrementQty() {
  quantity.value = Math.min(quantity.value + 1, props.product.stock || 99);
}

function decrementQty() {
  quantity.value = Math.max(1, quantity.value - 1);
}

async function addToCart() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

const currentUserId = computed(() => page.props.auth?.user?.id ?? null);
const canOffer = computed(() => isAuthenticated.value && props.product?.seller?.id != null && props.product.seller.id !== currentUserId.value);

function goOffer() {
  router.get('/agriverse/de-xuat-gia', {product: props.product.id}, {preserveState: true});
}

async function buyNow() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

function passportLabel(action) {
  const labels = {
    product_submitted: 'Sản phẩm được tạo',
    product_approved: 'Sản phẩm được duyệt',
    ordered: 'Đơn hàng được đặt',
    order_confirmed: 'Đơn hàng đã được xác nhận',
    order_shipped: 'Đơn hàng đã được giao cho vận chuyển',
    order_delivered: 'Đơn hàng đã được giao thành công',
    order_completed: 'Người mua đã xác nhận nhận hàng',
    order_cancelled: 'Đơn hàng đã bị hủy',
    seed_planted: 'Ươm hạt thành công',
    styled_bonsai: 'Uốn nắn tạo dáng lần 1',
    repotted: 'Sang chậu đất nung',
    certified_healthy: 'Chứng nhận sức khỏe định kỳ',
  };
  return labels[action] || action;
}

function careLogLabel(type) {
  const labels = {
    tuoi: 'Tưới nước',
    'bon-phan': 'Bón phân',
    'cat-tia': 'Cắt tỉa',
    'thay-dat': 'Thay đất',
    'phun-thuoc': 'Phun thuốc',
    other: 'Khác',
  };
  return labels[type] || type;
}

function careLogIcon(type) {
  const icons = {
    tuoi: 'water_drop',
    'bon-phan': 'grass',
    'cat-tia': 'content_cut',
    'thay-dat': 'potted_plant',
    'phun-thuoc': 'medication',
    other: 'notes',
  };
  return icons[type] || 'circle';
}

function passportIcon(action) {
  const icons = {
    product_submitted: 'upload_file',
    product_approved: 'verified',
    ordered: 'shopping_cart',
    order_confirmed: 'check_circle',
    order_shipped: 'local_shipping',
    order_delivered: 'inventory_2',
    order_completed: 'handshake',
    order_cancelled: 'cancel',
    seed_planted: 'eco',
    styled_bonsai: 'content_cut',
    repotted: 'potted_plant',
    certified_healthy: 'health_and_safety',
  };
  return icons[action] || 'circle';
}

async function toggleWishlist() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

function toggleCompare() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

const quickReplies = [
  'Cây này còn không?',
  'Giá có giảm được không?',
  'Giao hàng tận nơi không?',
  'Có kèm hướng dẫn chăm sóc không?',
  'Cây đã được kiểm dịch chưa?',
];

async function chatWithSeller(message = '') {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

function openARViewer() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

function closeARViewer() {
  arModalOpen.value = false;
}
</script>

<style scoped>
.product-hero-section {
  height: 716px;
}

@media (max-width: 640px) {
  .product-hero-section {
    height: auto;
    min-height: 0;
    padding-top: 280px;
    overflow: visible;
  }

  .product-hero-content {
    position: relative;
    bottom: auto;
    left: auto;
    padding: 0 20px 32px !important;
  }
}

.tag {
  padding: 4px 12px;
  border-radius: 9999px;
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
}

.tag-secondary {
  background: rgba(139, 79, 39, 0.08);
  color: var(--ag-secondary-500);
}

.tag-primary {
  background: rgba(72, 103, 48, 0.08);
  color: var(--ag-primary-500);
}

.title-lg {
  font-family: var(--ag-font-display);
  font-size: clamp(2.25rem, 1.7rem + 1.4vw, 3rem);
  font-weight: 500;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}

.subtitle {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  color: var(--ag-primary-500);
  opacity: 0.8;
  font-style: italic;
}

.pricing-card {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 24px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px -4px rgba(0, 0, 0, 0.08);
  width: 100%;
}

@media (max-width: 640px) {
  .pricing-card {
    padding: 16px;
  }

  .pricing-current {
    font-size: 26px;
  }

  .pricing-actions {
    gap: 8px;
  }

  .add-btn {
    height: 44px;
    font-size: 15px;
  }

  .qty-btn {
    width: 36px;
    height: 36px;
  }

  .qty-value {
    min-height: 36px;
  }

  .wishlist-btn, .chat-btn {
    width: 40px;
    height: 40px;
  }

  .ar-btn {
    height: 40px;
    font-size: 13px;
  }
}

@media (min-width: 768px) {
  .pricing-card {
    width: 320px;
    padding: 24px;
  }

  section.product-hero-section {
    height: 870px !important;
  }

  section.relative.w-full.overflow-hidden > div:first-child > span {
    font-size: 160px;
  }

  div.absolute.bottom-0.left-0.w-full {
    padding: 0 var(--ag-margin-desktop, 64px) 48px;
  }

  div.absolute.bottom-0.left-0.w-full > div {
    flex-direction: row;
    justify-content: space-between;
    align-items: flex-end;
  }
}

.pricing-header {
  margin-bottom: 16px;
}

.pricing-label {
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--ag-text-secondary);
  display: block;
  margin-bottom: 8px;
}

.pricing-main {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.pricing-current {
  font-family: var(--ag-font-display);
  font-size: 32px;
  font-weight: 500;
  line-height: 40px;
  color: var(--ag-primary-500);
}

.pricing-old {
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-secondary);
  text-decoration: line-through;
}

.pricing-badge {
  font-family: var(--ag-font-body);
  font-size: 11px;
  font-weight: 700;
  color: white;
  background: var(--ag-danger);
  padding: 2px 8px;
  border-radius: 6px;
}

.pricing-note {
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-text-secondary);
  margin-bottom: 24px;
}

.pricing-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.qty-selector {
  flex: 1;
  display: flex;
  align-items: center;
  border: 1px solid var(--ag-border);
  border-radius: 8px;
  overflow: hidden;
  background: white;
}

.qty-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  font-size: 18px;
  font-weight: 600;
  color: var(--ag-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.qty-btn:hover {
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  color: var(--ag-primary-500);
}

.qty-value {
  flex: 1;
  width: auto;
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-text-primary);
  border-left: 1px solid var(--ag-border);
  border-right: 1px solid var(--ag-border);
}

.add-btn {
  flex: 1 1 100%;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 8px;
  border: 1px solid var(--ag-primary-500);
  background: transparent;
  color: var(--ag-primary-500);
  font-family: var(--ag-font-body);
  font-size: 15px;
  white-space: nowrap;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.add-btn:hover {
  background: rgba(72, 103, 48, 0.04);
}

.add-btn:active {
  transform: scale(0.97);
}

.buy-now-btn {
  flex: 1 1 100%;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 8px;
  border: none;
  background: var(--ag-primary-500);
  color: white;
  font-family: var(--ag-font-body);
  font-size: 15px;
  white-space: nowrap;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.buy-now-btn:hover {
  background: var(--ag-primary-600);
}

.buy-now-btn:active {
  transform: scale(0.97);
}

.wishlist-btn {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--ag-border) 15%, transparent);
  background: white;
  color: var(--ag-text-secondary);
  cursor: pointer;
  transition: all 0.3s;
  flex-shrink: 0;
}

.compare-btn {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--ag-border) 15%, transparent);
  background: white;
  color: var(--ag-text-secondary);
  cursor: pointer;
  transition: all 0.3s;
  flex-shrink: 0;
}

.compare-btn:hover {
  border-color: var(--ag-primary-500);
  color: var(--ag-primary-500);
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
}

.compare-btn .material-symbols-outlined {
  font-size: 20px;
}

.wishlist-btn:hover {
  border-color: var(--ag-danger);
  color: var(--ag-danger);
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.08);
}

.wishlist-btn:active {
  transform: scale(0.9);
}

.chat-btn {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--ag-border) 15%, transparent);
  background: white;
  color: var(--ag-primary-500);
  cursor: pointer;
  transition: all 0.3s;
  flex-shrink: 0;
}

.chat-btn:hover {
  border-color: var(--ag-primary-500);
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  box-shadow: 0 2px 8px rgba(72, 103, 48, 0.08);
}

.chat-btn:active {
  transform: scale(0.9);
}

.ar-btn {
  margin-top: 10px;
  width: 100%;
  height: 40px;
  padding: 6px 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 8px;
  border: 1px solid rgba(72, 103, 48, 0.2);
  background: white;
  color: var(--ag-primary-500);
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  padding: 0 16px;
  white-space: nowrap;
  flex-shrink: 0;
}

.ar-btn:hover {
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
  border-color: var(--ag-primary-500);
  box-shadow: 0 2px 8px color-mix(in srgb, var(--ag-primary-500) 12%, transparent);
}

.ar-btn:active {
  transform: scale(0.97);
}

.content-section {
  max-width: var(--ag-container-max, 1280px);
  margin: 0 auto;
  padding: 80px var(--ag-margin-desktop, 64px);
}

@media (max-width: 768px) {
  .content-section {
    padding: 48px var(--ag-margin-mobile, 20px);
  }
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 64px;
}

@media (min-width: 1024px) {
  .content-grid {
    grid-template-columns: 7fr 5fr;
    gap: 64px;
  }
}

.content-main {
  min-width: 0;
}

.section-title {
  font-family: var(--ag-font-display);
  font-size: 32px;
  font-weight: 500;
  line-height: 40px;
  color: var(--ag-text-primary);
  margin-bottom: 24px;
}

.narrative-text {
  font-family: var(--ag-font-body);
  font-size: 18px;
  line-height: 28px;
  color: var(--ag-text-secondary);
}

.narrative-text p {
  margin-bottom: 24px;
}

.identity-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 48px;
}

.identity-card {
  flex: 1 1 180px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--ag-bg-card);
  border: 1px solid var(--ag-border);
  border-radius: 12px;
}

.identity-icon {
  font-size: 24px;
  color: var(--ag-primary-500);
  flex-shrink: 0;
}

.identity-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--ag-text-secondary);
  margin-bottom: 2px;
}

.identity-value {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-text-primary);
}

.care-grid {
  margin-bottom: 48px;
}

.care-title {
  font-family: var(--ag-font-display);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--ag-text-secondary);
  margin-bottom: 16px;
}

.care-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

@media (min-width: 768px) {
  .care-cards {
    grid-template-columns: repeat(4, 1fr);
  }
}

.care-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 8px;
  padding: 24px 16px;
  background: var(--ag-bg);
  border: 1px solid var(--ag-border);
  border-radius: 12px;
  transition: all 0.2s;
}

.care-card:hover {
  background: var(--ag-bg-card);
  border-color: color-mix(in srgb, var(--ag-primary-500) 20%, transparent);
  box-shadow: var(--ag-shadow-sm);
}

.care-icon {
  font-size: 28px;
  color: var(--ag-primary-500);
}

.care-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--ag-text-secondary);
}

.care-value {
  font-size: 13px;
  line-height: 18px;
  color: var(--ag-text-primary);
}

.fengshui-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px;
  background: color-mix(in srgb, var(--ag-accent-500) 6%, transparent);
  border: 1px solid color-mix(in srgb, var(--ag-accent-500) 15%, transparent);
  border-radius: 12px;
  margin-bottom: 48px;
}

.fengshui-icon {
  font-size: 32px;
  color: var(--ag-accent-500);
  flex-shrink: 0;
}

.fengshui-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--ag-accent-500);
  margin-bottom: 4px;
}

.fengshui-value {
  display: block;
  font-size: 15px;
  line-height: 22px;
  color: var(--ag-text-primary);
}

.sidebar-sticky {
  position: sticky;
  top: 100px;
  align-self: start;
}

.stats-card {
  background: var(--ag-bg-card);
  padding: 40px;
  border-radius: 16px;
  border: 1px solid var(--ag-border);
  box-shadow: var(--ag-shadow-sm);
  margin-bottom: 48px;
}

.sidebar-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  line-height: 32px;
  color: var(--ag-text-primary);
  margin-bottom: 24px;
}

.stats-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.stat-label {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-text-primary);
}

.stat-value {
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-primary-500);
}

.stat-bar {
  height: 6px;
  width: 100%;
  background: var(--ag-bg);
  border-radius: 9999px;
  overflow: hidden;
}

.stat-fill {
  height: 100%;
  background: var(--ag-primary-500);
  border-radius: 9999px;
  transform-origin: left;
  transition: transform 1s ease;
}

.concierge-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px;
  background: rgba(139, 79, 39, 0.04);
  border-radius: 12px;
  border: 1px solid rgba(139, 79, 39, 0.1);
}

.concierge-title {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--ag-secondary-500);
  margin-bottom: 4px;
}

.concierge-desc {
  font-family: var(--ag-font-body);
  font-size: 12px;
  line-height: 16px;
  color: var(--ag-text-secondary);
}

.reviews-section {
  background: var(--ag-bg);
  padding: 120px 0;
}

.reviews-header {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 64px;
  gap: 24px;
}

@media (min-width: 768px) {
  .reviews-header {
    flex-direction: row;
  }
}

.reviews-title {
  font-family: var(--ag-font-display);
  font-size: clamp(2.25rem, 1.7rem + 1.4vw, 2rem);
  font-weight: 500;
  line-height: 1.2;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stars {
  display: flex;
  gap: 2px;
}

.star-icon {
  font-size: 20px;
}

.star-filled {
  color: var(--ag-primary-500);
  font-variation-settings: 'FILL' 1;
}

.star-empty {
  color: var(--ag-text-muted);
}

.rating-text {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-text-secondary);
}

.reviews-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 24px;
  border: 1px dashed var(--ag-border);
  border-radius: 16px;
  background: var(--ag-surface-container-low);
  text-align: center;
}

.reviews-empty-icon {
  font-size: 40px;
  color: var(--ag-text-muted);
}

.reviews-empty-text {
  font: 400 14px/1.6 var(--ag-font-body);
  color: var(--ag-text-secondary);
  max-width: 420px;
}

.reviews-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

@media (min-width: 768px) {
  .reviews-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.review-card {
  background: var(--ag-bg-card);
  padding: 32px;
  border-radius: 16px;
  box-shadow: var(--ag-shadow-sm);
  border: 1px solid rgba(116, 121, 108, 0.06);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.review-author {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-text-primary);
}

.review-badge {
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-text-secondary);
}

.review-quote {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  font-style: italic;
  color: var(--ag-primary-500);
  line-height: 28px;
}

.seller-chat-section {
  padding: 80px 0 0;
}

@media (max-width: 768px) {
  .seller-chat-section {
    padding: 48px 0 0;
  }
}

.seller-chat-card {
  background: linear-gradient(135deg, rgba(72, 103, 48, 0.05), rgba(72, 103, 48, 0.02));
  border: 1px solid color-mix(in srgb, var(--ag-primary-500) 18%, var(--ag-border));
  border-radius: 24px;
  padding: 48px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  align-items: flex-start;
}

@media (max-width: 768px) {
  .seller-chat-card {
    padding: 32px 20px;
  }
}

.seller-chat-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.seller-chat-avatar {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--ag-primary-500);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.seller-chat-avatar .material-symbols-outlined {
  font-size: 28px;
}

.seller-chat-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  line-height: 32px;
  color: var(--ag-text-primary);
}

.seller-chat-sub {
  font-family: var(--ag-font-body);
  font-size: 14px;
  line-height: 20px;
  color: var(--ag-text-secondary);
  margin-top: 4px;
}

.quick-replies {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.quick-reply-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 9999px;
  border: 1px solid color-mix(in srgb, var(--ag-primary-500) 25%, transparent);
  background: var(--ag-bg-card);
  color: var(--ag-primary-500);
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
}

.quick-reply-btn:hover {
  background: var(--ag-primary-500);
  color: white;
  border-color: var(--ag-primary-500);
  box-shadow: 0 4px 12px color-mix(in srgb, var(--ag-primary-500) 25%, transparent);
}

.quick-reply-btn:active {
  transform: scale(0.96);
}

.seller-chat-cta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  border-radius: 12px;
  border: none;
  background: var(--ag-primary-500);
  color: white;
  font-family: var(--ag-font-body);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
}

.seller-chat-cta:hover {
  background: var(--ag-primary-600);
  box-shadow: 0 6px 16px color-mix(in srgb, var(--ag-primary-500) 30%, transparent);
}

.seller-chat-cta:active {
  transform: scale(0.97);
}

.sticky-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(16px);
  padding: 16px 0;
  border-top: 1px solid color-mix(in srgb, var(--ag-border) 30%, transparent);
  transform: translateY(100%);
  transition: transform 0.3s ease;
  z-index: 40;
}

.sticky-bar-visible {
  transform: translateY(0);
}

.sticky-name {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-text-primary);
}

.sticky-price {
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-primary-500);
}

.sticky-add-cart {
  padding: 12px 32px;
  border: 1px solid var(--ag-primary-500);
  color: var(--ag-primary-500);
  background: transparent;
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.sticky-add-cart:hover {
  background: rgba(72, 103, 48, 0.04);
}

.sticky-buy {
  padding: 12px 48px;
  background: var(--ag-primary-500);
  color: white;
  border: none;
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.sticky-buy:hover {
  background: var(--ag-primary-600);
}

.sticky-buy:active {
  transform: scale(0.98);
}

.sticky-ar {
  width: 44px;
  height: 44px;
  border: 1px solid var(--ag-border);
  background: white;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--ag-primary-500);
}

.sticky-ar:hover {
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
  border-color: var(--ag-primary-500);
}

.related-section {
  padding: 80px 0;
}

.related-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 48px;
}

.related-view-all {
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--ag-primary-500);
  text-decoration: none;
  transition: all 0.3s;
}

.related-view-all:hover {
  gap: 8px;
}

.carousel-wrap {
  position: relative;
}

.carousel-track {
  display: flex;
  gap: 24px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  padding: 4px 0;
}

.carousel-track::-webkit-scrollbar {
  display: none;
}

.carousel-item {
  flex: 0 0 calc((100% - 72px) / 4);
  scroll-snap-align: start;
  min-width: 0;
}

.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid var(--ag-border);
  background: var(--ag-bg-card);
  box-shadow: var(--ag-shadow-md);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--ag-text-secondary);
  opacity: 0;
}

.carousel-btn:hover {
  background: var(--ag-primary-500);
  color: white;
  border-color: var(--ag-primary-500);
}

.carousel-btn:disabled {
  opacity: 0 !important;
  cursor: default;
}

.carousel-wrap:hover .carousel-btn {
  opacity: 1;
}

.carousel-btn-prev {
  left: -22px;
}

.carousel-btn-next {
  right: -22px;
}

@media (max-width: 768px) {
  .sticky-bar {
    display: none;
  }

  .related-section {
    padding: 48px 0;
  }

  .carousel-item {
    flex: 0 0 calc((100% - 16px) / 2);
  }

  .carousel-btn {
    display: none;
  }
}

.passport-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 9999px;
  background: rgba(72, 103, 48, 0.08);
  color: var(--ag-primary-500);
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.passport-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ag-primary-500);
  animation: passport-pulse 2s infinite;
}

@keyframes passport-pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}

.passport-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 24px;
  border: 1px dashed var(--ag-border);
  border-radius: 16px;
  background: var(--ag-surface-container-low);
  text-align: center;
}

.passport-empty-icon {
  font-size: 40px;
  color: var(--ag-text-muted);
}

.passport-empty-text {
  font: 400 14px/1.6 var(--ag-font-body);
  color: var(--ag-text-secondary);
  max-width: 360px;
}

.tag-nft {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--ag-accent-600, #65625c);
  color: #fff;
  border: none;
}

.tag-nft:hover {
  background: var(--ag-accent-700, #48463f);
}

.passport-status-approved {
  background: rgba(101, 98, 92, 0.10);
  color: var(--ag-accent-600, #65625c);
}

.passport-status-approved .passport-status-dot {
  background: var(--ag-accent-600, #65625c);
}

.passport-card {
  border: 1px solid var(--ag-border);
  border-radius: 20px;
  overflow: hidden;
  background: var(--ag-surface-container-low);
}

.passport-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 20px 24px;
  border-bottom: 1px solid var(--ag-border);
  background: linear-gradient(135deg, rgba(101, 98, 92, 0.06), rgba(101, 98, 92, 0.02));
}

.passport-card-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.passport-token {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 9999px;
  background: var(--ag-accent-600, #65625c);
  color: #fff;
  font-weight: 700;
  font-size: 13px;
  font-family: var(--ag-font-body);
}

.passport-card-img {
  padding: 20px 24px 0;
}

.passport-card-img img {
  width: 100%;
  max-height: 360px;
  object-fit: cover;
  border-radius: 14px;
}

.passport-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px 20px;
  padding: 20px 24px;
}

.passport-fact {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.passport-fact span {
  font-size: 12px;
  color: var(--ag-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  font-weight: 600;
}

.passport-fact strong {
  font-size: 15px;
  color: var(--ag-text-primary);
  font-weight: 600;
  word-break: break-word;
}

.passport-desc {
  padding: 0 24px 20px;
  font: 400 14px/1.7 var(--ag-font-body);
  color: var(--ag-text-secondary);
  white-space: pre-line;
}

.passport-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 0 24px 24px;
}

.passport-lookup-btn {
  gap: 6px;
  text-transform: none;
}

.passport-open-icon {
  font-size: 14px;
  margin-left: 2px;
}

.passport-qr-wrap {
  display: flex;
  justify-content: center;
  padding: 8px 24px 24px;
}

.passport-qr-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border: 1px dashed var(--ag-border);
  border-radius: 16px;
  background: var(--ag-bg);
}

.passport-qr-canvas {
  width: 132px;
  height: 132px;
  border-radius: 8px;
  background: #fff;
}

.passport-qr-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ag-text-secondary);
}

.passport-care-history {
  border: 1px solid var(--ag-border);
  border-radius: 16px;
  padding: 24px;
  background: linear-gradient(180deg, rgba(245, 250, 245, 0.6), transparent);
}

.passport-care-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.passport-care-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--ag-text-secondary);
}

.passport-care-badge {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--ag-accent-700, #4c4a45);
  background: var(--ag-accent-100, #ece9e4);
}

.passport-care-img {
  width: 100%;
  max-width: 320px;
  height: auto;
  max-height: 220px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid var(--ag-border);
}
</style>
