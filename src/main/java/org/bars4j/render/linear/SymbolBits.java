package org.bars4j.render.linear;

import org.bars4j.symbol.BarElement;
import org.bars4j.symbol.LinearSymbol;

/**
 * Extracts the legacy bit array (one boolean per element) from a {@link LinearSymbol}.
 *
 * <p>The three painting strategies interpret the same {@link LinearSymbol} differently, exactly as
 * the legacy painters interpreted the bit arrays: width-coded and height-coded strategies read the
 * "dark" flag, wide-ratio strategies read the "wide" flag.
 */
final class SymbolBits {

    private SymbolBits() {
    }

    static boolean[] dark(LinearSymbol symbol) {
        boolean[] bits = new boolean[symbol.elements().size()];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = symbol.elements().get(i).dark();
        }
        return bits;
    }

    static boolean[] wide(LinearSymbol symbol) {
        boolean[] bits = new boolean[symbol.elements().size()];
        for (int i = 0; i < bits.length; i++) {
            BarElement element = symbol.elements().get(i);
            bits[i] = element.wide();
        }
        return bits;
    }
}
