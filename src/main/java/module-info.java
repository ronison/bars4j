/**
 * Bars4J: pure Java barcode library with self-contained linear (1D), QRCode, Data Matrix, PDF417
 * and Aztec encoders. No runtime dependency other than the JDK's {@code java.desktop} module for
 * rendering.
 */
module org.bars4j {
    requires java.desktop;

    exports org.bars4j;
    exports org.bars4j.api;
    exports org.bars4j.demo;
    exports org.bars4j.encode;
    exports org.bars4j.encode.api;
    exports org.bars4j.encode.linear;
    exports org.bars4j.encode.matrix;
    exports org.bars4j.encode.matrix.aztec;
    exports org.bars4j.encode.matrix.datamatrix;
    exports org.bars4j.encode.matrix.gf256;
    exports org.bars4j.encode.matrix.pdf417;
    exports org.bars4j.encode.matrix.qr;
    exports org.bars4j.format;
    exports org.bars4j.paint;
    exports org.bars4j.render;
    exports org.bars4j.render.linear;
    exports org.bars4j.render.matrix;
    exports org.bars4j.symbol;
    exports org.bars4j.util;
}
