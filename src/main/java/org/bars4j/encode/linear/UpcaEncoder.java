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
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text.length() < 11 || text.length() > 12) {
            throw new InvalidSymbolException("[UPCA] Invalid text length (" + text.length() + ").");
        }
        try {
            return ean13.encode("0" + text);
        } catch (InvalidSymbolException e) {
            throw new InvalidSymbolException("[UPCA] Only numbers suported.", e);
        }
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        return EanSupport.checkSum("0" + text);
    }
}
