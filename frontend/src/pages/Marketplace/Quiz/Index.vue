<template>
  <MarketplaceLayout :hide-footer="true">
    <main class="quiz-page">
      <div class="quiz-container">
        <div class="quiz-header">
          <span class="quiz-label">Công cụ Tìm Mẫu vật</span>
          <h1 class="quiz-title" id="quiz-title">{{ stepTitle }}</h1>

          <div class="progress-wrap">
            <div class="progress-track">
              <div class="progress-thumb" :style="{ transform: 'scaleX(' + (progressPercent / 100) + ')' }"></div>
            </div>
            <div class="progress-counter">{{ progressLabel }}</div>
          </div>
        </div>

        <div v-for="q in questions" :key="q.step" v-show="step === q.step" class="quiz-step">
          <h2 class="step-question">{{ q.question_text }}</h2>
          <div v-if="q.choice_type === 'grid'" class="choice-grid choice-grid-3">
            <button v-for="(choice, ci) in q.choices" :key="ci" class="choice-card" @click="answer(q, choice, q.step + 1)">
              <div class="choice-icon-wrap">
                <span class="material-symbols-outlined choice-icon">{{ choice.icon }}</span>
              </div>
              <span class="choice-label">{{ choice.label }}</span>
              <p v-if="choice.desc" class="choice-desc">{{ choice.desc }}</p>
            </button>
          </div>
          <div v-else-if="q.choice_type === 'image'" class="choice-grid choice-grid-2">
            <button v-for="(choice, ci) in q.choices" :key="ci" class="choice-image-card" @click="answer(q, choice, q.step + 1)">
              <div class="choice-image" :class="choice.image_class"></div>
              <div class="choice-image-overlay">
                <span class="choice-image-label">{{ choice.label }}</span>
                <span v-if="choice.sub" class="choice-image-sub">{{ choice.sub }}</span>
              </div>
            </button>
          </div>
          <div v-else class="choice-rows">
            <button v-for="(choice, ci) in q.choices" :key="ci" class="choice-row" @click="answer(q, choice, q.step + 1)">
              <div>
                <span class="choice-row-label">{{ choice.label }}</span>
                <span v-if="choice.desc" class="choice-row-desc">{{ choice.desc }}</span>
              </div>
              <span class="choice-row-arrow material-symbols-outlined">arrow_forward</span>
            </button>
          </div>
        </div>

        <div v-show="step === 4" class="quiz-step quiz-step-center">
          <div class="spinner-wrap">
            <div class="spinner-ring"></div>
            <span class="material-symbols-outlined spinner-icon">psychology</span>
          </div>
          <h2 class="step-question">Đang phân tích khả năng tương thích thực vật...</h2>
          <p class="step-loading-desc">Đang tuyển chọn từ kho lưu trữ quý hiếm của chúng tôi.</p>
        </div>

        <div v-show="step === 'results'" class="quiz-step">
          <div class="results-header">
            <h2 class="results-title">Mẫu vật được Tuyển chọn</h2>
            <p class="results-desc">Dựa trên môi trường và trình độ của bạn, chúng tôi đề xuất các mẫu vật phù hợp này.</p>
          </div>
          <div v-if="recommending" class="spinner-wrap">
            <div class="spinner-ring"></div>
            <span class="material-symbols-outlined spinner-icon">psychology</span>
          </div>
          <div v-else-if="recommendedProducts.length" class="results-grid">
            <div v-for="p in recommendedProducts" :key="p.id" class="result-card">
              <Link :href="route('agriverse.shop.products.show', p.id)" class="result-image" :style="{ backgroundImage: 'url(' + p.image + ')' }">
                <span v-if="p.category" class="result-badge result-badge-cat">{{ categoryLabel(p.category) }}</span>
              </Link>
              <div class="result-body">
                <Link :href="route('agriverse.shop.products.show', p.id)" class="result-name">{{ p.name }}</Link>
                <div class="result-specs">
                  <span v-if="p.light_need" class="result-spec">
                    <span class="material-symbols-outlined result-spec-icon">light_mode</span>
                    {{ cleanSpec(p.light_need) }}
                  </span>
                  <span v-if="p.watering" class="result-spec">
                    <span class="material-symbols-outlined result-spec-icon">water_drop</span>
                    {{ cleanSpec(p.watering) }}
                  </span>
                </div>
                <div class="result-price-row">
                  <div class="result-price">{{ formatPrice(p.price) }}₫</div>
                  <button class="result-btn" @click="addToCart(p)">
                    <span class="material-symbols-outlined">add_shopping_cart</span>
                    Thêm nhanh
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="results-empty">
            <span class="material-symbols-outlined results-empty-icon">sentiment_dissatisfied</span>
            <h3 class="results-empty-title">Chưa tìm thấy mẫu vật phù hợp</h3>
            <p class="results-empty-desc">Vui lòng thử lại với lựa chọn khác.</p>
            <button class="retake-btn" @click="resetQuiz">
              <span class="material-symbols-outlined">restart_alt</span>
              Làm lại
            </button>
          </div>
          <div v-if="!recommending && recommendedProducts.length" class="retake-wrap">
            <button class="retake-btn" @click="resetQuiz">
              <span class="material-symbols-outlined">restart_alt</span>
              Làm lại
            </button>
          </div>
        </div>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { router, Link } from '@inertiajs/vue3'
import { formatPrice } from '@agriverse/utils'
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue'

const props = defineProps({
  questions: { type: Array, default: () => [] },
})

const step = ref(1)
const answers = ref({})
const recommending = ref(false)
const recommendedProducts = ref([])
const totalSteps = computed(() => Math.max(props.questions.length + 1, 4))

const stepTitle = computed(() => {
  if (step.value === 'results') return 'Chúng tôi đã tìm thấy cặp đôi của bạn.'
  return 'Hãy cho chúng tôi biết về khu vườn của bạn.'
})

const progressPercent = computed(() => {
  if (step.value === 'results') return 100
  return (step.value / totalSteps.value) * 100
})

const progressLabel = computed(() => {
  if (step.value === 4) return 'Đang hoàn tất...'
  if (step.value === 'results') return 'Đã tìm thấy'
  return `Bước ${step.value} trên ${totalSteps.value}`
})

const currentQuestion = computed(() => {
  return props.questions.find(q => q.step === step.value)
})

const fieldKey = {
  1: 'light',
  2: 'humidity',
  3: 'level',
}

function answer(q, choice, next) {
  answers.value[fieldKey[q.step]] = choice.label
  goToStep(next)
}

function goToStep(next) {
  if (next === totalSteps.value) {
    step.value = totalSteps.value
    recommend()
  } else {
    step.value = next
  }
}

async function recommend() {
  recommending.value = true
  recommendedProducts.value = []
  try {
    const res = await fetch('/agriverse/api/quiz/recommend', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]')?.content, 'X-Requested-With': 'XMLHttpRequest' },
      body: JSON.stringify(answers.value),
    })
    const data = await res.json()
    recommendedProducts.value = data?.products || []
  } catch (e) {
    recommendedProducts.value = []
  } finally {
    recommending.value = false
    step.value = 'results'
  }
}

function categoryLabel(category) {
  if (!category) return 'Cây cảnh'
  const map = {
    'cay-canh-mini': 'Cây cảnh mini',
    'bonsai-co-thu': 'Bonsai cổ thụ',
    'cay-thuy-sinh': 'Cây thủy sinh',
  }
  return map[category] || (category.charAt(0).toUpperCase() + category.slice(1)).replace(/-/g, ' ')
}

function cleanSpec(value) {
  if (!value) return ''
  return value.replace(/[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}\u{FE0F}]/gu, '').replace(/\s+/g, ' ').trim()
}

function addToCart(product) {
  router.post(route('agriverse.api.cart.add'), { product_id: product.id, quantity: 1 }, {
    preserveState: true,
    preserveScroll: true,
  })
}

function resetQuiz() {
  step.value = 1
  answers.value = {}
  recommendedProducts.value = []
  recommending.value = false
}
</script>

<style scoped>
.quiz-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 64px;
  padding-top: 80px;
  padding-bottom: 96px;
  min-height: 100vh;
}
@media (max-width: 768px) {
  .quiz-page { padding: 64px 24px 80px; }
}
.quiz-container { max-width: 768px; margin: 0 auto; }

.quiz-header { text-align: center; margin-bottom: 48px; }
.quiz-label {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--ag-primary-500);
  display: block;
  margin-bottom: 8px;
}
.quiz-title {
  font-family: var(--ag-font-display);
  font-size: 48px;
  font-weight: 500;
  letter-spacing: -0.02em;
  color: var(--ag-text-primary);
  margin-bottom: 32px;
}
@media (max-width: 768px) { .quiz-title { font-size: 36px; } }

.progress-wrap { max-width: 100%; }
.progress-track {
  width: 100%;
  height: 4px;
  background: var(--ag-bg);
  border-radius: 9999px;
  overflow: hidden;
}
.progress-thumb {
  height: 100%;
  background: var(--ag-primary-500);
  border-radius: 9999px;
  transform-origin: left;
  transition: transform 0.7s ease-in-out;
}
.progress-counter {
  margin-top: 12px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--ag-text-muted);
  text-align: left;
}

.quiz-step {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}
.quiz-step-center {
  text-align: center;
  padding: 48px 0;
}
.step-question {
  font-family: var(--ag-font-display);
  font-size: 32px;
  font-weight: 500;
  color: var(--ag-text-secondary);
  text-align: center;
  margin-bottom: 40px;
}
.step-loading-desc {
  font-family: var(--ag-font-body);
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-text-secondary);
}

.choice-grid {
  display: grid;
  gap: 24px;
}
.choice-grid-3 { grid-template-columns: 1fr; }
@media (min-width: 768px) {
  .choice-grid-3 { grid-template-columns: repeat(3, 1fr); }
}
.choice-grid-2 { grid-template-columns: 1fr; }
@media (min-width: 768px) {
  .choice-grid-2 { grid-template-columns: repeat(2, 1fr); }
}

.choice-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px;
  background: var(--ag-bg-card);
  border-radius: 12px;
  border: 1px solid transparent;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  cursor: pointer;
  text-align: center;
  transition: all 0.3s;
}
.choice-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
  border-color: color-mix(in srgb, var(--ag-primary-500) 20%, transparent);
}
.choice-icon-wrap {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  border-radius: 50%;
  margin-bottom: 24px;
  transition: transform 0.3s;
}
.choice-card:hover .choice-icon-wrap { transform: scale(1.1); }
.choice-icon {
  font-size: 36px;
  color: var(--ag-primary-500);
}
.choice-label {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}
.choice-desc {
  font-family: var(--ag-font-body);
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-text-secondary);
}

.choice-image-card {
  position: relative;
  overflow: hidden;
  aspect-ratio: 4 / 3;
  border-radius: 16px;
  background: var(--ag-bg);
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.choice-image {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  transition: transform 0.7s;
}
.choice-image-card:hover .choice-image { transform: scale(1.05); }
.choice-image-humidity {
  background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuDQDqI4eWo4KI5OSD_wHo3XjGeQQYTFTvVZDo5GDSE5O43oT6hacAeyyESfr1ikbQWmSxljVl7jlY0PVnzvFJRqfk_0o94Bg443Aod1Pp6ojglqnDpX4r6FfHJpCX8Ym8_NuQH49Ge0D-UwopGdoJxPIF_wrJbqsrofxSRz5qTFZjn112eV7OGm_TBVouNxZRFbSbWDXVu63U0g-y9rI8Ow9VlwEYXTxJqhz7_n2xacTmqqRYh2TdbNRewePH4uQaDYoVMUBHzv-CE');
}
.choice-image-dry {
  background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuDVSI8871NzeDrO7ulEKK7j0kvXaCY3rBQAjxMtpiEdT81AhH3KxMA1lsPYA3Lw44Ho04cjsuDX0HFkLkOELi4CuBNandVZ0iIQEXb1EiI8XPYIiWkF0isTATBOW1findRk7ue7t-jEQlRQZlHJjR3FF1o4na_6ofPqnPIcBzOLqCLmsObfC2idL3-lVqIWAnRsIQ89ZeP-fxK4dbixQROcUBX0q4kmfkP0fKJdzz4HyF35gsSrHLKg6jbDp5H_NxOG9hOKWPBK2hA');
}
.choice-image-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(27, 28, 28, 0.8), transparent);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 32px;
  text-align: left;
}
.choice-image-label {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  color: white;
}
.choice-image-sub {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: rgba(255, 255, 255, 0.7);
}

.choice-rows {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 600px;
  margin: 0 auto;
}
.choice-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px;
  background: var(--ag-bg-card);
  border: 1px solid var(--ag-border);
  border-radius: 12px;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.3s;
}
.choice-row:hover { border-color: var(--ag-primary-500); }
.choice-row-label {
  display: block;
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  color: var(--ag-text-primary);
}
.choice-row-desc {
  display: block;
  font-family: var(--ag-font-body);
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-text-secondary);
}
.choice-row-arrow {
  color: var(--ag-primary-500);
  opacity: 0;
  transition: opacity 0.3s;
}
.choice-row:hover .choice-row-arrow { opacity: 1; }

.spinner-wrap {
  position: relative;
  width: 96px;
  height: 96px;
  margin: 0 auto 32px;
}
.spinner-ring {
  position: absolute;
  inset: 0;
  border: 4px solid color-mix(in srgb, var(--ag-primary-500) 12%, transparent);
  border-radius: 50%;
}
.spinner-ring::after {
  content: '';
  position: absolute;
  inset: -4px;
  border: 4px solid transparent;
  border-top-color: var(--ag-primary-500);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
.spinner-icon {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  color: var(--ag-primary-500);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.results-header { text-align: center; margin-bottom: 48px; }
.results-title {
  font-family: var(--ag-font-display);
  font-size: 32px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}
.results-desc {
  font-family: var(--ag-font-body);
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-text-secondary);
}
.results-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}
@media (min-width: 768px) {
  .results-grid { grid-template-columns: repeat(3, 1fr); }
}
.result-card {
  background: var(--ag-bg-card);
  border-radius: 20px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--ag-border);
  transition: box-shadow 0.3s, transform 0.3s;
}
.result-card:hover {
  box-shadow: 0 12px 32px rgba(0,0,0,0.1);
  transform: translateY(-4px);
}
.result-image {
  display: block;
  height: 220px;
  background-size: cover;
  background-position: center;
  position: relative;
}
.result-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 12px;
  border-radius: 9999px;
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  background: rgba(255, 255, 255, 0.92);
  color: var(--ag-primary-700);
  backdrop-filter: blur(4px);
}
.result-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  gap: 12px;
}
.result-name {
  font-family: var(--ag-font-display);
  font-size: 21px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--ag-text-primary);
  text-decoration: none;
}
.result-name:hover { color: var(--ag-primary-600); }
.result-specs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-grow: 1;
}
.result-spec {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  line-height: 20px;
  color: var(--ag-text-secondary);
}
.result-spec-icon {
  font-size: 18px;
  color: var(--ag-primary-500);
  flex-shrink: 0;
  margin-top: 1px;
}
.result-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  border-top: 1px solid var(--ag-border);
  padding-top: 14px;
}
.result-price {
  font-family: var(--ag-font-display);
  font-size: 19px;
  font-weight: 600;
  color: var(--ag-danger);
  white-space: nowrap;
}
.results-empty {
  text-align: center;
  padding: 48px 24px;
}
.results-empty-icon {
  font-size: 56px;
  color: var(--ag-text-muted);
  margin-bottom: 16px;
}
.results-empty-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}
.results-empty-desc {
  font-family: var(--ag-font-body);
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-text-secondary);
  max-width: 400px;
  margin: 0 auto 24px;
}
.result-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  gap: 12px;
}
.result-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 9px 14px;
  background: var(--ag-primary-500);
  color: white;
  border: none;
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.result-btn:hover { background: var(--ag-primary-600); }
.result-btn:active { transform: scale(0.95); }

.retake-wrap { text-align: center; padding-top: 32px; }
.retake-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--ag-text-muted);
  cursor: pointer;
  transition: color 0.2s;
}
</style>
