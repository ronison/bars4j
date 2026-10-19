package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native Standard/Industrial 2 of 5 encoder. Ported from the legacy {@code Standard2of5Encoder}.
 */
public class Standard2of5Encoder implements LinearEncoder {

    protected String startChar = "101000";
    protected String stopChar = "10001";

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text.length() < 1) {
            throw new InvalidSymbolException("[Standard2of5] Invalid text length (" + text.length() + ").");
        }
        StringBuilder bits = new StringBuilder();
        try {
            bits.append(startChar);
            for (int i = 0; i < text.length(); i++) {
                int ind = LinearBits.digit(text.charAt(i));
                bits.append(expand(ToFSupport.CODES_WIDTH[ind]));
            }
            bits.append(stopChar);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[Standard2of5] Only numbers suported.");
        }
        return LinearBits.symbol(bits.toString(), BitSemantics.WIDE_NARROW_ALTERNATING);
    }

    /** Expands a 5-element code into the legacy 10-bit standard 2-of-5 pattern. */
    private static String expand(String code) {
        StringBuilder sb = new StringBuilder(10);
        for (int j = 0; j < code.length(); j++) {
            sb.append(code.charAt(j));
            sb.append('0');
        }
        return sb.toString();
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        return ToFSupport.checkSum(text);
    }
}
