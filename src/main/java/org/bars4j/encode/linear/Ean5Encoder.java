package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native EAN-5 (supplemental) encoder. Ported from the legacy {@code EAN5Encoder}.
 */
public final class Ean5Encoder implements LinearEncoder {

    private static final String[] DIGIT_PARITY = {
            "11000", "10100", "10010", "10001", "01100",
            "00110", "00011", "01010", "01001", "00101"
    };

    private static final String START = "01011";
    private static final String SEPARATOR = "01";

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text.length() != 5) {
            throw new InvalidSymbolException("[EAN5] Invalid text length (" + text.length() + ").");
        }
        StringBuilder bits = new StringBuilder();
        try {
            int checksum = Integer.parseInt(computeCheckSum(text));
            bits.append(START);
            for (int i = 1; i <= 5; i++) {
                int atual = LinearBits.digit(text.charAt(i - 1));
                if (DIGIT_PARITY[checksum].charAt(i - 1) == '1') {
                    bits.append(LinearBits.reverse(EanSupport.CODES[atual]));
                } else {
                    bits.append(LinearBits.invert(EanSupport.CODES[atual]));
                }
                if (i < 5) {
                    bits.append(SEPARATOR);
                }
            }
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[EAN5] Only numbers suported.");
        }
        return LinearBits.symbol(bits.toString(), BitSemantics.MODULE_DARK);
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        int sum = 0;
        boolean odd = true;
        for (int charPos = 0; charPos < text.length(); charPos++) {
            if (odd) {
                sum += 3 * LinearBits.digit(text.charAt(charPos));
            } else {
                sum += 9 * LinearBits.digit(text.charAt(charPos));
            }
            odd = !odd;
        }
        return Integer.toString(sum % 10);
    }
}
