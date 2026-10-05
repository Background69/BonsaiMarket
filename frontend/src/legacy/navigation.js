import { defineComponent, h, reactive } from 'vue';
import { RouterLink } from 'vue-router';
import appRouter from '../router/index.js';

const page = reactive({ props: { auth: { user: null }, cartCount: 0 }, url: window.location.pathname });
appRouter.afterEach(to => { page.url = to.fullPath; });
export function usePage() { return page; }

export const Link = defineComponent({
  name: 'LegacyLink',
  props: { href: { type: String, default: '/' }, method: String, as: String },
  setup(props, { attrs, slots }) {
    return () => props.method && props.method !== 'get'
      ? h('button', { ...attrs, type: 'button', onClick: () => notifyPending() }, slots.default?.())
      : h(RouterLink, { ...attrs, to: props.href }, slots);
  },
});

function notifyPending() { window.dispatchEvent(new CustomEvent('bonsai:pending')); }
function visit(url, data = {}) {
  const query = Object.fromEntries(Object.entries(data || {}).filter(([, value]) => value !== '' && value !== null && value !== undefined));
  appRouter.push({ path: url.split('?')[0], query: { ...Object.fromEntries(new URLSearchParams(url.split('?')[1] || '')), ...query } });
}
function unsupported(_url, _data, options = {}) {
  notifyPending();
  options.onError?.({ message: 'Chức năng đang được phát triển.' });
  options.onFinish?.();
}
export const router = {
  visit,
  get(url, data = {}) {
    const isOptions = Object.keys(data).every(key => ['preserveState', 'preserveScroll', 'replace', 'only'].includes(key));
    visit(url, isOptions ? {} : data);
  },
  post: unsupported, put: unsupported, patch: unsupported, delete: unsupported,
};
