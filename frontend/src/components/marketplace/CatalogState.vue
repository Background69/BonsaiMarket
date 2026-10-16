<template>
  <div v-if="loading" class="catalog-loading" aria-busy="true">
    <p class="integration-loading" role="status">Đang tải sản phẩm…</p>
    <div class="products-grid" aria-hidden="true"><div v-for="n in 4" :key="n" class="catalog-skeleton"><span/><span/><span/></div></div>
  </div>
  <div v-else-if="error" class="catalog-state catalog-error" role="alert">
    <h2>Chưa tải được sản phẩm</h2><p>{{ message || 'Kết nối bị gián đoạn. Thử lại với các bộ lọc bạn đã chọn.' }}</p>
    <button class="button button-primary" @click="$emit('retry')">Thử lại</button>
  </div>
  <div v-else class="catalog-state products-empty" role="status">
    <h2>{{ filtered ? 'Không có sản phẩm phù hợp' : 'Chưa có sản phẩm được đăng bán' }}</h2>
    <p>{{ filtered ? 'Thử bỏ bớt bộ lọc hoặc tìm bằng từ khóa khác ở thanh tìm kiếm.' : 'Bạn có thể khám phá các gian hàng và quay lại sau.' }}</p>
    <button v-if="filtered" class="button button-primary" @click="$emit('clear')">Xóa bộ lọc và từ khóa</button>
    <RouterLink v-else class="button button-outline" to="/stores">Khám phá gian hàng</RouterLink>
  </div>
</template>
<script setup>
defineProps({loading:Boolean,error:Boolean,message:String,filtered:Boolean});
defineEmits(['retry','clear']);
</script>
