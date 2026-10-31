<template>
  <div :class="['workspace-'+role, {'workspace-collapsed':collapsed}]" class="workspace-shell"><a class="skip-link" href="#workspace-main">Bỏ qua điều
    hướng</a>
    <aside id="workspace-sidebar" class="workspace-sidebar">
      <RouterLink class="market-brand" to="/"><span class="brand-mark"><MarketIcon name="leaf"/></span><span>BonsaiMarket</span>
      </RouterLink>
      <p class="workspace-role-label">{{ role === 'admin' ? 'Quản trị marketplace' : 'Kênh người bán' }}</p>
      <nav aria-label="Điều hướng workspace">
        <RouterLink v-for="item in items" :key="item.to" :aria-current="active(item)?'page':undefined" :aria-label="item.label" :title="collapsed?item.label:undefined"
                    :class="{'workspace-link-active':active(item)}"
                    :to="item.to">
          <MarketIcon :name="item.icon"/>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
      <RouterLink class="workspace-back" to="/" aria-label="Về marketplace"><span>Về marketplace</span>
        <MarketIcon name="arrow"/>
      </RouterLink>
    </aside>
    <div class="workspace-body">
      <header class="workspace-topbar">
        <button class="workspace-collapse-toggle" type="button" aria-controls="workspace-sidebar" :aria-expanded="!collapsed" :aria-label="collapsed?'Mở rộng thanh điều hướng':'Thu gọn thanh điều hướng'" @click="collapsed=!collapsed"><MarketIcon name="menu"/></button>
        <button ref="mobileTrigger" aria-controls="workspace-navigation" aria-haspopup="dialog"
                aria-label="Mở điều hướng workspace" :aria-expanded="navigationOpen" class="workspace-mobile-toggle" @click="openNavigation">
          <MarketIcon name="menu"/>
        </button>
        <nav class="workspace-breadcrumbs" aria-label="Vị trí trong workspace">
          <RouterLink :to="base">{{ role === 'admin' ? 'Quản trị sàn' : 'Kênh người bán' }}</RouterLink>
          <span aria-hidden="true">/</span><strong>{{ pageTitle }}</strong></nav>
        <span class="workspace-role-badge">{{ role === 'admin' ? 'Admin' : 'Seller' }}</span>
        <div class="workspace-auth-actions"><button class="workspace-chat-toggle" type="button" aria-label="Mở trợ lý cây cảnh" aria-haspopup="dialog" aria-controls="bonsai-ai-panel" @click="openChat"><MarketIcon name="chat"/></button><RouterLink to="/profile">Tài khoản</RouterLink><button type="button" :disabled="loggingOut" @click="signOut">{{ loggingOut?'Đang đăng xuất…':'Đăng xuất' }}</button></div></header>
      <main id="workspace-main" class="workspace-main" tabindex="-1">
        <p v-if="authError" class="integration-error" role="alert">{{ authError }}</p>
        <slot/>
      </main>
      <footer class="workspace-footer">BonsaiMarket ·
        {{ role === 'admin' ? 'Điều hành hoạt động của sàn' : 'Quản lý gian hàng độc lập' }}
      </footer>
    </div>
    <dialog id="workspace-navigation" ref="dialog" aria-labelledby="workspace-navigation-title"
            class="workspace-mobile-dialog" @click="closeOnBackdrop" @close="restoreMobileFocus" @keydown.tab="trapNavigationFocus">
      <div class="workspace-dialog-header"><strong
          id="workspace-navigation-title">{{ role === 'admin' ? 'Quản trị sàn' : 'Kênh người bán' }}</strong>
        <button aria-label="Đóng điều hướng" @click="dialog.close()">
          <MarketIcon name="close"/>
        </button>
      </div>
      <nav aria-label="Điều hướng workspace trên điện thoại">
        <RouterLink v-for="item in items" :key="item.to" :to="item.to" :aria-current="active(item)?'page':undefined" @click="dialog.close()">
          <MarketIcon :name="item.icon"/>
          <span>{{ item.label }}</span>
        </RouterLink>
        <RouterLink to="/" @click="dialog.close()">Về marketplace</RouterLink>
      </nav>
</dialog>
  </div>
</template>
<script setup>
import {computed, ref, watch, onMounted, onBeforeUnmount} from 'vue';
import {useRoute} from 'vue-router';
import MarketIcon from './MarketIcon.vue';
import {workspaceNavigation} from '../../navigation/marketplace.js';
import {useAuth} from '../../composables/useAuth.js';
const {logout} = useAuth();
const loggingOut = ref(false), authError = ref('');
function openChat(event){window.dispatchEvent(new CustomEvent('agriverse-open-ai-expert',{detail:{opener:event.currentTarget}}));}
async function signOut() {
  if (loggingOut.value) return;
  loggingOut.value = true; authError.value = '';
  try {await logout();} catch (error) {authError.value = error.message;} finally {loggingOut.value = false;}
}

const props = defineProps({role: {type: String, default: 'seller'}}), current = useRoute(), dialog = ref(null),
    mobileTrigger = ref(null);
const base = computed(() => props.role === 'admin' ? '/admin' : '/seller');
const items = computed(() => workspaceNavigation[props.role] || []);
const collapsed=ref(false),navigationOpen=ref(false);
watch(()=>current.fullPath,()=>dialog.value?.close());
let desktopMedia;
function closeAtDesktop(){if(desktopMedia.matches&&dialog.value?.open)dialog.value.close();}
function openNavigation(){dialog.value.showModal();navigationOpen.value=true;}
function restoreMobileFocus(){navigationOpen.value=false;if(mobileTrigger.value?.getClientRects().length)mobileTrigger.value.focus();}
function trapNavigationFocus(event){
  const controls=[...dialog.value.querySelectorAll('button,a[href]')],first=controls[0],last=controls.at(-1);
  if(event.shiftKey&&document.activeElement===first){event.preventDefault();last?.focus();}
  else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first?.focus();}
}
onMounted(()=>{desktopMedia=window.matchMedia('(min-width:768px)');desktopMedia.addEventListener('change',closeAtDesktop);});
onBeforeUnmount(()=>desktopMedia?.removeEventListener('change',closeAtDesktop));
// Trang chi tiết như /admin/orders/12 vẫn đánh dấu mục danh sách tương ứng.
const active = item => current.path === item.to || (item.to !== base.value && current.path.startsWith(item.to + '/'));
const pageTitle = computed(() => items.value.find(active)?.label || 'Tổng quan');

function closeOnBackdrop(e) {
  if (e.target === dialog.value) {
    const r = dialog.value.getBoundingClientRect();
    if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) dialog.value.close()
  }
}
</script>
<style scoped>
.workspace-collapse-toggle {display:inline-flex;align-items:center;justify-content:center;flex:none;width:44px;height:44px;border:1px solid var(--bm-border);border-radius:var(--bm-radius-control);background:var(--bm-surface);color:var(--bm-forest);cursor:pointer;}
.workspace-breadcrumbs {margin-right:auto;}
.workspace-sidebar nav a[aria-current=page] {background:var(--bm-soft);color:var(--bm-forest);box-shadow:inset 0 0 0 1px var(--bm-border);}
.workspace-sidebar .market-brand {padding-bottom:20px;border-bottom:1px solid var(--bm-border);}
.workspace-mobile-dialog {max-height:calc(100dvh - 32px);overflow:auto;overscroll-behavior:contain;background:var(--bm-surface);box-shadow:var(--bm-shadow-dialog);}
.workspace-mobile-dialog::backdrop {background:var(--bm-scrim);}
.workspace-mobile-dialog nav a[aria-current=page] {background:var(--bm-soft);border-radius:var(--bm-radius-control);font-weight:600;}
@media(min-width:768px){
  .workspace-collapsed .workspace-sidebar {width:88px;padding-inline:16px;}
  .workspace-collapsed .workspace-sidebar :is(.market-brand>span:last-child,.workspace-role-label,nav a>span,.workspace-back>span) {display:none;}
  .workspace-collapsed .workspace-sidebar nav {margin-top:24px;}
  .workspace-collapsed .workspace-sidebar :is(nav a,.workspace-back,.market-brand) {justify-content:center;}
}
.workspace-auth-actions{display:flex;align-items:center;gap:12px;font-size:14px;flex-shrink:0}
.workspace-auth-actions a{display:inline-flex;align-items:center;min-height:44px;color:var(--bm-forest);text-underline-offset:4px}
.workspace-auth-actions button{min-height:44px;padding:8px 12px;border:1px solid var(--bm-border);border-radius:var(--bm-radius);background:var(--bm-surface);color:var(--bm-forest);font:inherit;cursor:pointer}
.workspace-auth-actions .workspace-chat-toggle {display:inline-flex;align-items:center;justify-content:center;width:44px;padding:8px;flex:none;}
.workspace-breadcrumbs{min-width:0;flex-wrap:wrap;overflow-wrap:anywhere}
@media(min-width:768px) and (max-width:1100px){
  .workspace-topbar{flex-wrap:wrap;gap:8px 16px}
  .workspace-breadcrumbs{flex:1 0 100%;font-size:14px}
  .workspace-auth-actions{margin-left:auto}
}
@media(max-width:767px){
  .workspace-collapse-toggle {display:none;}
  .workspace-topbar{display:grid;grid-template-columns:44px minmax(0,1fr);align-items:center;gap:8px 12px}
  .workspace-breadcrumbs{font-size:14px;gap:4px 8px;line-height:1.5}
  .workspace-breadcrumbs strong{flex-basis:100%;font-size:16px}
  .workspace-breadcrumbs>span{display:none}
  .workspace-role-badge{grid-column:1;grid-row:2;margin:0;order:initial;padding:4px 0;font-size:12px;text-align:center}
  .workspace-auth-actions{grid-column:2;grid-row:2;flex-wrap:wrap;gap:8px 16px}
}
</style>
