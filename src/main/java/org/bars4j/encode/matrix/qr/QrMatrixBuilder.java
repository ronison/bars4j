package org.bars4j.encode.matrix.qr;

/**
 * Builds the QRCode module matrix: function patterns, data placement, masking and the
 * format/version information (BCH encoded).
 *
 * <p>Matrix indexing is {@code modules[row][column]}, {@code true} meaning a dark module.
 * The algorithms mirror ISO/IEC 18004 so that the output can be cross-validated against a
 * reference implementation.
 */
public final class QrMatrixBuilder {

    // Penalty weights.
    private static final int N1 = 3;
    private static final int N2 = 3;
    private static final int N3 = 40;
    private static final int N4 = 10;

    private static final int FORMAT_POLY = 0x537;
    private static final int FORMAT_MASK = 0x5412;
    private static final int VERSION_POLY = 0x1F25;

    /** (column, row) coordinates of the first copy of the format information. */
    private static final int[][] FORMAT_COORDS = {
            {8, 0}, {8, 1}, {8, 2}, {8, 3}, {8, 4}, {8, 5}, {8, 7}, {8, 8},
            {7, 8}, {5, 8}, {4, 8}, {3, 8}, {2, 8}, {1, 8}, {0, 8}
    };

    private QrMatrixBuilder() {
    }

    public static boolean[][] build(QrVersion version, QrEccLevel level, int[] codewords) {
        boolean[][] modules = new boolean[version.size()][version.size()];
        boolean[][] reserved = new boolean[version.size()][version.size()];

        embedFunctionPatterns(modules, reserved, version);
        reserveFormatAndVersion(reserved, version);
        placeData(modules, reserved, codewords);

        boolean[][] best = null;
        int bestPenalty = Integer.MAX_VALUE;
        for (int mask = 0; mask < 8; mask++) {
            boolean[][] candidate = withMask(modules, reserved, version, level, mask);
            int penalty = penalty(candidate);
            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                best = candidate;
            }
        }
        return best;
    }

    // ----- function patterns ---------------------------------------------------------------

    private static void embedFunctionPatterns(boolean[][] modules, boolean[][] reserved, QrVersion version) {
        int size = version.size();
        embedFinder(modules, reserved, 0, 0, size);
        embedFinder(modules, reserved, size - 7, 0, size);
        embedFinder(modules, reserved, 0, size - 7, size);
        embedTiming(modules, reserved, size);
        embedAlignment(modules, reserved, version);
        // dark module
        modules[size - 8][8] = true;
        reserved[size - 8][8] = true;
    }

    private static void embedFinder(boolean[][] modules, boolean[][] reserved, int left, int top, int size) {
        for (int dy = -1; dy <= 7; dy++) {
            for (int dx = -1; dx <= 7; dx++) {
                int row = top + dy;
                int col = left + dx;
                if (row < 0 || row >= size || col < 0 || col >= size) {
                    continue;
                }
                boolean inPattern = dx >= 0 && dx <= 6 && dy >= 0 && dy <= 6;
                boolean dark = inPattern && (dx == 0 || dx == 6 || dy == 0 || dy == 6
                        || (dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4));
                modules[row][col] = dark;
                reserved[row][col] = true;
            }
        }
    }

    private static void embedTiming(boolean[][] modules, boolean[][] reserved, int size) {
        for (int i = 8; i < size - 8; i++) {
            boolean dark = (i + 1) % 2 == 1;
            modules[6][i] = dark;
            reserved[6][i] = true;
            modules[i][6] = dark;
            reserved[i][6] = true;
        }
    }

    private static void embedAlignment(boolean[][] modules, boolean[][] reserved, QrVersion version) {
        int[] centers = version.alignmentPatternCenters();
        int last = centers.length - 1;
        for (int i = 0; i < centers.length; i++) {
            for (int j = 0; j < centers.length; j++) {
                // Skip the three positions overlapping the finder patterns.
                boolean overlapsFinder = (i == 0 && j == 0) || (i == 0 && j == last) || (i == last && j == 0);
                if (overlapsFinder) {
                    continue;
                }
                embedAlignmentPattern(modules, reserved, centers[i], centers[j], version.size());
            }
        }
    }

    private static void embedAlignmentPattern(boolean[][] modules, boolean[][] reserved, int centerCol, int centerRow, int size) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int row = centerRow + dy;
                int col = centerCol + dx;
                if (row < 0 || row >= size || col < 0 || col >= size) {
                    continue;
                }
                boolean dark = Math.max(Math.abs(dy), Math.abs(dx)) == 2 || (dy == 0 && dx == 0);
                modules[row][col] = dark;
                reserved[row][col] = true;
            }
        }
    }

    private static void reserveFormatAndVersion(boolean[][] reserved, QrVersion version) {
        int size = version.size();
        for (int[] coord : FORMAT_COORDS) {
            reserved[coord[1]][coord[0]] = true;
        }
        for (int i = 0; i < 15; i++) {
            if (i < 8) {
                reserved[8][size - i - 1] = true;
            } else {
                reserved[size - 7 + (i - 8)][8] = true;
            }
        }
        if (version.versionNumber() >= 7) {
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 3; j++) {
                    reserved[size - 11 + j][i] = true;
                    reserved[i][size - 11 + j] = true;
                }
            }
        }
    }

    // ----- data placement ------------------------------------------------------------------

    private static void placeData(boolean[][] modules, boolean[][] reserved, int[] codewords) {
        int size = modules.length;
        int totalBits = codewords.length * 8;
        int bitIndex = 0;
        int direction = -1;
        int col = size - 1;
        while (col > 0) {
            if (col == 6) {
                col--;
            }
            int row = direction == -1 ? size - 1 : 0;
            while (row >= 0 && row < size) {
                for (int i = 0; i < 2; i++) {
                    int c = col - i;
                    if (!reserved[row][c]) {
                        boolean bit = false;
                        if (bitIndex < totalBits) {
                            bit = ((codewords[bitIndex / 8] >>> (7 - bitIndex % 8)) & 1) != 0;
                            bitIndex++;
                        }
                        modules[row][c] = bit;
                    }
                }
                row += direction;
            }
            direction = -direction;
            col -= 2;
        }
    }

    // ----- masking -------------------------------------------------------------------------

    private static boolean[][] withMask(boolean[][] base, boolean[][] reserved, QrVersion version,
                                        QrEccLevel level, int mask) {
        int size = base.length;
        boolean[][] result = new boolean[size][size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                boolean value = base[row][col];
                if (!reserved[row][col] && maskCondition(mask, row, col)) {
                    value = !value;
                }
                result[row][col] = value;
            }
        }
        embedFormatInfo(result, level, mask);
        embedVersionInfo(result, version);
        return result;
    }

    private static boolean maskCondition(int mask, int row, int col) {
        return switch (mask) {
            case 0 -> (row + col) % 2 == 0;
            case 1 -> row % 2 == 0;
            case 2 -> col % 3 == 0;
            case 3 -> (row + col) % 3 == 0;
            case 4 -> (row / 2 + col / 3) % 2 == 0;
            case 5 -> (row * col) % 2 + (row * col) % 3 == 0;
            case 6 -> ((row * col) % 2 + (row * col) % 3) % 2 == 0;
            case 7 -> ((row + col) % 2 + (row * col) % 3) % 2 == 0;
            default -> throw new IllegalArgumentException("Invalid mask: " + mask);
        };
    }

    private static void embedFormatInfo(boolean[][] modules, QrEccLevel level, int mask) {
        int size = modules.length;
        int typeInfo = (level.formatBits() << 3) | mask;
        int bch = calculateBCHCode(typeInfo, FORMAT_POLY);
        int value = ((typeInfo << 10) | bch) ^ FORMAT_MASK;
        for (int i = 0; i < 15; i++) {
            boolean bit = ((value >>> i) & 1) != 0;
            int col = FORMAT_COORDS[i][0];
            int row = FORMAT_COORDS[i][1];
            modules[row][col] = bit;
            if (i < 8) {
                modules[8][size - i - 1] = bit;
            } else {
                modules[size - 7 + (i - 8)][8] = bit;
            }
        }
    }

    private static void embedVersionInfo(boolean[][] modules, QrVersion version) {
        int number = version.versionNumber();
        if (number < 7) {
            return;
        }
        int size = modules.length;
        int bch = calculateBCHCode(number, VERSION_POLY);
        int value = (number << 12) | bch;
        int pos = 0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                boolean bit = ((value >>> pos) & 1) != 0;
                pos++;
                modules[size - 11 + j][i] = bit;
                modules[i][size - 11 + j] = bit;
            }
        }
    }

    private static int calculateBCHCode(int value, int poly) {
        int msbInPoly = 32 - Integer.numberOfLeadingZeros(poly);
        int shifted = value << (msbInPoly - 1);
        while (32 - Integer.numberOfLeadingZeros(shifted) >= msbInPoly) {
            shifted ^= poly << ((32 - Integer.numberOfLeadingZeros(shifted)) - msbInPoly);
        }
        return shifted;
    }

    // ----- penalty scoring -----------------------------------------------------------------

    private static int penalty(boolean[][] modules) {
        int size = modules.length;
        int result = 0;

        // Rule 1: runs of 5+ same-colour modules in rows and columns.
        for (int row = 0; row < size; row++) {
            result += runPenalty(modules, row, true);
        }
        for (int col = 0; col < size; col++) {
            result += runPenalty(modules, col, false);
        }

        // Rule 2: 2x2 blocks of same colour.
        for (int row = 0; row < size - 1; row++) {
            for (int col = 0; col < size - 1; col++) {
                boolean value = modules[row][col];
                if (value == modules[row][col + 1] && value == modules[row + 1][col]
                        && value == modules[row + 1][col + 1]) {
                    result += N2;
                }
            }
        }

        // Rule 3: finder-like patterns (penalty counted once per occurrence, even if both sides are white).
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size - 6; col++) {
                if (modules[row][col] && !modules[row][col + 1] && modules[row][col + 2]
                        && modules[row][col + 3] && modules[row][col + 4]
                        && !modules[row][col + 5] && modules[row][col + 6]) {
                    boolean lightBefore = col >= 4 && !modules[row][col - 1] && !modules[row][col - 2]
                            && !modules[row][col - 3] && !modules[row][col - 4];
                    boolean lightAfter = col + 11 <= size && !modules[row][col + 7] && !modules[row][col + 8]
                            && !modules[row][col + 9] && !modules[row][col + 10];
                    if (lightBefore || lightAfter) {
                        result += N3;
                    }
                }
            }
        }
        for (int col = 0; col < size; col++) {
            for (int row = 0; row < size - 6; row++) {
                if (modules[row][col] && !modules[row + 1][col] && modules[row + 2][col]
                        && modules[row + 3][col] && modules[row + 4][col]
                        && !modules[row + 5][col] && modules[row + 6][col]) {
                    boolean lightBefore = row >= 4 && !modules[row - 1][col] && !modules[row - 2][col]
                            && !modules[row - 3][col] && !modules[row - 4][col];
                    boolean lightAfter = row + 11 <= size && !modules[row + 7][col] && !modules[row + 8][col]
                            && !modules[row + 9][col] && !modules[row + 10][col];
                    if (lightBefore || lightAfter) {
                        result += N3;
                    }
                }
            }
        }

        // Rule 4: proportion of dark modules (integer arithmetic, matching the reference behaviour).
        int dark = 0;
        for (boolean[] module : modules) {
            for (boolean value : module) {
                if (value) {
                    dark++;
                }
            }
        }
        int total = size * size;
        int variances = Math.abs(dark * 2 - total) * 10 / total;
        result += variances * N4;

        return result;
    }

    private static int runPenalty(boolean[][] modules, int index, boolean row) {
        int size = modules.length;
        int penalty = 0;
        boolean color = row ? modules[index][0] : modules[0][index];
        int runLength = 1;
        for (int i = 1; i < size; i++) {
            boolean value = row ? modules[index][i] : modules[i][index];
            if (value == color) {
                runLength++;
                if (runLength == 5) {
                    penalty += N1;
                } else if (runLength > 5) {
                    penalty++;
                }
            } else {
                color = value;
                runLength = 1;
            }
        }
        return penalty;
    }
}
