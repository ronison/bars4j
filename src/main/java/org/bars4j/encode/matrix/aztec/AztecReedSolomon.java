package org.bars4j.encode.matrix.aztec;

import org.bars4j.encode.matrix.ecc.ReedSolomonCoder;

/**
 * Reed-Solomon check-word generation for Aztec symbols.
 *
 * <p>Aztec appends its check words as the remainder of a polynomial division. With {@code n} check
 * words the divisor is {@code g(x) = (x + a^1)(x + a^2)...(x + a^n)} over the symbol's field, and
 * the data words are multiplied by {@code x^n} before the division.
 *
 * <p>The division is delegated to the shared {@link ReedSolomonCoder} over the symbol's field;
 * this class keeps the in-place entry point the Aztec pipeline uses.
 */
final class AztecReedSolomon {

    private AztecReedSolomon() {
    }

    /**
     * Fills the last {@code checkWords} slots of {@code words} with the check words of the leading
     * {@code words.length - checkWords} data words. The data words themselves are left untouched.
     */
    static void appendCheckWords(int[] words, int checkWords, AztecField field) {
        // Aztec generator roots start at alpha^1, like ECC200.
        new ReedSolomonCoder(field.delegate(), ReedSolomonCoder.ECC200_FIRST_ROOT)
                .appendCheckWords(words, checkWords);
    }
}
