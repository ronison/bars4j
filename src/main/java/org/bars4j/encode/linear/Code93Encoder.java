package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native Code 93 encoder. Ported from the legacy {@code Code93Encoder}.
 */
public class Code93Encoder implements LinearEncoder {

    protected static final String[] CODES = {
            "100010100", "101001000", "101000100", "101000010", "100101000",
            "100100100", "100100010", "101010000", "100010010", "100001010",
            "110101000", "110100100", "110100010", "110010100", "110010010",
            "110001010", "101101000", "101100100", "101100010", "100110100",
            "100011010", "101011000", "101001100", "101000110", "100101100",
            "100010110", "110110100", "110110010", "110101100", "110100110",
            "110010110", "110011010", "101101100", "101100110", "100110110",
            "100111010", "100101110", "111010100", "111010010", "111001010",
            "101101110", "101110110", "110101110", "100100110", "111011010",
            "111010110", "100110010"
    };

    private static final String START_STOP_CHAR = "101011110";
    private static final String TERMINATION_BAR = "1";

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text.length() < 1) {
            throw new InvalidSymbolException("[Code93] Invalid text length (" + text.length() + ").");
        }
        StringBuilder bits = new StringBuilder();
        bits.append(START_STOP_CHAR);
        for (int i = 0; i < text.length(); i++) {
            bits.append(CODES[getCharIndex(text.charAt(i))]);
        }
        bits.append(START_STOP_CHAR);
        bits.append(TERMINATION_BAR);
        return LinearBits.symbol(bits.toString(), BitSemantics.MODULE_DARK);
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        // Calc 'C' check digit char
        int check = 0;
        for (int i = text.length() - 1; i >= 0; i--) {
            int weight = (text.length() - i) % 20;
            weight = weight == 0 ? 20 : weight;
            check += getCharIndex(text.charAt(i)) * weight;
        }
        StringBuilder result = new StringBuilder(Character.toString(getChar((byte) (check % 47))));

        // Calc 'K' check digit char
        check = 0;
        String temp = text + result;
        for (int i = temp.length() - 1; i >= 0; i--) {
            int weight = (temp.length() - i) % 15;
            weight = weight == 0 ? 15 : weight;
            check += getCharIndex(temp.charAt(i)) * weight;
        }
        result.append(Character.toString(getChar((byte) (check % 47))));
        return result.toString();
    }

    protected int getCharIndex(char c) throws InvalidSymbolException {
        if (c >= '0' && c <= '9') {
            return c - 48;
        } else if (c >= 'A' && c <= 'Z') {
            return c - 55;
        } else if (c == '-') {
            return 36;
        } else if (c == '.') {
            return 37;
        } else if (c == ' ') {
            return 38;
        } else if (c == '$') {
            return 39;
        } else if (c == '/') {
            return 40;
        } else if (c == '+') {
            return 41;
        } else if (c == '%') {
            return 42;
        } else if (c == '\u00B9') { // special char ($)
            return 43;
        } else if (c == '\u00B2') { // special char (%)
            return 44;
        } else if (c == '\u00B3') { // special char (/)
            return 45;
        } else if (c == '\u00A3') { // special char (+)
            return 46;
        }
        throw new InvalidSymbolException("[Code93] The text contains unsuported chars.");
    }

    protected char getChar(byte c) throws InvalidSymbolException {
        if (c >= 0 && c <= 9) {
            return (char) (c + 48);
        } else if (c >= 10 && c <= 35) {
            return (char) (c + 55);
        } else if (c == 36) {
            return '-';
        } else if (c == 37) {
            return '.';
        } else if (c == 38) {
            return ' ';
        } else if (c == 39) {
            return '$';
        } else if (c == 40) {
            return '/';
        } else if (c == 41) {
            return '+';
        } else if (c == 42) {
            return '%';
        } else if (c == 43) {
            return '\u00B9';
        } else if (c == 44) {
            return '\u00B2';
        } else if (c == 45) {
            return '\u00B3';
        } else if (c == 46) {
            return '\u00A3';
        }
        throw new InvalidSymbolException("[Code93] The text contains unsuported chars.");
    }
}
