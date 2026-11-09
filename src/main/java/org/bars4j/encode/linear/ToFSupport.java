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

    /**
     * GS1 mod-10 check digit over the given data digits.
     *
     * <p>The weights are anchored to the <em>right</em> (the rightmost data digit gets weight 3,
     * alternating 1/3 towards the left), as required by the GS1/GTIN and Interleaved 2 of 5
     * standards. Anchoring to the left of the string would only be equivalent for even-length
     * payloads and would produce a wrong digit for odd lengths (e.g. the 13 data digits of an
     * ITF-14/GTIN-14).
     */
    static String checkSum(String text) {
        int sum = 0;
        int length = text.length();
        for (int charPos = 0; charPos < length; charPos++) {
            int digit = LinearBits.digit(text.charAt(charPos));
            if ((length - charPos) % 2 == 1) {
                sum += digit * 3;
            } else {
                sum += digit;
            }
        }
        int result = sum % 10;
        return result == 0 ? "0" : String.valueOf(10 - result);
    }
}
