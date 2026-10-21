package org.bars4j.encode.matrix.aztec;

/**
 * The five character modes of the Aztec encodation, in the order used throughout the mode switch
 * tables: {@code UPPER}, {@code LOWER}, {@code DIGIT}, {@code MIXED} and {@code PUNCT} (ISO/IEC
 * 24778, Table 4).
 */
enum AztecMode {
    UPPER,
    LOWER,
    DIGIT,
    MIXED,
    PUNCT;

    /**
     * Width in bits of a character code emitted in this mode. DIGIT packs a digit into four bits;
     * every other mode uses five.
     */
    int codeWidth() {
        return this == DIGIT ? 4 : 5;
    }
}
