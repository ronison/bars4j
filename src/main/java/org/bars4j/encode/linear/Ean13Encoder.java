package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native EAN-13 encoder (12/13 digits).
 */
public final class Ean13Encoder implements LinearEncoder {

    private static final String[] DIGIT_PARITY = {
            "000000", "001011", "001101", "001110", "010011",
            "011001", "011100", "010101", "010110", "011010"
    };

    @Override
    public int completeLength() {
        return 13;
    }

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text == null || text.length() < 12 || text.length() > 13) {
            throw new InvalidSymbolException("[EAN13] Invalid text length (" + (text == null ? "null" : text.length()) + ").");
        }
        // A 12-digit input is the data part: complete it with its check digit. A 13-digit input is
        // already complete, so its check digit is validated and the text is encoded verbatim.
        String digits;
        try {
            if (text.length() == 13) {
                EanSupport.validateCheckDigit("EAN13", text.charAt(12), EanSupport.checkSum(text.substring(0, 12)));
                digits = text;
            } else {
                digits = text + computeCheckSum(text);
            }
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[EAN13] Only numbers suported.");
        }
        StringBuilder bits = new StringBuilder();
        try {
            int primeiro = LinearBits.digit(digits.charAt(0));
            bits.append(EanSupport.LEFT_GUARD);
            for (int i = 1; i <= 6; i++) {
                int atual = LinearBits.digit(digits.charAt(i));
                if (DIGIT_PARITY[primeiro].charAt(i - 1) == '1') {
                    bits.append(LinearBits.reverse(EanSupport.CODES[atual]));
                } else {
                    bits.append(LinearBits.invert(EanSupport.CODES[atual]));
                }
            }
            bits.append(EanSupport.CENTER_GUARD);
            for (int i = 7; i <= 12; i++) {
                bits.append(EanSupport.CODES[LinearBits.digit(digits.charAt(i))]);
            }
            bits.append(EanSupport.RIGHT_GUARD);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[EAN13] Only numbers suported.");
        }
        return LinearBits.symbol(bits.toString(), BitSemantics.MODULE_DARK);
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        try {
            return EanSupport.checkSum(text);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[EAN13] Only numbers suported.");
        }
    }
}
