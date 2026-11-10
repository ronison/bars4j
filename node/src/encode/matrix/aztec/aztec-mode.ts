/**
 * The five character modes of the Aztec encodation, in the order used throughout the mode switch
 * tables: {@code UPPER}, {@code LOWER}, {@code DIGIT}, {@code MIXED} and {@code PUNCT} (ISO/IEC
 * 24778, Table 4).
 *
 * Port of {@code org.bars4j.encode.matrix.aztec.AztecMode}.
 */
export const AztecMode = {
  UPPER: 0,
  LOWER: 1,
  DIGIT: 2,
  MIXED: 3,
  PUNCT: 4,
} as const;

export type AztecMode = (typeof AztecMode)[keyof typeof AztecMode];

/** The modes in ordinal order, equivalent to Java's {@code AztecMode.values()}. */
export const AZTEC_MODES: readonly AztecMode[] = [
  AztecMode.UPPER,
  AztecMode.LOWER,
  AztecMode.DIGIT,
  AztecMode.MIXED,
  AztecMode.PUNCT,
];

/**
 * Width in bits of a character code emitted in this mode. DIGIT packs a digit into four bits;
 * every other mode uses five.
 */
export function modeCodeWidth(mode: AztecMode): number {
  return mode === AztecMode.DIGIT ? 4 : 5;
}
