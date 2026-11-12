package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native Code 11 encoder.
 */
public final class Code11Encoder implements LinearEncoder {

    private static final String[] CODES_WIDTH = {
            "101011", "1101011", "1001011", "1100101", "1011011",
            "1101101", "1001101", "1010011", "1101001", "110101",
            "101101"
    };

    private static final String INTER_CHAR = "0";
    private static final String START_STOP_CHAR = "1011001";

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text == null || text.length() < 1) {
            throw new InvalidSymbolException("[Code11] Invalid text length (" + (text == null ? "null" : text.length()) + ").");
        }
        StringBuilder bits = new StringBuilder();
        bits.append(START_STOP_CHAR);
        bits.append(INTER_CHAR);
        for (int i = 0; i < text.length(); i++) {
            bits.append(CODES_WIDTH[getCharIndex(text.charAt(i))]);
            bits.append(INTER_CHAR);
        }
        bits.append(START_STOP_CHAR);
        return LinearBits.symbol(bits.toString(), BitSemantics.MODULE_DARK);
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        if (text == null || text.length() < 1) {
            throw new InvalidSymbolException("[Code11] Invalid text length (" + (text == null ? "null" : text.length()) + ").");
        }
        // Calc 'C' check digit char (modulo 11, weights 1..10 from the right)
        int check = 0;
        for (int i = text.length() - 1; i >= 0; i--) {
            int weight = (text.length() - i) % 10;
            weight = weight == 0 ? 10 : weight;
            check += getCharIndex(text.charAt(i)) * weight;
        }
        StringBuilder result = new StringBuilder(checkDigitToString(check % 11));

        if (text.length() >= 10) {
            // Calc 'K' check digit char (modulo 11, weights 1..9 from the right, with the C
            // check digit included as the right-most character).
            check = 0;
            String temp = text + result;
            for (int i = temp.length() - 1; i >= 0; i--) {
                int weight = (temp.length() - i) % 9;
                weight = weight == 0 ? 9 : weight;
                check += getCharIndex(temp.charAt(i)) * weight;
            }
            result.append(checkDigitToString(check % 11));
        }
        return result.toString();
    }

    /**
     * Renders a check digit value: the value 10 is the dash character, per the Code 11
     * specification.
     */
    private static String checkDigitToString(int value) {
        return value == 10 ? "-" : Integer.toString(value);
    }

    private int getCharIndex(char val) throws InvalidSymbolException {
        if (val >= '0' && val <= '9') {
            return val - '0';
        } else if (val == '-') {
            return 10;
        }
        throw new InvalidSymbolException("[Code11] The text contains unsuported chars.");
    }
}
