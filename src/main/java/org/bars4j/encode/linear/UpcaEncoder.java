package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native UPC-A encoder (11/12 digits)., which reuses the
 * EAN-13 encoding of {@code "0" + text}.
 */
public final class UpcaEncoder implements LinearEncoder {

    private final Ean13Encoder ean13 = new Ean13Encoder();

    @Override
    public int completeLength() {
        return 12;
    }

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text == null || text.length() < 11 || text.length() > 12) {
            throw new InvalidSymbolException("[UPCA] Invalid text length (" + (text == null ? "null" : text.length()) + ").");
        }
        if (text.length() == 12) {
            try {
                // UPC-A shares the EAN-13 arithmetic with a leading zero.
                EanSupport.validateCheckDigit("UPCA", text.charAt(11),
                        EanSupport.checkSum("0" + text.substring(0, 11)));
            } catch (NumberFormatException e) {
                throw new InvalidSymbolException("[UPCA] Only numbers suported.");
            }
        }
        try {
            return ean13.encode("0" + text);
        } catch (InvalidSymbolException e) {
            throw new InvalidSymbolException("[UPCA] Only numbers suported.", e);
        }
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        try {
            return EanSupport.checkSum("0" + text);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[UPCA] Only numbers suported.");
        }
    }

    /**
     * Compresses a UPC-A code to its UPC-E form (number system + 6 data digits, plus the check
     * digit when the input is the complete 12-digit code).
     *
     * @param upca the 11-digit payload or the complete 12-digit UPC-A code
     * @return the UPC-E value (7 chars, or 8 when the check digit is carried over)
     * @throws InvalidSymbolException when the code is not compressible or has an invalid length
     */
    public String convertUPCAtoUPCE(String upca) throws InvalidSymbolException {
        if (upca == null || (upca.length() != 11 && upca.length() != 12)) {
            throw new InvalidSymbolException("[UPCA] Invalid text length ("
                    + (upca == null ? "null" : upca.length()) + ").");
        }
        StringBuilder result = new StringBuilder();
        result.append(upca.charAt(0));
        if (upca.charAt(0) != '0' && upca.charAt(0) != '1') {
            throw new InvalidSymbolException("[UPCA] Invalid Number System,  only 0 & 1 are valid ("
                    + upca.charAt(0) + ").");
        } else if ("000".equals(upca.substring(3, 6)) || "100".equals(upca.substring(3, 6))
                || "200".equals(upca.substring(3, 6))) {
            // Compression case 0/1/2 additionally requires product digits I1 and I2 to be zero.
            if (upca.charAt(6) != '0' || upca.charAt(7) != '0') {
                throw new InvalidSymbolException("[UPCA] Code cannot be compressed to UPC-E: product digits I1/I2 must be 00.");
            }
            result.append(upca, 1, 3);
            result.append(upca, 8, 11);
            result.append(upca.charAt(3));
        } else if ("00".equals(upca.substring(4, 6))) {
            // Compression case 3 additionally requires product digits I1..I3 to be zero.
            if (upca.charAt(6) != '0' || upca.charAt(7) != '0' || upca.charAt(8) != '0') {
                throw new InvalidSymbolException("[UPCA] Code cannot be compressed to UPC-E: product digits I1..I3 must be 000.");
            }
            result.append(upca, 1, 4);
            result.append(upca, 9, 11);
            result.append('3');
        } else if (upca.charAt(5) == '0') {
            // Compression case 4 additionally requires product digits I1..I4 to be zero.
            if (upca.charAt(6) != '0' || upca.charAt(7) != '0' || upca.charAt(8) != '0' || upca.charAt(9) != '0') {
                throw new InvalidSymbolException("[UPCA] Code cannot be compressed to UPC-E: product digits I1..I4 must be 0000.");
            }
            result.append(upca, 1, 5);
            result.append(upca.charAt(10));
            result.append('4');
        } else if (upca.charAt(10) >= '5' && upca.charAt(10) <= '9') {
            result.append(upca, 1, 6);
            result.append(upca.charAt(10));
        } else {
            throw new InvalidSymbolException("[UPCA] Invalid code.");
        }
        if (upca.length() == 12) {
            result.append(upca.charAt(11));
        }
        return result.toString();
    }
}
