package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native Codabar encoder. Ported from the legacy {@code CodabarEncoder} (default start 'A',
 * stop '*').
 */
public final class CodabarEncoder implements LinearEncoder {

    private static final String[] CODES_WIDTH = {
            "0000011", "0000110", "0001001", "1100000", "0010010",
            "1000010", "0100001", "0100100", "0110000", "1001000",
            "0001100", "0011000", "1000101", "1010001", "1010100",
            "0011111", "0011010", "0001011", "0101001", "0001110"
    };

    private static final String INTER_CHAR = "0";
    private static final int START_CHAR_A = 16;
    private static final int STOP_CHAR_ASTERISC = 18;

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text.length() < 1) {
            throw new InvalidSymbolException("[Codabar] Invalid text length (" + text.length() + ").");
        }
        StringBuilder bits = new StringBuilder();
        bits.append(CODES_WIDTH[START_CHAR_A]);
        for (int i = 0; i < text.length(); i++) {
            bits.append(INTER_CHAR);
            bits.append(CODES_WIDTH[getCharIndex(text.charAt(i))]);
        }
        bits.append(INTER_CHAR);
        bits.append(CODES_WIDTH[STOP_CHAR_ASTERISC]);
        return LinearBits.symbol(bits.toString(), BitSemantics.WIDE_NARROW_ALTERNATING);
    }

    @Override
    public String computeCheckSum(String text) {
        return "";
    }

    private int getCharIndex(char c) throws InvalidSymbolException {
        if (c >= '0' && c <= '9') {
            return c - 48;
        } else if (c == '-') {
            return 10;
        } else if (c == '$') {
            return 11;
        } else if (c == ':') {
            return 12;
        } else if (c == '/') {
            return 13;
        } else if (c == '.') {
            return 14;
        } else if (c == '+') {
            return 15;
        }
        throw new InvalidSymbolException("[Codabar] The text contains not suported chars (" + c + ").");
    }
}
