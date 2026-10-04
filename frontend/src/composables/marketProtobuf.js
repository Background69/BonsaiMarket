// Bộ mã hóa / giải mã Protobuf tối giản cho MarketMessage (scheme proto.MarketMessage).
// Không cần dependency protobufjs — tự quản lý wire format (varint / length-delimited).
//
// Message fields:
//   1 eventType string, 2 action string, 3 from string, 4 fromName string,
//   5 to string, 6 payload string, 7 timestamp int64 (varint).

const encoder = new TextEncoder();
const decoder = new TextDecoder();

class Writer {
  constructor() {
    this.chunks = [];
    this.len = 0;
  }
  raw(bytes) {
    this.chunks.push(bytes);
    this.len += bytes.length;
    return this;
  }
  varint(value) {
    value = typeof value === 'bigint' ? value : BigInt(value || 0);
    const bytes = [];
    let v = value;
    while (v > 127n) {
      bytes.push(Number(v & 127n) | 0x80);
      v >>= 7n;
    }
    bytes.push(Number(v));
    return this.raw(new Uint8Array(bytes));
  }
  tag(field, wire) {
    return this.varint(BigInt((field << 3) | wire));
  }
  str(field, value) {
    if (value === undefined || value === null || value === '') return this;
    const s = encoder.encode(String(value));
    return this.tag(field, 2).varint(BigInt(s.length)).raw(s);
  }
  int64(field, value) {
    if (value === undefined || value === null) return this;
    return this.tag(field, 0).varint(BigInt(value));
  }
  concat() {
    const out = new Uint8Array(this.len);
    let off = 0;
    for (const c of this.chunks) {
      out.set(c, off);
      off += c.length;
    }
    return out;
  }
}

export function encodeMarketMessage(msg) {
  const w = new Writer();
  w.str(1, msg.eventType);
  w.str(2, msg.action);
  w.str(3, msg.from);
  w.str(4, msg.fromName);
  w.str(5, msg.to);
  w.str(6, msg.payload);
  w.int64(7, msg.timestamp);
  return w.concat();
}

export function decodeMarketMessage(bytes) {
  let offset = 0;
  const data = { eventType: '', action: '', from: '', fromName: '', to: '', payload: '', timestamp: null };

  const readVarint = () => {
    let result = 0n;
    let shift = 0n;
    while (offset < bytes.length) {
      const b = bytes[offset++];
      result |= BigInt(b & 0x7f) << shift;
      if (!(b & 0x80)) break;
      shift += 7n;
    }
    return result;
  };

  while (offset < bytes.length) {
    const tag = readVarint();
    const field = Number(tag >> 3n);
    const wire = Number(tag & 7n);
    if (wire === 0) {
      const val = readVarint();
      if (field === 7) data.timestamp = val;
    } else if (wire === 2) {
      const len = Number(readVarint());
      const strBytes = bytes.subarray(offset, offset + len);
      const str = decoder.decode(strBytes);
      if (field === 1) data.eventType = str;
      else if (field === 2) data.action = str;
      else if (field === 3) data.from = str;
      else if (field === 4) data.fromName = str;
      else if (field === 5) data.to = str;
      else if (field === 6) data.payload = str;
      offset += len;
    } else {
      break;
    }
  }
  return data;
}

export function parsePayload(msg) {
  if (!msg.payload) return {};
  try {
    return typeof msg.payload === 'string' ? JSON.parse(msg.payload) : msg.payload;
  } catch {
    return {};
  }
}