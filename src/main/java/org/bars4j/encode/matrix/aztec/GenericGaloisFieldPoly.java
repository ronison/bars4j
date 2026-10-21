package org.bars4j.encode.matrix.aztec;

import java.util.Arrays;

/**
 * A polynomial whose coefficients live in a {@link GenericGaloisField}.
 *
 * <p>Coefficients are stored from the highest-degree term down to the constant term, which is the
 * layout that makes the polynomial long division used by Reed-Solomon encoding straightforward.
 */
final class GenericGaloisFieldPoly {

    private final GenericGaloisField field;
    private final int[] coefficients;

    GenericGaloisFieldPoly(GenericGaloisField field, int[] rawCoefficients) {
        if (rawCoefficients.length == 0) {
            throw new IllegalArgumentException("coefficients must not be empty");
        }
        this.field = field;
        int firstNonZero = 0;
        while (firstNonZero < rawCoefficients.length - 1 && rawCoefficients[firstNonZero] == 0) {
            firstNonZero++;
        }
        this.coefficients = firstNonZero == 0
                ? Arrays.copyOf(rawCoefficients, rawCoefficients.length)
                : Arrays.copyOfRange(rawCoefficients, firstNonZero, rawCoefficients.length);
    }

    /** Returns a copy of the coefficients, highest degree first. */
    int[] coefficients() {
        return Arrays.copyOf(coefficients, coefficients.length);
    }

    /** Returns the degree of the polynomial (the zero polynomial has degree -1). */
    int degree() {
        return coefficients.length - 1;
    }

    /** Returns {@code true} for the zero polynomial. */
    boolean isZero() {
        return coefficients[0] == 0;
    }

    /** Adds or subtracts another polynomial (identical operations in a binary field). */
    GenericGaloisFieldPoly addOrSubtract(GenericGaloisFieldPoly other) {
        if (field != other.field) {
            throw new IllegalArgumentException("polynomials belong to different fields");
        }
        if (isZero()) {
            return other;
        }
        if (other.isZero()) {
            return this;
        }
        int[] smaller = coefficients;
        int[] larger = other.coefficients;
        if (smaller.length > larger.length) {
            int[] swap = smaller;
            smaller = larger;
            larger = swap;
        }
        int[] sum = Arrays.copyOf(larger, larger.length);
        int lengthDifference = larger.length - smaller.length;
        for (int i = 0; i < smaller.length; i++) {
            sum[i + lengthDifference] ^= smaller[i];
        }
        return new GenericGaloisFieldPoly(field, sum);
    }

    /** Multiplies two polynomials. */
    GenericGaloisFieldPoly multiply(GenericGaloisFieldPoly other) {
        if (field != other.field) {
            throw new IllegalArgumentException("polynomials belong to different fields");
        }
        if (isZero() || other.isZero()) {
            return new GenericGaloisFieldPoly(field, new int[]{0});
        }
        int[] product = new int[coefficients.length + other.coefficients.length - 1];
        for (int i = 0; i < coefficients.length; i++) {
            if (coefficients[i] == 0) {
                continue;
            }
            for (int j = 0; j < other.coefficients.length; j++) {
                product[i + j] ^= field.multiply(coefficients[i], other.coefficients[j]);
            }
        }
        return new GenericGaloisFieldPoly(field, product);
    }

    /** Multiplies by {@code coefficient * x^degree}. */
    GenericGaloisFieldPoly multiplyByMonomial(int degree, int coefficient) {
        if (degree < 0) {
            throw new IllegalArgumentException("degree must not be negative");
        }
        if (coefficient == 0) {
            return new GenericGaloisFieldPoly(field, new int[]{0});
        }
        int[] product = new int[coefficients.length + degree];
        for (int i = 0; i < coefficients.length; i++) {
            product[i] = field.multiply(coefficients[i], coefficient);
        }
        return new GenericGaloisFieldPoly(field, product);
    }

    /** Polynomial long division; returns {@code [quotient, remainder]}. */
    GenericGaloisFieldPoly[] divide(GenericGaloisFieldPoly other) {
        if (field != other.field) {
            throw new IllegalArgumentException("polynomials belong to different fields");
        }
        if (other.isZero()) {
            throw new ArithmeticException("division by zero polynomial");
        }
        GenericGaloisFieldPoly quotient = new GenericGaloisFieldPoly(field, new int[]{0});
        GenericGaloisFieldPoly remainder = this;
        int inverseDenominatorLeadingTerm = field.inverse(other.coefficients[0]);
        while (remainder.degree() >= other.degree() && !remainder.isZero()) {
            int degreeDifference = remainder.degree() - other.degree();
            int scale = field.multiply(remainder.coefficients[0], inverseDenominatorLeadingTerm);
            GenericGaloisFieldPoly term = other.multiplyByMonomial(degreeDifference, scale);
            GenericGaloisFieldPoly iterationQuotient = field.buildMonomial(degreeDifference, scale);
            quotient = quotient.addOrSubtract(iterationQuotient);
            remainder = remainder.addOrSubtract(term);
        }
        return new GenericGaloisFieldPoly[]{quotient, remainder};
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int coefficient : coefficients) {
            builder.append(Integer.toHexString(coefficient)).append(' ');
        }
        return builder.toString();
    }
}
