// Checkout chưa có font Material Symbols gốc.
// Tạm dùng ký tự Unicode cục bộ tại vị trí icon cũ đến khi có font.
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
    const root = document.getElementById('app');
    if (!root) return () => {};
    const apply = icon => {
            if (!icon?.matches?.('.material-symbols-outlined')) return;
            const name = icon.textContent.trim();
            if (!/^[a-z][a-z0-9_]*$/.test(name)) return;
            icon.dataset.iconName = name;
            icon.textContent = glyphs[name] || '◆';
            icon.setAttribute('aria-label', name.replaceAll('_', ' '));
    };
    const scan = node => {
        apply(node);
        node.querySelectorAll?.('.material-symbols-outlined').forEach(apply);
    };
    scan(root);
    const observer = new MutationObserver(records => {
        const added = new Set();
        for (const record of records) {
            apply(record.target.nodeType === 3 ? record.target.parentElement : record.target);
            for (const node of record.addedNodes) if (node.nodeType === 1) added.add(node);
        }
        // A parent and its children may occur in the same batch. Visit their subtree once.
        for (const node of added) {
            if (!root.contains(node)) continue;
            let parent = node.parentElement;
            while (parent && !added.has(parent)) parent = parent.parentElement;
            if (!parent) scan(node);
        }
    });
    observer.observe(root, {
        childList: true,
        characterData: true,
        subtree: true
    });
    return () => observer.disconnect();
}
