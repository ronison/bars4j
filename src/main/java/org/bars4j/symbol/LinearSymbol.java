package org.bars4j.symbol;

import java.util.List;

/**
 * A linear (1D) barcode symbol: an ordered sequence of {@link BarElement}s drawn as vertical bars.
 */
public final class LinearSymbol implements BarcodeSymbol {

    private final List<BarElement> elements;

    public LinearSymbol(List<BarElement> elements) {
        this.elements = List.copyOf(elements);
    }

    public List<BarElement> elements() {
        return elements;
    }

    @Override
    public SymbolKind kind() {
        return SymbolKind.LINEAR;
    }

    @Override
    public int widthInModules() {
        return elements.size();
    }

    @Override
    public int heightInModules() {
        return 1;
    }
}
