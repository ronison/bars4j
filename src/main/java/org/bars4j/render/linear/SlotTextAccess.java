package org.bars4j.render.linear;

/**
 * Test-only bridge to the package-private font resolution of {@link SlotText}. Keeps the helper
 * internal to the SPI package while letting the conformance tests assert the resolved family.
 */
public final class SlotTextAccess {

    private SlotTextAccess() {
    }

    /** The physical family the raster label font resolves to on this platform. */
    public static String resolveFamily() {
        return SlotText.resolveFamily();
    }
}
