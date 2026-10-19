package org.bars4j.format;

/**
 * How the legacy {@code org.bars4j.encode.BarSet} bits must be interpreted by a painter.
 *
 * <p>This is the piece that makes the legacy encoders/painters interoperate with the new
 * {@code LinearSymbol} model: a single bit array can mean different things depending on the
 * rendering strategy.
 */
public enum BitSemantics {
    /** Bit 1 = dark module, bit 0 = light module (width-coded). */
    MODULE_DARK,
    /** Bit 1 = wide element, bit 0 = narrow element; colour alternates starting dark. */
    WIDE_NARROW_ALTERNATING,
    /** Bit 1 = full-height bar, bit 0 = short bar (height-coded, e.g. PostNet). */
    HEIGHT_CODED
}
