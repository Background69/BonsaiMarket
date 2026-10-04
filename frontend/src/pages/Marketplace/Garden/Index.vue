<template>
  <MarketplaceLayout>
    <div class="garden-page">
      <section class="hero-section">
        <div class="hero-card">
          <div class="hero-content">
            <span class="hero-label">Bảng điều khiển Làm vườn</span>
            <h1 class="hero-title">Tình trạng Khu vườn của Bạn</h1>
            <p class="hero-desc">
              Chào mừng trở lại, {{ userName }}. Khu vườn của bạn hiện có
              <strong>{{ stats.totalPlants }}</strong> mẫu vật đang được theo dõi
              trên <strong>{{ zones.length }}</strong> khu vực.
            </p>
            <div class="hero-badges">
              <div class="badge badge-green">
                <span class="material-symbols-outlined badge-icon">eco</span>
                <span class="badge-text">Điểm vườn: {{ stats.healthScore }}/100</span>
              </div>
              <div class="badge" :class="careNeeded.length > 0 ? 'badge-terracotta' : 'badge-green'">
                <span class="material-symbols-outlined badge-icon">
                  {{ careNeeded.length > 0 ? 'priority_high' : 'check_circle' }}
                </span>
                <span class="badge-text">
                  {{ careNeeded.length > 0 ? `${careNeeded.length} cây cần chăm sóc` : 'Tất cả đều khỏe mạnh' }}
                </span>
              </div>
            </div>
          </div>
          <div class="hero-ring">
            <svg class="ring-svg" viewBox="0 0 100 100">
              <circle class="ring-bg" cx="50" cy="50" r="45" />
              <circle class="ring-progress" cx="50" cy="50" r="45"
                :style="{ stroke: ringColor }"
                :stroke-dashoffset="ringOffset" />
            </svg>
            <div class="ring-label">
              <span class="ring-grade" :style="{ color: ringColor }">{{ stats.grade }}</span>
              <span class="ring-tier">{{ stageDistributionLabel }}</span>
            </div>
          </div>
        </div>
      </section>

      <section v-if="careNeeded.length > 0" class="alert-section">
        <div class="alert-banner">
          <span class="material-symbols-outlined alert-icon">warning</span>
          <div class="alert-content">
            <strong class="alert-title">{{ careNeeded.length }} cây cần chú ý ngay</strong>
            <p class="alert-desc">Một số cây trong vườn có chỉ số hydration hoặc dinh dưỡng thấp. Hãy tưới nước hoặc bón phân kịp thời.</p>
          </div>
          <button class="alert-close" @click="dismissAlert">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </section>

      <div class="page-grid">
        <div class="main-col">
          <div class="section-header">
            <div>
              <h2 class="section-title">Khu vực & Mẫu vật</h2>
              <p class="section-subtitle">Bộ sưu tập cây của bạn theo từng khu vực trong vườn.</p>
            </div>
            <Link :href="route('agriverse.shop.products.index')" class="btn-primary">
              <span class="material-symbols-outlined">add</span>
              Thêm cây mới
            </Link>
          </div>

          <div class="zone-tabs">
            <button
              v-for="zone in zones"
              :key="zone.id"
              class="zone-tab"
              :class="{ 'zone-tab-active': activeZone === zone.id }"
              @click="activeZone = zone.id"
            >
              <span class="zone-tab-emoji">{{ zone.icon }}</span>
              <span class="zone-tab-name">{{ zone.name }}</span>
              <span class="zone-tab-count">{{ zone.plant_count }}</span>
            </button>
            <button
              class="zone-tab"
              :class="{ 'zone-tab-active': activeZone === null }"
              @click="activeZone = null"
            >
              <span class="zone-tab-emoji">🌱</span>
              <span class="zone-tab-name">Tất cả</span>
              <span class="zone-tab-count">{{ stats.totalPlants }}</span>
            </button>
          </div>

          <div v-if="filteredPlants.length === 0" class="empty-state">
            <span class="material-symbols-outlined empty-icon">yard</span>
            <h3 class="empty-title">Chưa có cây nào</h3>
            <p class="empty-desc">Khu vực này chưa có cây. Hãy mua cây giống từ cửa hàng để thêm vào vườn.</p>
            <Link :href="route('agriverse.shop.products.index')" class="btn-primary">
              Khám phá cây giống
            </Link>
          </div>

          <div v-else class="specimen-grid">
            <div v-for="(plant) in filteredPlants" :key="plant.id" class="specimen-card" :class="'stage-' + plant.stage">
              <div class="specimen-image-wrap">
                <img :src="plant.image_url || '/images/placeholder-plant.svg'" :alt="plant.name" class="specimen-image" />
                <div class="specimen-badge" :class="'badge-' + plant.health_status">
                  <span class="material-symbols-outlined">
                    {{ plant.health_status === 'healthy' ? 'check_circle' : 'warning' }}
                  </span>
                  <span>{{ stageLabels[plant.stage] || plant.stage }}</span>
                </div>
                <div class="specimen-actions" v-if="plant.health_status !== 'harvested'">
                  <button class="action-btn action-water" title="Tưới nước"
                    @click="waterPlant(plant)"
                    :disabled="plant.hydration_value >= 100 || loading[plant.id + '-water']">
                    <span v-if="loading[plant.id + '-water']" class="loading-spinner"></span>
                    <span v-else class="material-symbols-outlined">water_drop</span>
                  </button>
                  <button class="action-btn action-fertilize" title="Bón phân"
                    @click="fertilizePlant(plant)"
                    :disabled="plant.nutrient_value >= 100 || loading[plant.id + '-fertilize']">
                    <span v-if="loading[plant.id + '-fertilize']" class="loading-spinner"></span>
                    <span v-else class="material-symbols-outlined">spa</span>
                  </button>
                </div>
              </div>
              <div class="specimen-info">
                <h3 class="specimen-name">{{ plant.name }}</h3>
                <p class="specimen-species">{{ plant.species }}</p>
                <div class="specimen-meta">
                  <span class="meta-chip">
                    <span class="material-symbols-outlined">calendar_month</span>
                    {{ formatDate(plant.planted_at) }}
                  </span>
                  <span v-if="plant.last_watered_at" class="meta-chip">
                    <span class="material-symbols-outlined">water_drop</span>
                    {{ timeAgo(plant.last_watered_at) }}
                  </span>
                </div>
                <div class="progress-list">
                  <div class="progress-item">
                    <div class="progress-header">
                      <span>Độ ẩm</span>
                      <span class="progress-value" :class="{ 'progress-low': plant.hydration_value < 30 }">
                        {{ plant.hydration_value }}%
                      </span>
                    </div>
                    <div class="progress-bar">
                      <div class="progress-fill" :class="hydrationBarClass(plant.hydration_value)"
                        :style="{ transform: 'scaleX(' + (plant.hydration_value / 100) + ')' }"></div>
                    </div>
                  </div>
                  <div class="progress-item">
                    <div class="progress-header">
                      <span>Dinh dưỡng</span>
                      <span class="progress-value" :class="{ 'progress-low': plant.nutrient_value < 30 }">
                        {{ plant.nutrient_value }}%
                      </span>
                    </div>
                    <div class="progress-bar">
                      <div class="progress-fill" :class="nutrientBarClass(plant.nutrient_value)"
                        :style="{ transform: 'scaleX(' + (plant.nutrient_value / 100) + ')' }"></div>
                    </div>
                  </div>
                </div>
                <div class="specimen-stage-bar">
                  <div
                    v-for="s in stages"
                    :key="s.key"
                    class="stage-dot"
                    :class="{ 'stage-active': stageOrder.indexOf(s.key) <= stageOrder.indexOf(plant.stage), 'stage-current': s.key === plant.stage }"
                    :title="s.label"
                  >
                    <span class="material-symbols-outlined">{{ s.icon }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <section v-if="suggestions.length > 0" class="suggestions-section">
            <div class="section-header">
              <div>
                <h2 class="section-title">Gợi ý cho vườn của bạn</h2>
                <p class="section-subtitle">Những cây giống phù hợp để bổ sung vào bộ sưu tập.</p>
              </div>
              <Link :href="route('agriverse.shop.products.index')" class="section-link">Xem tất cả</Link>
            </div>
            <div class="suggestions-grid">
              <Link v-for="product in suggestions" :key="product.id"
                :href="route('agriverse.shop.products.show', { product: product.id })"
                class="suggestion-card"
              >
                <div class="suggestion-img-wrap">
                  <img :src="product.image || '/images/placeholder-plant.svg'" :alt="product.name" class="suggestion-img" />
                </div>
                <div class="suggestion-info">
                  <h4 class="suggestion-name">{{ product.name }}</h4>
                  <p class="suggestion-price">{{ formatPrice(product.price) }}</p>
                </div>
              </Link>
            </div>
          </section>
        </div>

        <aside class="sidebar-col">
          <div class="sidebar-sticky">
            <div class="sidebar-card">
              <div class="sidebar-card-header">
                <span class="material-symbols-outlined">auto_awesome</span>
                <h3 class="sidebar-card-title">Tổng quan vườn</h3>
              </div>
              <div class="stats-grid">
                <div class="stat-item">
                  <span class="stat-value green">{{ stats.avgHydration }}%</span>
                  <span class="stat-label">Độ ẩm TB</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value orange">{{ stats.avgNutrient }}%</span>
                  <span class="stat-label">Dinh dưỡng TB</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value blue">{{ stats.healthyCount }}/{{ stats.totalPlants }}</span>
                  <span class="stat-label">Khỏe mạnh</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value purple">{{ stats.careNeededCount }}</span>
                  <span class="stat-label">Cần chăm sóc</span>
                </div>
              </div>
            </div>

            <div class="sidebar-card">
              <div class="sidebar-card-header">
                <span class="material-symbols-outlined">equalizer</span>
                <h3 class="sidebar-card-title">Phân bố giai đoạn</h3>
              </div>
              <div class="stage-list">
                <div v-for="s in stages" :key="s.key" class="stage-item">
                  <span class="stage-item-icon">{{ s.icon }}</span>
                  <span class="stage-item-label">{{ s.label }}</span>
                  <span class="stage-item-count">{{ stageDistribution[s.key] || 0 }}</span>
                  <div class="stage-item-bar">
                    <div class="stage-item-fill" :style="{ transform: 'scaleX(' + (stagePercent(s.key) / 100) + ')' }"></div>
                  </div>
                </div>
              </div>
            </div>

            <div class="sidebar-card">
              <div class="sidebar-card-header">
                <span class="material-symbols-outlined">psychology</span>
                <h3 class="sidebar-card-title">AI Chăm sóc</h3>
              </div>
              <div class="ai-insights">
                <div v-for="(insight, i) in aiInsights" :key="i" class="insight-item">
                  <span class="material-symbols-outlined insight-icon" :class="insight.type">{{ insight.icon }}</span>
                  <div>
                    <p class="insight-title">{{ insight.title }}</p>
                    <p class="insight-desc">{{ insight.desc }}</p>
                  </div>
                </div>
              </div>
              <button class="btn-chat" @click="openAIChat">
                Trò chuyện với Botanist AI
                <span class="material-symbols-outlined">arrow_forward</span>
              </button>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Link, router } from '@inertiajs/vue3'
import { route } from 'ziggy-js'
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue'

const props = defineProps({
  garden: Object,
  zones: { type: Array, default: () => [] },
  plants: { type: Array, default: () => [] },
  stats: {
    type: Object,
    default: () => ({ totalPlants: 0, healthScore: 0, grade: 'D', avgHydration: 0, avgNutrient: 0, healthyCount: 0, careNeededCount: 0 }),
  },
  stageDistribution: { type: Object, default: () => ({}) },
  careNeeded: { type: Array, default: () => [] },
  suggestions: { type: Array, default: () => [] },
})

const activeZone = ref(null)
const showAlert = ref(true)
const loading = ref({})

const ringCircumference = 283
const stages = [
  { key: 'seedling', label: 'Cây con', icon: '🌱' },
  { key: 'growing', label: 'Đang lớn', icon: '🌿' },
  { key: 'mature', label: 'Trưởng thành', icon: '🌳' },
  { key: 'flowering', label: 'Ra hoa', icon: '🌸' },
  { key: 'fruiting', label: 'Kết trái', icon: '🍎' },
  { key: 'harvested', label: 'Thu hoạch', icon: '🎉' },
]
const stageOrder = stages.map(s => s.key)
const stageLabels = Object.fromEntries(stages.map(s => [s.key, s.label]))

const userName = computed(() => props.garden?.name?.replace('Vườn của ', '') || 'Bạn')
const filteredPlants = computed(() => {
  return props.plants.filter(p => activeZone.value === null || p.zone_id === activeZone.value)
})
const ringOffset = ref(ringCircumference)
const ringColor = computed(() => {
  const s = props.stats.healthScore
  if (s >= 85) return '#2e7d32'
  if (s >= 70) return '#43a047'
  if (s >= 50) return '#f9a825'
  if (s >= 30) return '#ef6c00'
  return '#c62828'
})
const stageDistributionLabel = computed(() => {
  const entries = Object.entries(props.stageDistribution)
  if (entries.length === 0) return 'Chưa có dữ liệu'
  const total = entries.reduce((a, [, c]) => a + c, 0)
  const top = entries.sort((a, b) => b[1] - a[1])[0]
  return `${stageLabels[top[0]] || top[0]}: ${Math.round(top[1] / total * 100)}%`
})
const aiInsights = computed(() => {
  const insights = []
  const thirstyCount = props.plants.filter(p => p.hydration_value < 30).length
  const hungryCount = props.plants.filter(p => p.nutrient_value < 30).length
  if (thirstyCount > 0) insights.push({
    type: 'warning', icon: 'water_drop',
    title: `${thirstyCount} cây thiếu nước`,
    desc: 'Cần tưới nước ngay để tránh héo úa.',
  })
  if (hungryCount > 0) insights.push({
    type: 'warning', icon: 'spa',
    title: `${hungryCount} cây thiếu dinh dưỡng`,
    desc: 'Bón phân bổ sung để cây phát triển tốt.',
  })
  if (props.stats.healthyCount === props.stats.totalPlants && props.stats.totalPlants > 0) {
    insights.push({
      type: 'success', icon: 'emoji_events',
      title: 'Vườn khỏe mạnh!',
      desc: 'Tất cả cây đều trong tình trạng tốt. Hãy duy trì chế độ chăm sóc hiện tại.',
    })
  }
  if (props.stats.totalPlants > 0 && insights.length === 0) {
    insights.push({
      type: 'info', icon: 'eco',
      title: 'Vườn ổn định',
      desc: 'Các chỉ số đều ở mức chấp nhận được. Kiểm tra định kỳ để đảm bảo cây luôn khỏe.',
    })
  }
  if (props.stats.totalPlants === 0) {
    insights.push({
      type: 'info', icon: 'yard',
      title: 'Bắt đầu trồng cây',
      desc: 'Mua cây giống từ cửa hàng và chúng sẽ tự động được thêm vào vườn khi đơn hàng được giao.',
    })
  }
  return insights
})

function hydrationBarClass(v) {
  if (v < 30) return 'progress-fill-error'
  if (v < 50) return 'progress-fill-warning'
  return 'progress-fill-primary'
}
function nutrientBarClass(v) {
  if (v < 30) return 'progress-fill-error'
  if (v < 50) return 'progress-fill-warning'
  return 'progress-fill-secondary'
}
function stagePercent(key) {
  const total = props.stats.totalPlants
  if (!total) return 0
  return ((props.stageDistribution[key] || 0) / total) * 100
}
function formatDate(d) {
  if (!d) return ''
  const date = new Date(d)
  return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}
function timeAgo(d) {
  if (!d) return ''
  const now = new Date()
  const date = new Date(d)
  const diff = Math.floor((now - date) / (1000 * 60 * 60))
  if (diff < 1) return 'Vừa xong'
  if (diff < 24) return `${diff} giờ trước`
  const days = Math.floor(diff / 24)
  return `${days} ngày trước`
}
function formatPrice(p) {
  if (!p) return ''
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(p)
}
function waterPlant(plant) {
  loading.value[plant.id + '-water'] = true
  router.post(route('agriverse.shop.garden.plants.water', { plant: plant.id }), {}, {
    preserveScroll: true,
    onFinish: () => { loading.value[plant.id + '-water'] = false },
  })
}
function fertilizePlant(plant) {
  loading.value[plant.id + '-fertilize'] = true
  router.post(route('agriverse.shop.garden.plants.fertilize', { plant: plant.id }), {}, {
    preserveScroll: true,
    onFinish: () => { loading.value[plant.id + '-fertilize'] = false },
  })
}
function dismissAlert() {
  showAlert.value = false
}
function openAIChat() {
  if (window.__agriverse_open_chat) {
    window.__agriverse_open_chat('botanist')
  }
}

onMounted(() => {
  setTimeout(() => {
    ringOffset.value = ringCircumference - (ringCircumference * props.stats.healthScore / 100)
  }, 300)
})
</script>

<style scoped>
.garden-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 64px;
  padding-top: 120px;
  padding-bottom: 80px;
}
@media (max-width: 768px) {
  .garden-page { padding: 80px 24px 40px; }
}

.hero-section { margin-bottom: 32px; }
.hero-card {
  background: var(--ag-bg-card, #ffffff);
  border-radius: var(--ag-radius-xl);
  padding: 40px;
  border: 1px solid color-mix(in srgb, var(--ag-border) 30%, transparent);
  display: flex;
  flex-direction: column;
  gap: 48px;
  align-items: center;
  box-shadow: var(--ag-shadow-sm, 0 1px 3px rgba(0,0,0,0.04));
}
@media (min-width: 768px) {
  .hero-card { flex-direction: row; justify-content: space-between; }
}
.hero-content { max-width: 520px; }
.hero-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--ag-primary);
  text-transform: uppercase;
  letter-spacing: 0.2em;
  display: block;
  margin-bottom: 16px;
}
.hero-title {
  font-size: 48px;
  line-height: 1.1;
  color: var(--ag-on-surface);
  margin-bottom: 24px;
}
@media (max-width: 768px) { .hero-title { font-size: 36px; } }
.hero-desc {
  font-size: 18px;
  line-height: 1.6;
  color: var(--ag-on-surface-variant);
  margin-bottom: 32px;
}
.hero-desc strong { color: var(--ag-on-surface); }
.hero-badges { display: flex; flex-wrap: wrap; gap: 16px; }
.badge {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  border-radius: var(--ag-radius-full);
  font-size: 14px;
  font-weight: 600;
}
.badge-green {
  background: color-mix(in srgb, var(--ag-primary) 5%, transparent);
  border: 1px solid color-mix(in srgb, var(--ag-primary) 10%, transparent);
  color: var(--ag-primary);
}
.badge-terracotta {
  background: color-mix(in srgb, var(--ag-secondary) 5%, transparent);
  border: 1px solid color-mix(in srgb, var(--ag-secondary) 10%, transparent);
  color: var(--ag-secondary);
}
.badge-icon { font-size: 20px; font-variation-settings: 'FILL' 1; }
.badge-text { font-size: 14px; font-weight: 600; }

.hero-ring {
  position: relative;
  width: 224px;
  height: 224px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ring-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}
.ring-bg {
  fill: transparent;
  stroke: var(--ag-surface-container);
  stroke-width: 6;
}
.ring-progress {
  fill: transparent;
  stroke-width: 10;
  stroke-linecap: round;
  stroke-dasharray: 283;
  transition: stroke-dashoffset 1s ease-out, stroke 0.3s;
}
.ring-label {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.ring-grade {
  font-size: 48px;
  font-style: italic;
  font-weight: 500;
  line-height: 1;
}
.ring-tier {
  font-size: 12px;
  font-weight: 600;
  color: var(--ag-on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-top: 4px;
}

.alert-section { margin-bottom: 32px; }
.alert-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  background: color-mix(in srgb, var(--ag-secondary) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--ag-secondary) 15%, transparent);
  border-radius: var(--ag-radius-lg);
}
.alert-icon { color: var(--ag-secondary); font-variation-settings: 'FILL' 1; }
.alert-content { flex: 1; }
.alert-title {
  font-size: 14px;
  color: var(--ag-on-surface);
  display: block;
  margin-bottom: 4px;
}
.alert-desc {
  font-size: 12px;
  color: var(--ag-on-surface-variant);
  margin: 0;
}
.alert-close {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--ag-on-surface-variant);
  padding: 4px;
}
.alert-close:hover { color: var(--ag-on-surface); }

.page-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 40px;
}
@media (min-width: 1024px) {
  .page-grid { grid-template-columns: 8fr 4fr; }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}
.section-title {
  font-size: 30px;
  color: var(--ag-on-surface);
  margin: 0;
}
.section-subtitle {
  font-size: 14px;
  color: var(--ag-on-surface-variant);
  margin-top: 4px;
}
.section-link {
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-primary);
  text-decoration: none;
}
.section-link:hover { text-decoration: underline; }

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 32px;
  background: var(--ag-primary);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  box-shadow: var(--ag-shadow-sm, 0 1px 3px rgba(0,0,0,0.06));
}
.btn-primary:hover { background: color-mix(in srgb, var(--ag-primary) 90%, black); }

.zone-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
  margin-bottom: 32px;
  scrollbar-width: none;
}
.zone-tabs::-webkit-scrollbar { display: none; }
.zone-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 1px solid var(--ag-border);
  border-radius: var(--ag-radius-full);
  background: var(--ag-bg-card, #ffffff);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  font-size: 14px;
}
.zone-tab:hover { border-color: var(--ag-primary); }
.zone-tab-active {
  background: var(--ag-primary);
  border-color: var(--ag-primary);
  color: white;
}
.zone-tab-emoji { font-size: 18px; }
.zone-tab-name { font-weight: 600; }
.zone-tab-count {
  font-size: 11px;
  font-weight: 700;
  background: color-mix(in srgb, var(--ag-border) 20%, transparent);
  padding: 2px 8px;
  border-radius: var(--ag-radius-full);
}
.zone-tab-active .zone-tab-count {
  background: rgba(255,255,255,0.2);
  color: white;
}

.empty-state {
  text-align: center;
  padding: 80px 24px;
}
.empty-icon {
  font-size: 64px;
  color: var(--ag-border);
  margin-bottom: 24px;
}
.empty-title {
  font-size: 24px;
  color: var(--ag-on-surface);
  margin-bottom: 12px;
}
.empty-desc {
  font-size: 14px;
  color: var(--ag-on-surface-variant);
  margin-bottom: 24px;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
}

.specimen-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}
@media (min-width: 768px) {
  .specimen-grid { grid-template-columns: 1fr 1fr; }
}
.specimen-card {
  background: var(--ag-bg-card, #ffffff);
  border-radius: var(--ag-radius-xl);
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--ag-border) 30%, transparent);
  transition: box-shadow 0.3s, transform 0.2s;
}
.specimen-card:hover { box-shadow: var(--ag-shadow-md, 0 4px 12px rgba(0,0,0,0.06)); }
.specimen-image-wrap {
  height: 200px;
  position: relative;
  overflow: hidden;
  background: var(--ag-surface-container);
}
.specimen-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.7s;
  display: block;
}
.specimen-card:hover .specimen-image { transform: scale(1.05); }
.specimen-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--ag-radius-full);
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  box-shadow: var(--ag-shadow-sm);
  backdrop-filter: blur(4px);
}
.badge-healthy {
  background: color-mix(in srgb, var(--ag-primary) 90%, white);
  color: white;
  border: none;
}
.badge-stressed {
  background: color-mix(in srgb, var(--ag-secondary) 90%, white);
  color: white;
  border: none;
}
.badge-diseased {
  background: color-mix(in srgb, var(--ag-error, #c62828) 90%, white);
  color: white;
  border: none;
}
.specimen-actions {
  position: absolute;
  bottom: 12px;
  right: 12px;
  display: flex;
  gap: 8px;
}
.action-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  box-shadow: var(--ag-shadow-sm, 0 1px 3px rgba(0,0,0,0.12));
  backdrop-filter: blur(4px);
}
.action-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.action-water {
  background: rgba(255,255,255,0.95);
  color: #1565c0;
}
.action-water:hover:not(:disabled) { background: #1565c0; color: white; }
.action-fertilize {
  background: rgba(255,255,255,0.95);
  color: #2e7d32;
}
.action-fertilize:hover:not(:disabled) { background: #2e7d32; color: white; }
.action-btn .material-symbols-outlined { font-size: 20px; font-variation-settings: 'FILL' 1; }

.specimen-info { padding: 20px; }
.specimen-name {
  font-size: 20px;
  color: var(--ag-on-surface);
  margin: 0 0 4px;
}
.specimen-species {
  font-size: 12px;
  color: var(--ag-on-surface-variant);
  font-style: italic;
  margin: 0 0 12px;
}
.specimen-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--ag-on-surface-variant);
  background: var(--ag-surface-container);
  padding: 4px 10px;
  border-radius: var(--ag-radius-full);
}
.meta-chip .material-symbols-outlined { font-size: 14px; }

.progress-list { display: flex; flex-direction: column; gap: 16px; }
.progress-header {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 6px;
}
.progress-header span:first-child { color: var(--ag-on-surface-variant); }
.progress-value { color: var(--ag-primary); }
.progress-low { color: var(--ag-error, #c62828) !important; font-weight: 700; }
.progress-bar {
  height: 6px;
  width: 100%;
  background: var(--ag-surface-container);
  border-radius: var(--ag-radius-full);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: var(--ag-radius-full);
  transform-origin: left;
  transition: transform 0.5s ease;
}
.progress-fill-primary { background: var(--ag-primary); }
.progress-fill-secondary { background: var(--ag-secondary); }
.progress-fill-warning { background: #f9a825; }
.progress-fill-error { background: var(--ag-error, #c62828); }

.specimen-stage-bar {
  display: flex;
  gap: 4px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--ag-border);
}
.stage-dot {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px 0;
  border-radius: 6px;
  background: var(--ag-surface-container);
  transition: all 0.3s;
}
.stage-dot .material-symbols-outlined { font-size: 16px; opacity: 0.3; }
.stage-active { background: color-mix(in srgb, var(--ag-primary) 8%, transparent); }
.stage-active .material-symbols-outlined { opacity: 0.5; color: var(--ag-primary); }
.stage-current {
  background: color-mix(in srgb, var(--ag-primary) 15%, transparent);
  box-shadow: inset 0 0 0 2px var(--ag-primary);
}
.stage-current .material-symbols-outlined { opacity: 1; color: var(--ag-primary); }

.suggestions-section { margin-top: 64px; }
.suggestions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
@media (min-width: 640px) {
  .suggestions-grid { grid-template-columns: repeat(3, 1fr); }
}
.suggestion-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: var(--ag-radius-lg);
  border: 1px solid var(--ag-border);
  background: var(--ag-bg-card, #ffffff);
  text-decoration: none;
  transition: all 0.2s;
}
.suggestion-card:hover {
  border-color: var(--ag-primary);
  box-shadow: var(--ag-shadow-sm);
}
.suggestion-img-wrap {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--ag-surface-container);
}
.suggestion-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.suggestion-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}
.suggestion-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-on-surface);
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.suggestion-price {
  font-size: 14px;
  font-weight: 700;
  color: var(--ag-primary);
  margin: 0;
}

.sidebar-col { position: relative; }
.sidebar-sticky {
  position: sticky;
  top: 112px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.sidebar-card {
  background: var(--ag-bg-card, #ffffff);
  border-radius: var(--ag-radius-xl);
  padding: 24px;
  border: 1px solid color-mix(in srgb, var(--ag-border) 30%, transparent);
  box-shadow: var(--ag-shadow-sm, 0 1px 3px rgba(0,0,0,0.04));
}
.sidebar-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.sidebar-card-header .material-symbols-outlined {
  color: var(--ag-primary);
  font-size: 24px;
  font-variation-settings: 'FILL' 1;
}
.sidebar-card-title {
  font-size: 20px;
  font-style: italic;
  color: var(--ag-on-surface);
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.stat-item {
  text-align: center;
  padding: 12px;
  background: var(--ag-bg);
  border-radius: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  display: block;
}
.stat-value.green { color: var(--ag-primary); }
.stat-value.orange { color: #ef6c00; }
.stat-value.blue { color: #1565c0; }
.stat-value.purple { color: #6a1b9a; }
.stat-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--ag-on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-top: 4px;
  display: block;
}

.stage-list { display: flex; flex-direction: column; gap: 12px; }
.stage-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.stage-item-icon { font-size: 16px; width: 24px; text-align: center; }
.stage-item-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--ag-on-surface);
  min-width: 80px;
}
.stage-item-count {
  font-size: 12px;
  font-weight: 700;
  color: var(--ag-primary);
  min-width: 24px;
  text-align: right;
}
.stage-item-bar {
  flex: 1;
  height: 6px;
  background: var(--ag-surface-container);
  border-radius: var(--ag-radius-full);
  overflow: hidden;
}
.stage-item-fill {
  height: 100%;
  background: var(--ag-primary);
  border-radius: var(--ag-radius-full);
  transform-origin: left;
  transition: transform 0.5s ease;
}

.ai-insights { display: flex; flex-direction: column; gap: 16px; }
.insight-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: var(--ag-bg);
  border-radius: 8px;
}
.insight-icon {
  font-size: 20px;
  flex-shrink: 0;
  margin-top: 2px;
}
.insight-icon.warning { color: var(--ag-secondary); }
.insight-icon.success { color: var(--ag-primary); }
.insight-icon.info { color: #1565c0; }
.insight-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--ag-on-surface);
  margin: 0 0 2px;
}
.insight-desc {
  font-size: 11px;
  color: var(--ag-on-surface-variant);
  margin: 0;
  line-height: 1.4;
}

.btn-chat {
  width: 100%;
  margin-top: 24px;
  padding: 14px;
  background: var(--ag-primary);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
  box-shadow: var(--ag-shadow-sm, 0 1px 3px rgba(0,0,0,0.06));
}
.btn-chat:hover { background: color-mix(in srgb, var(--ag-primary) 90%, black); }
.btn-chat .material-symbols-outlined {
  font-size: 16px;
  transition: transform 0.3s;
}
.btn-chat:hover .material-symbols-outlined { transform: translateX(4px); }

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--ag-border);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
