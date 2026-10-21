package org.bars4j.encode.matrix.aztec;

/**
 * Reed-Solomon check-word generation for Aztec symbols.
 *
 * <p>Aztec appends its check words as the remainder of a polynomial division. With {@code n} check
 * words the divisor is {@code g(x) = (x + a^1)(x + a^2)...(x + a^n)} over the symbol's field, and
 * the data words are multiplied by {@code x^n} before the division, which is implemented here as a
 * running synthetic division.
 */
final class AztecReedSolomon {

    private AztecReedSolomon() {
    }

    /**
     * Fills the last {@code checkWords} slots of {@code words} with the check words of the leading
     * {@code words.length - checkWords} data words. The data words themselves are left untouched.
     */
    static void appendCheckWords(int[] words, int checkWords, AztecField field) {
        int dataWords = words.length - checkWords;
        int[] divisor = generator(field, checkWords);

        int[] division = new int[words.length];
        System.arraycopy(words, 0, division, 0, dataWords);
        for (int i = 0; i < dataWords; i++) {
            int leading = division[i];
            if (leading != 0) {
                for (int j = 0; j < divisor.length; j++) {
                    division[i + j] ^= field.multiply(divisor[j], leading);
                }
            }
        }
        System.arraycopy(division, dataWords, words, dataWords, checkWords);
    }

    /** Builds {@code (x + a^1)...(x + a^n)} as highest-degree-first coefficients. */
    private static int[] generator(AztecField field, int checkWords) {
        int[] polynomial = {1};
        for (int root = 1; root <= checkWords; root++) {
            polynomial = multiply(field, polynomial, new int[] {1, field.exp(root)});
        }
        return polynomial;
    }

    /** Multiplies two highest-degree-first polynomials over {@code field}. */
    private static int[] multiply(AztecField field, int[] a, int[] b) {
        int[] product = new int[a.length + b.length - 1];
        for (int i = 0; i < a.length; i++) {
            if (a[i] == 0) {
                continue;
            }
            for (int j = 0; j < b.length; j++) {
                product[i + j] ^= field.multiply(a[i], b[j]);
            }
        }
        return product;
    }
}
