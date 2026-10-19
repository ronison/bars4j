package org.bars4j.encode.linear;

import org.bars4j.encode.BarcodeEncoder;
import org.bars4j.encode.InvalidAtributeException;
import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Adapts a legacy {@link BarcodeEncoder} (returning {@code BarSet[]}) to the new
 * {@link LinearEncoder} API, converting the bit array into a {@link LinearSymbol} according to
 * the format's {@link BitSemantics}.
 */
public final class LegacyLinearEncoderAdapter implements LinearEncoder {

    private final BarcodeEncoder legacy;
    private final BitSemantics semantics;

    public LegacyLinearEncoderAdapter(BarcodeEncoder legacy, BitSemantics semantics) {
        this.legacy = legacy;
        this.semantics = semantics;
    }

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        try {
            return BarSetBridge.toSymbol(legacy.encode(text), semantics);
        } catch (InvalidAtributeException e) {
            throw new InvalidSymbolException(e.getMessage(), e);
        }
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        try {
            return legacy.computeCheckSum(text);
        } catch (InvalidAtributeException e) {
            throw new InvalidSymbolException(e.getMessage(), e);
        }
    }
}
