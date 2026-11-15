<template>
  <nav v-if="totalPages > 0" :aria-label="label" class="products-pagination">
    <button :disabled="busy || page <= 0" class="pagination-btn" aria-label="Trang trước" @click="go(page - 1)">Trước</button>
    <template v-for="n in pages" :key="n">
      <span v-if="typeof n === 'string'" class="pagination-dots">…</span>
      <button v-else :aria-current="n === page ? 'page' : undefined" :aria-label="'Trang '+(n+1)" :disabled="busy || n === page"
              :class="{'pagination-active': n === page}" class="pagination-page" @click="go(n)">{{ n + 1 }}</button>
    </template>
    <span class="pagination-summary" aria-live="polite">{{ page >= totalPages ? 'Trang này không còn kết quả.' : 'Trang '+(page + 1)+' / '+totalPages }}</span>
    <button v-if="page >= totalPages" :disabled="busy" class="pagination-btn" @click="go(0)">Về trang đầu</button>
    <button :disabled="busy || page + 1 >= totalPages" class="pagination-btn" aria-label="Trang sau" @click="go(page + 1)">Sau</button>
  </nav>
</template>
<script setup>
import {computed, nextTick} from 'vue';
import {useRoute, useRouter} from 'vue-router';
const props = defineProps({page: {type: Number, default: 0}, totalPages: {type: Number, default: 0}, busy: Boolean, label:{type:String,default:'Phân trang sản phẩm'}, focusTarget:String});
const route = useRoute(), router = useRouter();
const pages = computed(() => {
  const values = [...new Set([0, props.page - 1, props.page, props.page + 1, props.totalPages - 1])].filter(n => n >= 0 && n < props.totalPages).sort((a,b) => a-b);
  return values.flatMap((n, i) => i && n - values[i-1] > 1 ? ['gap-' + n, n] : [n]);
});
async function go(page) {
  if(props.busy || page===props.page || page<0 || page>=props.totalPages)return;
  const failure=await router.push({path: route.path, query: {...route.query, page: String(page)}, hash: route.hash});
  if(failure || !props.focusTarget)return;
  await nextTick();
  const target=document.querySelector(props.focusTarget);
  target?.focus({preventScroll:true});
  target?.scrollIntoView({block:'start',behavior:'instant'});
}
</script>
