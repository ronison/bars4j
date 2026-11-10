import { qrSegment } from "./qr-segment.ts";
import type { QrSegment } from "./qr-segment.ts";
import { QrMode } from "./qr-mode.ts";

/**
 * Turns input text into a {@link QrSegment}, choosing the densest single data mode able to
 * represent the whole string.
 *
 * Port of {@code org.bars4j.encode.matrix.qr.QrSegmentEncoder}.
 */

/** The byte-mode charsets supported by the encoder. */
export type QrCharset = "ISO-8859-1" | "UTF-8";

/** Characters of alphanumeric mode, listed in their 6-bit code order (ISO/IEC 18004). */
export const ALPHANUMERIC_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";

/** Lower and upper Shift-JIS code ranges that Kanji mode can represent. */
const KANJI_SJIS_FIRST = 0x8140;
const KANJI_SJIS_FIRST_LAST = 0x9ffc;
const KANJI_SJIS_SECOND = 0xe040;
const KANJI_SJIS_SECOND_LAST = 0xebbf;

/** Reverse lookup for {@link ALPHANUMERIC_CHARSET}; negative where the character is absent. */
const ALPHANUMERIC_CODE = new Int32Array(128).fill(-1);
for (let i = 0; i < ALPHANUMERIC_CHARSET.length; i++) {
  ALPHANUMERIC_CODE[ALPHANUMERIC_CHARSET.charCodeAt(i)] = i;
}

/**
 * Reverse Shift-JIS lookup for Kanji mode, built once by decoding every valid two-byte pair.
 * Node exposes no built-in Shift-JIS <em>encoder</em>, but the full-ICU {@code TextDecoder} can
 * decode it, so we invert that mapping. Characters that Node maps to more than one Shift-JIS pair
 * keep the first pair seen, which may differ from the JDK's canonical choice in rare cases.
 */
const SHIFT_JIS_REVERSE = new Map<number, number>();

function buildShiftJisReverse(): void {
  const decoder = new TextDecoder("shift_jis", { fatal: true });
  const leads: number[] = [];
  for (let b = 0x81; b <= 0x9f; b++) {
    leads.push(b);
  }
  for (let b = 0xe0; b <= 0xef; b++) {
    leads.push(b);
  }
  for (const lead of leads) {
    for (let trail = 0x40; trail <= 0xfc; trail++) {
      if (trail === 0x7f) {
        continue;
      }
      let decoded: string;
      try {
        decoded = decoder.decode(new Uint8Array([lead, trail]));
      } catch {
        continue;
      }
      if (decoded.length === 1) {
        const codePoint = decoded.charCodeAt(0);
        if (!SHIFT_JIS_REVERSE.has(codePoint)) {
          SHIFT_JIS_REVERSE.set(codePoint, (lead << 8) | trail);
        }
      }
    }
  }
}

buildShiftJisReverse();

/** Charset used by byte mode when the caller does not choose one. */
export function defaultCharset(): QrCharset {
  return "ISO-8859-1";
}

/** Encodes text in the requested charset, mirroring {@code String.getBytes(charset)}. */
export function encodeText(text: string, charset: QrCharset): Uint8Array {
  if (charset === "UTF-8") {
    return new Uint8Array(Buffer.from(text, "utf8"));
  }
  const bytes = new Uint8Array(text.length);
  for (let i = 0; i < text.length; i++) {
    const code = text.charCodeAt(i);
    bytes[i] = code <= 0xff ? code : 0x3f;
  }
  return bytes;
}

export class QrSegmentEncoder {
  private constructor() {}

  static encode(text: string, charset: QrCharset): QrSegment {
    if (isAllDigits(text)) {
      return numericSegment(text);
    }
    if (isAllKanji(text)) {
      return kanjiSegment(text);
    }
    if (isAllAlphanumeric(text)) {
      return alphanumericSegment(text);
    }
    return byteSegment(text, charset);
  }
}

function isAllDigits(text: string): boolean {
  if (text.length === 0) {
    return false;
  }
  for (let i = 0; i < text.length; i++) {
    const c = text.charCodeAt(i);
    if (c < 0x30 || c > 0x39) {
      return false;
    }
  }
  return true;
}

function isAllAlphanumeric(text: string): boolean {
  if (text.length === 0) {
    return false;
  }
  for (let i = 0; i < text.length; i++) {
    if (alphanumericCode(text.charCodeAt(i)) < 0) {
      return false;
    }
  }
  return true;
}

/** True when every character is a two-byte Shift-JIS value inside the Kanji ranges. */
function isAllKanji(text: string): boolean {
  if (text.length === 0) {
    return false;
  }
  for (let i = 0; i < text.length; i++) {
    if (!isKanjiValue(shiftJisValue(text.charCodeAt(i)))) {
      return false;
    }
  }
  return true;
}

function alphanumericCode(codeUnit: number): number {
  return codeUnit < ALPHANUMERIC_CODE.length ? ALPHANUMERIC_CODE[codeUnit] : -1;
}

function shiftJisValue(codeUnit: number): number {
  const sjis = SHIFT_JIS_REVERSE.get(codeUnit);
  return sjis === undefined ? -1 : sjis;
}

function isKanjiValue(code: number): boolean {
  return (
    (code >= KANJI_SJIS_FIRST && code <= KANJI_SJIS_FIRST_LAST) ||
    (code >= KANJI_SJIS_SECOND && code <= KANJI_SJIS_SECOND_LAST)
  );
}

function numericSegment(text: string): QrSegment {
  const length = text.length;
  const groups = Math.trunc((length + 2) / 3);
  const values = new Array<number>(groups);
  const widths = new Array<number>(groups);
  for (let g = 0; g < groups; g++) {
    const from = g * 3;
    const to = Math.min(from + 3, length);
    let packed = 0;
    for (let i = from; i < to; i++) {
      packed = packed * 10 + (text.charCodeAt(i) - 0x30);
    }
    const digits = to - from;
    values[g] = packed;
    widths[g] = digits === 1 ? 4 : digits === 2 ? 7 : 10;
  }
  return qrSegment(QrMode.NUMERIC, values, widths, length);
}

function alphanumericSegment(text: string): QrSegment {
  const length = text.length;
  const pairs = Math.trunc(length / 2);
  const extra = length % 2;
  const values = new Array<number>(pairs + extra);
  const widths = new Array<number>(pairs + extra);
  let index = 0;
  for (let p = 0; p < pairs; p++) {
    values[index] =
      alphanumericCode(text.charCodeAt(2 * p)) * 45 + alphanumericCode(text.charCodeAt(2 * p + 1));
    widths[index++] = 11;
  }
  if (extra === 1) {
    values[index] = alphanumericCode(text.charCodeAt(length - 1));
    widths[index] = 6;
  }
  return qrSegment(QrMode.ALPHANUMERIC, values, widths, length);
}

function kanjiSegment(text: string): QrSegment {
  const length = text.length;
  const values = new Array<number>(length);
  const widths = new Array<number>(length);
  for (let i = 0; i < length; i++) {
    const code = shiftJisValue(text.charCodeAt(i));
    const base = code <= KANJI_SJIS_FIRST_LAST ? KANJI_SJIS_FIRST : 0xc140;
    const adjusted = code - base;
    values[i] = (adjusted >>> 8) * 0xc0 + (adjusted & 0xff);
    widths[i] = 13;
  }
  return qrSegment(QrMode.KANJI, values, widths, length);
}

function byteSegment(text: string, charset: QrCharset): QrSegment {
  const encoded = encodeText(text, charset);
  const values = new Array<number>(encoded.length);
  const widths = new Array<number>(encoded.length);
  for (let i = 0; i < encoded.length; i++) {
    values[i] = encoded[i] & 0xff;
    widths[i] = 8;
  }
  return qrSegment(QrMode.BYTE, values, widths, encoded.length);
}
