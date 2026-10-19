package org.bars4j.encode.linear;

import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.symbol.LinearSymbol;

/**
 * Native Code 39 Extended encoder. Ported from the legacy {@code Code39ExtEncoder}.
 */
public final class Code39ExtEncoder extends Code39Encoder {

    private static final String[] CODES_TABLE = {
            "%U", "$A", "$B", "$C", "$D", "$E", "$F", "$G",
            "$H", "$I", "$J", "$K", "$L", "$M", "$N", "$O",
            "$P", "$Q", "$R", "$S", "$T", "$U", "$V", "$W",
            "$X", "$Y", "$Z", "%A", "%B", "%C", "%D", "%E",
            " ", "/A", "/B", "/C", "/D", "/E", "/F", "/G",
            "/H", "/I", "/J", "/K", "/L", "-", ".", "/O",
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "/Z", "%F", "%G", "%H", "%I", "%J",
            "%V", "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
            "T", "U", "V", "W", "X", "Y", "Z", "%K", "%L", "%M",
            "%N", "%O", "%W", "+A", "+B", "+C", "+D", "+E",
            "+F", "+G", "+H", "+I", "+J", "+K", "+L", "+M",
            "+N", "+O", "+P", "+Q", "+R", "+S", "+T", "+U",
            "+V", "+W", "+X", "+Y", "+Z", "%P", "%Q", "%R",
            "%S", "%T"
    };

    @Override
    public LinearSymbol encode(String text) throws InvalidSymbolException {
        return super.encode(convertText(text));
    }

    @Override
    public String computeCheckSum(String text) throws InvalidSymbolException {
        return super.computeCheckSum(convertText(text));
    }

    private static String convertText(String str) {
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            tmp.append(CODES_TABLE[str.charAt(i)]);
        }
        return tmp.toString();
    }
}
