<template>
  <MarketplaceLayout>
    <main class="not-found">
      <section class="not-found-hero">
        <img
          class="not-found-hero-img"
          src="https://lh3.googleusercontent.com/aida-public/AB6AXuBWrPdV5KBvDDBXSzCwNhbqF8zsxJ7I_DVFz1S1FFctntDNxa3IDMae7EoHVhAUf4R668_vGdx4wkuRbeq8aTf70-Z7qMquvgRbPtZfjb5Xghq14aK7qd3RjeGr9V4JCXvlnShpmO3z8vLmV69zHNJyI9uWniw7DU8_UzjvG9-DubSoJFF0DwQV-3zXC2aObipFlNCnnTctSAzKSv6n70Ohz_PC6BoiuKHyxXPV5WBkaNJlqLTuRI0akBzfWQTcQ0-XevPn22diLXo"
          alt="Lối đi trong vườn sương mù"
        />
        <div class="not-found-mist"></div>
        <div class="not-found-hero-text">
          <h1 class="not-found-title">Đường dẫn này đã bị bỏ hoang</h1>
          <p class="not-found-subtitle">Cây cảnh bạn đang tìm kiếm đã trở về vườn ươm.</p>
        </div>
      </section>

      <section class="not-found-cards">
        <div class="not-found-grid">
          <Link :href="route('agriverse.shop.products.index')" class="not-found-card">
            <div class="not-found-card-icon not-found-card-icon-primary">
              <span class="material-symbols-outlined">potted_plant</span>
            </div>
            <h3 class="not-found-card-title">Mua Hàng Mới Về</h3>
            <p class="not-found-card-desc">Khám phá những bộ sưu tập thực vật mới nhất của chúng tôi, tươi tốt từ nhà kính và sẵn sàng cho ngôi nhà mới.</p>
            <span class="not-found-card-link">
              Xem Bộ sưu tập
              <span class="material-symbols-outlined">arrow_forward</span>
            </span>
          </Link>
          <Link :href="route('agriverse.shop.quiz.index')" class="not-found-card">
            <div class="not-found-card-icon not-found-card-icon-secondary">
              <span class="material-symbols-outlined">workspace_premium</span>
            </div>
            <h3 class="not-found-card-title">Phát hiện Quý hiếm</h3>
            <p class="not-found-card-desc">Các mẫu vật kỳ lạ độc đáo và giới hạn dành cho nhà làm vườn chuyên nghiệp và nhà sưu tầm tận tâm.</p>
            <span class="not-found-card-link">
              Khám phá Quý hiếm
              <span class="material-symbols-outlined">arrow_forward</span>
            </span>
          </Link>
          <Link :href="route('agriverse.shop.journal.index')" class="not-found-card">
            <div class="not-found-card-icon not-found-card-icon-fixed">
              <span class="material-symbols-outlined">menu_book</span>
            </div>
            <h3 class="not-found-card-title">Wiki Chăm sóc</h3>
            <p class="not-found-card-desc">Kho tàng kiến thức thực vật khoa học. Tìm hiểu cách nuôi dưỡng sự sống trong vi khí hậu cụ thể của bạn.</p>
            <span class="not-found-card-link">
              Khám phá Hướng dẫn
              <span class="material-symbols-outlined">arrow_forward</span>
            </span>
          </Link>
        </div>
      </section>

      <section class="not-found-quote">
        <span class="not-found-quote-icon material-symbols-outlined">format_quote</span>
        <blockquote class="not-found-quote-text">
          &ldquo;Trong mỗi bước đi với thiên nhiên, người ta nhận được nhiều hơn những gì họ tìm kiếm.&rdquo;
        </blockquote>
        <cite class="not-found-quote-cite">&mdash; John Muir</cite>
      </section>

      <div class="not-found-particles">
        <div v-for="n in 15" :key="n" class="not-found-particle" :style="particleStyle(n)"></div>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { Link } from '@inertiajs/vue3'
import { route } from 'ziggy-js'
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue'

function particleStyle(n) {
  const size = Math.random() * 8 + 4
  return {
    width: size + 'px',
    height: size + 'px',
    left: Math.random() * 100 + 'vw',
    top: Math.random() * 100 + 'vh',
    '--drift-x': (Math.random() * 100 - 50) + 'px',
    '--drift-y': (Math.random() * -100 - 50) + 'px',
    '--duration': (Math.random() * 15 + 10) + 's',
    '--delay': (Math.random() * 5) + 's',
  }
}
</script>

<style scoped>
.not-found {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
}

.not-found-hero {
  position: relative;
  width: 100%;
  height: 614px;
  overflow: hidden;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
@media (max-width: 768px) {
  .not-found-hero { height: 480px; }
}
.not-found-hero-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.not-found-mist {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, transparent 0%, var(--ag-bg) 95%);
  pointer-events: none;
}
.not-found-hero-text {
  position: relative;
  z-index: 10;
  text-align: center;
  padding-bottom: 48px;
  padding-left: 20px;
  padding-right: 20px;
  max-width: 600px;
}
.not-found-title {
  font-family: var(--ag-font-display);
  font-size: 36px;
  font-weight: 500;
  line-height: 42px;
  letter-spacing: -0.01em;
  color: var(--ag-text-primary);
  margin-bottom: 16px;
}
@media (min-width: 768px) {
  .not-found-title {
    font-size: 48px;
    line-height: 56px;
    letter-spacing: -0.02em;
  }
}
.not-found-subtitle {
  font-family: var(--ag-font-body);
  font-size: 18px;
  line-height: 28px;
  color: var(--ag-text-secondary);
  opacity: 0.9;
}

.not-found-cards {
  max-width: 1280px;
  margin: 0 auto;
  padding: 80px 64px;
}
@media (max-width: 768px) {
  .not-found-cards { padding: 80px 20px; }
}
.not-found-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}
@media (min-width: 768px) {
  .not-found-grid { grid-template-columns: repeat(3, 1fr); }
}
.not-found-card {
  background: var(--ag-bg-card);
  padding: 32px;
  border-radius: var(--ag-radius-xl);
  border: 1px solid color-mix(in srgb, var(--ag-primary-500) 5%, transparent);
  box-shadow: var(--ag-shadow-lg);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: pointer;
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.not-found-card:hover {
  transform: translateY(-8px);
}
.not-found-card-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}
.not-found-card-icon .material-symbols-outlined {
  font-size: 24px;
}
.not-found-card-icon-primary {
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  color: var(--ag-primary-500);
}
.not-found-card-icon-secondary {
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  color: var(--ag-primary-500);
}
.not-found-card-icon-fixed {
  background: color-mix(in srgb, color-mix(in srgb, var(--ag-primary-500) 10%, transparent) 30%, transparent);
  color: var(--ag-primary-500);
}
.not-found-card-title {
  font-family: var(--ag-font-display);
  font-size: 32px;
  font-weight: 500;
  line-height: 40px;
  color: var(--ag-text-primary);
  margin-bottom: 12px;
}
.not-found-card-desc {
  font-family: var(--ag-font-body);
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-text-secondary);
  margin-bottom: 32px;
  flex-grow: 1;
}
.not-found-card-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--ag-primary-500);
  text-decoration: none;
  transition: gap 0.3s;
}
.not-found-card:hover .not-found-card-link {
  gap: 16px;
}
.not-found-card-link .material-symbols-outlined {
  font-size: 18px;
}

.not-found-quote {
  width: 100%;
  background: var(--ag-bg);
  padding: 128px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.not-found-quote-icon {
  font-size: 48px;
  color: color-mix(in srgb, var(--ag-primary-500) 30%, transparent);
  margin-bottom: 24px;
}
.not-found-quote-text {
  max-width: 720px;
  font-family: var(--ag-font-display);
  font-size: 32px;
  font-weight: 500;
  font-style: italic;
  line-height: 40px;
  color: var(--ag-text-secondary);
  margin-bottom: 16px;
}
.not-found-quote-cite {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--ag-text-muted);
  font-style: normal;
}

.not-found-particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}
.not-found-particle {
  position: fixed;
  border-radius: 50%;
  background: var(--ag-primary-500);
  opacity: 0.2;
  animation: not-found-float var(--duration, 12s) ease-in-out var(--delay, 0s) infinite;
}

@keyframes not-found-float {
  0% { transform: translate(0, 0); opacity: 0.1; }
  50% { transform: translate(var(--drift-x, 30px), var(--drift-y, -60px)); opacity: 0.3; }
  100% { transform: translate(0, 0); opacity: 0.1; }
}
</style>
