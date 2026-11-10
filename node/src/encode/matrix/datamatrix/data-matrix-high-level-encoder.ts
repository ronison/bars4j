import { UnsupportedTextException } from "../../api/unsupported-text-exception.ts";

/**
 * High-level encodation for ECC200, using the ASCII scheme of ISO/IEC 16022 Annex P.
 *
 * <p>The ASCII scheme represents every ISO-8859-1 byte and is always valid. A single character
 * normally becomes one codeword; three rules adjust that: a run of two decimal digits collapses
 * into one codeword (values 130-229), bytes 128-255 are escaped by a preceding upper-shift
 * codeword (235), and the trailing filler consists of the pad codeword (129) followed by a
 * 253-state pseudo-random sequence that spreads the filler across the symbol.
 *
 * <p>Other encodation schemes (C40, Text, X12, EDIFACT, Base256) are intentionally omitted: they
 * only improve compactness and are never required for correctness.
 *
 * Port of {@code org.bars4j.encode.matrix.datamatrix.DataMatrixHighLevelEncoder}.
 */

const PAD_CODEWORD = 129;
const UPPER_SHIFT = 235;
const DIGIT_PAIR_BASE = 130;
const FILLER_MULTIPLIER = 149;
const FILLER_MODULUS = 253;
const FILLER_LIMIT = 254;

/**
 * Encodes the text into Data Matrix codewords without the final filler. The length returned
 * determines which symbol is selected.
 */
export function encodeToCodewords(text: string): number[] {
  if (text.length === 0) {
    throw new UnsupportedTextException("Data Matrix requires a non-empty message");
  }
  const codewords: number[] = [];
  let index = 0;
  while (index < text.length) {
    const current = text.charCodeAt(index);
    if (current > 0xff) {
      throw new UnsupportedTextException(
        `Data Matrix ASCII encodation supports ISO-8859-1 characters only, found '${text.charAt(index)}'`,
      );
    }
    const next = index + 1 < text.length ? text.charCodeAt(index + 1) : 0;
    if (isDecimalDigit(current) && isDecimalDigit(next)) {
      codewords.push(DIGIT_PAIR_BASE + 10 * (current - 0x30) + (next - 0x30));
      index += 2;
    } else if (current >= 128) {
      codewords.push(UPPER_SHIFT);
      codewords.push(current - 128 + 1);
      index += 1;
    } else {
      codewords.push(current + 1);
      index += 1;
    }
  }
  return codewords;
}

/**
 * Fills the codeword sequence up to the symbol capacity using the ECC200 pad codeword and the
 * 253-state randomisation algorithm.
 *
 * @param codewords the unpadded codewords
 * @param capacity  the symbol's data capacity
 * @return a new array of exactly {@code capacity} codewords
 */
export function padToCapacity(codewords: number[], capacity: number): number[] {
  const padded = new Array<number>(capacity).fill(0);
  for (let i = 0; i < codewords.length; i++) {
    padded[i] = codewords[i];
  }
  if (codewords.length < capacity) {
    padded[codewords.length] = PAD_CODEWORD;
    for (let position = codewords.length + 2; position <= capacity; position++) {
      padded[position - 1] = fillerCodeword(position);
    }
  }
  return padded;
}

/** Computes the randomised pad codeword for a given one-based codeword position. */
function fillerCodeword(position: number): number {
  const pseudoRandom = ((FILLER_MULTIPLIER * position) % FILLER_MODULUS) + 1;
  const value = PAD_CODEWORD + pseudoRandom;
  return value <= FILLER_LIMIT ? value : value - FILLER_LIMIT;
}

function isDecimalDigit(c: number): boolean {
  return c >= 0x30 && c <= 0x39;
}
