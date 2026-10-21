package org.bars4j.encode.matrix.gf256;

/**
 * Systematic Reed-Solomon error correction over {@link Gf256}, as used by QR Code symbols.
 *
 * <p>For data codewords forming a polynomial {@code d(x)}, the appended error correction codewords
 * are the remainder of {@code d(x) * x^ecLength} upon division by the generator polynomial
 * {@code g(x) = (x + a^0)(x + a^1) ... (x + a^(ecLength-1))}. Both polynomials are written with the
 * highest-degree coefficient first.
 */
public final class ReedSolomon {

    private ReedSolomon() {
    }

    /**
     * Returns the coefficients of the generator polynomial with the given number of factors,
     * highest degree first. The leading coefficient is always 1 and the roots are
     * {@code a^0, a^1, ...}.
     */
    static int[] generatorPolynomial(int degree) {
        int[] coefficients = {1};
        for (int root = 0; root < degree; root++) {
            int alphaToTheRoot = Gf256.exp(root);
            // Multiply the current polynomial by (x + alpha^root), one degree at a time.
            int[] extended = new int[coefficients.length + 1];
            extended[0] = coefficients[0];
            for (int i = 1; i < coefficients.length; i++) {
                extended[i] = coefficients[i]
                        ^ Gf256.multiply(alphaToTheRoot, coefficients[i - 1]);
            }
            extended[coefficients.length] =
                    Gf256.multiply(alphaToTheRoot, coefficients[coefficients.length - 1]);
            coefficients = extended;
        }
        return coefficients;
    }

    /**
     * Computes {@code ecLength} error correction codewords for the given data codewords.
     *
     * @param data     data codewords, most significant first
     * @param ecLength number of error correction codewords to produce
     * @return the error correction codewords, most significant first
     */
    public static int[] encode(int[] data, int ecLength) {
        int[] generator = generatorPolynomial(ecLength);

        // Running remainder of the division by the generator. Index 0 holds the coefficient of the
        // highest power (x^(ecLength-1)) and the last index holds the constant term.
        int[] remainder = new int[ecLength];
        int streamLength = data.length + ecLength;
        for (int position = 0; position < streamLength; position++) {
            // The stream is the data codewords followed by ecLength zero coefficients,
            // i.e. the coefficients of d(x) * x^ecLength from the highest power down.
            int next = position < data.length ? data[position] : 0;
            int leading = remainder[0];
            for (int i = 0; i < ecLength - 1; i++) {
                remainder[i] = remainder[i + 1] ^ Gf256.multiply(leading, generator[i + 1]);
            }
            remainder[ecLength - 1] = next ^ Gf256.multiply(leading, generator[ecLength]);
        }
        return remainder;
    }
}
