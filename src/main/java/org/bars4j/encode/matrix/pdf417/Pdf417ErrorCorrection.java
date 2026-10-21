package org.bars4j.encode.matrix.pdf417;

/**
 * Reed-Solomon check codeword generation for PDF417, following clause 4.10 and Annex F of
 * ISO/IEC 15438.
 *
 * <p>PDF417 works over the prime field with 929 elements and primitive element 3. A chosen error
 * correction level with {@code k} check codewords uses the generator polynomial
 * {@code g(x) = (x - 3)(x - 9) ... (x - 3^k)}, reduced modulo 929. Rather than carrying those
 * coefficients as a table, this class derives {@code g(x)} from the field itself, once per level,
 * and then obtains the check codewords as the negative remainder of the message polynomial shifted
 * by {@code k} and divided by {@code g(x)}.
 *
 * <p>The correction information is internal to the symbol: it is not an externally visible check
 * digit, so no caller-facing checksum is produced here.
 */
public final class Pdf417ErrorCorrection {

    /** Modulus of the PDF417 field. */
    private static final int FIELD_MODULUS = 929;

    /** Primitive element of the field; its powers enumerate every non-zero field element. */
    private static final int FIELD_GENERATOR = 3;

    /** Highest error correction level described by the standard. */
    public static final int MAX_LEVEL = 8;

    /**
     * Generator polynomials indexed by error correction level. Entry {@code level} holds the
     * coefficients of {@code g(x)} in ascending order of degree, from the constant term up to the
     * leading coefficient, which is always one.
     */
    private static final int[][] GENERATOR_POLYNOMIALS = new int[MAX_LEVEL + 1][];

    static {
        for (int level = 0; level <= MAX_LEVEL; level++) {
            GENERATOR_POLYNOMIALS[level] = buildGeneratorPolynomial(codewordCount(level));
        }
    }

    private Pdf417ErrorCorrection() {
    }

    /**
     * Number of check codewords for an error correction level.
     *
     * @param level error correction level, 0-8
     * @return the number of check codewords, {@code 2^(level + 1)}
     */
    public static int codewordCount(int level) {
        if (level < 0 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Error correction level must be between 0 and 8");
        }
        return 1 << (level + 1);
    }

    /**
     * Multiplies out {@code (x - 3^1) ... (x - 3^roots)} over the PDF417 field.
     *
     * @param roots number of roots, equal to the number of check codewords
     * @return coefficients in ascending order of degree, length {@code roots + 1}
     */
    private static int[] buildGeneratorPolynomial(int roots) {
        int[] polynomial = {1};
        int root = 1;
        for (int i = 0; i < roots; i++) {
            root = root * FIELD_GENERATOR % FIELD_MODULUS;
            int[] grown = new int[polynomial.length + 1];
            for (int degree = 0; degree < polynomial.length; degree++) {
                grown[degree] = reduce(grown[degree] - polynomial[degree] * root);
                grown[degree + 1] = reduce(grown[degree + 1] + polynomial[degree]);
            }
            polynomial = grown;
        }
        return polynomial;
    }

    /**
     * Computes the check codewords that complete a block of data codewords.
     *
     * <p>The data codewords form a message polynomial whose first entry is the highest-degree
     * coefficient. Shifting it by the number of check codewords and taking the remainder modulo the
     * generator polynomial yields the error-detecting residue; the negated remainder, most
     * significant coefficient first, is what the standard appends to the data region.
     *
     * @param dataCodewords data codewords, each in the range 0-928
     * @param level         error correction level, 0-8
     * @return the check codewords in the order they follow the data
     */
    public static int[] generate(int[] dataCodewords, int level) {
        int checkCount = codewordCount(level);
        int[] generator = GENERATOR_POLYNOMIALS[level];

        int[] dividend = new int[dataCodewords.length + checkCount];
        for (int i = 0; i < dataCodewords.length; i++) {
            dividend[checkCount + dataCodewords.length - 1 - i] = reduce(dataCodewords[i]);
        }

        // Synthetic division by the monic generator: cancel each high coefficient in turn.
        for (int degree = dividend.length - 1; degree >= checkCount; degree--) {
            int coefficient = dividend[degree];
            if (coefficient == 0) {
                continue;
            }
            int offset = degree - checkCount;
            for (int j = 0; j <= checkCount; j++) {
                dividend[offset + j] = reduce(dividend[offset + j] - coefficient * generator[j]);
            }
        }

        int[] check = new int[checkCount];
        for (int i = 0; i < checkCount; i++) {
            check[i] = reduce(-dividend[checkCount - 1 - i]);
        }
        return check;
    }

    /** Normalises a field value into {@code [0, FIELD_MODULUS)}. */
    private static int reduce(int value) {
        int remainder = value % FIELD_MODULUS;
        return remainder < 0 ? remainder + FIELD_MODULUS : remainder;
    }
}
