package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native Interleaved 2 of 5 encoder. Ported from the legacy {@code Interleaved2of5Encoder}.
 */
public final class Interleaved2of5Encoder implements LinearEncoder {

    private static final String START_CHAR = "0000";
    private static final String STOP_CHAR = "100";

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text.length() < 1) {
            throw new InvalidSymbolException("[Interleaved2of5] Invalid text length (" + text.length() + ").");
        }
        StringBuilder bits = new StringBuilder();
        try {
            bits.append(START_CHAR);
            for (int i = 0; i < text.length() / 2; i++) {
                String first = ToFSupport.CODES_WIDTH[LinearBits.digit(text.charAt(2 * i))];
                String second = ToFSupport.CODES_WIDTH[LinearBits.digit(text.charAt((2 * i) + 1))];
                for (int j = 0; j < first.length(); j++) {
                    bits.append(first.charAt(j));
                    bits.append(second.charAt(j));
                }
            }
            bits.append(STOP_CHAR);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[Interleaved2of5] Only numbers suported.");
        }
        return LinearBits.symbol(bits.toString(), BitSemantics.WIDE_NARROW_ALTERNATING);
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        return ToFSupport.checkSum(text);
    }
}
