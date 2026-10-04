export function formatPrice(price) {
  const num = Number(price);
  if (isNaN(num) || !isFinite(num)) return '0';
  return new Intl.NumberFormat('vi-VN').format(num);
}

export const statusLabels = {
  pending: 'Chờ xác nhận', confirmed: 'Đã xác nhận', shipping: 'Đang giao',
  delivered: 'Đã giao', completed: 'Hoàn thành', cancelled: 'Đã hủy', refunded: 'Đã hoàn tiền',
  awaiting_payment: 'Chờ thanh toán',
  published: 'Đã đăng', draft: 'Nháp', archived: 'Lưu trữ',
  active: 'Hoạt động', inactive: 'Tạm ngưng', suspended: 'Khóa',
  paid: 'Đã TT', failed: 'Thất bại',
  processing: 'Đang xử lý',
  percent: '%', fixed: 'VNĐ',
};

export const statusClasses = {
  pending: 'bg-amber-50 text-amber-600', confirmed: 'bg-blue-50 text-blue-600',
  shipping: 'bg-sky-50 text-sky-600', delivered: 'bg-indigo-50 text-indigo-600',
  completed: 'bg-emerald-50 text-emerald-600', cancelled: 'bg-red-50 text-red-600',
  refunded: 'bg-purple-50 text-purple-600', awaiting_payment: 'bg-orange-50 text-orange-600',
  approved: 'bg-emerald-50 text-emerald-600', rejected: 'bg-red-50 text-red-600',
  published: 'bg-emerald-50 text-emerald-600',
  draft: 'bg-amber-50 text-amber-600', archived: 'bg-stone-100 text-stone-600',
  active: 'bg-emerald-50 text-emerald-600', inactive: 'bg-amber-50 text-amber-600',
  suspended: 'bg-red-50 text-red-600', paid: 'bg-emerald-50 text-emerald-600',
  failed: 'bg-red-50 text-red-600', processing: 'bg-blue-50 text-blue-600',
};

export function statusLabel(status) {
  return statusLabels[status] || status;
}

export function statusClass(status) {
  return statusClasses[status] || 'bg-stone-100 text-stone-600';
}

/* --------------------------------------------------------------
   Parse emoji-marked plant description into structured info.
   Format examples:
     🌿 *Tên khoa học:* Juniperus spp.
     ☀️ *Ánh sáng:* Nhiều nắng
     💰 *Ý nghĩa phong thủy:* Trường thọ
   Returns { identity, care, fengshui, cleanDesc }
   -------------------------------------------------------------- */
const plantEmojiMap = {
  '🌿': { label: 'Tên khoa học', icon: 'biotech' },
  '🏷️': { label: 'Họ thực vật', icon: 'category' },
  '🌍': { label: 'Nguồn gốc', icon: 'public' },
  '☀️': { label: 'Ánh sáng', icon: 'light_mode' },
  '💧': { label: 'Tưới nước', icon: 'water_drop' },
  '🧪': { label: 'Phân bón', icon: 'nutrition' },
  '🌡️': { label: 'Nhiệt độ tối thiểu', icon: 'thermostat' },
  '🧫': { label: 'Độ pH đất', icon: 'science' },
  '🏠': { label: 'Vị trí', icon: 'home' },
  '💰': null,
};

const plantIdentityKeys = ['Tên khoa học', 'Họ thực vật', 'Nguồn gốc'];

function parseEmojiValue(line) {
  const match = line.match(/:[\s*]+(.+)$/);
  return match ? match[1].trim() : '';
}

export function parsePlantDescription(desc) {
  if (!desc) return { identity: [], care: [], fengshui: '', cleanDesc: '' };
  const lines = String(desc).split('\n');
  const identity = [];
  const care = [];
  let fengshui = '';
  let inDesc = false;
  const descParts = [];

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) continue;
    if (trimmed === '---') { inDesc = true; continue; }
    if (trimmed.startsWith('**📋')) continue;

    if (trimmed.startsWith('**📝')) {
      inDesc = true;
      const rest = trimmed.replace(/^\*\*📝\s*Mô\s*tả:\s*\*\*/, '').trim();
      if (rest) descParts.push(rest);
      continue;
    }

    let parsed = false;
    for (const [emoji, map] of Object.entries(plantEmojiMap)) {
      if (!trimmed.startsWith(emoji)) continue;
      if (map === null) {
        fengshui = parseEmojiValue(trimmed.replace(/^💰\s*\*?/, ''));
        parsed = true;
        break;
      }
      const val = parseEmojiValue(trimmed);
      if (!val) continue;
      const item = { label: map.label, value: val, icon: map.icon };
      if (plantIdentityKeys.includes(map.label)) {
        identity.push(item);
      } else {
        care.push(item);
      }
      parsed = true;
      break;
    }
    if (!parsed && inDesc) {
      descParts.push(trimmed);
    }
  }
  return { identity, care, fengshui, cleanDesc: descParts.join(' ') };
}

/* Clean a raw emoji-marked description for plain-text display
   (strip emoji glyphs, markdown stars and list markers). */
export function cleanPlantText(desc) {
  if (!desc) return '';
  return String(desc)
    .replace(/\*\*/g, '')
    .replace(/^[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}\u{FE0F}]\s*/gmu, '')
    .replace(/---/g, '')
    .split('\n')
    .map(l => l.trim())
    .filter(l => l)
    .join('\n');
}
