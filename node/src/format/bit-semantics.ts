/**
 * How the legacy {@code org.bars4j.encode.BarSet} bits must be interpreted by a painter.
 *
 * Port of {@code org.bars4j.format.BitSemantics}.
 */
export const BitSemantics = {
  /** Bit 1 = dark module, bit 0 = light module (width-coded). */
  MODULE_DARK: "MODULE_DARK",
  /** Bit 1 = wide element, bit 0 = narrow element; colour alternates starting dark. */
  WIDE_NARROW_ALTERNATING: "WIDE_NARROW_ALTERNATING",
  /** Bit 1 = full-height bar, bit 0 = short bar (height-coded, e.g. PostNet). */
  HEIGHT_CODED: "HEIGHT_CODED",
} as const;

export type BitSemantics = (typeof BitSemantics)[keyof typeof BitSemantics];
