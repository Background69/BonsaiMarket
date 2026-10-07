<script setup>
defineProps({title: String, message: String})

const links = [
  {name: 'home', label: 'Trang chủ'},
  {name: 'products', label: 'Sản phẩm'},
  {name: 'login', label: 'Đăng nhập'},
  {name: 'register', label: 'Đăng ký'},
  {name: 'admin', label: 'Quản trị'},
  {name: 'seller', label: 'Người bán'},
]
</script>

<template>
  <main class="foundation-shell">
    <nav aria-label="Điều hướng chính">
      <RouterLink v-for="link in links" :key="link.name" :to="{ name: link.name }">{{ link.label }}</RouterLink>
    </nav>
    <h1>{{ title }}</h1>
    <p>{{ message }}</p>
  </main>
</template>

<style scoped>
.foundation-shell {
  max-width: 900px;
  margin: 4rem auto;
  padding: 2rem;
  font-family: system-ui, sans-serif;
}

nav {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

nav a {
  color: #287443;
}

nav a.router-link-active {
  font-weight: 700;
}
</style>
