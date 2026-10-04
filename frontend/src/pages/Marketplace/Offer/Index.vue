<template>
  <MarketplaceLayout>
    <section class="max-w-[1320px] mx-auto px-5 py-8">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-lg sm:text-xl font-bold text-[var(--ag-text-primary)] tracking-tight">Đề xuất giá (Offer)</h1>
          <p class="text-sm text-[var(--ag-text-secondary)] mt-1">Chọn sản phẩm → đề xuất giá → người bán trả giá → admin duyệt → bạn xác nhận đơn</p>
        </div>
        <span class="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full"
          :class="connected ? 'text-[#15803d] bg-[#16a34a]/10' : 'text-[var(--ag-text-secondary)] bg-[var(--ag-bg)]'">
          <span class="w-2 h-2 rounded-full animate-pulse" :class="connected ? 'bg-[#16a34a]' : 'bg-[var(--ag-neutral-300)]'"></span>
          {{ connected ? 'Đang kết nối' : 'Đang kết nối lại…' }}
        </span>
      </div>

      <!-- Admin: cần duyệt -->
      <div v-if="isAdmin && pendingApprovals.length" class="bg-[#fef3c7] border border-[#f59e0b]/30 rounded-2xl p-4 mb-6">
        <div class="flex items-center gap-2 mb-3">
          <span class="material-symbols-outlined text-[#b45309] text-base">fact_check</span>
          <h2 class="text-sm font-bold text-[#92400e]">Đề xuất chờ duyệt ({{ pendingApprovals.length }})</h2>
        </div>
        <div class="space-y-3">
          <div v-for="o in pendingApprovals" :key="o.offerId" class="bg-white rounded-xl p-4">
              <div class="flex flex-wrap items-center gap-4">
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-bold text-[var(--ag-text-primary)]">{{ o.productName }}</span>
                    <span class="text-[10px] px-2 py-0.5 rounded-full font-semibold text-[#92400e] bg-[#f59e0b]/15">Chờ duyệt</span>
                  </div>
                  <div class="text-xs text-[var(--ag-text-secondary)] mt-0.5">
                    <b class="text-[var(--ag-text-primary)]">{{ o.buyerName || ('#' + o.buyerId) }}</b> (mua)
                    → <b class="text-[var(--ag-text-primary)]">{{ o.sellerName || ('#' + o.sellerId) }}</b> (bán) · thương lượng
                    <b class="text-[var(--ag-danger)]">{{ formatPrice(o.price) }}₫</b>
                  </div>
                  <div v-if="o.quantity" class="text-xs text-[var(--ag-text-secondary)] mt-0.5">Số lượng: x{{ o.quantity }}</div>
                </div>
                <div class="flex items-center gap-2">
                  <button @click="adminDecision(o, 'APPROVE')" class="h-9 px-4 rounded-xl bg-[#16a34a] text-white text-xs font-semibold hover:bg-[#15803d] transition-all">
                    Duyệt & tạo đơn
                  </button>
                  <button @click="adminDecision(o, 'DECLINE')" class="h-9 px-4 rounded-xl bg-[var(--ag-danger)]/10 text-[var(--ag-danger)] text-xs font-semibold hover:bg-[var(--ag-danger)]/20 transition-all">
                    Từ chối
                  </button>
                </div>
              </div>
            </div>
        </div>
      </div>

      <!-- Đơn chờ xác nhận của tôi -->
      <div v-if="myPendingOrders.length" class="mb-6 space-y-3">
        <div class="flex items-center gap-2">
          <span class="material-symbols-outlined text-[var(--ag-primary-500)] text-lg">assignment</span>
          <h2 class="text-sm font-bold text-[var(--ag-text-primary)]">Đơn hàng chờ bạn xác nhận ({{ myPendingOrders.length }})</h2>
        </div>
        <div v-for="(ord, idx) in myPendingOrders" :key="ord.offer_id || idx"
          class="bg-white rounded-2xl border border-[var(--ag-primary-500)]/30 p-4 flex flex-wrap items-center gap-4">
          <div class="min-w-0 flex-1">
            <div class="text-sm font-bold text-[var(--ag-text-primary)]">Đơn #{{ ord.id }} · {{ offerName(ord.offer_id) }}</div>
            <div class="text-xs text-[var(--ag-text-secondary)] mt-0.5">
              {{ formatPrice(ord.total_amount) }}₫ · SL {{ ord.quantity }}
              <span v-if="ord.shipping_address">· {{ ord.shipping_address }}</span>
            </div>
            <div v-if="orderRemaining(ord) != null" class="mt-1.5 flex items-center gap-1">
              <span class="text-[10px] font-semibold text-[var(--ag-danger)] mr-1">Còn lại</span>
              <span v-for="(u, i) in orderCountdown(ord)" :key="i"
                class="px-1.5 py-0.5 rounded bg-[var(--ag-danger)] text-white text-[10px] font-bold min-w-[24px] text-center">{{ u }}</span>
            </div>
          </div>
          <button @click="openConfirmOrder(ord)" class="h-10 px-4 rounded-xl bg-[var(--ag-primary-500)] text-white text-xs font-semibold hover:bg-[var(--ag-primary-600)] transition-all">
            Xác nhận đơn hàng
          </button>
          <button @click="cancelOrder(ord)" class="h-10 px-4 rounded-xl bg-[var(--ag-danger)]/10 text-[var(--ag-danger)] text-xs font-semibold hover:bg-[var(--ag-danger)]/20 transition-all">
            Hủy
          </button>
        </div>
      </div>

      <!-- Tabs -->
      <div class="flex gap-1 bg-white rounded-2xl border border-[var(--ag-border)] p-1 mb-6 w-fit">
        <button v-for="t in tabs" :key="t.key" @click="activeTab = t.key"
          class="px-4 h-9 rounded-xl text-xs font-semibold transition-all"
          :class="activeTab === t.key ? 'bg-[var(--ag-primary-500)]/10 text-[var(--ag-primary-500)]' : 'text-[var(--ag-text-secondary)] hover:text-[var(--ag-text-primary)]'" >
          {{ t.label }}
        </button>
      </div>

      <!-- Tab: Sản phẩm nhận đề xuất -->
      <template v-if="activeTab === 'products'">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
          <h2 class="text-sm font-bold text-[var(--ag-text-primary)]">Sản phẩm có thể đề xuất giá ({{ filteredProducts.length }})</h2>
          <div class="relative w-full sm:w-72">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-base text-[var(--ag-text-secondary)]">search</span>
            <input v-model="search" type="text" placeholder="Tìm sản phẩm hoặc người bán…"
              class="w-full h-10 pl-10 pr-4 rounded-xl border border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 transition-all" />
          </div>
        </div>
        <div v-if="filteredProducts.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          <div v-for="p in filteredProducts" :key="p.id"
            class="bg-white rounded-2xl border border-[var(--ag-border)] overflow-hidden hover:border-[var(--ag-primary-500)]/25 hover:shadow-sm transition-all duration-300 flex flex-col">
            <div class="relative aspect-[4/3] bg-[var(--ag-bg)] overflow-hidden">
              <img v-if="p.image" :src="p.image" :alt="p.name" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full flex items-center justify-center">
                <span class="material-symbols-outlined text-5xl text-[var(--ag-neutral-300)]">local_florist</span>
              </div>
            </div>
            <div class="p-4 flex flex-col flex-1">
              <h3 class="text-sm font-bold text-[var(--ag-text-primary)] leading-snug line-clamp-2">{{ p.name }}</h3>
              <div class="text-xs text-[var(--ag-text-secondary)] mt-1">Người bán: {{ p.seller_name }}</div>
              <div class="flex items-baseline gap-1 mt-2">
                <span class="text-base font-extrabold text-[var(--ag-danger)]">{{ formatPrice(p.price) }}₫</span>
                <span class="text-[10px] text-[var(--ag-text-secondary)]">/ hiện tại</span>
              </div>
              <button @click="openPropose(p)"
                class="mt-3 h-10 rounded-xl bg-[var(--ag-primary-500)] text-white text-sm font-bold hover:bg-[var(--ag-primary-600)] active:scale-95 transition-all flex items-center justify-center gap-1.5">
                <span class="material-symbols-outlined text-sm">handshake</span> Đề xuất giá
              </button>
            </div>
          </div>
        </div>
        <div v-else class="bg-white rounded-2xl border border-[var(--ag-border)] p-16 text-center">
          <div class="w-20 h-20 rounded-2xl bg-[var(--ag-bg)] flex items-center justify-center mx-auto mb-4">
            <span class="material-symbols-outlined text-4xl text-[var(--ag-neutral-300)]">{{ search ? 'search_off' : 'handshake' }}</span>
          </div>
          <p class="text-sm text-[var(--ag-text-secondary)]">{{ search ? 'Không tìm thấy sản phẩm nào phù hợp.' : 'Chưa có sản phẩm nào để đề xuất giá.' }}</p>
        </div>
      </template>

      <!-- Tab: Đề xuất tôi đã gửi -->
      <template v-if="activeTab === 'outgoing'">
        <h2 class="text-sm font-bold text-[var(--ag-text-primary)] mb-3">Đề xuất tôi đã gửi</h2>
        <div v-if="offers.out.length" class="space-y-3">
          <div v-for="o in offers.out" :key="o.offerId" class="bg-white rounded-2xl border border-[var(--ag-border)] p-4">
            <div class="flex flex-wrap items-center gap-3">
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2">
                  <span class="text-sm font-bold text-[var(--ag-text-primary)]">{{ o.productName }}</span>
                  <span class="text-[10px] px-2 py-0.5 rounded-full font-semibold" :class="statusBadge(o.status)">{{ statusLabel(o.status) }}</span>
                </div>
                <div class="text-xs text-[var(--ag-text-secondary)] mt-1">Người bán {{ o.sellerName || ('#' + o.sellerId) }} · {{ formatPrice(o.price) }}₫ x{{ o.quantity || 1 }}</div>
                <div v-if="!offerExpired(o)" class="mt-1.5 flex items-center gap-1.5 flex-wrap">
                  <span class="material-symbols-outlined text-sm" :class="offerUrgent(o) ? 'text-[var(--ag-danger)]' : 'text-[var(--ag-text-secondary)]'">hourglass</span>
                  <span class="text-[10px] font-semibold" :class="offerUrgent(o) ? 'text-[var(--ag-danger)]' : 'text-[var(--ag-text-secondary)]'">
                    {{ offerCountdown(o) }} · kết thúc {{ offerDeadlineText(o) }}
                  </span>
                </div>
                <div v-else class="mt-1.5 text-[11px] font-semibold text-[var(--ag-danger)]">Phiên đề xuất đã hết hạn</div>
              </div>
              <div class="flex items-center gap-2">
                <button v-if="o.status === 'COUNTERED' && !offerExpired(o)" @click="acceptOffer(o, 'seller_id')"
                  class="h-9 px-4 rounded-xl bg-[var(--ag-primary-500)] text-white text-xs font-semibold hover:bg-[var(--ag-primary-600)] transition-all">
                  Đồng ý giá
                </button>
                <button v-if="o.status === 'APPROVED' && !orderExists(o.offerId)" @click="ensureOrder(o)"
                  class="h-9 px-4 rounded-xl bg-[#16a34a] text-white text-xs font-semibold hover:bg-[#15803d] transition-all">
                  Tạo đơn hàng
                </button>
              </div>
            </div>
            <div class="mt-3">
              <OfferStepper :status="o.status" />
            </div>
          </div>
        </div>
        <p v-else class="text-sm text-[var(--ag-text-secondary)]">Bạn chưa gửi đề xuất nào.</p>
      </template>

      <!-- Tab: Đề xuất đến (người bán) -->
      <template v-if="activeTab === 'incoming'">
        <h2 class="text-sm font-bold text-[var(--ag-text-primary)] mb-3">Đề xuất người mua gửi đến</h2>
        <div v-if="offers.in.length" class="space-y-3">
          <div v-for="o in offers.in" :key="o.offerId" class="bg-white rounded-2xl border border-[var(--ag-border)] p-4">
<div class="flex flex-wrap items-center gap-3">
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2">
                  <span class="text-sm font-bold text-[var(--ag-text-primary)]">{{ o.productName }}</span>
                  <span class="text-[10px] px-2 py-0.5 rounded-full font-semibold" :class="statusBadge(o.status)">{{ statusLabel(o.status) }}</span>
                </div>
                <div class="text-xs text-[var(--ag-text-secondary)] mt-1">
                  Người mua {{ o.buyerName || ('#' + o.buyerId) }} đề nghị <b class="text-[var(--ag-danger)]">{{ formatPrice(o.price) }}₫</b>
                  <span v-if="o.quantity" class="ml-1">· x{{ o.quantity }}</span>
                  <span v-if="o.message" class="block mt-1 italic text-[var(--ag-text-secondary)]">“{{ o.message }}”</span>
                </div>
                <div v-if="!offerExpired(o)" class="mt-1.5 flex items-center gap-1.5 flex-wrap">
                  <span class="material-symbols-outlined text-sm" :class="offerUrgent(o) ? 'text-[var(--ag-danger)]' : 'text-[var(--ag-text-secondary)]'">hourglass</span>
                  <span class="text-[10px] font-semibold" :class="offerUrgent(o) ? 'text-[var(--ag-danger)]' : 'text-[var(--ag-text-secondary)]'">
                    {{ offerCountdown(o) }} · kết thúc {{ offerDeadlineText(o) }}
                  </span>
                </div>
                <div v-else class="mt-1.5 text-[11px] font-semibold text-[var(--ag-danger)]">Phiên đề xuất đã hết hạn</div>
              </div>
              <div v-if="isOfferActive(o) && !offerExpired(o)" class="flex items-center gap-2 flex-wrap justify-end">
                <button v-if="o.status === 'PROPOSED'" @click="openCounter(o)" class="h-9 px-4 rounded-xl bg-[var(--ag-primary-500)]/10 text-[var(--ag-primary-500)] text-xs font-semibold hover:bg-[var(--ag-primary-500)]/20 transition-all">Trả giá</button>
                <button v-if="o.status === 'PROPOSED'" @click="acceptOffer(o, 'buyer_id')" class="h-9 px-4 rounded-xl bg-[#16a34a] text-white text-xs font-semibold hover:bg-[#15803d] transition-all">Đồng ý</button>
                <button @click="rejectOffer(o)" class="h-9 px-4 rounded-xl bg-[var(--ag-danger)]/10 text-[var(--ag-danger)] text-xs font-semibold hover:bg-[var(--ag-danger)]/20 transition-all">
                  {{ o.status === 'PROPOSED' ? 'Từ chối' : 'Kết thúc phiên' }}
                </button>
              </div>
              <div v-else-if="o.status === 'COUNTERED' && !offerExpired(o)" class="text-xs font-semibold text-[var(--ag-primary-500)]">Đã trả giá, chờ người mua quyết định</div>
              <div v-else-if="o.status === 'ACCEPTED'" class="text-xs font-semibold text-[#b45309]">Đã đồng ý — chờ admin duyệt</div>
              <div v-else-if="o.status === 'APPROVED'" class="text-xs font-semibold text-[#15803d]">Đã được admin duyệt ✓</div>
              <div v-else-if="o.status === 'DECLINED'" class="text-xs font-semibold text-[var(--ag-danger)]">Admin từ chối duyệt</div>
            </div>
            <div class="mt-3">
              <OfferStepper :status="o.status" />
            </div>
          </div>
        </div>
        <p v-else class="text-sm text-[var(--ag-text-secondary)]">Chưa có đề xuất nào được gửi đến bạn.</p>
      </template>

      <!-- Propose modal -->
      <div v-if="proposeTarget" class="fixed inset-0 z-50 flex items-center justify-center bg-[var(--ag-text-primary)]/30 backdrop-blur-sm p-4" @click.self="proposeTarget = null">
        <div class="bg-white rounded-3xl p-6 w-full max-w-md">
          <h3 class="text-base font-bold text-[var(--ag-text-primary)] mb-1">Đề xuất giá</h3>
          <div class="flex items-center gap-3 mb-4">
            <img v-if="proposeTarget.image" :src="proposeTarget.image" :alt="proposeTarget.name" class="w-14 h-14 rounded-xl object-cover bg-[var(--ag-bg)]" />
            <div class="min-w-0 text-sm">
              <div class="font-bold text-[var(--ag-text-primary)] line-clamp-2">{{ proposeTarget.name }}</div>
              <div class="text-xs text-[var(--ag-text-secondary)]">Giá đang bán: <b class="text-[var(--ag-danger)]">{{ formatPrice(proposeTarget.price) }}₫</b></div>
            </div>
          </div>
          <form @submit.prevent="submitPropose" class="space-y-3">
            <div class="grid grid-cols-1 gap-3">
              <div>
                <label class="text-xs font-semibold text-[var(--ag-text-secondary)]">Giá đề nghị (₫)</label>
                <input v-model.number="proposePrice" required type="number" :min="minProposePrice"
                  :placeholder="'Nhập giá đề nghị'"
                  class="mt-1 w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40" />
                <div class="flex flex-wrap gap-1.5 mt-2">
                  <button v-for="chip in proposeChips" :key="chip.label" type="button" @click="proposePrice = chip.value"
                    class="px-3 h-7 rounded-lg border border-[var(--ag-border)] text-[11px] font-semibold text-[var(--ag-text-secondary)] hover:border-[var(--ag-primary-500)]/40 hover:text-[var(--ag-primary-500)] transition-all">
                    {{ chip.label }} · {{ formatPrice(chip.value) }}₫
                  </button>
                </div>
                <p v-if="proposePrice && proposePrice > proposeTarget.price" class="text-[11px] text-[var(--ag-danger)] mt-1.5">Giá đề nghị cao hơn giá bán hiện tại — người bán có thể không đồng ý.</p>
              </div>
              <div>
                <label class="text-xs font-semibold text-[var(--ag-text-secondary)]">Số lượng</label>
                <div class="flex items-center gap-2 mt-1">
                  <button type="button" @click="proposeQty = Math.max(1, proposeQty - 1)" class="w-11 h-11 rounded-xl border-2 border-[var(--ag-border)] text-[var(--ag-text-secondary)] hover:border-[var(--ag-primary-500)]/40 transition-all">−</button>
                  <input v-model.number="proposeQty" type="number" min="1"
                    class="w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm text-center outline-none focus:border-[var(--ag-primary-500)]/40" />
                  <button type="button" @click="proposeQty = Math.min(999, proposeQty + 1)" class="w-11 h-11 rounded-xl border-2 border-[var(--ag-border)] text-[var(--ag-text-secondary)] hover:border-[var(--ag-primary-500)]/40 transition-all">+</button>
                </div>
              </div>
            </div>
            <div>
              <label class="text-xs font-semibold text-[var(--ag-text-secondary)]">Lời nhắn (tuỳ chọn)</label>
              <textarea v-model="proposeMsg" rows="2" placeholder="VD: mình lấy nhiều thì giảm thêm được không ạ?"
                class="mt-1 w-full px-4 py-3 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none resize-none focus:border-[var(--ag-primary-500)]/40" />
            </div>
            <div v-if="proposePrice" class="flex items-center justify-between bg-[var(--ag-bg)] rounded-xl px-4 py-3 text-sm">
              <span class="text-[var(--ag-text-secondary)]">Tạm tính</span>
              <b class="text-[var(--ag-danger)]">{{ formatPrice(Number(proposePrice) * (proposeQty || 1)) }}₫</b>
            </div>
            <p class="text-[11px] text-[var(--ag-text-secondary)] flex items-center gap-1.5">
              <span class="material-symbols-outlined text-sm">hourglass</span>
              Phiên đề xuất có thời hạn <b>24 giờ</b>. Người bán sẽ phản hồi trong thời gian đó.
            </p>
            <button type="submit" :disabled="!connected"
              class="w-full h-11 rounded-2xl bg-[var(--ag-primary-500)] text-white text-sm font-semibold hover:bg-[var(--ag-primary-600)] transition-all disabled:opacity-50 disabled:cursor-not-allowed">
              Gửi đề xuất giá
            </button>
          </form>
        </div>
      </div>

      <!-- Counter modal -->
      <div v-if="counterTarget" class="fixed inset-0 z-50 flex items-center justify-center bg-[var(--ag-text-primary)]/30 backdrop-blur-sm p-4" @click.self="counterTarget = null">
        <div class="bg-white rounded-3xl p-6 w-full max-w-sm">
          <h3 class="text-base font-bold text-[var(--ag-text-primary)] mb-1">Trả giá lại</h3>
          <p class="text-xs text-[var(--ag-text-secondary)] mb-3">{{ counterTarget.productName }} · người mua đề nghị <b class="text-[var(--ag-danger)]">{{ formatPrice(counterTarget.price) }}₫</b></p>
          <input v-model.number="counterPrice" type="number" :min="minCounterPrice" placeholder="Giá bạn muốn chốt"
            class="w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 mb-2" />
          <div class="flex flex-wrap gap-1.5 mb-3">
            <button v-for="chip in counterChips" :key="chip.label" type="button" @click="counterPrice = chip.value"
              class="px-3 h-7 rounded-lg border border-[var(--ag-border)] text-[11px] font-semibold text-[var(--ag-text-secondary)] hover:border-[var(--ag-primary-500)]/40 hover:text-[var(--ag-primary-500)] transition-all">
              {{ chip.label }} · {{ formatPrice(chip.value) }}₫
            </button>
          </div>
          <textarea v-model="counterMsg" rows="2" placeholder="Lời nhắn (tuỳ chọn)"
            class="w-full px-4 py-3 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none resize-none focus:border-[var(--ag-primary-500)]/40 mb-4" />
          <div class="flex gap-3">
            <button @click="submitCounter" class="flex-1 h-11 rounded-2xl bg-[var(--ag-primary-500)] text-white text-sm font-semibold hover:bg-[var(--ag-primary-600)] transition-all">Gửi trả giá</button>
            <button @click="counterTarget = null" class="flex-1 h-11 rounded-2xl border-2 border-[var(--ag-border)] text-[var(--ag-text-secondary)] text-sm font-semibold hover:bg-[var(--ag-bg)] transition-all">Hủy</button>
          </div>
        </div>
      </div>

      <!-- Confirm order modal -->
      <div v-if="confirmTarget" class="fixed inset-0 z-50 flex items-center justify-center bg-[var(--ag-text-primary)]/30 backdrop-blur-sm p-4" @click.self="confirmTarget = null">
        <div class="bg-white rounded-3xl p-6 w-full max-w-md">
          <div class="flex items-center gap-2 mb-4">
            <span class="material-symbols-outlined text-[var(--ag-primary-500)]">assignment</span>
            <h3 class="text-base font-bold text-[var(--ag-text-primary)]">Xác nhận đơn hàng #{{ confirmTarget.id }}</h3>
          </div>
          <div class="bg-[var(--ag-bg)] rounded-xl p-4 text-sm space-y-1 mb-4">
            <div class="flex justify-between"><span class="text-[var(--ag-text-secondary)]">Sản phẩm</span><b class="text-[var(--ag-text-primary)]">{{ offerName(confirmTarget.offer_id) }}</b></div>
            <div class="flex justify-between"><span class="text-[var(--ag-text-secondary)]">Giá</span><b class="text-[var(--ag-danger)]">{{ formatPrice(confirmTarget.total_amount) }}₫</b></div>
            <div class="flex justify-between"><span class="text-[var(--ag-text-secondary)]">Số lượng</span><b class="text-[var(--ag-text-primary)]">x{{ confirmTarget.quantity }}</b></div>
          </div>
          <form @submit.prevent="submitConfirmOrder" class="space-y-3">
            <div>
              <label class="text-xs font-semibold text-[var(--ag-text-secondary)]">Địa chỉ giao hàng</label>
              <textarea v-model="confirmAddress" rows="2" placeholder="Nhập địa chỉ giao hàng"
                class="mt-1 w-full px-4 py-3 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none resize-none focus:border-[var(--ag-primary-500)]/40" />
            </div>
            <div>
              <label class="text-xs font-semibold text-[var(--ag-text-secondary)]">Số điện thoại</label>
              <input v-model="confirmPhone" placeholder="Nhập số điện thoại nhận hàng"
                class="mt-1 w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40" />
            </div>
            <button type="submit" :disabled="confirmSubmitting"
              class="w-full h-11 rounded-2xl bg-[var(--ag-primary-500)] text-white text-sm font-semibold hover:bg-[var(--ag-primary-600)] transition-all disabled:opacity-50 disabled:cursor-not-allowed">
              Xác nhận đơn hàng
            </button>
            <p class="text-[10px] text-[var(--ag-text-secondary)] text-center">Nếu không xác nhận trong 12h, đơn sẽ tự động bị hủy.</p>
          </form>
        </div>
      </div>

      <!-- event toasts -->
      <div v-if="connected && events.length" class="fixed bottom-5 right-5 z-50 space-y-2 w-80">
        <div v-for="(ev, i) in events.slice(0, 3)" :key="ev.at + '-' + i"
          class="bg-white border border-[var(--ag-border)] rounded-xl px-4 py-3 text-xs shadow-lg flex items-start gap-2.5 animate-[slideIn_0.2s_ease-out]">
          <span class="material-symbols-outlined text-base mt-px"
            :class="toastIcon(ev.kind).cls">{{ toastIcon(ev.kind).icon }}</span>
          <span class="text-[var(--ag-text-primary)] flex-1">{{ ev.text }}</span>
        </div>
      </div>
    </section>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import OfferStepper from './OfferStepper.vue';
import { useMarketSocket } from '@agriverse/Composables/useMarketSocket';
import { useAuth } from '@agriverse/Composables/useAuth';
import api from '@agriverse/services/api';

const props = defineProps({
  pickableProducts: { type: Array, default: () => [] },
});

const { user, isAdmin } = useAuth();
const userId = computed(() => user.value?.id ?? '');

const { connected, offers, pendingApprovals, events, connect, disconnect, send, setUserId } = useMarketSocket();

const activeTab = ref('products');
const tabs = computed(() => {
  const t = [{ key: 'products', label: 'Sản phẩm đề xuất' }, { key: 'outgoing', label: 'Tôi đã gửi' }];
  if (user.value?.role === 'seller') t.push({ key: 'incoming', label: `Đề xuất đến (${offers.in.length})` });
  return t;
});

const search = ref('');
const filteredProducts = computed(() => {
  const q = search.value.toLowerCase().trim();
  if (!q) return props.pickableProducts;
  return props.pickableProducts.filter(p => (p.name || '').toLowerCase().includes(q) || (p.seller_name || '').toLowerCase().includes(q));
});

// ==== propose ====
const proposeTarget = ref(null);
const proposePrice = ref(null);
const proposeQty = ref(1);
const proposeMsg = ref('');

// Gợi ý giá: 90% / 80% / 70% giá hiện tại (làm tròn đẹp)
const proposeChips = computed(() => {
  const base = Number(proposeTarget.value?.price || 0);
  return [0.9, 0.8, 0.7].map(r => {
    const raw = Math.round(base * r / 1000) * 1000;
    return { label: `−${(1 - r) * 100}%`, value: raw };
  });
});
// Giá đề nghị tối thiểu: 50% giá hiện tại (tránh spam)
const minProposePrice = computed(() => Math.max(1, Math.round(Number(proposeTarget.value?.price || 0) * 0.5 / 1000) * 1000));

function openPropose(p) { proposeTarget.value = p; proposePrice.value = null; proposeQty.value = 1; proposeMsg.value = ''; }

function submitPropose() {
  if (!proposeTarget.value || !proposePrice.value) return;
  if (proposePrice.value < minProposePrice.value) {
    window.alert(`Giá đề nghị tối thiểu là ${formatPrice(minProposePrice.value)}₫.`);
    return;
  }
  const p = proposeTarget.value;
  send({
    eventType: 'OFFER', action: 'PROPOSE', from: String(userId.value), fromName: user.value?.name || '',
    to: String(p.seller_id),
    payload: JSON.stringify({ productId: p.id, productName: p.name, productImage: p.image,
      price: Number(proposePrice.value), quantity: Number(proposeQty.value) || 1, message: proposeMsg.value,
      sellerId: String(p.seller_id), sellerName: p.seller_name }),
  });
  proposeTarget.value = null;
  proposePrice.value = null;
}

// ==== counter / reject / accept ====
const counterTarget = ref(null);
const counterPrice = ref(null);
const counterMsg = ref('');

// Người bán trả giá: từ 60% → 100% đề nghị của người mua
const counterChips = computed(() => {
  const base = Number(counterTarget.value?.price || 0);
  return [1, 1.1, 1.2, 1.3].map(r => {
    const raw = Math.round(base * r / 1000) * 1000;
    return { label: r === 1 ? 'Đúng đề nghị' : `+${(r - 1) * 100}%`, value: raw };
  });
});
const minCounterPrice = computed(() => Math.max(1, Math.round(Number(counterTarget.value?.price || 0) * 0.6 / 1000) * 1000));

function openCounter(o) { counterTarget.value = o; counterPrice.value = o.price; counterMsg.value = ''; }
function submitCounter() {
  if (!counterTarget.value || !counterPrice.value) return;
  if (counterPrice.value < minCounterPrice.value) {
    window.alert(`Giá trả tối thiểu là ${formatPrice(minCounterPrice.value)}₫.`);
    return;
  }
  const o = counterTarget.value;
  send({ eventType: 'OFFER', action: 'COUNTER', from: String(userId.value), to: String(o.buyerId),
    payload: JSON.stringify({ offerId: o.offerId, price: Number(counterPrice.value), message: counterMsg.value }) });
  counterTarget.value = null;
}
function rejectOffer(o) {
  if (!window.confirm(o.status === 'ACCEPTED'
    ? 'Đồng ý bỏ phiên đang chờ admin duyệt này chứ?'
    : 'Từ chối đề xuất này của người mua?')) return;
  send({ eventType: 'OFFER', action: 'REJECT', from: String(userId.value), to: String(o.buyerId),
    payload: JSON.stringify({ offerId: o.offerId }) });
}

// ==== phiên đề xuất: thời hạn (offerDeadline) + helper trạng thái hoạt động ====
const OFFER_DURATION = 24 * 3600 * 1000; // 24h mặc định
const isOfferActive = (o) => ['PROPOSED', 'COUNTERED', 'ACCEPTED'].includes(o.status);
const offerDeadline = (o) => {
  if (o.offerDeadline) return new Date(o.offerDeadline).getTime();
  if (o.createdAt) return Number(o.createdAt) + OFFER_DURATION;
  return Date.now() + OFFER_DURATION;
};
const offerExpired = (o) => offerDeadline(o) <= now.value;
const offerCountdown = (o) => {
  const ms = offerDeadline(o) - now.value;
  if (ms <= 0) return 'hết hạn';
  const s = Math.floor(ms / 1000);
  const h = String(Math.floor(s / 3600)).padStart(2, '0');
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0');
  const sec = String(s % 60).padStart(2, '0');
  return `Còn ${h}:${m}:${sec}`;
};
const offerDeadlineText = (o) => {
  const d = new Date(offerDeadline(o));
  const p = (x) => String(x).padStart(2, '0');
  return `${p(d.getHours())}:${p(d.getMinutes())} ${p(d.getDate())}/${p(d.getMonth() + 1)}`;
};
const offerUrgent = (o) => offerDeadline(o) - now.value <= 3600 * 1000;
function acceptOffer(o, otherRoleKey) {
  const to = otherRoleKey === 'seller_id' ? o.sellerId : o.buyerId;
  send({ eventType: 'OFFER', action: 'ACCEPT', from: String(userId.value), to: String(to),
    payload: JSON.stringify({ offerId: o.offerId }) });
}

// ==== admin duyệt ====
async function adminDecision(o, decision) {
  send({ eventType: 'OFFER', action: decision, from: String(userId.value), to: '',
    payload: JSON.stringify({ offerId: o.offerId, productId: o.productId, price: o.price,
      sellerId: o.sellerId, buyerId: o.buyerId }) });
  if (decision === 'APPROVE') await ensureOrder(o);
}

// ==== tạo đơn từ offer APPROVED ====
const pendingOrders = ref([]);

function offerName(offerId) {
  const o = offers.out.find(x => x.offerId === offerId) || offers.in.find(x => x.offerId === offerId);
  return o?.productName || ('Offer ' + offerId);
}

async function ensureOrder(offer) {
  try {
    const { data } = await api.post(`/agriverse/api/offers/${offer.offerId}/initialize-order`, {
      product_id: offer.productId, product_name: offer.productName, price: offer.price,
      quantity: offer.quantity || 1, seller_id: offer.sellerId, product_image: offer.productImage,
    });
    if (data.order) upsertOrder(data.order);
    return data.order;
  } catch (e) { /* ignore */ }
}

function upsertOrder(order) {
  const idx = pendingOrders.value.findIndex(o => o.offer_id === order.offer_id);
  if (idx >= 0) pendingOrders.value[idx] = { ...pendingOrders.value[idx], ...order };
  else pendingOrders.value.push(order);
}

function orderExists(offerId) {
  return pendingOrders.value.some(o => o.offer_id === offerId);
}

const myPendingOrders = computed(() =>
  pendingOrders.value.filter(o => o.status === 'pending_confirmation')
);

// ==== confirm order ====
const confirmTarget = ref(null);
const confirmAddress = ref('');
const confirmPhone = ref('');
const confirmSubmitting = ref(false);

function openConfirmOrder(ord) {
  confirmTarget.value = ord;
  confirmAddress.value = ord.shipping_address || '';
  confirmPhone.value = ord.metadata?.shipping_phone || '';
}

async function submitConfirmOrder() {
  if (!confirmTarget.value) return;
  confirmSubmitting.value = true;
  try {
    const { data } = await api.post(`/agriverse/api/orders/${confirmTarget.value.id}/confirm-offer`, {
      shipping_address: confirmAddress.value, shipping_phone: confirmPhone.value,
    });
    if (data.ok) upsertOrder(data.order);
    confirmTarget.value = null;
  } catch (e) {
    const msg = e.response?.data?.message || 'Không thể xác nhận đơn.';
    window.alert(msg);
  } finally {
    confirmSubmitting.value = false;
  }
}

async function cancelOrder(ord) {
  if (!window.confirm('Hủy đơn hàng này?')) return;
  try {
    const { data } = await api.post(`/agriverse/api/orders/${ord.id}/cancel-offer`);
    if (data.ok) upsertOrder(data.order);
  } catch (e) { window.alert(e.response?.data?.message || 'Hủy đơn thất bại.'); }
}

// ==== countdown 12h ====
const now = ref(Date.now());
let ticker = null;
const orderRemaining = (ord) => {
  if (!ord.confirm_deadline) return null;
  const d = new Date(ord.confirm_deadline).getTime() - now.value;
  return d > 0 ? d : null;
};
const orderCountdown = (ord) => {
  const ms = orderRemaining(ord);
  if (ms == null) return [];
  const s = Math.floor(ms / 1000);
  const h = String(Math.floor(s / 3600)).padStart(2, '0');
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0');
  const sec = String(s % 60).padStart(2, '0');
  return [h, m, sec];
};

// ==== life cycle ====
onMounted(async () => {
  setUserId(user.value?.id);
  connect();
  ticker = setInterval(() => { now.value = Date.now(); }, 1000);

  // Pre-select product từ query ?product=ID
  const qp = new URLSearchParams(window.location.search).get('product');
  if (qp && props.pickableProducts.length) {
    const hit = props.pickableProducts.find(p => String(p.id) === qp);
    if (hit) openPropose(hit);
  }

  // Khôi phục đơn chờ xác nhận cho các offer APPROVED của tôi
  for (const o of offers.out) {
    if (o.status === 'APPROVED' && String(o.buyerId) === String(userId.value)) {
      await ensureOrder(o);
    }
  }
});
onBeforeUnmount(() => { clearInterval(ticker); disconnect(); });

// Hook vào sự kiện APPROVED: tạo đơn ngay khi nhận (buyer)
watch(() => offers.out.map(o => o.status), async () => {
  for (const o of offers.out) {
    if (o.status === 'APPROVED' && String(o.buyerId) === String(userId.value)) {
      await ensureOrder(o);
    }
  }
});

const statusLabel = (s) => ({
  PROPOSED: 'Đang chờ người bán', COUNTERED: 'Đã trả giá', ACCEPTED: 'Đã đồng ý',
  APPROVED: 'Admin đã duyệt', DECLINED: 'Bị từ chối', REJECTED: 'Bị từ chối',
}[s] || s);

const statusBadge = (s) => ({
  PROPOSED: 'text-[var(--ag-primary-500)] bg-[var(--ag-primary-500)]/10',
  COUNTERED: 'text-[#b45309] bg-[#f59e0b]/10',
  ACCEPTED: 'text-[#b45309] bg-[#f59e0b]/10',
  APPROVED: 'text-[#15803d] bg-[#16a34a]/10',
  DECLINED: 'text-[var(--ag-danger)] bg-[var(--ag-danger)]/10',
  REJECTED: 'text-[var(--ag-danger)] bg-[var(--ag-danger)]/10',
}[s] || 'text-[var(--ag-text-secondary)] bg-[var(--ag-bg)]');

const formatPrice = (n) => Number(n || 0).toLocaleString('vi-VN');

const toastIcon = (kind) => ({
  offer: { icon: 'handshake', cls: 'text-[var(--ag-primary-500)]' },
  flash_deal: { icon: 'local_fire_department', cls: 'text-[var(--ag-danger)]' },
  chat: { icon: 'chat', cls: 'text-[#2563eb]' },
}[kind] || { icon: 'notifications', cls: 'text-[var(--ag-text-secondary)]' });
</script>

<style>
@keyframes slideIn {
  from { opacity: 0; transform: translateX(12px); }
  to { opacity: 1; transform: translateX(0); }
}
</style>