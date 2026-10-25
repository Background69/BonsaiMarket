<template>
  <Teleport to="body">
  <div ref="rootRef" class="ai-expert" :style="viewportStyle" :inert="obstructed" :aria-hidden="obstructed ? 'true' : undefined">
    <!-- Floating trigger -->
    <button ref="triggerRef" :aria-expanded="opened" aria-controls="bonsai-ai-panel"
            aria-label="Mở trợ lý AI BonsaiMarket"
            class="ai-expert-fab" title="Mở trợ lý AI BonsaiMarket" type="button" @click="opened ? close() : open($event)">
      <MarketIcon height="24" name="chat" width="24"/>
      <span>Trợ lý cây cảnh</span>
    </button>

    <!-- Chat panel -->
    <Transition name="ai-fade">
      <section v-if="opened" id="bonsai-ai-panel" ref="panelRef" aria-labelledby="bonsai-ai-title" class="ai-expert-panel"
               role="dialog"
               @keydown.esc.stop.prevent="close">
        <header class="ai-expert-header">
          <div class="ai-expert-header-avatar">
            <MarketIcon name="leaf"/>
          </div>
          <div class="ai-expert-header-info">
            <p id="bonsai-ai-title" class="ai-expert-header-title">Trợ lý AI BonsaiMarket</p>
            <p class="ai-expert-header-status">{{ availability || 'Tham khảo chăm cây · tìm catalog' }}</p>
          </div>
          <button aria-label="Đóng trợ lý AI" class="ai-expert-close" type="button" @click="close">
            <MarketIcon name="close"/>
          </button>
        </header>

        <div ref="scrollRef" :aria-busy="pending" aria-live="polite" aria-relevant="additions" class="ai-expert-body"
             role="log">
          <div v-if="!messages.length" class="ai-expert-welcome">
            <div class="ai-expert-welcome-icon">
              <MarketIcon height="36" name="robot" width="36"/>
            </div>
            <h3 class="ai-expert-welcome-title">Bạn cần tư vấn gì về cây cảnh?</h3>
            <p class="ai-expert-welcome-desc">
              Xin chào! Tôi là trợ lý AI của BonsaiMarket. Tôi có thể giúp bạn tìm cây cảnh hoặc tham khảo cách chăm sóc
              bonsai.
            </p>
            <div class="ai-expert-suggestions">
              <button v-for="s in suggestions" :key="s" :disabled="pending" class="ai-expert-chip" type="button"
                      @click="send(s)">
                {{ s }}
              </button>
            </div>
          </div>

          <div v-for="m in messages" :key="m.id" :class="m.role === 'user' ? 'ai-expert-msg-user' : 'ai-expert-msg-ai'"
               class="ai-expert-msg">
            <div class="ai-expert-msg-bubble">
              <p>{{ m.content }}</p>
              <ul v-if="m.sources?.length" aria-label="Nguồn tham khảo" class="ai-expert-sources">
                <li v-for="s in m.sources" :key="`${s.type}-${s.id}-${s.name}`">
                  <RouterLink v-if="sourcePath(s)" :to="sourcePath(s)">{{ s.name }}</RouterLink>
                  <a v-else-if="referenceUrl(s)" :href="referenceUrl(s)" target="_blank" rel="noopener noreferrer"
                     :aria-label="`${s.name} (nguồn tham khảo, mở trong thẻ mới)`">{{ s.name }}</a>
                  <span v-else>{{ s.name }}</span>
                </li>
              </ul>
            </div>
          </div>

          <div v-if="pending" class="ai-expert-msg ai-expert-msg-ai" role="status">
            <p class="ai-expert-msg-bubble">
              <span aria-hidden="true" class="ai-expert-typing">
                <span></span><span></span><span></span>
              </span>
              AI đang trả lời...
            </p>
          </div>

        </div>

        <footer class="ai-expert-footer">
          <div v-if="error" id="bonsai-ai-error" class="ai-expert-error" role="alert"><p>{{ error }}</p><button v-if="failedMessage" class="ai-expert-retry" type="button" :disabled="pending" @click="retryMessage">Gửi lại câu hỏi trước</button></div>
          <label class="ai-expert-label" for="bonsai-ai-input">Câu hỏi cho trợ lý AI</label>
          <form class="ai-expert-form" @submit.prevent="submit">
            <textarea id="bonsai-ai-input" ref="inputRef" v-model="input" :aria-describedby="error ? 'bonsai-ai-error bonsai-ai-hint' : 'bonsai-ai-hint'" :disabled="pending"
                      class="ai-expert-input" maxlength="2000"
                      placeholder="Hỏi về cây hoặc sản phẩm #1..."
                      rows="2" @keydown.enter="onEnter"/>
            <button :disabled="pending || !input.trim()" aria-label="Gửi câu hỏi" class="ai-expert-send" type="submit">
              <MarketIcon height="20" name="send" width="20"/>
            </button>
          </form>
          <p id="bonsai-ai-hint" class="ai-expert-hint">{{ input.length }}/2.000 · Enter gửi, Shift+Enter xuống dòng.
            Nội dung AI chỉ để tham khảo. Hội thoại chỉ lưu trong phiên mở website.</p>
        </footer>
      </section>
    </Transition>
  </div>
  </Teleport>
</template>

<script setup>
import {ref, watch, nextTick, onMounted, onUnmounted} from 'vue';
import {RouterLink} from 'vue-router';
import MarketIcon from './marketplace/MarketIcon.vue';
import {referenceUrl} from '../utils/aiSources.js';
import {useAuth} from '../composables/useAuth.js';

const opened = ref(false);
const input = ref('');
const messages = ref([]);
const pending = ref(false);
const error = ref('');
const availability = ref('');
const scrollRef = ref(null);
const inputRef = ref(null);
const triggerRef = ref(null);
const panelRef = ref(null), failedMessage = ref(''), viewportStyle = ref({});
const rootRef=ref(null),obstructed=ref(false);
const {user}=useAuth();
let surroundingsObserver,layoutFrame,conversationGeneration=0;
let opener;
let sequence = 0;
let requestController;
let disposed = false;

const suggestions = [
  'Cây bonsai dưới 500.000đ',
  'Cây phù hợp để bàn',
  'Cách tưới bonsai',
  'Tìm gian hàng',
];

function open(event) {
  if(obstructed.value)return;
  if (!opened.value) opener = event?.detail?.opener || (event?.currentTarget instanceof HTMLElement ? event.currentTarget : document.activeElement?.matches('a[href],button,input,textarea,select,[tabindex]') ? document.activeElement : triggerRef.value);
  opened.value = true;
  nextTick(() => {
    scrollToBottom();
    inputRef.value?.focus();
  });
}

function close() {
  opened.value = false;
  nextTick(() => (opener?.isConnected && opener.getClientRects().length ? opener : triggerRef.value)?.focus());
}

function updateViewport() {
  const viewport = window.visualViewport;
  let clearance=0;
  // Existing floating commerce controls can opt in without altering their workflow.
  document.querySelectorAll('#app [data-chat-obstacle],#app .compare-bar,#app .chatbox,#app .chat-panel,#app [class*="-fab"]').forEach(node=>{
    const box=node.getBoundingClientRect();
    if(getComputedStyle(node).position==='fixed' && box.width && box.height && box.right>window.innerWidth-88 && box.left<window.innerWidth-16 && box.bottom>0)
      clearance=Math.max(clearance,window.innerHeight-box.top+12);
  });
  viewportStyle.value = {'--ai-control-clearance':clearance+'px',...(viewport ? {'--ai-viewport-height':viewport.height+'px','--ai-viewport-bottom':Math.max(0,window.innerHeight-viewport.height-viewport.offsetTop)+'px'} : {})};
}

function checkSurroundings() {
  cancelAnimationFrame(layoutFrame);
  layoutFrame=requestAnimationFrame(()=>{
    obstructed.value=[...document.querySelectorAll('dialog[open],[aria-modal="true"]')].some(node=>!rootRef.value?.contains(node) && node.getAttribute('aria-hidden')!=='true' && node.getClientRects().length);
    if(obstructed.value)opened.value=false; // Never steal focus from a commerce/auth modal.
    updateViewport();
  });
}

// Public conversation follows SPA navigation, but never survives an identity switch.
watch(()=>user.value?.id,()=>{
  conversationGeneration++;requestController?.abort();messages.value=[];input.value='';error.value='';failedMessage.value='';pending.value=false;
});

onMounted(() => {
  window.__agriverse_open_chat = open;
  window.addEventListener('agriverse-open-ai-expert', open);
  updateViewport();
  window.visualViewport?.addEventListener('resize', updateViewport);
  window.visualViewport?.addEventListener('scroll', updateViewport);
  window.addEventListener('resize',checkSurroundings);
  surroundingsObserver=new MutationObserver(records=>{
    if(records.some(record=>!rootRef.value?.contains(record.target)))checkSurroundings();
  });
  surroundingsObserver.observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['open','aria-modal','aria-hidden','class']});
});

onUnmounted(() => {
  disposed = true;
  requestController?.abort();
  surroundingsObserver?.disconnect();cancelAnimationFrame(layoutFrame);
  window.removeEventListener('resize',checkSurroundings);
  if (window.__agriverse_open_chat === open) window.__agriverse_open_chat = null;
  window.removeEventListener('agriverse-open-ai-expert', open);
  window.visualViewport?.removeEventListener('resize', updateViewport);
  window.visualViewport?.removeEventListener('scroll', updateViewport);
});

function submit() {
  const text = input.value.trim();
  if (!text || pending.value) return;
  send(text);
}

function retryMessage() { if (failedMessage.value && !pending.value) send(failedMessage.value, true); }

async function send(text, retry = false) {
  text = text.trim();
  if (pending.value || !text || text.length > 2000) return;
  if (!retry) messages.value.push({id: ++sequence, role: 'user', content: text});
  if (messages.value.length > 40) messages.value.splice(0, messages.value.length - 40);
  error.value = '';
  failedMessage.value = '';
  input.value = '';
  pending.value = true;
  const generation=conversationGeneration;
  nextTick(scrollToBottom);
  const controller = new AbortController();
  requestController = controller;
  const timer = setTimeout(() => controller.abort(), 35000);
  try {
    const response = await fetch('/api/ai/chat', {
      method: 'POST', headers: {'Content-Type': 'application/json', Accept: 'application/json'},
      credentials: 'omit', signal: controller.signal, body: JSON.stringify({message: text}),
    });
    const data = await response.json().catch(cause => {
      if (cause.name === 'AbortError') throw cause;
      return null;
    });
    if (disposed || generation!==conversationGeneration) return;
    if (!response.ok) {
      const code = data?.code;
      if (response.status === 429) throw new Error('Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau ít phút.');
      if (response.status === 504 || code === 'AI_TIMEOUT') throw new Error('Trợ lý phản hồi quá chậm. Vui lòng thử lại sau.');
      if (response.status === 503) {
        availability.value = 'Trợ lý AI hiện chưa sẵn sàng.';
        throw new Error(availability.value);
      }
      throw new Error('Trợ lý chưa trả lời được. Vui lòng thử lại.');
    }
    if (!data || typeof data.answer !== 'string' || !data.answer.trim() || !Array.isArray(data.sources)) {
      throw new Error('Trợ lý chưa trả lời được. Vui lòng thử lại.');
    }
    if (disposed || generation!==conversationGeneration) return;
    availability.value = '';
    const sources = data.sources.filter(s => s && ['PRODUCT', 'STORE', 'FAQ'].includes(s.type)
        && typeof s.name === 'string' && s.name.length <= 300).slice(0, 34);
    messages.value.push({id: ++sequence, role: 'ai', content: data.answer, sources});
    // Keep this component session bounded; no persistent history or replay to the provider.
    if (messages.value.length > 40) messages.value.splice(0, messages.value.length - 40);
  } catch (cause) {
    if (!disposed && generation===conversationGeneration) {
      failedMessage.value = text;
      error.value = cause.name === 'AbortError' ? 'Trợ lý phản hồi quá chậm. Vui lòng thử lại sau.'
          : cause instanceof TypeError ? 'Không thể kết nối với trợ lý BonsaiMarket. Vui lòng thử lại.' : cause.message;
    }
  } finally {
    clearTimeout(timer);
    if(generation===conversationGeneration)pending.value = false;
    if (!disposed && generation===conversationGeneration) nextTick(() => {
      scrollToBottom();
      if (opened.value && (panelRef.value?.contains(document.activeElement) || document.activeElement===document.body)) inputRef.value?.focus();
    });
  }
}

function onEnter(event) {
  if (event.isComposing || event.shiftKey) return;
  event.preventDefault();
  submit();
}

function sourcePath(source) {
  if (!Number.isSafeInteger(source.id) || source.id <= 0) return null;
  if (source.type === 'PRODUCT') return `/products/${source.id}`;
  if (source.type === 'STORE') return `/stores/${source.id}`;
  return null;
}

function scrollToBottom() {
  if (scrollRef.value) scrollRef.value.scrollTop = messages.value.length || pending.value ? scrollRef.value.scrollHeight : 0;
}

</script>

<style scoped>
.ai-expert {position:fixed;right:max(24px,env(safe-area-inset-right));bottom:calc(var(--ai-viewport-bottom,0px) + max(24px,env(safe-area-inset-bottom)) + var(--ai-control-clearance,0px));z-index:85;font-family:var(--bm-font);color:var(--bm-ink);}
.ai-expert-fab {display:flex;align-items:center;justify-content:center;position:relative;width:56px;height:56px;padding:0;border:0;border-radius:50%;background:var(--bm-forest);color:white;font:600 14px var(--bm-font);cursor:pointer;box-shadow:0 6px 20px -4px rgb(11 61 43 / 35%);transition:transform var(--bm-motion-fast),background-color var(--bm-motion-fast);}
.ai-expert-fab>span {position:absolute;right:calc(100% + 12px);padding:8px 12px;border-radius:6px;white-space:nowrap;background:var(--bm-dark);color:white;opacity:0;pointer-events:none;transition:opacity var(--bm-motion-fast);}
.ai-expert-fab:is(:hover,:focus-visible)>span {opacity:1;}
.ai-expert-fab:hover {background:var(--bm-dark);transform:translateY(-2px);}
.ai-expert-fab:active {transform:scale(.97);}
.ai-expert[inert] {pointer-events:none;}
.ai-expert :is(button,a,textarea):focus-visible {outline:2px solid var(--bm-forest);outline-offset:3px;}
.ai-expert-fab svg {width:24px;height:24px;}
.ai-expert-panel {position:absolute;right:0;bottom:72px;width:400px;max-width:calc(100vw - 32px);height:600px;max-height:calc(var(--ai-viewport-height,100dvh) - 120px - var(--ai-control-clearance,0px));display:flex;flex-direction:column;overflow:auto;border:1px solid var(--bm-border);border-radius:12px;background:var(--bm-surface);box-shadow:0 16px 48px -12px rgb(11 61 43 / 24%);transform-origin:bottom right;}
.ai-expert-header {display:flex;align-items:center;gap:12px;padding:16px;background:var(--bm-dark);color:white;flex-shrink:0;}
.ai-expert-header-avatar {display:flex;align-items:center;justify-content:center;width:36px;height:36px;flex-shrink:0;}
.ai-expert-header-avatar svg {width:24px;height:24px;}
.ai-expert-header-info {flex:1;min-width:0;}
.ai-expert-header-title {font-size:16px;font-weight:700;line-height:1.4;margin:0;}
.ai-expert-header-status {font-size:13px;line-height:1.5;color:#e3eee6;margin:4px 0 0;}
.ai-expert-close,.ai-expert-send {display:flex;align-items:center;justify-content:center;min-width:44px;min-height:44px;flex-shrink:0;border:0;border-radius:6px;background:var(--bm-forest);color:white;cursor:pointer;}
.ai-expert-close {background:transparent;border:1px solid #7da38b;}
.ai-expert-close svg,.ai-expert-send svg {width:20px;height:20px;}
.ai-expert-body {flex:1;min-height:0;overflow-y:auto;padding:16px;display:flex;flex-direction:column;gap:12px;scrollbar-color:var(--bm-control-border) var(--bm-bg);}
.ai-expert-welcome {padding:8px 0;}
.ai-expert-welcome-icon {display:none;}
.ai-expert-welcome-title {font-size:18px;line-height:1.4;font-weight:600;margin:0 0 12px;}
.ai-expert-welcome-desc {font-size:14px;line-height:1.6;color:var(--bm-muted);margin:0;}
.ai-expert-suggestions {display:flex;flex-direction:column;gap:8px;margin-top:16px;}
.ai-expert-chip {text-align:left;min-height:44px;padding:10px 12px;border:1px solid var(--bm-control-border);border-radius:6px;background:white;font:inherit;font-size:14px;color:var(--bm-ink);cursor:pointer;}
.ai-expert-chip:hover {background:var(--bm-soft);}
.ai-expert-msg {display:flex;}
.ai-expert-msg-user {justify-content:flex-end;}
.ai-expert-msg-ai {justify-content:flex-start;}
.ai-expert-msg-bubble {max-width:90%;padding:12px;border-radius:8px;font-size:16px;line-height:1.5;white-space:pre-wrap;overflow-wrap:anywhere;}
.ai-expert-msg-bubble p {margin:0;}
.ai-expert-msg-user .ai-expert-msg-bubble {background:var(--bm-forest);color:white;}
.ai-expert-msg-ai .ai-expert-msg-bubble {background:var(--bm-bg);color:var(--bm-ink);}
.ai-expert-typing {display:none;}
.ai-expert-footer {padding:16px;flex-shrink:0;border-top:1px solid var(--bm-border);background:white;}
.ai-expert-label {display:block;margin-bottom:8px;color:var(--bm-ink);font-size:14px;}
.ai-expert-form {display:flex;align-items:start;gap:8px;}
.ai-expert-input {flex:1;min-width:0;min-height:64px;max-height:120px;padding:10px 12px;border:1px solid var(--bm-control-border);border-radius:6px;background:white;font:inherit;font-size:16px;color:var(--bm-ink);resize:vertical;}
.ai-expert-hint {font-size:13px;line-height:1.5;color:var(--bm-muted);margin:8px 0 0;}
.ai-expert-error {color:var(--bm-error);background:var(--bm-error-surface);padding:12px;margin:0 0 12px;border-radius:6px;font-size:14px;line-height:1.5;}
.ai-expert-error p {margin:0;}
.ai-expert-retry {min-height:44px;padding:8px 0;border:0;background:none;color:var(--bm-error);font:inherit;text-decoration:underline;text-underline-offset:4px;cursor:pointer;}
.ai-expert-sources {white-space:normal;margin:12px 0 0;padding-left:16px;}
.ai-expert-sources a {display:inline-block;min-height:24px;color:var(--bm-forest);text-decoration:underline;overflow-wrap:anywhere;}
.ai-expert-header button:focus-visible {outline-color:var(--bm-yellow) !important;}
.ai-expert button:disabled {opacity:.55;cursor:default;}
.ai-fade-enter-active,.ai-fade-leave-active {transition:opacity var(--bm-motion-fast) var(--bm-motion-ease),transform var(--bm-motion-fast) var(--bm-motion-ease);}
.ai-fade-enter-from,.ai-fade-leave-to {opacity:0;transform:translateY(8px) scale(.98);}
@media(max-width:599px){.ai-expert {right:max(16px,env(safe-area-inset-right));bottom:calc(var(--ai-viewport-bottom,0px) + max(16px,env(safe-area-inset-bottom)) + var(--ai-control-clearance,0px));}.ai-expert-fab {width:52px;height:52px;}.ai-expert-fab>span {display:none;}.ai-expert-panel {bottom:68px;width:calc(100vw - 32px);max-height:calc(var(--ai-viewport-height,100dvh) - 110px - var(--ai-control-clearance,0px));}}
@media(prefers-reduced-motion:reduce){.ai-expert *,.ai-fade-enter-active,.ai-fade-leave-active {transition:none !important;animation:none !important;}.ai-expert-fab:hover,.ai-expert-fab:active {transform:none;}}
</style>
