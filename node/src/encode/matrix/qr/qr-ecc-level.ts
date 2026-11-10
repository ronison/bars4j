/**
 * QRCode error correction level. The {@code formatBits} are the 2-bit values stored in format info.
 * {@code ordinal} mirrors the Java enum declaration order (L, M, Q, H), which indexes the per-level
 * block layouts of {@link QrVersion}.
 *
 * Port of {@code org.bars4j.encode.matrix.qr.QrEccLevel}.
 */
export const QrEccLevel = {
  /** ~7% recovery. */
  L: { formatBits: 0b01, ordinal: 0 },
  /** ~15% recovery. */
  M: { formatBits: 0b00, ordinal: 1 },
  /** ~25% recovery. */
  Q: { formatBits: 0b11, ordinal: 2 },
  /** ~30% recovery. */
  H: { formatBits: 0b10, ordinal: 3 },
} as const;

export type QrEccLevel = (typeof QrEccLevel)[keyof typeof QrEccLevel];
