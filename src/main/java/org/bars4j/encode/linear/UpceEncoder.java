package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native UPC-E encoder (8 characters: 7 data + check digit). Ported from the legacy
 * {@code UPCEEncoder}.
 */
public final class UpceEncoder implements LinearEncoder {

    private static final String[] DIGIT_PARITY = {
            "111000", "110100", "110010", "110001", "101100",
            "100110", "100011", "101010", "101001", "100101"
    };

    private static final String RIGHT_GUARD_EXT = "010101";

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        if (text.length() != 8) {
            throw new InvalidSymbolException("[UPCE] Invalid text length (" + text.length() + ").");
        }
        StringBuilder bits = new StringBuilder();
        try {
            int check = LinearBits.digit(text.charAt(7));
            int primeiro = LinearBits.digit(text.charAt(0));
            String par = primeiro == 0
                    ? DIGIT_PARITY[check]
                    : LinearBits.invert(DIGIT_PARITY[check]);

            bits.append(EanSupport.LEFT_GUARD);
            for (int i = 0; i < 6; i++) {
                int atual = LinearBits.digit(text.charAt(i + 1));
                if (par.charAt(i) == '1') {
                    bits.append(LinearBits.reverse(EanSupport.CODES[atual]));
                } else {
                    bits.append(LinearBits.invert(EanSupport.CODES[atual]));
                }
            }
            bits.append(RIGHT_GUARD_EXT);
        } catch (NumberFormatException e) {
            throw new InvalidSymbolException("[UPCE] O padrao suporta apenas numeros.");
        }
        return LinearBits.symbol(bits.toString(), BitSemantics.MODULE_DARK);
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        if (text.length() != 7) {
            throw new InvalidSymbolException("[UPCE] Invalid text length (" + text.length() + ").");
        }
        return EanSupport.checkSum(convertUPCEtoUPCA(text));
    }

    public String convertUPCEtoUPCA(String upce) throws InvalidSymbolException {
        if (upce.length() != 7 && upce.length() != 8) {
            throw new InvalidSymbolException("[UPCE] Invalid text length (" + upce.length() + ").");
        }
        StringBuilder result = new StringBuilder();
        result.append(upce.charAt(0));
        switch (upce.charAt(6)) {
            case '0':
            case '1':
            case '2':
                result.append(upce.charAt(1));
                result.append(upce.charAt(2));
                result.append(upce.charAt(6));
                result.append("0000");
                result.append(upce.charAt(3));
                result.append(upce.charAt(4));
                result.append(upce.charAt(5));
                break;
            case '3':
                result.append(upce.charAt(1));
                result.append(upce.charAt(2));
                result.append(upce.charAt(3));
                result.append("00000");
                result.append(upce.charAt(4));
                result.append(upce.charAt(5));
                break;
            case '4':
                result.append(upce.charAt(1));
                result.append(upce.charAt(2));
                result.append(upce.charAt(3));
                result.append(upce.charAt(4));
                result.append("00000");
                result.append(upce.charAt(5));
                break;
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                result.append(upce.charAt(1));
                result.append(upce.charAt(2));
                result.append(upce.charAt(3));
                result.append(upce.charAt(4));
                result.append(upce.charAt(5));
                result.append("0000");
                result.append(upce.charAt(6));
                break;
            default:
                throw new InvalidSymbolException("[UPCE] Invalid char (" + upce.charAt(6) + ").");
        }
        if (upce.length() == 8) {
            result.append(upce.charAt(7));
        }
        return result.toString();
    }
}
