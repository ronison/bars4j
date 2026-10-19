package org.bars4j.encode.matrix.gf256;

/**
 * Reed-Solomon error correction codeword computation over {@link Gf256}.
 */
public final class ReedSolomon {

    private ReedSolomon() {
    }

    /** Builds the generator polynomial {@code (x - a^0)(x - a^1)...(x - a^(degree-1))}. */
    static int[] generatorPolynomial(int degree) {
        int[] result = {1};
        for (int i = 0; i < degree; i++) {
            result = multiply(result, new int[]{1, Gf256.exp(i)});
        }
        return result;
    }

    /** Polynomial multiplication over GF(256). */
    static int[] multiply(int[] a, int[] b) {
        int[] result = new int[a.length + b.length - 1];
        for (int i = 0; i < a.length; i++) {
            if (a[i] == 0) {
                continue;
            }
            for (int j = 0; j < b.length; j++) {
                result[i + j] ^= Gf256.multiply(a[i], b[j]);
            }
        }
        return result;
    }

    /**
     * Computes {@code ecLength} error correction codewords for the given data codewords.
     *
     * @param data     data codewords
     * @param ecLength number of error correction codewords to generate
     * @return the error correction codewords
     */
    public static int[] encode(int[] data, int ecLength) {
        int[] generator = generatorPolynomial(ecLength);
        int[] info = new int[data.length + ecLength];
        System.arraycopy(data, 0, info, 0, data.length);

        for (int i = 0; i < data.length; i++) {
            int coefficient = info[i];
            if (coefficient != 0) {
                for (int j = 0; j < generator.length; j++) {
                    info[i + j] ^= Gf256.multiply(generator[j], coefficient);
                }
            }
        }

        int[] ecc = new int[ecLength];
        System.arraycopy(info, data.length, ecc, 0, ecLength);
        return ecc;
    }
}
