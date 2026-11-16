package org.bars4j.encode.matrix.ecc;

import java.util.ArrayList;
import java.util.List;

/**
 * A binary extension field GF(2^m) with tabulated exponentials and logarithms.
 *
 * <p>Field elements are m-bit words viewed as polynomials over GF(2): addition is bitwise XOR, and
 * multiplication is polynomial multiplication reduced modulo a primitive polynomial of degree m.
 * The element {@code 2} (the polynomial {@code x}) is a generator whenever the reducing polynomial
 * is primitive, so the {@code elementCount - 1} non-zero elements are exactly the powers of 2;
 * multiplication therefore turns into addition of exponents, which the two lookup tables make
 * cheap.
 *
 * <p>Instances are immutable and shared through {@link #of}, so every symbology that uses the same
 * field (QRCode and Data Matrix both use a GF(256), with different primitive polynomials) reuses
 * one table set.
 */
public final class GfField {

    /** Fields built so far; small (a handful of entries) and read-mostly. */
    private static final List<GfField> CACHE = new ArrayList<>();

    private final int primitivePolynomial;
    private final int elementCount;
    private final int cycle;
    /** {@code alphaPower[k]} holds {@code alpha^k} for {@code 0 <= k < cycle}. */
    private final int[] alphaPower;
    /** {@code logarithm[v]} holds the {@code k} with {@code alpha^k = v}, for non-zero {@code v}. */
    private final int[] logarithm;

    /**
     * Returns the field of {@code elementCount} elements reduced modulo
     * {@code primitivePolynomial}.
     *
     * @param primitivePolynomial the primitive polynomial as a bitmask, bit {@code i} carrying the
     *                            coefficient of {@code x^i} (the degree must match {@code m} of
     *                            {@code 2^m = elementCount})
     * @param elementCount        number of field elements, a power of two
     */
    public static synchronized GfField of(int primitivePolynomial, int elementCount) {
        if (Integer.bitCount(elementCount) != 1 || elementCount < 2) {
            throw new IllegalArgumentException(
                    "elementCount must be a power of two: " + elementCount);
        }
        if (Integer.highestOneBit(primitivePolynomial) != elementCount) {
            throw new IllegalArgumentException(
                    "Polynomial " + Integer.toHexString(primitivePolynomial)
                            + " does not have degree " + Integer.numberOfTrailingZeros(elementCount));
        }
        for (GfField field : CACHE) {
            if (field.primitivePolynomial == primitivePolynomial
                    && field.elementCount == elementCount) {
                return field;
            }
        }
        GfField field = new GfField(primitivePolynomial, elementCount);
        CACHE.add(field);
        return field;
    }

    private GfField(int primitivePolynomial, int elementCount) {
        this.primitivePolynomial = primitivePolynomial;
        this.elementCount = elementCount;
        this.cycle = elementCount - 1;
        this.alphaPower = new int[cycle];
        this.logarithm = new int[elementCount];

        // Successive powers of alpha = 2, reduced by the primitive polynomial whenever the degree
        // reaches m. For a primitive polynomial the orbit visits every non-zero element exactly
        // once and returns to 1 after `cycle` steps.
        int element = 1;
        for (int power = 0; power < cycle; power++) {
            alphaPower[power] = element;
            logarithm[element] = power;
            element <<= 1;
            if (element >= elementCount) {
                element ^= primitivePolynomial;
            }
        }
        if (element != 1) {
            throw new IllegalArgumentException(
                    "Polynomial " + Integer.toHexString(primitivePolynomial)
                            + " is not primitive over GF(2^"
                            + Integer.numberOfTrailingZeros(elementCount) + ")");
        }
    }

    /**
     * Returns {@code alpha} raised to {@code exponent}. Exponents are taken modulo the
     * multiplicative cycle, so negative and out-of-range inputs wrap around.
     */
    public int exp(int exponent) {
        return alphaPower[Math.floorMod(exponent, cycle)];
    }

    /** Returns the discrete logarithm of {@code value}, which must be a non-zero element. */
    public int log(int value) {
        if (value <= 0 || value >= elementCount) {
            throw new IllegalArgumentException("log(" + value + ") is undefined");
        }
        return logarithm[value];
    }

    /** Multiplies two field elements. */
    public int multiply(int a, int b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        return alphaPower[(logarithm[a] + logarithm[b]) % cycle];
    }
}
