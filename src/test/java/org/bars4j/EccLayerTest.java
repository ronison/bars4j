package org.bars4j;

import org.bars4j.encode.matrix.ecc.GfField;
import org.bars4j.encode.matrix.ecc.ReedSolomonCoder;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the shared error-correction layer: the {@link GfField} arithmetic and the
 * {@link ReedSolomonCoder} polynomial division, including the RS identity that ties them together.
 */
class EccLayerTest {

    /** RS identity: appending the check words to the data makes the stream divisible by g(x). */
    private static void assertCheckWordsCloseTheStream(ReedSolomonCoder coder, int[] data, int checkLength) {
        int[] check = coder.encode(data, checkLength);
        int[] stream = new int[data.length + checkLength];
        System.arraycopy(data, 0, stream, 0, data.length);
        System.arraycopy(check, 0, stream, data.length, checkLength);

        // Re-encoding the whole stream must produce all-zero check words.
        int[] residue = coder.encode(stream, checkLength);
        for (int value : residue) {
            assertEquals(0, value, "stream not divisible by the generator: " + java.util.Arrays.toString(residue));
        }
    }

    @Test
    void ofRejectsNonPowerOfTwoElementCount() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> GfField.of(0x11D, 255)),
                () -> assertThrows(IllegalArgumentException.class, () -> GfField.of(0x3, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> GfField.of(0, 256)));
    }

    @Test
    void ofRejectsPolynomialOfWrongDegree() {
        // 0x13 (x^4 + x + 1) has degree 4, so it cannot generate GF(2^8).
        assertThrows(IllegalArgumentException.class, () -> GfField.of(0x13, 256));
    }

    @Test
    void ofRejectsReduciblePolynomial() {
        // x^8 + x^4 + 1 (0x131) is reducible (it factors over GF(2)).
        assertThrows(IllegalArgumentException.class, () -> GfField.of(0x131, 256));
    }

    @Test
    void ofCachesInstances() {
        assertSame(GfField.of(0x11D, 256), GfField.of(0x11D, 256));
        assertNotEquals(GfField.of(0x11D, 256), GfField.of(0x12D, 256));
    }

    @Test
    void qrFieldTablesCoverEveryNonZeroElement() {
        GfField field = GfField.of(0x11D, 256);
        Set<Integer> seen = new HashSet<>();
        for (int power = 0; power < 255; power++) {
            int value = field.exp(power);
            assertTrue(value >= 1 && value < 256);
            seen.add(value);
            // exp/log are inverse maps.
            assertEquals(power, field.log(value));
        }
        // The orbit of a primitive element visits every non-zero element exactly once.
        assertEquals(255, seen.size());
    }

    @Test
    void multiplyMatchesExpLogDefinition() {
        GfField field = GfField.of(0x12D, 256);
        Random random = new Random(42);
        for (int i = 0; i < 1_000; i++) {
            int a = random.nextInt(256), b = random.nextInt(256);
            int expected = (a == 0 || b == 0) ? 0 : field.exp(field.log(a) + field.log(b));
            assertEquals(expected, field.multiply(a, b));
        }
        assertEquals(0, field.multiply(0, 200));
        assertEquals(0, field.multiply(200, 0));
    }

    @Test
    void expWrapsNegativeAndOutOfRangeExponents() {
        GfField field = GfField.of(0x11D, 256);
        assertEquals(field.exp(0), field.exp(255));
        assertEquals(field.exp(1), field.exp(256));
        assertEquals(field.exp(0), field.exp(-255));
    }

    @Test
    void logRejectsZeroAndOutOfRangeValues() {
        GfField field = GfField.of(0x11D, 256);
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> field.log(0)),
                () -> assertThrows(IllegalArgumentException.class, () -> field.log(-1)),
                () -> assertThrows(IllegalArgumentException.class, () -> field.log(256)));
    }

    @Test
    void qrCheckWordsCloseTheStream() {
        ReedSolomonCoder coder = new ReedSolomonCoder(GfField.of(0x11D, 256), 0);
        Random random = new Random(7);
        for (int dataLength : new int[]{1, 7, 19, 26, 64}) {
            for (int checkLength : new int[]{1, 7, 10, 13, 17, 26}) {
                int[] data = new int[dataLength];
                for (int i = 0; i < dataLength; i++) {
                    data[i] = random.nextInt(256);
                }
                assertCheckWordsCloseTheStream(coder, data, checkLength);
            }
        }
    }

    @Test
    void ecc200CheckWordsCloseTheStream() {
        ReedSolomonCoder coder = new ReedSolomonCoder(GfField.of(0x12D, 256), 1);
        Random random = new Random(11);
        for (int dataLength : new int[]{1, 3, 10, 62, 155}) {
            for (int checkLength : new int[]{1, 5, 7, 10, 11, 16}) {
                int[] data = new int[dataLength];
                for (int i = 0; i < dataLength; i++) {
                    data[i] = random.nextInt(256);
                }
                assertCheckWordsCloseTheStream(coder, data, checkLength);
            }
        }
    }

    @Test
    void aztecFieldsCheckWordsCloseTheStream() {
        // One coder per Aztec field, roots starting at alpha^1.
        int[][] fields = {{0x13, 16}, {0x43, 64}, {0x12D, 256}, {0x409, 1024}, {0x1069, 4096}};
        Random random = new Random(13);
        for (int[] f : fields) {
            ReedSolomonCoder coder = new ReedSolomonCoder(GfField.of(f[0], f[1]), 1);
            for (int dataLength : new int[]{3, 8, 12}) {
                for (int checkLength : new int[]{1, 4, 8}) {
                    int[] data = new int[dataLength];
                    for (int i = 0; i < dataLength; i++) {
                        data[i] = random.nextInt(f[1]);
                    }
                    assertCheckWordsCloseTheStream(coder, data, checkLength);
                }
            }
        }
    }

    @Test
    void appendCheckWordsMatchesEncode() {
        ReedSolomonCoder coder = new ReedSolomonCoder(GfField.of(0x11D, 256), 0);
        int[] data = {0x40, 0x90, 0x10, 0x0d};
        int[] expected = coder.encode(data, 4);
        // appendCheckWords works in place: the array must have room for the check words.
        int[] words = new int[8];
        System.arraycopy(data, 0, words, 0, 4);
        coder.appendCheckWords(words, 4);
        for (int i = 0; i < 4; i++) {
            assertEquals(expected[i], words[4 + i]);
        }
        // The data words are left untouched.
        for (int i = 0; i < 4; i++) {
            assertEquals(data[i], words[i]);
        }
    }

    @Test
    void encodeSliceMatchesEncodeOnCopy() {
        ReedSolomonCoder coder = new ReedSolomonCoder(GfField.of(0x12D, 256), 1);
        int[] message = {1, 2, 3, 4, 5, 6};
        assertDoesNotThrow(() -> coder.encodeSlice(message, 2, 3, 4));
        int[] slice = java.util.Arrays.copyOfRange(message, 2, 5);
        assertArrayEquals(coder.encode(slice, 4), coder.encodeSlice(message, 2, 3, 4));
        // The source slice is untouched.
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6}, message);
    }

    private static void assertArrayEquals(int[] expected, int[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
    }

    @Test
    void coderRejectsInvalidLengths() {
        ReedSolomonCoder coder = new ReedSolomonCoder(GfField.of(0x11D, 256), 0);
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> coder.encode(new int[]{1}, -1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> coder.appendCheckWords(new int[]{1, 2}, 3)));
    }

    @Test
    void emptyDataWithZeroCheckLengthYieldsEmpty() {
        ReedSolomonCoder coder = new ReedSolomonCoder(GfField.of(0x11D, 256), 0);
        assertEquals(0, coder.encode(new int[0], 0).length);
    }
}
