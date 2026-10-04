<template>
  <MarketplaceLayout>
    <main class="diagnostic">
      <section class="diagnostic__header">
        <h1 class="diagnostic__title">Phòng Chẩn Đoán Sức Khỏe Cây Trồng</h1>
        <p class="diagnostic__subtitle">Sử dụng phân tích mẫu vật tiên tiến bằng AI để phát hiện thiếu hụt dinh dưỡng, sâu bệnh và stress môi trường. Làm vườn chính xác bắt đầu từ dữ liệu chính xác.</p>
      </section>

      <div class="diagnostic__grid">
        <div class="diagnostic__left">
          <div class="diagnostic__upload" @click="handleUpload">
            <input id="specimen-upload" type="file" class="diagnostic__upload-input" @change="onFileChange" />
            <label for="specimen-upload" class="diagnostic__upload-label">
              <div class="diagnostic__upload-icon">
                <span class="material-symbols-outlined">photo_camera</span>
              </div>
              <h3 class="diagnostic__upload-title">Tải Ảnh Mẫu Vật</h3>
              <p class="diagnostic__upload-desc">Đảm bảo chụp độ phân giải cao, từ trên xuống phần tán lá bị ảnh hưởng để xử lý thần kinh chính xác.</p>
            </label>
            <div class="diagnostic__corner diagnostic__corner--tl"></div>
            <div class="diagnostic__corner diagnostic__corner--tr"></div>
            <div class="diagnostic__corner diagnostic__corner--bl"></div>
            <div class="diagnostic__corner diagnostic__corner--br"></div>
          </div>

          <div class="diagnostic__checklist">
            <div class="diagnostic__checklist-header">
              <h2 class="diagnostic__checklist-title">Danh Sách Triệu Chứng</h2>
              <span class="diagnostic__checklist-badge">Ghi Đè Thủ Công</span>
            </div>
            <div class="diagnostic__checklist-grid">
              <label v-for="symptom in symptomNames" :key="symptomLabel(symptom)" class="diagnostic__symptom-item">
                <input type="checkbox" class="diagnostic__checkbox" v-model="selectedSymptoms" :value="symptom" />
                <span class="diagnostic__symptom-label">{{ symptomLabel(symptom) }}</span>
              </label>
            </div>
            <div class="diagnostic__analyze">
              <input type="text" class="diagnostic__plant-input" v-model="plantName" placeholder="Tên cây (không bắt buộc)..." />
              <button class="diagnostic__analyze-btn" @click="analyze" :disabled="analyzing">
                <span v-if="analyzing" class="diagnostic__spinner"></span>
                <span class="material-symbols-outlined" v-else>psychology</span>
                {{ analyzing ? 'Đang chẩn đoán...' : 'Chẩn Đoán' }}
              </button>
            </div>
            <p v-if="error" class="diagnostic__error">{{ error }}</p>
          </div>
        </div>

        <div class="diagnostic__right">
          <div class="diagnostic__status">
            <div class="diagnostic__status-inner">
              <div class="diagnostic__status-indicator">
                <span class="diagnostic__pulse"></span>
                <span class="diagnostic__status-text">{{ statusText }}</span>
              </div>
              <h2 class="diagnostic__status-title">Trạng Thái Hệ Thống</h2>
              <div class="diagnostic__metrics">
                <div class="diagnostic__metric">
                  <div class="diagnostic__metric-head">
                    <span class="diagnostic__metric-label">Sức Sống Tổng Thể</span>
                    <span class="diagnostic__metric-value diagnostic__metric-value--primary">{{ result?.diagnosis ? vigorPercent : '—' }}</span>
                  </div>
                  <div class="diagnostic__bar">
                    <div class="diagnostic__bar-fill diagnostic__bar-fill--primary" :style="{ transform: 'scaleX(' + (overallVigor / 100) + ')' }"></div>
                  </div>
                </div>
                <div class="diagnostic__metric">
                  <div class="diagnostic__metric-head">
                    <span class="diagnostic__metric-label">Độ Tin Cậy</span>
                    <span class="diagnostic__metric-value diagnostic__metric-value--secondary">{{ result?.diagnosis ? Math.round(result.diagnosis.confidence * 100) + '%' : '—' }}</span>
                  </div>
                  <div class="diagnostic__bar">
                    <div class="diagnostic__bar-fill diagnostic__bar-fill--secondary" :style="{ transform: 'scaleX(' + (result?.diagnosis?.confidence || 0) + ')' }"></div>
                  </div>
                </div>
              </div>
              <div class="diagnostic__minicards">
                <div class="diagnostic__minicard">
                  <span class="material-symbols-outlined diagnostic__minicard-icon diagnostic__minicard-icon--primary">diagnosis</span>
                  <p class="diagnostic__minicard-label">Mức Nghiêm Trọng</p>
                  <p class="diagnostic__minicard-value">{{ result?.diagnosis ? severityLabel(result.diagnosis.severity) : '—' }}</p>
                </div>
                <div class="diagnostic__minicard">
                  <span class="material-symbols-outlined diagnostic__minicard-icon diagnostic__minicard-icon--secondary">psychology_alt</span>
                  <p class="diagnostic__minicard-label">Triệu Chứng Đã Chọn</p>
                  <p class="diagnostic__minicard-value">{{ selectedSymptoms.length ? selectedSymptoms.length + ' triệu chứng' : '—' }}</p>
                </div>
              </div>
            </div>
            <div class="diagnostic__status-bg"></div>
          </div>

          <div class="diagnostic__report" ref="reportRef">
            <template v-if="!result">
              <div class="diagnostic__report-header">
                <div class="diagnostic__report-icon">
                  <span class="material-symbols-outlined">psychology</span>
                </div>
                <div>
                  <h3 class="diagnostic__report-title">Báo Cáo Chẩn Đoán</h3>
                  <p class="diagnostic__report-subtitle">Chọn triệu chứng và bấm "Chẩn Đoán" để có kết quả.</p>
                </div>
              </div>
            </template>
            <template v-else-if="result.matched">
              <div class="diagnostic__report-header">
                <div class="diagnostic__report-icon">
                  <span class="material-symbols-outlined">psychology</span>
                </div>
                <div>
                  <h3 class="diagnostic__report-title">Báo Cáo: {{ result.diagnosis.code }}</h3>
                  <p class="diagnostic__report-subtitle">Phát Hiện: {{ result.diagnosis.disease_name }}</p>
                </div>
              </div>
              <div class="diagnostic__report-body">
                <div class="diagnostic__report-tags">
                  <span class="diagnostic__report-tag diagnostic__report-tag--{{ result.diagnosis.severity }}">{{ severityLabel(result.diagnosis.severity) }}</span>
                  <span v-if="result.diagnosis.plant_name" class="diagnostic__report-tag">{{ result.diagnosis.plant_name }}</span>
                  <span class="diagnostic__report-tag">Tin cậy {{ Math.round(result.diagnosis.confidence * 100) }}%</span>
                </div>
                <div class="diagnostic__report-section">
                  <h4 class="diagnostic__report-section-title">Nguyên nhân có thể</h4>
                  <ul class="diagnostic__treatment-list">
                    <li v-for="c in result.diagnosis.causes" :key="c" class="diagnostic__treatment-item">
                      <span class="material-symbols-outlined diagnostic__treatment-icon">warning</span>
                      <span>{{ c }}</span>
                    </li>
                  </ul>
                </div>
                <div class="diagnostic__report-section">
                  <h4 class="diagnostic__report-section-title">Cách xử lý</h4>
                  <ul class="diagnostic__treatment-list">
                    <li v-for="t in result.diagnosis.treatments" :key="t" class="diagnostic__treatment-item">
                      <span class="material-symbols-outlined diagnostic__treatment-icon">check_circle</span>
                      <span>{{ t }}</span>
                    </li>
                  </ul>
                </div>
                <div class="diagnostic__report-section" v-if="result.diagnosis.care">
                  <h4 class="diagnostic__report-section-title">Lưu ý chăm sóc</h4>
                  <p class="diagnostic__report-desc">{{ result.diagnosis.care }}</p>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="diagnostic__report-header">
                <div class="diagnostic__report-icon">
                  <span class="material-symbols-outlined">help</span>
                </div>
                <div>
                  <h3 class="diagnostic__report-title">Chưa xác định</h3>
                  <p class="diagnostic__report-subtitle">{{ result.message }}</p>
                </div>
              </div>
              <div class="diagnostic__report-body">
                <p class="diagnostic__report-desc">Hãy chọn thêm triệu chứng hoặc liên hệ đặt lịch tư vấn chuyên gia để được hỗ trợ chi tiết hơn.</p>
              </div>
            </template>
          </div>
        </div>
      </div>

      <section class="diagnostic__gallery">
        <h2 class="diagnostic__gallery-title">Sản Phẩm Phù Hợp</h2>
        <p v-if="!result?.products?.length" class="diagnostic__gallery-empty">
          Chọn triệu chứng và chẩn đoán để xem gợi ý cây cảnh phù hợp.
        </p>
        <div v-else class="diagnostic__gallery-grid">
          <Link v-for="p in result.products" :key="p.id" :href="route('agriverse.shop.products.show', p.slug)" class="diagnostic__gallery-card">
            <div class="diagnostic__gallery-image">
              <img :src="p.image" :alt="p.name" />
              <span class="diagnostic__gallery-badge diagnostic__gallery-badge--healthy">Gợi ý</span>
            </div>
            <div class="diagnostic__gallery-info">
              <h4 class="diagnostic__gallery-name">{{ p.name }}</h4>
              <p class="diagnostic__gallery-desc">{{ new Intl.NumberFormat('vi-VN').format(p.price) }}₫</p>
            </div>
          </Link>
        </div>
      </section>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { Link } from '@inertiajs/vue3'
import { route } from 'ziggy-js'
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue'

const props = defineProps({
  symptoms: { type: Array, default: () => [] },
})

const selectedSymptoms = ref([])
const plantName = ref('')
const reportRef = ref(null)
const analyzing = ref(false)
const result = ref(null)
const error = ref('')

const statusText = ref('Sẵn sàng phân tích')

const symptomNames = computed(() => {
  const list = props.symptoms.length ? props.symptoms : [
    { name: 'Vàng Lá', description: 'Lá chuyển vàng do thiếu dinh dưỡng hoặc tưới quá nhiều', category: 'Lá' },
    { name: 'Rụng Lá', description: 'Lá rụng sớm do sốc nhiệt hoặc thay đổi môi trường', category: 'Lá' },
    { name: 'Đầu Lá Nâu', description: 'Đầu lá khô nâu do độ ẩm thấp hoặc tưới không đều', category: 'Lá' },
    { name: 'Chậm Phát Triển', description: 'Cây phát triển chậm do thiếu ánh sáng hoặc dinh dưỡng', category: 'Tăng trưởng' },
    { name: 'Lá Đốm', description: 'Đốm trên lá do nấm hoặc vi khuẩn', category: 'Bệnh' },
    { name: 'Mốc Trắng', description: 'Lớp mốc trắng trên lá hoặc thân do nấm', category: 'Bệnh' },
  ]
  return list
})

const symptomLabel = (item) => (typeof item === 'string' ? item : item.name)
const symptomDescription = (item) => (typeof item === 'string' ? '' : item.description)

function selectedNames() {
  return selectedSymptoms.value.map(symptomLabel)
}

async function analyze() {
  error.value = ''
  if (!selectedSymptoms.value.length) {
    error.value = 'Vui lòng chọn ít nhất một triệu chứng để chẩn đoán.'
    return
  }
  analyzing.value = true
  statusText.value = 'Đang phân tích triệu chứng...'
  try {
    const { data } = await window.axios.post(route('agriverse.shop.diagnostic.analyze'), {
      symptoms: selectedNames(),
      plant_name: plantName.value || null,
    })
    result.value = data
    nextTick(() => {
      if (reportRef.value) {
        reportRef.value.scrollIntoView({ behavior: 'smooth', block: 'center' })
        reportRef.value.classList.add('diagnostic__report--highlight')
        setTimeout(() => reportRef.value.classList.remove('diagnostic__report--highlight'), 1200)
      }
    })
    statusText.value = 'Hoàn tất phân tích'
  } catch (e) {
    error.value = e?.response?.data?.message || 'Đã có lỗi khi chẩn đoán. Vui lòng thử lại.'
    statusText.value = 'Đã có lỗi xảy ra'
  } finally {
    analyzing.value = false
  }
}

const overallVigor = computed(() => {
  const d = result.value?.diagnosis
  if (!d) return 0
  const map = { low: 82, medium: 64, high: 40, critical: 20 }
  return map[d.severity] ?? 60
})
const vigorPercent = computed(() => overallVigor.value + '%')

const chlorophyll = computed(() => {
  const d = result.value?.diagnosis
  if (!d) return 0
  return Math.round((d.confidence || 0) * 60)
})
const chlorophyllPercent = computed(() => chlorophyll.value + '%')

function severityLabel(sev) {
  return ({
    low: 'Nhẹ',
    medium: 'Trung bình',
    high: 'Nghiêm trọng',
    critical: 'Nguy kịch',
  }[sev]) || 'Không xác định'
}
</script>

<style scoped>
.diagnostic {
  font-family: var(--ag-font-body);
  max-width: 1280px;
  margin: 0 auto;
  padding: 48px 64px;
  color: var(--ag-on-surface);
}

.diagnostic__header {
  margin-bottom: 64px;
}

.diagnostic__title {
  font-family: var(--ag-font-display);
  font-size: 48px;
  font-weight: 500;
  line-height: 56px;
  letter-spacing: -0.02em;
  color: var(--ag-primary);
  margin: 0 0 16px;
}

.diagnostic__subtitle {
  font-size: 18px;
  line-height: 28px;
  color: var(--ag-on-surface-variant);
  max-width: 576px;
  margin: 0;
}

.diagnostic__grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

@media (min-width: 1024px) {
  .diagnostic__grid {
    grid-template-columns: 7fr 5fr;
  }
}

.diagnostic__left,
.diagnostic__right {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.diagnostic__upload {
  position: relative;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.5s;
  overflow: hidden;
  height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-image: url("data:image/svg+xml,%3csvg width='100%25' height='100%25' xmlns='http://www.w3.org/2000/svg'%3e%3crect width='100%25' height='100%25' fill='none' stroke='%23486730' stroke-width='3' stroke-dasharray='12%2c 12' stroke-dashoffset='0' stroke-linecap='square'/%3e%3c/svg%3e");
}

.diagnostic__upload:hover {
  background: var(--ag-surface-container-low);
}

.diagnostic__upload-input {
  display: none;
}

.diagnostic__upload-label {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.diagnostic__upload-icon {
  margin-bottom: 24px;
  padding: 24px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--ag-primary) 10%, transparent);
  color: var(--ag-primary);
  transition: transform 0.5s;
}

.diagnostic__upload:hover .diagnostic__upload-icon {
  transform: scale(1.1);
}

.diagnostic__upload-icon .material-symbols-outlined {
  font-size: 48px;
}

.diagnostic__upload-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  line-height: 32px;
  color: var(--ag-on-surface);
  margin: 0 0 8px;
}

.diagnostic__upload-desc {
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-on-surface-variant);
  max-width: 288px;
  padding: 0 16px;
  margin: 0;
}

.diagnostic__corner {
  position: absolute;
  width: 32px;
  height: 32px;
}

.diagnostic__corner--tl {
  top: 16px;
  left: 16px;
  border-top: 2px solid color-mix(in srgb, var(--ag-primary) 40%, transparent);
  border-left: 2px solid color-mix(in srgb, var(--ag-primary) 40%, transparent);
}

.diagnostic__corner--tr {
  top: 16px;
  right: 16px;
  border-top: 2px solid color-mix(in srgb, var(--ag-primary) 40%, transparent);
  border-right: 2px solid color-mix(in srgb, var(--ag-primary) 40%, transparent);
}

.diagnostic__corner--bl {
  bottom: 16px;
  left: 16px;
  border-bottom: 2px solid color-mix(in srgb, var(--ag-primary) 40%, transparent);
  border-left: 2px solid color-mix(in srgb, var(--ag-primary) 40%, transparent);
}

.diagnostic__corner--br {
  bottom: 16px;
  right: 16px;
  border-bottom: 2px solid color-mix(in srgb, var(--ag-primary) 40%, transparent);
  border-right: 2px solid color-mix(in srgb, var(--ag-primary) 40%, transparent);
}

.diagnostic__checklist {
  background: #fff;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  border: 1px solid color-mix(in srgb, var(--ag-outline-variant) 30%, transparent);
}

.diagnostic__checklist-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

.diagnostic__checklist-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  line-height: 32px;
  color: var(--ag-primary);
  margin: 0;
}

.diagnostic__checklist-badge {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--ag-tertiary);
  background: var(--ag-tertiary-fixed);
  padding: 4px 12px;
  border-radius: 9999px;
  white-space: nowrap;
}

.diagnostic__checklist-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (min-width: 768px) {
  .diagnostic__checklist-grid {
    grid-template-columns: 1fr 1fr 1fr;
  }
}

.diagnostic__symptom-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  background: var(--ag-surface-container-low);
  border: 1px solid transparent;
  transition: all 0.2s;
  cursor: pointer;
}

.diagnostic__symptom-item:hover {
  border-color: color-mix(in srgb, var(--ag-primary) 30%, transparent);
}

.diagnostic__symptom-item:hover .diagnostic__symptom-label {
  color: var(--ag-primary);
}

.diagnostic__checkbox {
  border-radius: 4px;
  color: var(--ag-primary);
  margin-right: 12px;
  border-color: var(--ag-outline-variant);
  accent-color: var(--ag-primary);
}

.diagnostic__symptom-label {
  font-size: 16px;
  line-height: 24px;
  transition: color 0.2s;
}

.diagnostic__status {
  background: #fff;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  border: 1px solid color-mix(in srgb, var(--ag-outline-variant) 30%, transparent);
  overflow: hidden;
  position: relative;
}

.diagnostic__status-inner {
  position: relative;
  z-index: 10;
}

.diagnostic__status-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.diagnostic__pulse {
  display: block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--ag-secondary-fixed);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.diagnostic__status-text {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--ag-on-surface-variant);
}

.diagnostic__status-title {
  font-family: var(--ag-font-display);
  font-size: 32px;
  font-weight: 500;
  line-height: 40px;
  color: var(--ag-on-surface);
  margin: 0 0 24px;
}

.diagnostic__metrics {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 32px;
}

.diagnostic__metric-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.diagnostic__metric-label {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  color: var(--ag-tertiary);
}

.diagnostic__metric-value {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
}

.diagnostic__metric-value--primary {
  color: var(--ag-primary);
}

.diagnostic__metric-value--secondary {
  color: var(--ag-secondary);
}

.diagnostic__bar {
  height: 8px;
  width: 100%;
  background: var(--ag-surface-container-high);
  border-radius: 9999px;
  overflow: hidden;
}

.diagnostic__bar-fill {
  height: 100%;
  border-radius: 9999px;
  transform-origin: left;
  transition: transform 1s;
}

.diagnostic__bar-fill--primary {
  background: var(--ag-primary);
}

.diagnostic__bar-fill--secondary {
  background: var(--ag-secondary);
}

.diagnostic__minicards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.diagnostic__minicard {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid color-mix(in srgb, var(--ag-outline-variant) 20%, transparent);
  background: var(--ag-surface-container-lowest);
}

.diagnostic__minicard-icon {
  margin-bottom: 8px;
  font-size: 24px;
}

.diagnostic__minicard-icon--primary {
  color: var(--ag-primary);
}

.diagnostic__minicard-icon--secondary {
  color: var(--ag-secondary);
}

.diagnostic__minicard-label {
  font-size: 12px;
  line-height: 16px;
  color: var(--ag-tertiary);
  text-transform: uppercase;
  margin: 0;
}

.diagnostic__minicard-value {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  color: var(--ag-on-surface);
  margin: 0;
}

.diagnostic__status-bg {
  position: absolute;
  right: -48px;
  bottom: -48px;
  width: 192px;
  height: 192px;
  border-radius: 50%;
  border: 16px solid color-mix(in srgb, var(--ag-primary-fixed) 20%, transparent);
  z-index: 0;
}

.diagnostic__report {
  background: var(--ag-primary);
  color: #fff;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -2px rgba(0,0,0,0.05);
  transition: all 0.7s;
}

.diagnostic__report--highlight {
  box-shadow: 0 0 0 4px var(--ag-primary-fixed);
  transform: scale(1.05);
}

.diagnostic__report-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.diagnostic__report-icon {
  padding: 12px;
  background: color-mix(in srgb, #fff 20%, transparent);
  border-radius: 8px;
}

.diagnostic__report-icon .material-symbols-outlined {
  font-size: 32px;
}

.diagnostic__report-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  line-height: 32px;
  margin: 0;
}

.diagnostic__report-subtitle {
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-primary-fixed);
  margin: 0;
}

.diagnostic__report-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 32px;
}

.diagnostic__report-desc {
  font-size: 16px;
  line-height: 24px;
  line-height: 1.625;
  margin: 0;
}

.diagnostic__treatment-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.diagnostic__treatment-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
}

.diagnostic__treatment-icon {
  font-size: 14px;
  color: var(--ag-primary-fixed);
}

.diagnostic__cta {
  width: 100%;
  background: #fff;
  color: var(--ag-primary);
  padding: 16px 0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background 0.2s;
}

.diagnostic__cta:hover {
  background: var(--ag-primary-fixed);
}

.diagnostic__analyze {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 24px;
  border-top: 1px solid color-mix(in srgb, var(--ag-outline-variant) 30%, transparent);
  padding-top: 24px;
}
.diagnostic__plant-input {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 16px;
  font-family: var(--ag-font-body);
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-on-surface);
  background: var(--ag-surface-container-lowest);
  border: 1px solid color-mix(in srgb, var(--ag-outline-variant) 40%, transparent);
  border-radius: 8px;
  outline: none;
  transition: border-color 0.2s;
}
.diagnostic__plant-input:focus {
  border-color: var(--ag-primary);
}
.diagnostic__analyze-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 0;
  border-radius: 8px;
  border: none;
  background: var(--ag-primary);
  color: var(--ag-on-primary);
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: background 0.2s;
}
.diagnostic__analyze-btn:hover { background: var(--ag-primary-dark, var(--ag-primary)); }
.diagnostic__analyze-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.diagnostic__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: diagnostic-spin 0.8s linear infinite;
}
@keyframes diagnostic-spin { to { transform: rotate(360deg); } }
.diagnostic__error {
  margin: 16px 0 0;
  font-size: 13px;
  line-height: 20px;
  color: var(--ag-error, #b3261e);
}

.diagnostic__report-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.diagnostic__report-tag {
  font-size: 12px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  padding: 2px 12px;
  border-radius: 9999px;
  background: color-mix(in srgb, #fff 20%, transparent);
}
.diagnostic__report-tag--low { background: color-mix(in srgb, #4caf50 40%, transparent); }
.diagnostic__report-tag--medium { background: color-mix(in srgb, #ff9800 45%, transparent); }
.diagnostic__report-tag--high,
.diagnostic__report-tag--critical { background: color-mix(in srgb, #f44336 50%, transparent); }
.diagnostic__report-section { margin-bottom: 16px; }
.diagnostic__report-section-title {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.05em;
  margin: 0 0 8px;
}

.diagnostic__gallery-empty {
  text-align: center;
  font-size: 16px;
  color: var(--ag-on-surface-variant);
  padding: 24px 0;
}
</style>
