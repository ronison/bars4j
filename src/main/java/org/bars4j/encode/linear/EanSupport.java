package org.bars4j.encode.linear;

/**
 * Shared EAN/UPC tables and arithmetic (ported from the legacy {@code EANEncoder}).
 */
final class EanSupport {

    static final String[] CODES = {
            "1110010", "1100110", "1101100", "1000010", "1011100",
            "1001110", "1010000", "1000100", "1001000", "1110100"
    };

    static final String LEFT_GUARD = "101";
    static final String CENTER_GUARD = "01010";
    static final String RIGHT_GUARD = "101";

    private EanSupport() {
    }

    static String checkSum(String text) {
        int sum = 0;
        int odd = text.length() % 2;
        for (int charPos = 0; charPos < text.length(); charPos++) {
            if ((charPos + odd) % 2 > 0) {
                sum += LinearBits.digit(text.charAt(charPos)) * 3;
            } else {
                sum += LinearBits.digit(text.charAt(charPos));
            }
        }
        int result = sum % 10;
        return result == 0 ? "0" : Integer.toString(10 - result);
    }
}
