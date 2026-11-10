/**
 * High-level compaction schemes defined by ISO/IEC 15438.
 *
 * <p>{@link AUTO} chooses a scheme (or a mix of schemes) for every stretch of input; the other
 * values force a single scheme for the whole message.
 *
 * Port of {@code org.bars4j.encode.matrix.pdf417.Pdf417Compaction}.
 */
export const Pdf417Compaction = {
  /** Let the encoder choose the most suitable compaction for each part of the input. */
  AUTO: "AUTO",

  /** Force Text compaction for the whole message. */
  TEXT: "TEXT",

  /** Force Byte compaction for the whole message. */
  BYTE: "BYTE",

  /** Force Numeric compaction for the whole message (digits only). */
  NUMERIC: "NUMERIC",
} as const;

export type Pdf417Compaction = (typeof Pdf417Compaction)[keyof typeof Pdf417Compaction];
