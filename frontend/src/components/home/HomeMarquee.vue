<template>
  <div aria-label="BonsaiMarket · Cây cảnh · Bonsai · Gian hàng · Khám phá thiên nhiên" class="home-marquee">
    <div aria-hidden="true" class="home-marquee-track">
      <template v-for="copy in 3" :key="copy">
        <span v-for="word in words" :key="`${copy}-${word}`"><svg fill="currentColor" viewBox="0 0 24 24"><path
            d="M20 3C8 2 3 7 5 14c2 6 12 5 15-11ZM4 21l9-11-1-1L3 20Z"/></svg>{{ word }}</span>
      </template>
    </div>
  </div>
</template>
<script setup>
const words = ['BonsaiMarket', 'Cây cảnh', 'Bonsai', 'Gian hàng', 'Khám phá thiên nhiên'];
</script>
<style scoped>
.home-marquee {
  overflow: hidden;
  background: var(--bm-yellow);
  color: var(--bm-forest);
  padding: 14px 0;
}

.home-marquee-track {
  display: flex;
  width: max-content;
  gap: 48px;
  transform: translateX(-28px);
}

.home-marquee span {
  display: flex;
  align-items: center;
  gap: 18px;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 650;
}

.home-marquee svg {
  width: 20px;
  height: 20px;
}

/* Static like the reference screenshot; no continuous animation or pause control required. */
@media (max-width: 600px) {
  .home-marquee {
    padding: 12px 0;
  }

  .home-marquee-track {
    gap: 30px;
  }
}
</style>
