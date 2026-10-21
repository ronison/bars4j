package org.bars4j.encode.matrix.aztec;

import java.util.Arrays;

/**
 * Arithmetic over a binary extension field {@code GF(2^m)} defined by a primitive polynomial,
 * together with the exponent/logarithm tables used by Reed-Solomon error correction.
 *
 * <p>Aztec symbols use one field per codeword width: {@code GF(16)} for the 4-bit words of the mode
 * message, and {@code GF(64)}, {@code GF(256)}, {@code GF(1024)} or {@code GF(4096)} for the data
 * words of symbols whose codeword width is 6, 8, 10 or 12 bits respectively.
 */
public final class GenericGaloisField {

    /** Field for the 4-bit mode message words of compact symbols: {@code x^4 + x + 1}. */
    public static final GenericGaloisField AZTEC_PARAM = new GenericGaloisField(0x13, 16, 1);
    /** Field for 6-bit data codewords: {@code x^6 + x + 1}. */
    public static final GenericGaloisField AZTEC_DATA_6 = new GenericGaloisField(0x43, 64, 1);
    /** Field for 8-bit data codewords: {@code x^8 + x^5 + x^3 + x^2 + 1}. */
    public static final GenericGaloisField AZTEC_DATA_8 = new GenericGaloisField(0x12D, 256, 1);
    /** Field for 10-bit data codewords: {@code x^10 + x^3 + 1}. */
    public static final GenericGaloisField AZTEC_DATA_10 = new GenericGaloisField(0x409, 1024, 1);
    /** Field for 12-bit data codewords: {@code x^12 + x^6 + x^5 + x^3 + 1}. */
    public static final GenericGaloisField AZTEC_DATA_12 = new GenericGaloisField(0x1069, 4096, 1);

    private final int primitive;
    private final int size;
    private final int generatorBase;
    private final int[] expTable;
    private final int[] logTable;

    /**
     * Builds the field from its primitive polynomial.
     *
     * @param primitive irreducible polynomial, bit {@code i} giving the coefficient of {@code x^i}
     * @param size      number of elements, a power of two
     * @param generatorBase first exponent of the generator root, {@code 1} for Aztec symbols
     */
    public GenericGaloisField(int primitive, int size, int generatorBase) {
        if (size <= 0 || Integer.bitCount(size) != 1) {
            throw new IllegalArgumentException("size must be a power of two: " + size);
        }
        this.primitive = primitive;
        this.size = size;
        this.generatorBase = generatorBase;
        this.expTable = new int[size];
        this.logTable = new int[size];

        int x = 1;
        for (int i = 0; i < size; i++) {
            expTable[i] = x;
            x <<= 1;
            if (x >= size) {
                x ^= primitive;
                x &= size - 1;
            }
        }
        // logTable[0] stays 0; taking the logarithm of zero is not meaningful.
        for (int i = 0; i < size - 1; i++) {
            logTable[expTable[i]] = i;
        }
    }

    /** Returns {@code 2^power}; the exponent is reduced modulo {@code size - 1}. */
    public int exp(int power) {
        int p = power % (size - 1);
        if (p < 0) {
            p += size - 1;
        }
        return expTable[p];
    }

    /** Returns the discrete logarithm of {@code value} (which must be non-zero). */
    public int log(int value) {
        if (value == 0) {
            throw new IllegalArgumentException("log(0) is undefined");
        }
        return logTable[value];
    }

    /** Multiplies two field elements. */
    public int multiply(int a, int b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        return expTable[(logTable[a] + logTable[b]) % (size - 1)];
    }

    /** Returns the multiplicative inverse of {@code a}. */
    public int inverse(int a) {
        if (a == 0) {
            throw new ArithmeticException("0 has no multiplicative inverse");
        }
        return expTable[size - logTable[a] - 1];
    }

    /** Adds/subtracts two field elements (both operations coincide as XOR). */
    public static int addOrSubtract(int a, int b) {
        return a ^ b;
    }

    /** Returns the monomial {@code coefficient * x^degree} as a polynomial. */
    GenericGaloisFieldPoly buildMonomial(int degree, int coefficient) {
        if (degree < 0) {
            throw new IllegalArgumentException("degree must not be negative");
        }
        if (coefficient == 0) {
            return new GenericGaloisFieldPoly(this, new int[]{0});
        }
        int[] coefficients = new int[degree + 1];
        coefficients[0] = coefficient;
        return new GenericGaloisFieldPoly(this, coefficients);
    }

    /** Returns the number of field elements. */
    public int size() {
        return size;
    }

    /** Returns the first exponent of the Reed-Solomon generator roots. */
    public int generatorBase() {
        return generatorBase;
    }

    /** Returns the primitive polynomial that defines the field. */
    public int primitive() {
        return primitive;
    }

    @Override
    public String toString() {
        return "GF(0x" + Integer.toHexString(primitive) + ',' + size + ')';
    }

    /** Copies the exponent table (used only for diagnostics/tests). */
    int[] copyExpTable() {
        return Arrays.copyOf(expTable, expTable.length);
    }
}
