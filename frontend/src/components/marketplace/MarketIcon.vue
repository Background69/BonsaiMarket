<template>
  <svg :data-market-icon="name" aria-hidden="true" fill="none" focusable="false" height="22" stroke="currentColor"
       stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" viewBox="0 0 24 24" width="22">
    <path :d="paths[name] || paths.leaf"/>
  </svg>
</template>
<script setup>
defineProps({name: {type: String, default: 'leaf'}});
const paths = {
  chevron: 'm9 5 7 7-7 7',
  list: 'M9 5h12M9 12h12M9 19h12M3 5h.01M3 12h.01M3 19h.01',
  chat: 'M21 11a9 9 0 0 1-9 9H4l-3 3V11a10 10 0 0 1 20 0Z',
  sprout: 'M12 22V12M12 14C4 15 2 10 2 5c7-1 10 3 10 9ZM12 10C12 3 17 1 22 2c0 6-4 9-10 8Z',
  robot: 'M12 2v3M10 2h4M5 8h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2ZM8 12v2m8-2v2m-7 3h6M1 12v4m22-4v4M8 22v-2m8 2v-2',
  leaf: 'M20 4C9 2 3 7 5 14c2 7 14 6 15-10ZM4 21 15 9',
  search: 'm21 21-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
  store: 'M3 10h18L19 3H5ZM4 10v11h16V10M9 21v-7h6v7',
  cart: 'M2 3h3l3 12h11l3-9H6M9 20h.01M18 20h.01',
  user: 'M16 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0M3 22v-3a9 9 0 0 1 18 0v3',
  arrow: 'M4 12h16m-6-6 6 6-6 6',
  grid: 'M3 3h7v7H3ZM14 3h7v7h-7ZM3 14h7v7H3ZM14 14h7v7h-7Z',
  bag: 'M5 7h14l1 15H4ZM8 8V5a4 4 0 0 1 8 0v3',
  chart: 'M3 3v18h18M7 17v-5m5 5V8m5 9V4',
  ai: 'M12 3 9 9 3 12l6 3 3 6 3-6 6-3-6-3Z',
  menu: 'M3 6h18M3 12h18M3 18h18',
  close: 'm6 6 12 12M6 18 18 6',
  pin: 'M12 22s8-8 8-13A8 8 0 0 0 4 9c0 5 8 13 8 13ZM15 9a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  filter: 'M4 6h16M7 12h10M10 18h4'
};
Object.assign(paths, {
  trash: 'M3 6h18M5 6l1 16h12l1-16M9 6V2h6v4M10 10v8M14 10v8',
  heart: 'M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z',
  box: 'm3 7 9-5 9 5v10l-9 5-9-5ZM3 7l9 5 9-5M12 12v10M7.5 4.5l9 5',
  users: 'M15 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0M3 21v-2a8 8 0 0 1 16 0v2M18 3a4 4 0 0 1 0 8M22 21v-2a8 8 0 0 0-3-6',
  wallet: 'M3 6h17v15H3a1 1 0 0 1-1-1V6a2 2 0 0 1 2-2h14v2M20 11h-5v5h5M17 13.5h.01',
  clock: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0M12 6v6l4 2',
  truck: 'M1 4h13v13H1ZM14 9h5l4 5v3h-9M5 17a2 2 0 1 0 0 4 2 2 0 0 0 0-4M19 17a2 2 0 1 0 0 4 2 2 0 0 0 0-4',
  check: 'm5 12 4 4L19 6',
  plus: 'M12 5v14M5 12h14',
  info: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0M12 11v6M12 7h.01',
  pinOff: 'm3 3 18 18M7 3a8 8 0 0 1 13 6c0 2-1 4-2 6M15 19l-3 3S4 14 4 9c0-1 .2-2 .6-3M10 8a3 3 0 0 0 4 4',
  send: 'm22 2-7 20-4-9-9-4ZM22 2 11 13',
  chevronLeft: 'm15 5-7 7 7 7',
});
</script>
<style scoped>
svg {
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
