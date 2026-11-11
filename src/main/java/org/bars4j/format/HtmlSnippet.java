package org.bars4j.format;

import org.bars4j.util.Escapes;

/**
 * Assembles the embeddable HTML/SVG snippet shared by the barcode formats. Unlike the standalone
 * SVG document ({@code toSvg}) the snippet has no XML prologue, no generator comment and no
 * fallback text, and carries a {@code viewBox} so it scales with its container.
 */
final class HtmlSnippet {

    private HtmlSnippet() {
    }

    /** Assembles the snippet at its natural size. */
    static String assemble(int width, int height, String ariaLabel, String fragment) {
        return assemble(width, height, width, height, ariaLabel, fragment);
    }

    /**
     * Assembles the snippet with an explicit display size. The {@code viewBox} keeps the natural
     * geometry so the symbol scales proportionally.
     */
    static String assemble(int viewBoxWidth, int viewBoxHeight, int displayWidth, int displayHeight,
                           String ariaLabel, String fragment) {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\""
                + " viewBox=\"0 0 " + viewBoxWidth + " " + viewBoxHeight + "\""
                + " width=\"" + displayWidth + "\" height=\"" + displayHeight + "\""
                + " preserveAspectRatio=\"xMidYMid meet\""
                + " role=\"img\" aria-label=\"" + Escapes.attr(ariaLabel) + "\">"
                + "<style>text{font-family: 'Courier New', monospace;font-size: 14px;}</style>"
                + fragment
                + "</svg>";
    }
}
