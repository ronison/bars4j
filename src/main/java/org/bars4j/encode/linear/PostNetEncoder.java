package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native PostNet encoder. Ported from the legacy {@code PostNetEncoder}.
 */
public final class PostNetEncoder implements LinearEncoder {

    private static final String[] CODES = {
            "11000", "00011", "00101", "00110", "01001",
            "01010", "01100", "10001", "10010", "10100"
    };

    private static final String START_STOP_CHAR = "1";

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text.length() < 1) {
            throw new InvalidSymbolException("[PostNet] Tamanho de texto invalido (" + text.length() + ").");
        }
        StringBuilder bits = new StringBuilder();
        try {
            bits.append(START_STOP_CHAR);
            for (int i = 0; i < text.length(); i++) {
                int atual = Integer.parseInt(String.valueOf(text.charAt(i)));
                bits.append(CODES[atual]);
            }
            bits.append(START_STOP_CHAR);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[PostNet] O padrao suporta apenas numeros.");
        }
        return LinearBits.symbol(bits.toString(), BitSemantics.HEIGHT_CODED);
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        int check = 0;
        for (int i = 0; i < text.length(); i++) {
            check += Integer.parseInt(String.valueOf(text.charAt(i)));
        }
        int result = check % 10;
        if (result == 0) {
            return "0";
        }
        return String.valueOf(10 - result);
    }
}
