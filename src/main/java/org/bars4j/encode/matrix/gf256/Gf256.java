package org.bars4j.encode.matrix.gf256;

/**
 * Arithmetic over GF(256) with primitive polynomial {@code x^8 + x^4 + x^3 + x^2 + 1} (0x11D)
 * and generator {@code alpha = 2}, as required by QRCode error correction.
 */
public final class Gf256 {

    private static final int PRIMITIVE_POLYNOMIAL = 0x11D;
    private static final int[] EXP = new int[512];
    private static final int[] LOG = new int[256];

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            EXP[i] = x;
            LOG[x] = i;
            x <<= 1;
            if ((x & 0x100) != 0) {
                x ^= PRIMITIVE_POLYNOMIAL;
            }
        }
        for (int i = 255; i < 512; i++) {
            EXP[i] = EXP[i - 255];
        }
    }

    private Gf256() {
    }

    public static int exp(int power) {
        int p = power % 255;
        if (p < 0) {
            p += 255;
        }
        return EXP[p];
    }

    public static int log(int value) {
        if (value == 0) {
            throw new IllegalArgumentException("log(0) is undefined");
        }
        return LOG[value];
    }

    public static int multiply(int a, int b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        return EXP[LOG[a] + LOG[b]];
    }
}
