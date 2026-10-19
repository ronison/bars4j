package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native Matrix 2 of 5 encoder. Ported from the legacy {@code Matrix2of5Encoder}.
 */
public class Matrix2of5Encoder extends Standard2of5Encoder {

    public Matrix2of5Encoder() {
        this.startChar = "100000";
        this.stopChar = "10000";
    }

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text.length() < 1) {
            throw new InvalidSymbolException("[Matrix2of5] Invalid text length (" + text.length() + ").");
        }
        StringBuilder bits = new StringBuilder();
        try {
            bits.append(startChar);
            for (int i = 0; i < text.length(); i++) {
                int ind = LinearBits.digit(text.charAt(i));
                bits.append(ToFSupport.CODES_WIDTH[ind]);
                bits.append('0');
            }
            bits.append(stopChar);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[Matrix2of5] Only numbers suported.");
        }
        return LinearBits.symbol(bits.toString(), BitSemantics.WIDE_NARROW_ALTERNATING);
    }
}
