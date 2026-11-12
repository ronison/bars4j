package org.bars4j.format;

/**
 * How a symbology's bit string must be interpreted when building a {@code LinearSymbol}.
 *
 * <p>A single bit pattern can mean different things depending on the rendering strategy of the
 * symbology.
 */
public enum BitSemantics {
    /** Bit 1 = dark module, bit 0 = light module (width-coded). */
    MODULE_DARK,
    /** Bit 1 = wide element, bit 0 = narrow element; colour alternates starting dark. */
    WIDE_NARROW_ALTERNATING,
    /** Bit 1 = full-height bar, bit 0 = short bar (height-coded, e.g. PostNet). */
    HEIGHT_CODED
}
