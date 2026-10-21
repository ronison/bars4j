package org.bars4j.encode.matrix.aztec;

/**
 * Geometry of an Aztec symbol of a given layout and layer count: how many modules it spans, where
 * the layers sit relative to the centre, and how the data bits, mode message, central bull's-eye and
 * (for full symbols) the reference grid are placed.
 *
 * <p>The data modules are laid out in four runs per layer, spiralling around the centre. Full
 * symbols interleave the reference grid between layers, so a mapping from layer coordinates to
 * module coordinates is needed; compact symbols have no grid and use the identity mapping.
 */
final class AztecLayout {

    private final boolean compact;
    private final int layers;
    /** Symbol size before the reference grid is interleaved (equals the final size when compact). */
    private final int baseSize;
    private final int matrixSize;
    private final int[] moduleOfBase;

    private AztecLayout(boolean compact, int layers) {
        this.compact = compact;
        this.layers = layers;
        this.baseSize = (compact ? 11 : 14) + layers * 4;
        this.moduleOfBase = new int[baseSize];

        if (compact) {
            this.matrixSize = baseSize;
            for (int i = 0; i < baseSize; i++) {
                moduleOfBase[i] = i;
            }
        } else {
            // Each group of 15 layer positions gains one reference-grid line (and one extra at the
            // centre), so the final symbol is wider than the layers alone.
            this.matrixSize = baseSize + 1 + 2 * ((baseSize / 2 - 1) / 15);
            int halfBase = baseSize / 2;
            int center = matrixSize / 2;
            for (int i = 0; i < halfBase; i++) {
                int gridOffset = i + i / 15;
                moduleOfBase[halfBase - i - 1] = center - gridOffset - 1;
                moduleOfBase[halfBase + i] = center + gridOffset + 1;
            }
        }
    }

    /** Returns the layout for the given symbol kind and layer count. */
    static AztecLayout of(boolean compact, int layers) {
        return new AztecLayout(compact, layers);
    }

    /** Returns the final symbol size in modules. */
    int matrixSize() {
        return matrixSize;
    }

    /** Builds the complete module grid for {@code dataBits} and {@code modeMessage}. */
    AztecBitMatrix build(AztecBitBuffer dataBits, AztecBitBuffer modeMessage) {
        AztecBitMatrix matrix = new AztecBitMatrix(matrixSize);
        placeDataBits(matrix, dataBits);
        placeModeMessage(matrix, modeMessage);
        drawBullseye(matrix);
        if (!compact) {
            drawReferenceGrid(matrix);
        }
        return matrix;
    }

    /**
     * Places the data bits. For each layer the bits form four runs, each two modules wide: the top
     * side runs left to right, the right side top to bottom, the bottom side right to left, and the
     * left side bottom to top. Inner layers have shorter sides, so the run length shrinks towards
     * the centre.
     */
    private void placeDataBits(AztecBitMatrix matrix, AztecBitBuffer dataBits) {
        int cursor = 0;
        for (int layer = 0; layer < layers; layer++) {
            int sideLength = (layers - layer) * 4 + (compact ? 9 : 12);
            for (int pair = 0; pair < sideLength; pair++) {
                int offset = pair * 2;
                for (int bit = 0; bit < 2; bit++) {
                    int outer = layer * 2 + bit;
                    int inner = layer * 2 + pair;
                    int far = baseSize - 1 - outer;
                    int near = baseSize - 1 - inner;

                    if (dataBits.get(cursor + offset + bit)) {
                        matrix.set(moduleOfBase[outer], moduleOfBase[inner]);
                    }
                    if (dataBits.get(cursor + sideLength * 2 + offset + bit)) {
                        matrix.set(moduleOfBase[inner], moduleOfBase[far]);
                    }
                    if (dataBits.get(cursor + sideLength * 4 + offset + bit)) {
                        matrix.set(moduleOfBase[far], moduleOfBase[near]);
                    }
                    if (dataBits.get(cursor + sideLength * 6 + offset + bit)) {
                        matrix.set(moduleOfBase[near], moduleOfBase[outer]);
                    }
                }
            }
            cursor += sideLength * 8;
        }
    }

    /**
     * Places the mode message around the bull's-eye: seven (compact) or ten (full) bits per side,
     * read counter-clockwise starting on the top edge.
     */
    private void placeModeMessage(AztecBitMatrix matrix, AztecBitBuffer modeMessage) {
        int center = matrixSize / 2;
        if (compact) {
            for (int i = 0; i < 7; i++) {
                int offset = center - 3 + i;
                if (modeMessage.get(i)) {
                    matrix.set(offset, center - 5);
                }
                if (modeMessage.get(i + 7)) {
                    matrix.set(center + 5, offset);
                }
                if (modeMessage.get(20 - i)) {
                    matrix.set(offset, center + 5);
                }
                if (modeMessage.get(27 - i)) {
                    matrix.set(center - 5, offset);
                }
            }
        } else {
            for (int i = 0; i < 10; i++) {
                // The middle of each side is one module further out to clear the reference grid.
                int offset = center - 5 + i + i / 5;
                if (modeMessage.get(i)) {
                    matrix.set(offset, center - 7);
                }
                if (modeMessage.get(i + 10)) {
                    matrix.set(center + 7, offset);
                }
                if (modeMessage.get(29 - i)) {
                    matrix.set(offset, center + 7);
                }
                if (modeMessage.get(39 - i)) {
                    matrix.set(center - 7, offset);
                }
            }
        }
    }

    /** Draws the concentric square rings of the finder, plus the three orientation marks. */
    private void drawBullseye(AztecBitMatrix matrix) {
        int center = matrixSize / 2;
        int radius = compact ? 5 : 7;
        for (int ring = 0; ring < radius; ring += 2) {
            for (int position = center - ring; position <= center + ring; position++) {
                matrix.set(position, center - ring);
                matrix.set(position, center + ring);
                matrix.set(center - ring, position);
                matrix.set(center + ring, position);
            }
        }
        // Three dark corner modules break the ring's symmetry.
        matrix.set(center - radius, center - radius);
        matrix.set(center - radius + 1, center - radius);
        matrix.set(center - radius, center - radius + 1);
        matrix.set(center + radius, center - radius);
        matrix.set(center + radius, center - radius + 1);
        matrix.set(center + radius, center + radius - 1);
    }

    /** Draws the full-symbol reference grid: dashed lines every 16 modules, both ways. */
    private void drawReferenceGrid(AztecBitMatrix matrix) {
        for (int base = 0, module = 0; base < baseSize / 2 - 1; base += 15, module += 16) {
            for (int position = (matrixSize / 2) & 1; position < matrixSize; position += 2) {
                matrix.set(matrixSize / 2 - module, position);
                matrix.set(matrixSize / 2 + module, position);
                matrix.set(position, matrixSize / 2 - module);
                matrix.set(position, matrixSize / 2 + module);
            }
        }
    }
}
