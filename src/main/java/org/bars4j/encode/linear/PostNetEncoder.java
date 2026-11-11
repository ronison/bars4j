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
        if (text == null || text.length() < 1) {
            throw new InvalidSymbolException("[PostNet] Tamanho de texto invalido (" + (text == null ? "null" : text.length()) + ").");
        }
        // The USPS POSTNET pattern only defines ZIP (5 data + check digit = 6), ZIP+4
        // (9 data + check digit = 10) and delivery point (11 data + check digit = 12)
        // barcodes; the encoder receives the complete code including the check digit.
        if (text.length() != 6 && text.length() != 10 && text.length() != 12) {
            throw new InvalidSymbolException("[PostNet] Invalid text length (" + text.length() + "): only 6, 10 or 12 digits are supported.");
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
        try {
            for (int i = 0; i < text.length(); i++) {
                check += Integer.parseInt(String.valueOf(text.charAt(i)));
            }
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[PostNet] O padrao suporta apenas numeros.");
        }
        int result = check % 10;
        if (result == 0) {
            return "0";
        }
        return String.valueOf(10 - result);
    }
}
