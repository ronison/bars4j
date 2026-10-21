package org.bars4j.encode.matrix.qr;

/**
 * Renders a QRCode symbol: function patterns, codeword placement, data masking and the
 * BCH-protected format/version markers.
 *
 * <p>Modules are addressed as {@code [row][column]} and a {@code true} entry is a dark module.
 * The layout, the eight data-mask formulas, the penalty scoring and the BCH generator
 * polynomials follow ISO/IEC 18004.
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

    /** The 1:1:3:1:1 module sequence that penalty rule 3 looks for. */
    private static final boolean[] FINDER_LIKE = {true, false, true, true, true, false, true};

    private QrMatrixBuilder() {
    }

    public static boolean[][] build(QrVersion version, QrEccLevel eccLevel, int[] codewords) {
        Board board = new Board(version.size());
        stampFinders(board);
        stampTiming(board);
        stampAlignment(board, version);
        stampDarkModule(board);
        reserveFormatAreas(board);
        reserveVersionAreas(board, version);
        placeCodewords(board, codewords);

        boolean[][] best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int mask = 0; mask < 8; mask++) {
            boolean[][] candidate = masked(board, version, eccLevel, mask);
            int score = penalty(candidate);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
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
                    if (board.fixed[row][c]) {
                        continue;
                    }
                    boolean bit = false;
                    if (placed < totalBits) {
                        bit = ((codewords[placed >> 3] >>> (7 - (placed & 7))) & 1) != 0;
                        placed++;
                    }
                    board.dark[row][c] = bit;
                }
            }
            rowStep = -rowStep;
            right = col - 2;
        }
    }

    // ----- masking and information markers ---------------------------------------------------

    /** Returns a copy with data modules masked and both information markers written. */
    private static boolean[][] masked(Board board, QrVersion version, QrEccLevel eccLevel, int mask) {
        int size = board.size;
        boolean[][] grid = new boolean[size][size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                boolean value = board.dark[row][col];
                if (!board.fixed[row][col] && maskFlips(mask, row, col)) {
                    value = !value;
                }
                grid[row][col] = value;
            }
        }
        writeFormatInfo(grid, eccLevel, mask);
        writeVersionInfo(grid, version);
        return grid;
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
    private static void writeFormatInfo(boolean[][] grid, QrEccLevel eccLevel, int mask) {
        int size = grid.length;
        int summary = (eccLevel.formatBits() << 3) | mask;
        int check = bchRemainder(summary, FORMAT_GENERATOR);
        int word = ((summary << 10) | check) ^ FORMAT_XOR_MASK;

        int bit = 0;
        for (int row = 0; row <= 8; row++) {
            if (row != 6) {
                grid[row][8] = bitAt(word, bit++);
            }
        }
        for (int col = 7; col >= 0; col--) {
            if (col != 6) {
                grid[8][col] = bitAt(word, bit++);
            }
        }

        bit = 0;
        for (int i = 0; i < 8; i++) {
            grid[8][size - 1 - i] = bitAt(word, bit++);
        }
        for (int i = 0; i < 7; i++) {
            grid[size - 7 + i][8] = bitAt(word, bit++);
        }
    }

    /** Writes the 18 version bits into the two 6x3 blocks for versions 7 and above. */
    private static void writeVersionInfo(boolean[][] grid, QrVersion version) {
        int number = version.versionNumber();
        if (number < 7) {
            return;
        }
        int size = grid.length;
        int word = (number << 12) | bchRemainder(number, VERSION_GENERATOR);
        int bit = 0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                boolean value = bitAt(word, bit++);
                grid[size - 11 + j][i] = value;
                grid[i][size - 11 + j] = value;
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

    private static int penalty(boolean[][] grid) {
        return runPenalty(grid)
                + blockPenalty(grid)
                + finderLikePenalty(grid)
                + balancePenalty(grid);
    }

    /** Penalty rule 1: runs of five or more same-colour modules in any row or column. */
    private static int runPenalty(boolean[][] grid) {
        int size = grid.length;
        int total = 0;
        for (int row = 0; row < size; row++) {
            int run = 1;
            for (int col = 1; col < size; col++) {
                if (grid[row][col] == grid[row][col - 1]) {
                    run++;
                } else {
                    total += runScore(run);
                    run = 1;
                }
            }
            total += runScore(run);
        }
        for (int col = 0; col < size; col++) {
            int run = 1;
            for (int row = 1; row < size; row++) {
                if (grid[row][col] == grid[row - 1][col]) {
                    run++;
                } else {
                    total += runScore(run);
                    run = 1;
                }
            }
            total += runScore(run);
        }
        return total;
    }

    private static int runScore(int run) {
        return run >= 5 ? RUN_WEIGHT + (run - 5) : 0;
    }

    /** Penalty rule 2: every 2x2 square of a single colour. */
    private static int blockPenalty(boolean[][] grid) {
        int size = grid.length;
        int total = 0;
        for (int row = 0; row < size - 1; row++) {
            for (int col = 0; col < size - 1; col++) {
                boolean value = grid[row][col];
                if (value == grid[row][col + 1]
                        && value == grid[row + 1][col]
                        && value == grid[row + 1][col + 1]) {
                    total += BLOCK_WEIGHT;
                }
            }
        }
        return total;
    }

    /** Penalty rule 3: the finder-like 1:1:3:1:1 sequence with four light modules on a side. */
    private static int finderLikePenalty(boolean[][] grid) {
        int size = grid.length;
        int total = 0;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col + FINDER_LIKE.length <= size; col++) {
                if (isFinderLike(grid, row, col, true)) {
                    total += FINDER_WEIGHT;
                }
            }
        }
        for (int col = 0; col < size; col++) {
            for (int row = 0; row + FINDER_LIKE.length <= size; row++) {
                if (isFinderLike(grid, row, col, false)) {
                    total += FINDER_WEIGHT;
                }
            }
        }
        return total;
    }

    private static boolean isFinderLike(boolean[][] grid, int row, int col, boolean horizontal) {
        for (int i = 0; i < FINDER_LIKE.length; i++) {
            if (module(grid, row, col, horizontal, i) != FINDER_LIKE[i]) {
                return false;
            }
        }
        int start = horizontal ? col : row;
        boolean lightBefore = start >= 4 && !anyDark(grid, row, col, horizontal, -4, -1);
        boolean lightAfter = start + 11 <= grid.length && !anyDark(grid, row, col, horizontal, 7, 10);
        return lightBefore || lightAfter;
    }

    private static boolean anyDark(boolean[][] grid, int row, int col, boolean horizontal, int from, int to) {
        for (int offset = from; offset <= to; offset++) {
            if (module(grid, row, col, horizontal, offset)) {
                return true;
            }
        }
        return false;
    }

    private static boolean module(boolean[][] grid, int row, int col, boolean horizontal, int offset) {
        return horizontal ? grid[row][col + offset] : grid[row + offset][col];
    }

    /** Penalty rule 4: deviation of the dark-module proportion from 50%. */
    private static int balancePenalty(boolean[][] grid) {
        int dark = 0;
        for (boolean[] line : grid) {
            for (boolean value : line) {
                if (value) {
                    dark++;
                }
            }
        }
        int modules = grid.length * grid.length;
        int excess = Math.abs(dark * 2 - modules) * 10 / modules;
        return excess * BALANCE_WEIGHT;
    }

    // -----------------------------------------------------------------------------------------

    /** Symbol under construction: module colours plus a marker for locked (function) modules. */
    private static final class Board {
        private final boolean[][] dark;
        private final boolean[][] fixed;
        private final int size;

        Board(int size) {
            this.size = size;
            this.dark = new boolean[size][size];
            this.fixed = new boolean[size][size];
        }

        void fix(int row, int col, boolean value) {
            dark[row][col] = value;
            fixed[row][col] = true;
        }

        void reserve(int row, int col) {
            fixed[row][col] = true;
        }
    }
}
