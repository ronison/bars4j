import { UnsupportedTextException } from "../../api/unsupported-text-exception.ts";
import { AztecBitBuffer } from "./aztec-bit-buffer.ts";
import { AztecCode } from "./aztec-code.ts";
import { AztecField } from "./aztec-field.ts";
import { AztecHighLevelEncoder } from "./aztec-high-level-encoder.ts";
import { AztecLayout } from "./aztec-layout.ts";
import { appendCheckWords } from "./aztec-reed-solomon.ts";

/**
 * Builds a complete Aztec symbol (ISO/IEC 24778) from input text: chooses the symbol size, performs
 * bit stuffing and error correction, generates the mode message and delegates the module placement
 * to {@code AztecLayout}.
 *
 * Port of {@code org.bars4j.encode.matrix.aztec.AztecSymbolEncoder}.
 */

/** Largest layer count of a full symbol. */
const MAX_LAYERS = 32;
/** Largest layer count of a compact symbol. */
const MAX_COMPACT_LAYERS = 4;

/**
 * Codeword width in bits for each layer count: 4 bits at one layer, then 6, 8, 10 and finally
 * 12 bits as the symbol grows.
 */
const CODEWORD_WIDTH = [
  4, 6, 6, 8, 8, 8, 8, 8, 8, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
  12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
];

/** A chosen symbol size together with its stuffed data. */
interface Sizing {
  compact: boolean;
  layers: number;
  wordSize: number;
  totalBitsInLayer: number;
  stuffed: AztecBitBuffer;
}

/**
 * Encodes {@code content} as an Aztec symbol.
 *
 * @param content      text to encode; without an explicit {@code charsetName} it must be
 *                     representable as ISO-8859-1 (Latin-1)
 * @param eccPercent   minimum percentage of error correction words
 * @param forcedLayers {@code null} or {@code 0} for automatic sizing; a negative value forces a
 *                     compact symbol with {@code abs(value)} layers, a positive value forces a
 *                     full symbol with that many layers
 * @param charsetName  character set to announce with an ECI, or {@code null} for no ECI
 */
export function encodeAztec(
  content: string,
  eccPercent: number,
  forcedLayers: number | null,
  charsetName: string | null,
): AztecCode {
  const bytes = encodeText(content, charsetName);
  const dataBits = new AztecHighLevelEncoder(bytes, charsetName).encode();

  const checkBits = Math.trunc((dataBits.size() * eccPercent) / 100) + 11;

  let sizing: Sizing;
  if (forcedLayers !== null && forcedLayers !== 0) {
    sizing = forcedSize(dataBits, checkBits, forcedLayers);
  } else {
    sizing = automaticSize(dataBits, checkBits);
  }

  const dataWords = Math.trunc(sizing.stuffed.size() / sizing.wordSize);
  const message = addCheckWords(sizing.stuffed, sizing.totalBitsInLayer, sizing.wordSize);
  const modeMessage = buildModeMessage(sizing.compact, sizing.layers, dataWords);
  const matrix = AztecLayout.of(sizing.compact, sizing.layers).build(message, modeMessage);
  return new AztecCode(sizing.compact, sizing.layers, dataWords, matrix);
}

/**
 * Encodes {@code text} to bytes. Without an explicit character set the text is encoded as
 * ISO-8859-1: each UTF-16 code unit becomes one byte, and code units above {@code 0xFF} become
 * {@code '?'} (0x3F), matching the JVM's replacement behaviour.
 */
function encodeText(text: string, charsetName: string | null): Uint8Array {
  if (charsetName !== null && charsetName.toUpperCase() === "UTF-8") {
    return new TextEncoder().encode(text);
  }
  const bytes = new Uint8Array(text.length);
  for (let i = 0; i < text.length; i++) {
    const code = text.charCodeAt(i);
    bytes[i] = code <= 0xff ? code : 0x3f;
  }
  return bytes;
}

/** Validates a caller-requested layer count and prepares its data. */
function forcedSize(dataBits: AztecBitBuffer, checkBits: number, forcedLayers: number): Sizing {
  const compact = forcedLayers < 0;
  const layers = Math.abs(forcedLayers);
  const maxLayers = compact ? MAX_COMPACT_LAYERS : MAX_LAYERS;
  if (layers > maxLayers) {
    throw new RangeError("Illegal value " + forcedLayers + " for layers");
  }
  const wordSize = CODEWORD_WIDTH[layers];
  const totalBitsInLayer = dataCapacity(compact, layers);
  const stuffed = insertStuffing(dataBits, wordSize);
  const usableBits = totalBitsInLayer - (totalBitsInLayer % wordSize);
  const tooLarge =
    stuffed.size() + checkBits > usableBits || (compact && stuffed.size() > wordSize * 64);
  if (tooLarge) {
    throw new UnsupportedTextException("Data too large for user specified layer");
  }
  return { compact, layers, wordSize, totalBitsInLayer, stuffed };
}

/**
 * Finds the smallest symbol that fits. Compact symbols are tried first (Compact1 through
 * Compact4); the smallest full symbol is Normal4, because a smaller full symbol would have the
 * same size as a compact one with less capacity.
 */
function automaticSize(dataBits: AztecBitBuffer, checkBits: number): Sizing {
  let stuffed: AztecBitBuffer | null = null;
  let wordSize = 0;
  for (let candidate = 0; candidate <= MAX_LAYERS; candidate++) {
    const compact = candidate <= 3;
    const layers = compact ? candidate + 1 : candidate;
    const totalBitsInLayer = dataCapacity(compact, layers);
    if (dataBits.size() + checkBits > totalBitsInLayer) {
      continue;
    }
    if (stuffed === null || wordSize !== CODEWORD_WIDTH[layers]) {
      wordSize = CODEWORD_WIDTH[layers];
      stuffed = insertStuffing(dataBits, wordSize);
    }
    // Compact symbols can carry at most 64 data words.
    if (compact && stuffed.size() > wordSize * 64) {
      continue;
    }
    const usableBits = totalBitsInLayer - (totalBitsInLayer % wordSize);
    if (stuffed.size() + checkBits <= usableBits) {
      return { compact, layers, wordSize, totalBitsInLayer, stuffed };
    }
  }
  throw new UnsupportedTextException("Data too large for an Aztec code");
}

/** Number of data bits available in {@code layers} layers of the given layout. */
function dataCapacity(compact: boolean, layers: number): number {
  return ((compact ? 88 : 112) + 16 * layers) * layers;
}

/**
 * Splits {@code bits} into {@code wordSize}-bit words. A word whose upper bits would all be one
 * or all be zero is ambiguous, so the final bit is forced and one input bit is deferred to the
 * following word. Missing bits at the end are treated as ones.
 */
export function insertStuffing(bits: AztecBitBuffer, wordSize: number): AztecBitBuffer {
  const out = new AztecBitBuffer();
  const bitCount = bits.size();
  const allOne = (1 << wordSize) - 1;
  const upperMask = allOne - 1;

  let read = 0;
  while (read < bitCount) {
    let word = 0;
    for (let offset = 0; offset < wordSize; offset++) {
      const position = read + offset;
      if (position >= bitCount || bits.get(position)) {
        word |= 1 << (wordSize - 1 - offset);
      }
    }
    const upper = word & upperMask;
    if (upper === upperMask) {
      // Force the last bit to zero and let the borrowed one open the next word.
      out.appendBits(upperMask, wordSize);
      read += wordSize - 1;
    } else if (upper === 0) {
      // Force a lone one in the last position.
      out.appendBits(1, wordSize);
      read += wordSize - 1;
    } else {
      out.appendBits(word, wordSize);
      read += wordSize;
    }
  }
  return out;
}

/** Builds the mode message: symbol geometry, protected by Reed-Solomon check words. */
export function buildModeMessage(
  compact: boolean,
  layers: number,
  dataWords: number,
): AztecBitBuffer {
  const raw = new AztecBitBuffer();
  if (compact) {
    raw.appendBits(layers - 1, 2);
    raw.appendBits(dataWords - 1, 6);
    return addCheckWords(raw, 28, 4);
  }
  raw.appendBits(layers - 1, 5);
  raw.appendBits(dataWords - 1, 11);
  return addCheckWords(raw, 40, 4);
}

/**
 * Packs {@code dataBits} into {@code totalBits / wordSize} codewords and fills the trailing
 * slots with Reed-Solomon check words, returning the complete message as a bit buffer.
 */
function addCheckWords(
  dataBits: AztecBitBuffer,
  totalBits: number,
  wordSize: number,
): AztecBitBuffer {
  const dataWords = Math.trunc(dataBits.size() / wordSize);
  const totalWords = Math.trunc(totalBits / wordSize);
  const words = new Array<number>(totalWords).fill(0);
  for (let w = 0; w < dataWords; w++) {
    let value = 0;
    for (let bit = 0; bit < wordSize; bit++) {
      if (dataBits.get(w * wordSize + bit)) {
        value |= 1 << (wordSize - 1 - bit);
      }
    }
    words[w] = value;
  }
  appendCheckWords(words, totalWords - dataWords, AztecField.forWordSize(wordSize));

  const message = new AztecBitBuffer();
  // Prepend the filler that aligns the codeword block inside the available bits.
  const leadingFiller = totalBits % wordSize;
  message.appendBits(0, leadingFiller);
  for (const word of words) {
    message.appendBits(word, wordSize);
  }
  return message;
}
