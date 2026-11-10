/**
 * QRCode data encoding modes.
 *
 * <p>{@code charCountBits} holds the width of the character-count indicator per version group:
 * index 0 = versions 1-9, 1 = versions 10-26, 2 = versions 27-40.
 *
 * Port of {@code org.bars4j.encode.matrix.qr.QrMode}.
 */
export const QrMode = {
  NUMERIC: { indicatorBits: 0x1, charCountBits: [10, 12, 14] },
  ALPHANUMERIC: { indicatorBits: 0x2, charCountBits: [9, 11, 13] },
  BYTE: { indicatorBits: 0x4, charCountBits: [8, 16, 16] },
  KANJI: { indicatorBits: 0x8, charCountBits: [8, 10, 12] },
} as const;

export type QrMode = (typeof QrMode)[keyof typeof QrMode];

/** Width of the character-count indicator for the given version (1-40). */
export function charCountBitsForVersion(mode: QrMode, version: number): number {
  const bits = mode.charCountBits;
  if (version <= 9) {
    return bits[0];
  }
  if (version <= 26) {
    return bits[1];
  }
  return bits[2];
}
