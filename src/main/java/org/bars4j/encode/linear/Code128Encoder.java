package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native Code 128 encoder. Ported from the legacy {@code Code128Encoder} (character sets B and C).
 */
public final class Code128Encoder implements LinearEncoder {

    private static final int START_CODE_B_ALPHA = 104;
    private static final int START_CODE_C_NUMERIC = 105;
    private static final int SWITCH_TO_CODE_B_ALPHA = 100;
    private static final int SWITCH_TO_CODE_C_NUMERIC = 99;
    private static final int STOP = 106;

    private static final String[] BARSETS = {
            "11011001100", "11001101100", "11001100110", "10010011000", "10010001100",
            "10001001100", "10011001000", "10011000100", "10001100100", "11001001000",
            "11001000100", "11000100100", "10110011100", "10011011100", "10011001110",
            "10111001100", "10011101100", "10011100110", "11001110010", "11001011100",
            "11001001110", "11011100100", "11001110100", "11101101110", "11101001100",
            "11100101100", "11100100110", "11101100100", "11100110100", "11100110010",
            "11011011000", "11011000110", "11000110110", "10100011000", "10001011000",
            "10001000110", "10110001000", "10001101000", "10001100010", "11010001000",
            "11000101000", "11000100010", "10110111000", "10110001110", "10001101110",
            "10111011000", "10111000110", "10001110110", "11101110110", "11010001110",
            "11000101110", "11011101000", "11011100010", "11011101110", "11101011000",
            "11101000110", "11100010110", "11101101000", "11101100010", "11100011010",
            "11101111010", "11001000010", "11110001010", "10100110000", "10100001100",
            "10010110000", "10010000110", "10000101100", "10000100110", "10110010000",
            "10110000100", "10011010000", "10011000010", "10000110100", "10000110010",
            "11000010010", "11001010000", "11110111010", "11000010100", "10001111010",
            "10100111100", "10010111100", "10010011110", "10111100100", "10011110100",
            "10011110010", "11110100100", "11110010100", "11110010010", "11011011110",
            "11011110110", "11110110110", "10101111000", "10100011110", "10001011110",
            "10111101000", "10111100010", "11110101000", "11110100010", "10111011110",
            "10111101110", "11101011110", "11110101110", "11010000100", "11010010000",
            "11010011100", "11000111010"
    };

    private static final String TERMINATION_BAR = "11";

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text == null || text.length() < 1) {
            throw new InvalidSymbolException("[Code128] Invalid text (" + text + ").");
        }

        StringBuilder bits = new StringBuilder();
        byte[] bytes = text.getBytes();

        boolean usingNumericEncoding = false;
        int code = START_CODE_B_ALPHA;

        if (bytes.length > 1 && isDigit(bytes[0]) && isDigit(bytes[1])) {
            usingNumericEncoding = true;
            code = START_CODE_C_NUMERIC;
        }

        int checksum = code;
        bits.append(BARSETS[code]);

        int checkMultiplier = 1;

        for (int i = 0; i < bytes.length; i++) {
            if (i < bytes.length - 1 && isDigit(bytes[i]) && isDigit(bytes[i + 1])) {
                if (!usingNumericEncoding) {
                    usingNumericEncoding = true;
                    code = SWITCH_TO_CODE_C_NUMERIC;
                    checksum += code * checkMultiplier;
                    checkMultiplier++;
                    bits.append(BARSETS[code]);
                }
                code = Integer.parseInt(new String(bytes, i, 2));
                i++;
            } else {
                if (usingNumericEncoding) {
                    usingNumericEncoding = false;
                    code = SWITCH_TO_CODE_B_ALPHA;
                    checksum += code * checkMultiplier;
                    checkMultiplier++;
                    bits.append(BARSETS[code]);
                }
                code = getCode(bytes[i]);
            }

            checksum += code * checkMultiplier;
            checkMultiplier++;

            bits.append(BARSETS[code]);
        }

        int checkDigit = checksum % 103;
        bits.append(BARSETS[checkDigit]);
        bits.append(BARSETS[STOP]);
        bits.append(TERMINATION_BAR);

        return LinearBits.symbol(bits.toString(), BitSemantics.MODULE_DARK);
    }

    @Override
    public String computeCheckSum(String text) {
        return "";
    }

    private static boolean isDigit(byte b) {
        return b >= '0' && b <= '9';
    }

    private static int getCode(byte s) {
        return s - 32;
    }
}
