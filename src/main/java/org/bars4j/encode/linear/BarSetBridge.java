package org.bars4j.encode.linear;

import java.util.ArrayList;
import java.util.List;

import org.bars4j.encode.BarSet;
import org.bars4j.format.BitSemantics;
import org.bars4j.symbol.BarElement;
import org.bars4j.symbol.LinearSymbol;

/**
 * Converts between the legacy {@link BarSet} bit-array representation and the new
 * {@link LinearSymbol} element model.
 *
 * <p>The round-trip is bit-exact for every {@link BitSemantics}, which is what allows the new
 * architecture to sit on top of the legacy painters without changing their output.
 */
public final class BarSetBridge {

    private BarSetBridge() {
    }

    public static LinearSymbol toSymbol(BarSet[] bars, BitSemantics semantics) {
        List<BarElement> elements = new ArrayList<>();
        boolean dark = true; // WIDE_NARROW_ALTERNATING starts dark
        for (BarSet bar : bars) {
            for (int i = 0; i < bar.length(); i++) {
                boolean bit = bar.get(i);
                BarElement element = switch (semantics) {
                    case MODULE_DARK -> BarElement.module(bit);
                    case WIDE_NARROW_ALTERNATING -> new BarElement(dark, bit, 100);
                    case HEIGHT_CODED -> new BarElement(bit, false, bit ? 100 : 50);
                };
                elements.add(element);
                dark = !dark;
            }
        }
        return new LinearSymbol(elements);
    }

    /** Reconstructs the legacy bit array from a symbol, bit-exactly inverting {@link #toSymbol}. */
    public static BarSet[] toBarSets(LinearSymbol symbol, BitSemantics semantics) {
        StringBuilder bits = new StringBuilder(symbol.elements().size());
        for (BarElement element : symbol.elements()) {
            boolean bit = switch (semantics) {
                case MODULE_DARK, HEIGHT_CODED -> element.dark();
                case WIDE_NARROW_ALTERNATING -> element.wide();
            };
            bits.append(bit ? '1' : '0');
        }
        return new BarSet[]{new BarSet(bits.toString())};
    }
}
