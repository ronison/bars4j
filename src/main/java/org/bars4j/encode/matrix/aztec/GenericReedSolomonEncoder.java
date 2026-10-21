package org.bars4j.encode.matrix.aztec;

import java.util.ArrayList;
import java.util.List;

/**
 * Reed-Solomon encoder for an arbitrary {@link GenericGaloisField}. It appends the requested number
 * of error correction (check) words to the data words supplied in {@code toEncode}, in place.
 */
public final class GenericReedSolomonEncoder {

    private final GenericGaloisField field;
    private final List<GenericGaloisFieldPoly> cachedGenerators;

    public GenericReedSolomonEncoder(GenericGaloisField field) {
        this.field = field;
        this.cachedGenerators = new ArrayList<>();
        cachedGenerators.add(new GenericGaloisFieldPoly(field, new int[]{1}));
    }

    /**
     * Returns the generator polynomial for {@code degree} check words, building and caching it on
     * first use. The roots are {@code a^generatorBase .. a^(generatorBase + degree - 1)}.
     */
    private GenericGaloisFieldPoly buildGenerator(int degree) {
        if (degree >= cachedGenerators.size()) {
            GenericGaloisFieldPoly lastGenerator = cachedGenerators.get(cachedGenerators.size() - 1);
            for (int d = cachedGenerators.size(); d <= degree; d++) {
                GenericGaloisFieldPoly nextGenerator = lastGenerator.multiply(
                        new GenericGaloisFieldPoly(field,
                                new int[]{1, field.exp(d - 1 + field.generatorBase())}));
                cachedGenerators.add(nextGenerator);
                lastGenerator = nextGenerator;
            }
        }
        return cachedGenerators.get(degree);
    }

    /**
     * Encodes the data words in place.
     *
     * @param toEncode data words followed by {@code ecBytes} spare slots for the check words
     * @param ecBytes  number of error correction words to append
     */
    public void encode(int[] toEncode, int ecBytes) {
        if (ecBytes == 0) {
            throw new IllegalArgumentException("No error correction bytes");
        }
        int dataBytes = toEncode.length - ecBytes;
        if (dataBytes <= 0) {
            throw new IllegalArgumentException("No data bytes provided");
        }
        GenericGaloisFieldPoly generator = buildGenerator(ecBytes);
        int[] infoCoefficients = new int[dataBytes];
        System.arraycopy(toEncode, 0, infoCoefficients, 0, dataBytes);
        GenericGaloisFieldPoly info = new GenericGaloisFieldPoly(field, infoCoefficients);
        info = info.multiplyByMonomial(ecBytes, 1);
        GenericGaloisFieldPoly remainder = info.divide(generator)[1];
        int[] coefficients = remainder.coefficients();
        int numZeroCoefficients = ecBytes - coefficients.length;
        for (int i = 0; i < numZeroCoefficients; i++) {
            toEncode[dataBytes + i] = 0;
        }
        System.arraycopy(coefficients, 0, toEncode, dataBytes + numZeroCoefficients, coefficients.length);
    }
}
