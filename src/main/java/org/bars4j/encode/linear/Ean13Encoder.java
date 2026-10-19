package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native EAN-13 encoder (12/13 digits). Ported from the legacy {@code EAN13Encoder}.
 */
public final class Ean13Encoder implements LinearEncoder {

    private static final String[] DIGIT_PARITY = {
            "000000", "001011", "001101", "001110", "010011",
            "011001", "011100", "010101", "010110", "011010"
    };

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text.length() < 12 || text.length() > 13) {
            throw new InvalidSymbolException("[EAN13] Invalid text length (" + text.length() + ").");
        }
        StringBuilder bits = new StringBuilder();
        try {
            int primeiro = LinearBits.digit(text.charAt(0));
            bits.append(EanSupport.LEFT_GUARD);
            for (int i = 1; i <= 6; i++) {
                int atual = LinearBits.digit(text.charAt(i));
                if (DIGIT_PARITY[primeiro].charAt(i - 1) == '1') {
                    bits.append(LinearBits.reverse(EanSupport.CODES[atual]));
                } else {
                    bits.append(LinearBits.invert(EanSupport.CODES[atual]));
                }
            }
            bits.append(EanSupport.CENTER_GUARD);
            for (int i = 7; i <= 12; i++) {
                bits.append(EanSupport.CODES[LinearBits.digit(text.charAt(i))]);
            }
            bits.append(EanSupport.RIGHT_GUARD);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[EAN13] Only numbers suported.");
        }
        return LinearBits.symbol(bits.toString(), BitSemantics.MODULE_DARK);
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        return EanSupport.checkSum(text);
    }
}
