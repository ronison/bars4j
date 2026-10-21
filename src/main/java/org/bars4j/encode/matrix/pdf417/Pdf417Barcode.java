package org.bars4j.encode.matrix.pdf417;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.bars4j.encode.api.UnsupportedTextException;

/**
 * Core PDF417 encoder, turning a message into a grid of modules.
 *
 * <p>The path through the encoder is the one described by ISO/IEC 15438: high-level encoding into
 * codewords, selection of a symbol shape, padding of the data region, Reed-Solomon check codewords
 * and finally the low-level bar patterns. Rendering and external libraries are out of scope.
 */
public final class Pdf417Barcode {

    /** Modules in the row-start pattern, which begins every row. */
    private static final int ROW_START_MODULES = 17;
    /** Bar pattern beginning every row. */
    private static final int ROW_START_PATTERN = 0x1FEA8;

    /** Modules in the row-stop pattern at the end of a full-width row. */
    private static final int ROW_STOP_MODULES = 18;
    /** Bar pattern ending every full-width row. */
    private static final int ROW_STOP_PATTERN = 0x3FA29;

    /** Codeword filling unused positions at the right of the data region. */
    private static final int PAD_CODEWORD = 900;

    /** Width-to-height ratio a symbol shape is chosen to approximate. */
    private static final float TARGET_ASPECT = 3.0f;
    /** Assumed module width, in millimetres, used by the aspect estimate. */
    private static final float MODULE_MM = 0.357f;
    /** Assumed row height, in millimetres, used by the aspect estimate. */
    private static final float ROW_MM = 2.0f;

    /** Upper bound on the total number of codewords a PDF417 symbol can hold. */
    private static final int MAX_CODEWORDS = 929;

    private final Pdf417Compaction compaction;
    private final Charset charset;
    private final boolean compact;
    private final Pdf417Dimensions dimensions;

    /**
     * @param compaction high-level compaction scheme
     * @param charset    character set used for Text/Byte compaction
     * @param compact    whether to build the reduced-width compact variant
     * @param dimensions allowed column/row ranges
     */
    public Pdf417Barcode(Pdf417Compaction compaction, Charset charset, boolean compact,
                         Pdf417Dimensions dimensions) {
        this.compaction = compaction != null ? compaction : Pdf417Compaction.AUTO;
        this.charset = charset != null ? charset : StandardCharsets.ISO_8859_1;
        this.compact = compact;
        this.dimensions = dimensions != null ? dimensions : Pdf417Dimensions.standard();
    }

    /**
     * Encodes a message into a grid of modules.
     *
     * @param msg                  the message
     * @param errorCorrectionLevel error correction level, 0-8
     * @return the module grid, row 0 being the top row
     * @throws UnsupportedTextException if the message is empty, contains characters that the
     *                                  character set cannot represent, or does not fit in PDF417
     */
    public Pdf417BarcodeMatrix generateBarcodeLogic(String msg, int errorCorrectionLevel)
            throws UnsupportedTextException {
        int checkCount = Pdf417ErrorCorrection.codewordCount(errorCorrectionLevel);
        String highLevel = Pdf417HighLevelEncoder.encodeHighLevel(msg, compaction, charset);
        int dataCount = highLevel.length();

        int[] shape = chooseShape(dataCount, checkCount);
        int cols = shape[0];
        int rows = shape[1];

        if (dataCount + checkCount + 1 > MAX_CODEWORDS) {
            throw new UnsupportedTextException("Encoded message contains too many codewords ("
                    + msg.length() + " characters)");
        }

        int pad = padCount(dataCount, checkCount, cols, rows);
        int[] data = new int[dataCount + pad + 1];
        data[0] = data.length;
        for (int i = 0; i < dataCount; i++) {
            data[1 + i] = highLevel.charAt(i);
        }
        for (int i = 0; i < pad; i++) {
            data[1 + dataCount + i] = PAD_CODEWORD;
        }

        int[] check = Pdf417ErrorCorrection.generate(data, errorCorrectionLevel);
        int[] allCodewords = new int[data.length + check.length];
        System.arraycopy(data, 0, allCodewords, 0, data.length);
        System.arraycopy(check, 0, allCodewords, data.length, check.length);

        Pdf417BarcodeMatrix matrix = new Pdf417BarcodeMatrix(rows, cols);
        paintRows(allCodewords, cols, rows, errorCorrectionLevel, matrix);
        return matrix;
    }

    /** Rows needed to hold the data and check codewords in the given number of columns. */
    private static int rowsFor(int dataCount, int checkCount, int cols) {
        int rows = (dataCount + 1 + checkCount) / cols + 1;
        if (cols * rows >= dataCount + 1 + checkCount + cols) {
            rows--;
        }
        return rows;
    }

    /** Number of pad codewords needed to fill the data region exactly. */
    private static int padCount(int dataCount, int checkCount, int cols, int rows) {
        int fill = cols * rows - checkCount;
        return fill > dataCount + 1 ? fill - dataCount - 1 : 0;
    }

    /**
     * Picks the allowed shape whose estimated aspect ratio is closest to the target ratio.
     */
    private int[] chooseShape(int dataCount, int checkCount) throws UnsupportedTextException {
        int[] best = null;
        float bestAspect = 0.0f;
        for (int cols = dimensions.minCols(); cols <= dimensions.maxCols(); cols++) {
            int rows = rowsFor(dataCount, checkCount, cols);
            if (rows < dimensions.minRows()) {
                break;
            }
            if (rows > dimensions.maxRows()) {
                continue;
            }
            // Full-width rows hold one 17-module symbol character per data column plus the
            // start pattern, the two indicators and the 18-module stop (17 * cols + 69).
            int rowModules = ROW_START_MODULES * (cols + 4) + 1;
            float aspect = ((float) rowModules * MODULE_MM) / (rows * ROW_MM);
            if (best != null && Math.abs(aspect - TARGET_ASPECT) > Math.abs(bestAspect - TARGET_ASPECT)) {
                continue;
            }
            bestAspect = aspect;
            best = new int[] {cols, rows};
        }
        if (best == null) {
            int rows = rowsFor(dataCount, checkCount, dimensions.minCols());
            if (rows < dimensions.minRows()) {
                best = new int[] {dimensions.minCols(), dimensions.minRows()};
            }
        }
        if (best == null) {
            throw new UnsupportedTextException("Unable to fit message in columns");
        }
        return best;
    }

    /**
     * Appends a bar pattern to a row as alternating runs of dark and light modules.
     *
     * @param pattern the pattern, most significant bit first
     * @param length  number of significant bits in the pattern
     * @param row     the row being filled
     */
    private static void appendPattern(int pattern, int length, Pdf417BarcodeRow row) {
        int bit = 1 << (length - 1);
        boolean previousDark = (pattern & bit) != 0;
        int run = 0;
        for (int i = 0; i < length; i++) {
            boolean dark = (pattern & bit) != 0;
            if (dark == previousDark) {
                run++;
            } else {
                row.addRun(previousDark, run);
                previousDark = dark;
                run = 1;
            }
            bit >>= 1;
        }
        row.addRun(previousDark, run);
    }

    /** Fills every row: start pattern, row indicators, data codewords and the stop pattern. */
    private void paintRows(int[] codewords, int cols, int rows, int ecLevel,
                           Pdf417BarcodeMatrix matrix) {
        int index = 0;
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            int cluster = rowIndex % 3;
            matrix.startRow();
            Pdf417BarcodeRow row = matrix.currentRow();
            appendPattern(ROW_START_PATTERN, ROW_START_MODULES, row);

            int leftIndicator;
            int rightIndicator;
            int block = 30 * (rowIndex / 3);
            switch (cluster) {
                case 0:
                    leftIndicator = block + (rows - 1) / 3;
                    rightIndicator = block + (cols - 1);
                    break;
                case 1:
                    leftIndicator = block + ecLevel * 3 + (rows - 1) % 3;
                    rightIndicator = block + (rows - 1) / 3;
                    break;
                default:
                    leftIndicator = block + (cols - 1);
                    rightIndicator = block + ecLevel * 3 + (rows - 1) % 3;
                    break;
            }

            appendPattern(Pdf417CodeWords.pattern(cluster, leftIndicator), ROW_START_MODULES, row);

            for (int col = 0; col < cols; col++) {
                appendPattern(Pdf417CodeWords.pattern(cluster, codewords[index]), ROW_START_MODULES, row);
                index++;
            }

            if (compact) {
                appendPattern(ROW_STOP_PATTERN, 1, row);
            } else {
                appendPattern(Pdf417CodeWords.pattern(cluster, rightIndicator), ROW_START_MODULES, row);
                appendPattern(ROW_STOP_PATTERN, ROW_STOP_MODULES, row);
            }
        }
    }
}
