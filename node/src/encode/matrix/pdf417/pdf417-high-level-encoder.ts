import { UnsupportedTextException } from "../../api/unsupported-text-exception.ts";
import { Pdf417Compaction } from "./pdf417-compaction.ts";

/**
 * High-level encoder for PDF417, implementing the compaction schemes of Annex P of ISO/IEC 15438.
 *
 * <p>A message is turned into a stream of codewords (0-928). Text, Byte and Numeric compaction are
 * supported, alone or blended by {@link Pdf417Compaction.AUTO}. When the configured character set
 * is not the default, an Extended Channel Interpretation is announced before the first codeword.
 *
 * <p>The automatic scheme first splits the message into stretches that suit one compaction each,
 * then writes those stretches out. Splitting and writing are kept separate so that the
 * "which scheme fits" decision is independent of the codeword bookkeeping.
 *
 * Port of {@code org.bars4j.encode.matrix.pdf417.Pdf417HighLevelEncoder}.
 */

/** Latch codeword selecting Text compaction. */
const TEXT_LATCH = 900;
/** Latch codeword selecting Byte compaction for a byte count that is not a multiple of six. */
const BYTE_LATCH_PADDED = 901;
/** Latch codeword selecting Numeric compaction. */
const NUMERIC_LATCH = 902;
/** Shift codeword writing a single byte without leaving Text compaction. */
const BYTE_SHIFT = 913;
/** Latch codeword selecting Byte compaction for a byte count that is a multiple of six. */
const BYTE_LATCH_ALIGNED = 924;
/** Codeword introducing an Extended Channel Interpretation designator. */
const ECI_MARKER = 927;

/** Codewords in a Text compaction group are base 30, with this value used as the odd pad. */
const TEXT_RADIX = 30;
const TEXT_PAD = 29;

/** Largest number of digits combined into one Numeric compaction group. */
const NUMERIC_GROUP_DIGITS = 44;

/** Number of bytes that share a base-900 Byte compaction group. */
const BYTE_GROUP_SIZE = 6;
/** Codewords produced by one Byte compaction group. */
const BYTE_GROUP_CODEWORDS = 5;
/** Radix shared by Numeric and Byte compaction groups. */
const GROUP_RADIX = 900;

/** Characters reachable from the "Mixed" sub-mode, indexed by their compaction code. */
const MIXED_SUBMODE_CHARS = [
  48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 38, 13, 9, 44, 58,
  35, 45, 46, 36, 47, 43, 37, 42, 61, 94, 0, 32, 0, 0, 0,
];

/** Characters reachable from the "Punctuation" sub-mode, indexed by their compaction code. */
const PUNCTUATION_SUBMODE_CHARS = [
  59, 60, 62, 64, 91, 92, 93, 95, 96, 126, 33, 13, 9, 44, 58,
  10, 45, 46, 36, 47, 34, 124, 42, 40, 41, 63, 123, 125, 39, 0,
];

/** Reverse lookups from an ASCII character to its compaction code, or -1. */
const MIXED_CODE = new Array<number>(128).fill(-1);
const PUNCTUATION_CODE = new Array<number>(128).fill(-1);

indexCodes(MIXED_SUBMODE_CHARS, MIXED_CODE);
indexCodes(PUNCTUATION_SUBMODE_CHARS, PUNCTUATION_CODE);

/**
 * Minimal character-set abstraction matching the parts of {@code java.nio.charset.Charset} used by
 * the high-level encoder.
 */
export interface Pdf417Charset {
  /** Canonical charset name, as reported by {@code Charset.name()}. */
  readonly name: string;
  /** Whether a single UTF-16 code unit can be represented by this charset. */
  canEncode(charCode: number): boolean;
  /** Encodes text into bytes, replacing unrepresentable characters with '?'. */
  encode(text: string): Uint8Array;
}

/**
 * ISO-8859-1 (Latin-1) charset: every UTF-16 code unit up to 0xFF maps to a single byte, and
 * anything above is replaced by '?' (0x3F).
 */
export const ISO_8859_1: Pdf417Charset = {
  name: "ISO-8859-1",
  canEncode(charCode: number): boolean {
    return charCode <= 0xff;
  },
  encode(text: string): Uint8Array {
    const bytes = new Uint8Array(text.length);
    for (let i = 0; i < text.length; i++) {
      const code = text.charCodeAt(i);
      bytes[i] = code <= 0xff ? code : 0x3f;
    }
    return bytes;
  },
};

/** The four Text compaction sub-modes. */
const TextSubmode = {
  UPPER: 0,
  LOWER: 1,
  MIXED: 2,
  PUNCTUATION: 3,
} as const;
type TextSubmode = (typeof TextSubmode)[keyof typeof TextSubmode];

/** The three compaction schemes, used as the state carried between stretches of a message. */
const Scheme = {
  TEXT: 0,
  BYTE: 1,
  NUMERIC: 2,
} as const;
type Scheme = (typeof Scheme)[keyof typeof Scheme];

/** A stretch of the message that will be written with a single compaction scheme. */
interface Stretch {
  readonly scheme: Scheme;
  readonly start: number;
  readonly length: number;
}

/**
 * Encodes a message into PDF417 high-level codewords.
 *
 * @param msg        the message, must not be empty
 * @param compaction the compaction scheme to apply
 * @param encoding   character set used for Text/Byte compaction, {@code null} for ISO-8859-1
 * @return the codewords as numbers in the range 0-928
 * @throws UnsupportedTextException if the message is empty or contains characters that cannot be
 *                                  represented with the selected character set
 */
export function encodeHighLevel(
  msg: string,
  compaction: Pdf417Compaction | null,
  encoding: Pdf417Charset | null,
): number[] {
  if (msg.length === 0) {
    throw new UnsupportedTextException("Empty message not allowed");
  }
  const charset = encoding !== null ? encoding : ISO_8859_1;

  const codewords: number[] = [];
  if (charset.name !== ISO_8859_1.name) {
    const designator = designatorFor(charset);
    if (designator !== null) {
      appendEci(designator, codewords);
    }
  }

  const mode = compaction !== null ? compaction : Pdf417Compaction.AUTO;
  switch (mode) {
    case Pdf417Compaction.TEXT:
      appendText(msg, 0, msg.length, codewords, TextSubmode.UPPER);
      break;
    case Pdf417Compaction.NUMERIC:
      codewords.push(NUMERIC_LATCH);
      appendNumeric(msg, 0, msg.length, codewords);
      break;
    case Pdf417Compaction.BYTE:
      appendBytes(codewords, charset.encode(msg), Scheme.BYTE);
      break;
    default:
      appendAutomatic(msg, charset, codewords);
      break;
  }
  return codewords;
}

/**
 * Splits the message into stretches and writes them with the scheme best suited to each.
 */
function appendAutomatic(msg: string, charset: Pdf417Charset, out: number[]): void {
  const stretches = split(msg, charset);
  let previous: Scheme = Scheme.TEXT;
  let submode: TextSubmode = TextSubmode.UPPER;

  for (const stretch of stretches) {
    switch (stretch.scheme) {
      case Scheme.NUMERIC:
        out.push(NUMERIC_LATCH);
        appendNumeric(msg, stretch.start, stretch.length, out);
        previous = Scheme.NUMERIC;
        submode = TextSubmode.UPPER;
        break;
      case Scheme.TEXT:
        if (previous !== Scheme.TEXT) {
          out.push(TEXT_LATCH);
          submode = TextSubmode.UPPER;
        }
        submode = appendText(msg, stretch.start, stretch.length, out, submode);
        previous = Scheme.TEXT;
        break;
      default: {
        // BYTE
        const bytes = charset.encode(msg.substring(stretch.start, stretch.start + stretch.length));
        if (bytes.length === 1 && previous === Scheme.TEXT) {
          out.push(BYTE_SHIFT);
          out.push(bytes[0] & 0xff);
        } else {
          appendBytes(out, bytes, previous);
          previous = Scheme.BYTE;
          submode = TextSubmode.UPPER;
        }
        break;
      }
    }
  }
}

/**
 * Divides the message into stretches, greedily preferring Numeric, then Text, then Byte.
 *
 * @throws UnsupportedTextException if a character cannot be represented
 */
function split(msg: string, charset: Pdf417Charset): Stretch[] {
  const stretches: Stretch[] = [];
  const length = msg.length;
  let pos = 0;
  while (pos < length) {
    const digits = countLeadingDigits(msg, pos);
    if (digits >= 13) {
      stretches.push({ scheme: Scheme.NUMERIC, start: pos, length: digits });
      pos += digits;
      continue;
    }
    const text = countTextRun(msg, pos);
    if (text >= 5 || digits === length) {
      stretches.push({ scheme: Scheme.TEXT, start: pos, length: text });
      pos += text;
    } else {
      let bytes = countByteRun(msg, pos, charset);
      if (bytes === 0) {
        bytes = 1;
      }
      stretches.push({ scheme: Scheme.BYTE, start: pos, length: bytes });
      pos += bytes;
    }
  }
  return stretches;
}

/**
 * Writes a run of characters using Text compaction.
 *
 * @return the sub-mode reached at the end, so a following text run can continue without
 *         re-latching
 */
function appendText(
  msg: string,
  start: number,
  count: number,
  out: number[],
  initial: TextSubmode,
): TextSubmode {
  const codes: number[] = [];
  let submode = initial;
  let index = 0;
  while (true) {
    const ch = msg.charCodeAt(start + index);
    switch (submode) {
      case TextSubmode.UPPER:
        if (isUpper(ch)) {
          codes.push(ch === 32 ? 26 : ch - 65);
        } else if (isLower(ch)) {
          submode = TextSubmode.LOWER;
          codes.push(27);
          continue;
        } else if (isMixed(ch)) {
          submode = TextSubmode.MIXED;
          codes.push(28);
          continue;
        } else {
          codes.push(29);
          codes.push(PUNCTUATION_CODE[ch]);
        }
        break;
      case TextSubmode.LOWER:
        if (isLower(ch)) {
          codes.push(ch === 32 ? 26 : ch - 97);
        } else if (isUpper(ch)) {
          codes.push(27);
          codes.push(ch - 65);
        } else if (isMixed(ch)) {
          submode = TextSubmode.MIXED;
          codes.push(28);
          continue;
        } else {
          codes.push(29);
          codes.push(PUNCTUATION_CODE[ch]);
        }
        break;
      case TextSubmode.MIXED:
        if (isMixed(ch)) {
          codes.push(MIXED_CODE[ch]);
        } else if (isUpper(ch)) {
          submode = TextSubmode.UPPER;
          codes.push(28);
          continue;
        } else if (isLower(ch)) {
          submode = TextSubmode.LOWER;
          codes.push(27);
          continue;
        } else if (start + index + 1 < count && isPunctuation(msg.charCodeAt(start + index + 1))) {
          submode = TextSubmode.PUNCTUATION;
          codes.push(25);
          continue;
        } else {
          codes.push(29);
          codes.push(PUNCTUATION_CODE[ch]);
        }
        break;
      default:
        // PUNCTUATION
        if (isPunctuation(ch)) {
          codes.push(PUNCTUATION_CODE[ch]);
        } else {
          submode = TextSubmode.UPPER;
          codes.push(29);
          continue;
        }
        break;
    }
    index++;
    if (index >= count) {
      break;
    }
  }
  packBase30(codes, out);
  return submode;
}

/** Packs base-30 text codes two at a time into codewords, padding an odd run with 29. */
function packBase30(codes: number[], out: number[]): void {
  const count = codes.length;
  for (let i = 0; i + 1 < count; i += 2) {
    out.push(codes[i] * TEXT_RADIX + codes[i + 1]);
  }
  if (count % 2 !== 0) {
    out.push(codes[count - 1] * TEXT_RADIX + TEXT_PAD);
  }
}

/** Writes a run of digits using Numeric compaction, in groups of at most 44 digits. */
function appendNumeric(msg: string, start: number, count: number, out: number[]): void {
  const radix = BigInt(GROUP_RADIX);
  let index = 0;
  while (index < count) {
    const take = Math.min(NUMERIC_GROUP_DIGITS, count - index);
    let value = BigInt("1" + msg.substring(start + index, start + index + take));
    const group: number[] = [];
    do {
      group.push(Number(value % radix));
      value = value / radix;
    } while (value !== 0n);
    for (let i = group.length - 1; i >= 0; i--) {
      out.push(group[i]);
    }
    index += take;
  }
}

/**
 * Writes bytes using Byte compaction.
 *
 * @param previous the scheme in effect beforehand; a lone byte may be shifted from Text
 *                 compaction instead of latching to Byte compaction
 */
function appendBytes(out: number[], bytes: Uint8Array, previous: Scheme): void {
  if (bytes.length === 1 && previous === Scheme.TEXT) {
    out.push(BYTE_SHIFT);
  } else if (bytes.length % BYTE_GROUP_SIZE === 0) {
    out.push(BYTE_LATCH_ALIGNED);
  } else {
    out.push(BYTE_LATCH_PADDED);
  }

  let index = 0;
  while (bytes.length - index >= BYTE_GROUP_SIZE) {
    let packed = 0;
    for (let i = 0; i < BYTE_GROUP_SIZE; i++) {
      packed = packed * 256 + (bytes[index + i] & 0xff);
    }
    const group = new Array<number>(BYTE_GROUP_CODEWORDS);
    for (let i = 0; i < BYTE_GROUP_CODEWORDS; i++) {
      group[i] = packed % GROUP_RADIX;
      packed = Math.trunc(packed / GROUP_RADIX);
    }
    for (let i = BYTE_GROUP_CODEWORDS - 1; i >= 0; i--) {
      out.push(group[i]);
    }
    index += BYTE_GROUP_SIZE;
  }
  for (; index < bytes.length; index++) {
    out.push(bytes[index] & 0xff);
  }
}

/** Writes the ECI designator sequence for a character set. */
function appendEci(designator: number, out: number[]): void {
  if (designator >= 0 && designator < GROUP_RADIX) {
    out.push(ECI_MARKER);
    out.push(designator);
  } else {
    throw new Error("ECI designator out of range: " + designator);
  }
}

/** Maps a character set to its ECI designator, or {@code null} when it has none here. */
function designatorFor(charset: Pdf417Charset): number | null {
  const name = charset.name;
  if (name === "UTF-8") {
    return 26;
  }
  if (name === "US-ASCII") {
    return 27;
  }
  if (name === "UTF-16BE") {
    return 25;
  }
  for (let part = 1; part <= 16; part++) {
    if (part === 12 || part === 14) {
      continue; // no such ISO-8859 part exists
    }
    if (name === "ISO-8859-" + part) {
      return part === 1 ? 1 : part + 2; // ECI 1 is ISO-8859-1, then ECI 4 onward
    }
  }
  if (name === "windows-1250") {
    return 21;
  }
  if (name === "windows-1251") {
    return 22;
  }
  if (name === "windows-1252") {
    return 23;
  }
  if (name === "windows-1256") {
    return 24;
  }
  return null;
}

/** Fills a reverse lookup table from compaction codes to ASCII characters. */
function indexCodes(characters: readonly number[], codes: number[]): void {
  for (let code = 0; code < characters.length; code++) {
    if (characters[code] > 0) {
      codes[characters[code]] = code;
    }
  }
}

function isDigit(ch: number): boolean {
  return ch >= 48 && ch <= 57;
}

function isUpper(ch: number): boolean {
  return ch === 32 || (ch >= 65 && ch <= 90);
}

function isLower(ch: number): boolean {
  return ch === 32 || (ch >= 97 && ch <= 122);
}

function isMixed(ch: number): boolean {
  return ch < MIXED_CODE.length && MIXED_CODE[ch] !== -1;
}

function isPunctuation(ch: number): boolean {
  return ch < PUNCTUATION_CODE.length && PUNCTUATION_CODE[ch] !== -1;
}

function isText(ch: number): boolean {
  return ch === 9 || ch === 10 || ch === 13 || (ch >= 32 && ch <= 126);
}

/** Number of consecutive digits starting at a position. */
function countLeadingDigits(msg: string, start: number): number {
  let count = 0;
  while (start + count < msg.length && isDigit(msg.charCodeAt(start + count))) {
    count++;
  }
  return count;
}

/**
 * Length of the run that Text compaction can cover starting at a position.
 *
 * <p>Digits count as text in short runs, but a run of thirteen or more digits belongs to
 * Numeric compaction, so the run ends before it.
 */
function countTextRun(msg: string, start: number): number {
  const length = msg.length;
  let index = start;
  while (index < length) {
    let digits = 0;
    while (digits < 13 && index < length && isDigit(msg.charCodeAt(index))) {
      digits++;
      index++;
    }
    if (digits >= 13) {
      return index - start - digits;
    }
    if (digits > 0) {
      continue;
    }
    if (!isText(msg.charCodeAt(index))) {
      break;
    }
    index++;
  }
  return index - start;
}

/**
 * Length of the run that Byte compaction should cover starting at a position.
 *
 * <p>The run ends before a Numeric run, and rejects characters the character set cannot
 * represent.
 *
 * @throws UnsupportedTextException if a character cannot be represented
 */
function countByteRun(msg: string, start: number, charset: Pdf417Charset): number {
  const length = msg.length;
  let index = start;
  while (index < length) {
    let digits = 0;
    let scan = index;
    while (digits < 13 && scan < length && isDigit(msg.charCodeAt(scan))) {
      digits++;
      scan++;
    }
    if (digits >= 13) {
      return index - start;
    }
    if (!charset.canEncode(msg.charCodeAt(index))) {
      const ch = msg.charAt(index);
      throw new UnsupportedTextException(
        "Non-encodable character detected: " + ch + " (Unicode: " + msg.charCodeAt(index) + ")",
      );
    }
    index++;
  }
  return index - start;
}
