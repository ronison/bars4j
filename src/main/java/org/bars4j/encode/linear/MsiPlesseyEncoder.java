package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native MSI/Plessey encoder. Ported from the legacy {@code MSIPlesseyEncoder}.
 */
public final class MsiPlesseyEncoder implements LinearEncoder {

    private static final String[] CODES_WIDTH = {
            "100100100100", "100100100110", "100100110100", "100100110110", "100110100100",
            "100110100110", "100110110100", "100110110110", "110100100100", "110100100110"
    };

    private static final String START_CHAR = "110";
    private static final String STOP_CHAR = "1001";

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        StringBuilder bits = new StringBuilder();
        try {
            bits.append(START_CHAR);
            for (int i = 0; i < text.length(); i++) {
                bits.append(CODES_WIDTH[charToInt(text.charAt(i))]);
            }
            bits.append(STOP_CHAR);
        } catch (Exception e) {
            throw new InvalidSymbolException("[MSIPlessey] Only numbers suported.");
        }
        return LinearBits.symbol(bits.toString(), BitSemantics.MODULE_DARK);
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        StringBuilder newNumber = new StringBuilder();
        StringBuilder numTotal = new StringBuilder();
        for (int i = text.length() - 1; i >= 0; i -= 2) {
            newNumber.insert(0, text.charAt(i));
            if (i > 0) {
                numTotal.append(text.charAt(i - 1));
            }
        }
        numTotal.append(2 * Integer.parseInt(newNumber.toString()));

        int sum = 0;
        String num = numTotal.toString();
        for (int i = 0; i < num.length(); i++) {
            sum += charToInt(num.charAt(i));
        }

        return Integer.toString(10 - (sum % 10 == 0 ? 10 : sum % 10));
    }

    private static int charToInt(char val) {
        return val - '0';
    }
}
