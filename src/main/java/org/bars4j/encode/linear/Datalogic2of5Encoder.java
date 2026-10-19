package org.bars4j.encode.linear;

/**
 * Native Datalogic 2 of 5 encoder. Ported from the legacy {@code Datalogic2of5Encoder}, which only
 * changes the start/stop patterns of the matrix 2 of 5 encoder.
 */
public final class Datalogic2of5Encoder extends Matrix2of5Encoder {

    public Datalogic2of5Encoder() {
        this.startChar = "0000";
        this.stopChar = "1000";
    }
}
