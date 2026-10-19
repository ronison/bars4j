package org.bars4j.encode.linear;

/**
 * Shared 2-of-5 tables and arithmetic (ported from the legacy {@code ToFEncoder}).
 */
final class ToFSupport {

    static final String[] CODES_WIDTH = {
            "00110", "10001", "01001", "11000", "00101",
            "10100", "01100", "00011", "10010", "01010"
    };

    private ToFSupport() {
    }

    static String checkSum(String text) {
        int sum = 0;
        for (int charPos = 0; charPos < text.length(); charPos++) {
            if (charPos % 2 == 0) {
                sum += LinearBits.digit(text.charAt(charPos));
            } else {
                sum += LinearBits.digit(text.charAt(charPos)) * 3;
            }
        }
        int result = sum % 10;
        if (result == 0) {
            return "0";
        }
        return String.valueOf(10 - result);
    }
}
