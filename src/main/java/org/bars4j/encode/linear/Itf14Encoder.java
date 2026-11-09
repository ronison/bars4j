package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * ITF-14 (GS1 GTIN-14) encoder.
 *
 * <p>ITF-14 is the Interleaved 2 of 5 symbology restricted to a 14-digit code: 13 data digits
 * followed by the GS1 mod-10 check digit. Unlike the generic {@link Interleaved2of5Encoder},
 * this encoder enforces the fixed length and validates a supplied check digit.
 *
 * <p>The input may be either the 13 data digits (the check digit is then computed and appended)
 * or the complete 14-digit code (its check digit is validated and kept verbatim).
 */
public final class Itf14Encoder implements LinearEncoder {

    /** ITF-14 always encodes exactly 14 digits (13 data + 1 check digit). */
    public static final int CODE_LENGTH = 14;

    /** ITF-14 uses the Interleaved 2 of 5 start pattern (4 narrow elements). */
    private static final String START_CHAR = "0000";

    /** ITF-14 uses the Interleaved 2 of 5 stop pattern (2 narrow bars + 1 narrow space / bar). */
    private static final String STOP_CHAR = "100";

    @Override
    public int completeLength() {
        return CODE_LENGTH;
    }

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        String digits = complete(text);
        StringBuilder bits = new StringBuilder();
        try {
            bits.append(START_CHAR);
            for (int i = 0; i < digits.length() / 2; i++) {
                String first = ToFSupport.CODES_WIDTH[LinearBits.digit(digits.charAt(2 * i))];
                String second = ToFSupport.CODES_WIDTH[LinearBits.digit(digits.charAt((2 * i) + 1))];
                for (int j = 0; j < first.length(); j++) {
                    bits.append(first.charAt(j));
                    bits.append(second.charAt(j));
                }
            }
            bits.append(STOP_CHAR);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[ITF14] Only numbers suported.");
        }
        return LinearBits.symbol(bits.toString(), BitSemantics.WIDE_NARROW_ALTERNATING);
    }

    /**
     * Normalises the input to the complete 14-digit code, appending the computed check digit to a
     * 13-digit payload or validating the supplied one for a 14-digit code.
     */
    private static String complete(String text) throws InvalidSymbolException {
        if (text == null || (text.length() != CODE_LENGTH - 1 && text.length() != CODE_LENGTH)) {
            int length = text == null ? 0 : text.length();
            throw new InvalidSymbolException("[ITF14] Invalid text length (" + length + ").");
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                throw new InvalidSymbolException("[ITF14] Only numbers suported.");
            }
        }
        if (text.length() == CODE_LENGTH) {
            String payload = text.substring(0, CODE_LENGTH - 1);
            EanSupport.validateCheckDigit("ITF14", text.charAt(CODE_LENGTH - 1),
                    ToFSupport.checkSum(payload));
            return text;
        }
        return text + ToFSupport.checkSum(text);
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        if (text == null || (text.length() != CODE_LENGTH - 1 && text.length() != CODE_LENGTH)) {
            int length = text == null ? 0 : text.length();
            throw new InvalidSymbolException("[ITF14] Invalid text length (" + length + ").");
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                throw new InvalidSymbolException("[ITF14] Only numbers suported.");
            }
        }
        String payload = text.substring(0, CODE_LENGTH - 1);
        return ToFSupport.checkSum(payload);
    }
}
