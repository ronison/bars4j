package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native EAN-8 encoder (8 digits). Ported from the legacy {@code EAN8Encoder}.
 */
public final class Ean8Encoder implements LinearEncoder {

    @Override
    public int completeLength() {
        return 8;
    }

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text.length() != 8) {
            throw new InvalidSymbolException("[EAN8] Invalid text length (" + text.length() + ").");
        }
        StringBuilder bits = new StringBuilder();
        try {
            EanSupport.validateCheckDigit("EAN8", text.charAt(7), EanSupport.checkSum(text.substring(0, 7)));
            bits.append(EanSupport.LEFT_GUARD);
            for (int i = 0; i < 4; i++) {
                bits.append(LinearBits.invert(EanSupport.CODES[LinearBits.digit(text.charAt(i))]));
            }
            bits.append(EanSupport.CENTER_GUARD);
            for (int i = 4; i < 8; i++) {
                bits.append(EanSupport.CODES[LinearBits.digit(text.charAt(i))]);
            }
            bits.append(EanSupport.RIGHT_GUARD);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[EAN8] Only numbers suported.");
        }
        return LinearBits.symbol(bits.toString(), BitSemantics.MODULE_DARK);
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        try {
            return EanSupport.checkSum(text);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[EAN8] Only numbers suported.");
        }
    }
}
