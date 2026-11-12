/**
 * Bars4J: pure Java barcode library with self-contained linear (1D), QRCode, Data Matrix, PDF417
 * and Aztec encoders. No runtime dependency other than the JDK's {@code java.desktop} module for
 * rendering.
 *
 * <p>Only the public API surface is exported; the {@code encode.linear}, {@code encode.matrix.*},
 * {@code symbol} and {@code util} packages are internal implementation details (accessible via
 * reflection is not part of the contract). The legacy facade packages ({@code org.bars4j.demo} and
 * {@code org.bars4j.paint}) remain exported for backward compatibility.
 */
module org.bars4j {
    requires java.desktop;

    exports org.bars4j;
    exports org.bars4j.api;
    exports org.bars4j.demo;
    exports org.bars4j.encode;
    exports org.bars4j.encode.api;
    exports org.bars4j.format;
    exports org.bars4j.paint;
    exports org.bars4j.render;
    exports org.bars4j.render.linear;
    exports org.bars4j.render.matrix;
}
