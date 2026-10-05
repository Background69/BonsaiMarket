// The original Material Symbols font is absent from this checkout. Keep the
// legacy icon positions usable with local Unicode glyphs until that asset arrives.
const glyphs = {
  eco: '❧', favorite: '♡', shopping_bag: '▣', chat: '☏', expand_more: '⌄',
  menu: '☰', close: '×', arrow_forward: '→', arrow_back: '←',
  chevron_right: '›', chevron_left: '‹', search: '⌕', filter_list: '☷',
  category: '▦', inventory_2: '▣', payments: '₫', add_shopping_cart: '+',
  person: '♙', storefront: '⌂', store: '⌂', dashboard: '▤',
  receipt_long: '▤', notifications: '♧', local_shipping: '⇢',
  verified: '✓', check: '✓', done: '✓', info: 'ⓘ', warning: '!',
  sensors: '✦', ecg_heart: '♥', forest: '♣', potted_plant: '♣',
  spa: '✿', yard: '❦', water_drop: '◉', light_mode: '☼',
  analytics: '▥', box_edit: '▣', psychology: '✧', support_agent: '☎',
  search_insights: '⌕', compare_arrows: '⇄', view_in_ar: '◇',
  star: '☆', star_rate: '★', home: '⌂', garden_cart: '❧',
  public: '◎', language: '◎', location_on: '⌖', call: '☎',
  mail: '✉', account_circle: '♙', logout: '↪', delete: '×',
  edit: '✎', add: '+', remove: '−', calendar_today: '□',
  schedule: '◷', trending_up: '↗', share: '↗', visibility: '◉',
  qr_code_2: '▦', biotech: '✿', energy_savings_leaf: '❧',
  contact_support: '?', quiz: '?', loyalty: '◇', forum: '☷',
  article: '▤', handshake: '◇', flash_on: 'ϟ', settings: '⚙',
};

export function restoreLegacyIcons() {
  const apply = () => {
    document.querySelectorAll('.material-symbols-outlined').forEach(icon => {
      const name = icon.textContent.trim();
      if (!/^[a-z][a-z0-9_]*$/.test(name)) return;
      icon.dataset.iconName = name;
      icon.textContent = glyphs[name] || '◆';
      icon.setAttribute('aria-label', name.replaceAll('_', ' '));
    });
  };
  apply();
  new MutationObserver(apply).observe(document.getElementById('app'), { childList: true, characterData: true, subtree: true });
}
