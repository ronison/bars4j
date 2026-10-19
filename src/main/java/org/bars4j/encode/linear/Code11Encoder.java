package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native Code 11 encoder. Ported from the legacy {@code Code11Encoder}.
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
        if (text.length() < 1) {
            throw new InvalidSymbolException("[Code11] Invalid text length (" + text.length() + ").");
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
        // Calc 'C' check digit char
        int check = 0;
        for (int i = text.length() - 1; i >= 0; i--) {
            int weight = (text.length() - i) % 10;
            weight = weight == 0 ? 10 : weight;
            check += getCharIndex(text.charAt(i)) * weight;
        }
        StringBuilder result = new StringBuilder(Integer.toString(check % 11));

        if (text.length() + result.length() >= 10) {
            // Calc 'K' check digit char
            check = 0;
            String temp = text + result;
            for (int i = temp.length() - 1; i >= 0; i--) {
                int weight = (temp.length() - i) % 9;
                weight = weight == 0 ? 9 : weight;
                check += getCharIndex(temp.charAt(i)) * weight;
            }
            result.append(Integer.toString(check % 9));
        }
        return result.toString();
    }

    private int getCharIndex(char val) {
        return val == '-' ? 10 : val - '0';
    }
}
