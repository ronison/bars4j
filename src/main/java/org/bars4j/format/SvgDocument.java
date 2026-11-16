package org.bars4j.format;

/**
 * Assembles the standalone SVG document shared by the barcode formats. Unlike the embeddable
 * snippet ({@link HtmlSnippet}) the document carries an XML prologue, a generator comment and a
 * fallback text, and has a fixed canvas with no {@code viewBox}.
 */
final class SvgDocument {

    private SvgDocument() {
    }

    /**
     * Assembles the standalone document with the given fixed canvas. The {@code styleElement} is
     * inserted before the drawing markup (the linear formats emit the human-readable label's font
     * style; the matrix formats pass {@code null}).
     */
    static String assemble(int width, int height, String styleElement, String fragment) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n"
                + "<!-- Created with Bars4J -->\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.2\" width=\"" + width
                + "\" height=\"" + height + "\""
                // xMidYMid is the spec-correct centred alignment; a previous version emitted the
                // canvas size here, which is not a valid SVG value.
                + " preserveAspectRatio=\"xMidYMid meet\">"
                + (styleElement != null ? styleElement : "")
                + fragment
                + "Sorry, your browser does not support inline SVG.</svg>";
    }
}
