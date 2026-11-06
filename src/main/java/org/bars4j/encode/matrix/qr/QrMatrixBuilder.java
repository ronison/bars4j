package org.bars4j.encode.matrix.qr;

/**
 * Renders a QRCode symbol: function patterns, codeword placement, data masking and the
 * BCH-protected format/version markers.
 *
 * <p>A {@code true} entry is a dark module. The layout, the eight data-mask formulas, the penalty
 * scoring and the BCH generator polynomials follow ISO/IEC 18004.
 *
 * <p>The working state is kept in flat {@code boolean[]} arrays ({@code row * size + col}) for
 * cache locality, and the same raster buffer is reused for every candidate mask so a full symbol
 * is only allocated twice (working buffer + winner) instead of once per mask.
 */
public final class QrMatrixBuilder {

    // Penalty weights for the four "undesirable feature" rules.
    private static final int RUN_WEIGHT = 3;
    private static final int BLOCK_WEIGHT = 3;
    private static final int FINDER_WEIGHT = 40;
    private static final int BALANCE_WEIGHT = 10;

    // Generator polynomials and the fixed mask applied to the format bits (ISO/IEC 18004).
    private static final int FORMAT_GENERATOR = 0x537;
    private static final int FORMAT_XOR_MASK = 0x5412;
    private static final int VERSION_GENERATOR = 0x1F25;

    private static final int MASK_COUNT = 8;

    private QrMatrixBuilder() {
    }

    public static boolean[][] build(QrVersion version, QrEccLevel eccLevel, int[] codewords) {
        int size = version.size();
        Board board = new Board(size);
        stampFinders(board);
        stampTiming(board);
        stampAlignment(board, version);
        stampDarkModule(board);
        reserveFormatAreas(board);
        reserveVersionAreas(board, version);
        placeCodewords(board, codewords);

        boolean[] work = new boolean[size * size];
        int bestMask = 0;
        int bestScore = Integer.MAX_VALUE;
        for (int mask = 0; mask < MASK_COUNT; mask++) {
            maskedInto(work, board, version, eccLevel, mask);
            int score = penalty(work, size);
            if (score < bestScore) {
                bestScore = score;
                bestMask = mask;
            }
        }

        boolean[] winner = new boolean[size * size];
        maskedInto(winner, board, version, eccLevel, bestMask);
        boolean[][] grid = new boolean[size][size];
        for (int row = 0; row < size; row++) {
            System.arraycopy(winner, row * size, grid[row], 0, size);
        }
        return grid;
    }

    // ----- function patterns ----------------------------------------------------------------

    private static void stampFinders(Board board) {
        int far = board.size - 7;
        stampFinder(board, 0, 0);
        stampFinder(board, 0, far);
        stampFinder(board, far, 0);
    }

    /**
     * Stamps one 7x7 finder pattern anchored at {@code (top,left)} plus its one-module light
     * separator, clipping whatever falls outside the symbol.
     */
    private static void stampFinder(Board board, int top, int left) {
        int size = board.size;
        for (int dy = -1; dy <= 7; dy++) {
            for (int dx = -1; dx <= 7; dx++) {
                int row = top + dy;
                int col = left + dx;
                if (row < 0 || row >= size || col < 0 || col >= size) {
                    continue;
                }
                boolean inside = dx >= 0 && dx <= 6 && dy >= 0 && dy <= 6;
                boolean border = dx == 0 || dx == 6 || dy == 0 || dy == 6;
                boolean core = dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4;
                board.fix(row, col, inside && (border || core));
            }
        }
    }

    /** Stamps the alternating timing modules along row 6 and column 6. */
    private static void stampTiming(Board board) {
        int size = board.size;
        for (int i = 8; i <= size - 9; i++) {
            boolean dark = i % 2 == 0;
            board.fix(6, i, dark);
            board.fix(i, 6, dark);
        }
    }

    private static void stampAlignment(Board board, QrVersion version) {
        int[] centers = version.alignmentPatternCenters();
        int last = centers.length - 1;
        for (int rowIndex = 0; rowIndex < centers.length; rowIndex++) {
            for (int colIndex = 0; colIndex < centers.length; colIndex++) {
                boolean overlapsFinder = (rowIndex == 0 && colIndex == 0)
                        || (rowIndex == 0 && colIndex == last)
                        || (rowIndex == last && colIndex == 0);
                if (overlapsFinder) {
                    continue;
                }
                stampAlignmentPattern(board, centers[rowIndex], centers[colIndex]);
            }
        }
    }

    /** Stamps the 5x5 alignment pattern centred on {@code (centerRow,centerCol)}. */
    private static void stampAlignmentPattern(Board board, int centerRow, int centerCol) {
        int size = board.size;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int row = centerRow + dy;
                int col = centerCol + dx;
                if (row < 0 || row >= size || col < 0 || col >= size) {
                    continue;
                }
                boolean dark = Math.max(Math.abs(dy), Math.abs(dx)) == 2 || (dy == 0 && dx == 0);
                board.fix(row, col, dark);
            }
        }
    }

    /** The mandatory dark module just above the bottom-left finder. */
    private static void stampDarkModule(Board board) {
        board.fix(board.size - 8, 8, true);
    }

    // ----- reserved areas -------------------------------------------------------------------

    /** Marks every cell occupied by the two copies of the format information. */
    private static void reserveFormatAreas(Board board) {
        int size = board.size;
        // Copy 1: along column 8 above the top-left finder, then along row 8 to its left.
        for (int row = 0; row <= 8; row++) {
            if (row != 6) {
                board.reserve(row, 8);
            }
        }
        for (int col = 0; col <= 8; col++) {
            if (col != 6) {
                board.reserve(8, col);
            }
        }
        // Copy 2: along row 8 at the right edge, then along column 8 at the bottom edge.
        for (int i = 0; i < 8; i++) {
            board.reserve(8, size - 1 - i);
        }
        for (int i = 0; i < 7; i++) {
            board.reserve(size - 7 + i, 8);
        }
    }

    /** Marks the two 6x3 version-information blocks present from version 7 onwards. */
    private static void reserveVersionAreas(Board board, QrVersion version) {
        if (version.versionNumber() < 7) {
            return;
        }
        int size = board.size;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                board.reserve(size - 11 + j, i);
                board.reserve(i, size - 11 + j);
            }
        }
    }

    // ----- codeword placement ---------------------------------------------------------------

    /**
     * Walks the data area in the standard two-column zig-zag: upward when the pair index is even,
     * downward when odd, skipping the vertical timing column and any function module.
     */
    private static void placeCodewords(Board board, int[] codewords) {
        int size = board.size;
        int totalBits = codewords.length * 8;
        int placed = 0;
        int rowStep = -1;
        int right = size - 1;
        while (right > 0) {
            int col = right == 6 ? 5 : right;
            for (int step = 0; step < size; step++) {
                int row = rowStep < 0 ? size - 1 - step : step;
                for (int side = 0; side < 2; side++) {
                    int c = col - side;
                    if (board.fixed(row, c)) {
                        continue;
                    }
                    boolean bit = false;
                    if (placed < totalBits) {
                        bit = ((codewords[placed >> 3] >>> (7 - (placed & 7))) & 1) != 0;
                        placed++;
                    }
                    board.setDark(row, c, bit);
                }
            }
            rowStep = -rowStep;
            right = col - 2;
        }
    }

    // ----- masking and information markers ---------------------------------------------------

    /** Fills {@code out} from the board's data modules, applies {@code mask} and writes markers. */
    private static void maskedInto(boolean[] out, Board board, QrVersion version,
                                   QrEccLevel eccLevel, int mask) {
        int size = board.size;
        boolean[] dark = board.dark;
        boolean[] fixed = board.fixed;
        for (int row = 0; row < size; row++) {
            int base = row * size;
            for (int col = 0; col < size; col++) {
                int i = base + col;
                boolean value = dark[i];
                if (!fixed[i] && maskFlips(mask, row, col)) {
                    value = !value;
                }
                out[i] = value;
            }
        }
        writeFormatInfo(out, size, eccLevel, mask);
        writeVersionInfo(out, size, version);
    }

    /** Data-mask condition: {@code true} where the module colour must be inverted. */
    private static boolean maskFlips(int mask, int row, int col) {
        return switch (mask) {
            case 0 -> (row + col) % 2 == 0;
            case 1 -> row % 2 == 0;
            case 2 -> col % 3 == 0;
            case 3 -> (row + col) % 3 == 0;
            case 4 -> (row / 2 + col / 3) % 2 == 0;
            case 5 -> (row * col) % 2 + (row * col) % 3 == 0;
            case 6 -> ((row * col) % 2 + (row * col) % 3) % 2 == 0;
            case 7 -> ((row + col) % 2 + (row * col) % 3) % 2 == 0;
            default -> throw new IllegalArgumentException("Unknown mask pattern: " + mask);
        };
    }

    /**
     * Writes the 15 format bits (ECC level plus mask, BCH-protected and XOR-masked) around the
     * top-left finder and again along the top-right / bottom-left edges.
     */
    private static void writeFormatInfo(boolean[] grid, int size, QrEccLevel eccLevel, int mask) {
        int summary = (eccLevel.formatBits() << 3) | mask;
        int check = bchRemainder(summary, FORMAT_GENERATOR);
        int word = ((summary << 10) | check) ^ FORMAT_XOR_MASK;

        int bit = 0;
        for (int row = 0; row <= 8; row++) {
            if (row != 6) {
                grid[row * size + 8] = bitAt(word, bit++);
            }
        }
        for (int col = 7; col >= 0; col--) {
            if (col != 6) {
                grid[8 * size + col] = bitAt(word, bit++);
            }
        }

        bit = 0;
        for (int i = 0; i < 8; i++) {
            grid[8 * size + size - 1 - i] = bitAt(word, bit++);
        }
        for (int i = 0; i < 7; i++) {
            grid[(size - 7 + i) * size + 8] = bitAt(word, bit++);
        }
    }

    /** Writes the 18 version bits into the two 6x3 blocks for versions 7 and above. */
    private static void writeVersionInfo(boolean[] grid, int size, QrVersion version) {
        int number = version.versionNumber();
        if (number < 7) {
            return;
        }
        int word = (number << 12) | bchRemainder(number, VERSION_GENERATOR);
        int bit = 0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                boolean value = bitAt(word, bit++);
                grid[(size - 11 + j) * size + i] = value;
                grid[i * size + size - 11 + j] = value;
            }
        }
    }

    private static boolean bitAt(int word, int index) {
        return ((word >>> index) & 1) != 0;
    }

    /**
     * Remainder of {@code value * x^degree} divided by {@code generator} over GF(2), where
     * {@code degree} is the generator's own degree. This is the BCH check value appended to the
     * format and version data.
     */
    private static int bchRemainder(int value, int generator) {
        int degree = 31 - Integer.numberOfLeadingZeros(generator);
        int work = value << degree;
        for (int bit = 31; bit >= degree; bit--) {
            if ((work & (1 << bit)) != 0) {
                work ^= generator << (bit - degree);
            }
        }
        return work;
    }

    // ----- penalty scoring ------------------------------------------------------------------

    /**
     * All four penalty rules in a few flat passes. The dark-module count needed by rule 4 is
     * gathered for free during the row pass of rule 1.
     */
    private static int penalty(boolean[] grid, int size) {
        int total = 0;
        int dark = 0;

        // Rule 1, horizontal runs (+ dark count).
        for (int row = 0; row < size; row++) {
            int base = row * size;
            boolean prev = grid[base];
            if (prev) {
                dark++;
            }
            int run = 1;
            for (int col = 1; col < size; col++) {
                boolean value = grid[base + col];
                if (value) {
                    dark++;
                }
                if (value == prev) {
                    run++;
                } else {
                    total += runScore(run);
                    run = 1;
                    prev = value;
                }
            }
            total += runScore(run);
        }

        // Rule 1, vertical runs.
        for (int col = 0; col < size; col++) {
            boolean prev = grid[col];
            int run = 1;
            for (int row = 1; row < size; row++) {
                boolean value = grid[row * size + col];
                if (value == prev) {
                    run++;
                } else {
                    total += runScore(run);
                    run = 1;
                    prev = value;
                }
            }
            total += runScore(run);
        }

        // Rule 2: 2x2 same-colour blocks.
        for (int row = 0; row < size - 1; row++) {
            int base = row * size;
            for (int col = 0; col < size - 1; col++) {
                int i = base + col;
                boolean value = grid[i];
                if (value == grid[i + 1] && value == grid[i + size] && value == grid[i + size + 1]) {
                    total += BLOCK_WEIGHT;
                }
            }
        }

        // Rule 3: finder-like 1:1:3:1:1 sequences (pattern T F T T T F T).
        for (int row = 0; row < size; row++) {
            int base = row * size;
            for (int col = 0; col + 7 <= size; col++) {
                int i = base + col;
                if (grid[i] && !grid[i + 1] && grid[i + 2] && grid[i + 3] && grid[i + 4]
                        && !grid[i + 5] && grid[i + 6]
                        && ((col >= 4 && light(grid, i - 4, i - 1, 1))
                                || (col + 11 <= size && light(grid, i + 7, i + 10, 1)))) {
                    total += FINDER_WEIGHT;
                }
            }
        }
        for (int col = 0; col < size; col++) {
            for (int row = 0; row + 7 <= size; row++) {
                int i = row * size + col;
                if (grid[i] && !grid[i + size] && grid[i + 2 * size] && grid[i + 3 * size]
                        && grid[i + 4 * size] && !grid[i + 5 * size] && grid[i + 6 * size]
                        && ((row >= 4 && light(grid, i - 4 * size, i - size, size))
                                || (row + 11 <= size && light(grid, i + 7 * size, i + 10 * size, size)))) {
                    total += FINDER_WEIGHT;
                }
            }
        }

        // Rule 4: deviation of the dark-module proportion from 50%.
        int modules = size * size;
        int excess = Math.abs(dark * 2 - modules) * 10 / modules;
        return total + excess * BALANCE_WEIGHT;
    }

    private static int runScore(int run) {
        return run >= 5 ? RUN_WEIGHT + (run - 5) : 0;
    }

    /** True when every module in the flat range {@code [from,to]} (step {@code stride}) is light. */
    private static boolean light(boolean[] grid, int from, int to, int stride) {
        for (int i = from; i <= to; i += stride) {
            if (grid[i]) {
                return false;
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------------------------

    /** Symbol under construction: module colours plus a marker for locked (function) modules. */
    private static final class Board {
        private final boolean[] dark;
        private final boolean[] fixed;
        private final int size;

        Board(int size) {
            this.size = size;
            this.dark = new boolean[size * size];
            this.fixed = new boolean[size * size];
        }

        void fix(int row, int col, boolean value) {
            int i = row * size + col;
            dark[i] = value;
            fixed[i] = true;
        }

        void reserve(int row, int col) {
            fixed[row * size + col] = true;
        }

        void setDark(int row, int col, boolean value) {
            dark[row * size + col] = value;
        }

        boolean fixed(int row, int col) {
            return fixed[row * size + col];
        }
    }
}
