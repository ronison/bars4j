package org.bars4j.encode.matrix.ecc;

/**
 * Systematic Reed-Solomon check-word generation over a {@link GfField}.
 *
 * <p>For data codewords forming the polynomial {@code d(x)}, the appended check codewords are the
 * coefficients of the remainder of {@code d(x) * x^n} upon division by the generator polynomial
 * {@code g(x) = (x + alpha^firstRoot)(x + alpha^(firstRoot+1)) ... (x + alpha^(firstRoot+n-1))}.
 * Both polynomials are written with the highest-degree coefficient first.
 *
 * <p>The symbologies share the same division algorithm and differ only in the field and in the
 * first root exponent: QRCode roots start at {@code alpha^0}, Data Matrix (ECC200) and Aztec start
 * at {@code alpha^1}. Instances are immutable and cheap to share; the generator polynomial is
 * derived once per instance.
 */
public final class ReedSolomonCoder {

    /** QRCode: generator roots are the first {@code n} powers starting at {@code alpha^0}. */
    public static final int QR_FIRST_ROOT = 0;
    /** Data Matrix (ECC200) and Aztec: generator roots start at {@code alpha^1}. */
    public static final int ECC200_FIRST_ROOT = 1;

    private final GfField field;
    private final int firstRoot;

    /**
     * Creates a coder over {@code field} whose generator polynomial roots start at
     * {@code alpha^firstRoot}.
     */
    public ReedSolomonCoder(GfField field, int firstRoot) {
        this.field = field;
        this.firstRoot = firstRoot;
    }

    /**
     * Computes {@code checkLength} check codewords for the given data codewords.
     *
     * @param data        data codewords, most significant first
     * @param checkLength number of check codewords to produce
     * @return the check codewords, most significant first
     */
    public int[] encode(int[] data, int checkLength) {
        if (checkLength < 0) {
            throw new IllegalArgumentException("checkLength must be >= 0: " + checkLength);
        }
        int[] generator = generatorPolynomial(checkLength);

        // Running remainder of the division by the generator. Index 0 holds the coefficient of the
        // highest power (x^(checkLength-1)) and the last index holds the constant term.
        int[] remainder = new int[checkLength];
        int streamLength = data.length + checkLength;
        for (int position = 0; position < streamLength; position++) {
            // The stream is the data codewords followed by checkLength zero coefficients,
            // i.e. the coefficients of d(x) * x^checkLength from the highest power down.
            int next = position < data.length ? data[position] : 0;
            int leading = remainder[0];
            for (int i = 0; i < checkLength - 1; i++) {
                remainder[i] = remainder[i + 1] ^ field.multiply(leading, generator[i + 1]);
            }
            remainder[checkLength - 1] = next ^ field.multiply(leading, generator[checkLength]);
        }
        return remainder;
    }

    /**
     * Fills the last {@code checkLength} slots of {@code words} with the check words of the leading
     * {@code words.length - checkLength} data words. The data words themselves are left untouched.
     *
     * <p>Equivalent to {@link #encode} on the leading words followed by copying the result back;
     * kept as a separate entry point because the callers (Aztec, ECC200 multi-block) work in place
     * on interleaved buffers.
     */
    public void appendCheckWords(int[] words, int checkLength) {
        int dataWords = words.length - checkLength;
        if (dataWords < 0) {
            throw new IllegalArgumentException(
                    "checkLength " + checkLength + " exceeds words.length " + words.length);
        }
        int[] check = encodeSlice(words, 0, dataWords, checkLength);
        System.arraycopy(check, 0, words, dataWords, checkLength);
    }

    /**
     * Computes the check codewords of the slice {@code message[offset, offset+length)} treated as
     * one block.
     */
    public int[] encodeSlice(int[] message, int offset, int length, int checkLength) {
        int[] data = new int[length];
        System.arraycopy(message, offset, data, 0, length);
        return encode(data, checkLength);
    }

    /**
     * Builds the monic generator polynomial of the given degree, ordered from the highest power
     * downwards; the returned array has {@code degree + 1} entries and its roots are
     * {@code alpha^firstRoot ... alpha^(firstRoot+degree-1)}.
     */
    private int[] generatorPolynomial(int degree) {
        int[] coefficients = {1};
        for (int root = 0; root < degree; root++) {
            coefficients = multiplyByLinearFactor(coefficients, field.exp(firstRoot + root));
        }
        return coefficients;
    }

    /** Multiplies a polynomial (high power first) by the linear factor {@code (x + root)}. */
    private int[] multiplyByLinearFactor(int[] polynomial, int root) {
        int[] product = new int[polynomial.length + 1];
        for (int i = 0; i < polynomial.length; i++) {
            int term = polynomial[i];
            product[i] ^= term;
            product[i + 1] ^= field.multiply(term, root);
        }
        return product;
    }
}
