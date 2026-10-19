package org.bars4j.encode.linear;

/**
 * Native IATA 2 of 5 encoder. Ported from the legacy {@code IATA2of5Encoder}, which only changes the
 * start/stop patterns of the standard 2 of 5 encoder.
 */
public final class Iata2of5Encoder extends Standard2of5Encoder {

    public Iata2of5Encoder() {
        this.startChar = "0000";
        this.stopChar = "1000";
    }
}
