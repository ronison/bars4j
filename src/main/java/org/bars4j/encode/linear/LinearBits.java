package org.bars4j.encode.linear;

import java.util.ArrayList;
import java.util.List;

import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.BarElement;
import org.bars4j.symbol.LinearSymbol;

/**
 * Helpers shared by the native linear encoders.
 *
 * <p>Encoders build the bit string exactly like the legacy {@code BarSet[]} arrays did (concatenating
 * the per-character bit patterns in order); this class turns that bit string into a
 * {@link LinearSymbol} using the same {@link BitSemantics} mapping as
 * {@link BarSetBridge#toSymbol}.
 */
final class LinearBits {

    private LinearBits() {
    }

    static String invert(String bits) {
        StringBuilder sb = new StringBuilder(bits.length());
        for (int i = 0; i < bits.length(); i++) {
            sb.append(bits.charAt(i) == '1' ? '0' : '1');
        }
        return sb.toString();
    }

    static String reverse(String bits) {
        return new StringBuilder(bits).reverse().toString();
    }

    static LinearSymbol symbol(String bits, BitSemantics semantics) {
        List<BarElement> elements = new ArrayList<>(bits.length());
        boolean dark = true; // WIDE_NARROW_ALTERNATING starts dark
        for (int i = 0; i < bits.length(); i++) {
            boolean bit = bits.charAt(i) == '1';
            elements.add(switch (semantics) {
                case MODULE_DARK -> BarElement.module(bit);
                case WIDE_NARROW_ALTERNATING -> new BarElement(dark, bit, 100);
                case HEIGHT_CODED -> new BarElement(bit, false, bit ? 100 : 50);
            });
            dark = !dark;
        }
        return new LinearSymbol(elements);
    }

    static int digit(char c) {
        if (!Character.isDigit(c)) {
            throw new NumberFormatException("Invalid number");
        }
        return c - '0';
    }
}
