package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native UPC-A encoder (11/12 digits). Ported from the legacy {@code UPCAEncoder}, which reuses the
 * EAN-13 encoding of {@code "0" + text}.
 */
public final class UpcaEncoder implements LinearEncoder {

    private final Ean13Encoder ean13 = new Ean13Encoder();

    @Override
    public int completeLength() {
        return 12;
    }

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text == null || text.length() < 11 || text.length() > 12) {
            throw new InvalidSymbolException("[UPCA] Invalid text length (" + (text == null ? "null" : text.length()) + ").");
        }
        if (text.length() == 12) {
            try {
                // UPC-A shares the EAN-13 arithmetic with a leading zero.
                EanSupport.validateCheckDigit("UPCA", text.charAt(11),
                        EanSupport.checkSum("0" + text.substring(0, 11)));
            } catch (NumberFormatException e) {
                throw new InvalidSymbolException("[UPCA] Only numbers suported.");
            }
        }
        try {
            return ean13.encode("0" + text);
        } catch (InvalidSymbolException e) {
            throw new InvalidSymbolException("[UPCA] Only numbers suported.", e);
        }
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        try {
            return EanSupport.checkSum("0" + text);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[UPCA] Only numbers suported.");
        }
    }
}
