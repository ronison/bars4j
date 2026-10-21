package org.bars4j.encode.matrix.pdf417;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.bars4j.encode.api.UnsupportedTextException;

/**
 * Core PDF417 encoder: turns a message into a low-level module grid.
 *
 * <p>The pipeline follows ISO/IEC 15438: high-level encoding into codewords, choice of a symbol
 * shape, padding, Reed-Solomon error correction and finally the low-level bar patterns. The class
 * is independent of any rendering or external library.
 */
public final class Pdf417Barcode {

    /** 17-module start pattern found at the beginning of every row. */
    private static final int START_PATTERN = 0x1FEA8;
    /** 18-module stop pattern found at the end of every non-compact row. */
    private static final int STOP_PATTERN = 0x3FA29;

    /** Codeword value used to fill unused data positions. */
    private static final int PAD_CODEWORD = 900;

    /** Preferred width-to-height ratio used when choosing the symbol shape. */
    private static final float PREFERRED_RATIO = 3.0f;
    /** Assumed width of one module, in millimetres. */
    private static final float MODULE_WIDTH = 0.357f;
    /** Assumed height of one row, in millimetres. */
    private static final float ROW_HEIGHT = 2.0f;

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
     * Encodes a message into a low-level module grid.
     *
     * @param msg                the message
     * @param errorCorrectionLevel error correction level, 0-8
     * @return the module grid, row 0 being the top row
     * @throws UnsupportedTextException if the message is empty, contains characters that the
     *                                  character set cannot represent, or does not fit in PDF417
     */
    public Pdf417BarcodeMatrix generateBarcodeLogic(String msg, int errorCorrectionLevel)
            throws UnsupportedTextException {
        int ecCount = Pdf417ErrorCorrection.codewordCount(errorCorrectionLevel);
        String highLevel = Pdf417HighLevelEncoder.encodeHighLevel(msg, compaction, charset);
        int sourceCodeWords = highLevel.length();

        int[] shape = determineDimensions(sourceCodeWords, ecCount);
        int cols = shape[0];
        int rows = shape[1];

        int pad = getNumberOfPadCodewords(sourceCodeWords, ecCount, cols, rows);

        if (sourceCodeWords + ecCount + 1 > 929) {
            throw new UnsupportedTextException("Encoded message contains too many codewords ("
                    + msg.length() + " characters)");
        }

        int n = sourceCodeWords + pad + 1;
        int[] dataCodewords = new int[n];
        dataCodewords[0] = n;
        for (int i = 0; i < sourceCodeWords; i++) {
            dataCodewords[1 + i] = highLevel.charAt(i);
        }
        for (int i = 0; i < pad; i++) {
            dataCodewords[1 + sourceCodeWords + i] = PAD_CODEWORD;
        }

        int[] errorCorrection = Pdf417ErrorCorrection.generate(dataCodewords, errorCorrectionLevel);

        int[] fullCodewords = new int[dataCodewords.length + errorCorrection.length];
        System.arraycopy(dataCodewords, 0, fullCodewords, 0, dataCodewords.length);
        System.arraycopy(errorCorrection, 0, fullCodewords, dataCodewords.length, errorCorrection.length);

        Pdf417BarcodeMatrix matrix = new Pdf417BarcodeMatrix(rows, cols);
        encodeLowLevel(fullCodewords, cols, rows, errorCorrectionLevel, matrix);
        return matrix;
    }

    /**
     * Number of rows needed for the given counts, as described in Annex Q of ISO/IEC 15438.
     */
    private static int calculateNumberOfRows(int sourceCodeWords, int ecCount, int cols) {
        int rows = (sourceCodeWords + 1 + ecCount) / cols + 1;
        if (cols * rows >= sourceCodeWords + 1 + ecCount + cols) {
            rows--;
        }
        return rows;
    }

    /** Number of pad codewords as described in 4.9.2 of ISO/IEC 15438. */
    private static int getNumberOfPadCodewords(int sourceCodeWords, int ecCount, int cols, int rows) {
        int n = cols * rows - ecCount;
        return n > sourceCodeWords + 1 ? n - sourceCodeWords - 1 : 0;
    }

    /**
     * Chooses the column/row shape whose width-to-height ratio is closest to the preferred ratio.
     */
    private int[] determineDimensions(int sourceCodeWords, int ecCount) throws UnsupportedTextException {
        float ratio = 0.0f;
        int[] shape = null;
        for (int cols = dimensions.minCols(); cols <= dimensions.maxCols(); cols++) {
            int rows = calculateNumberOfRows(sourceCodeWords, ecCount, cols);
            if (rows < dimensions.minRows()) {
                break;
            }
            if (rows > dimensions.maxRows()) {
                continue;
            }
            float newRatio = ((float) (17 * cols + 69) * MODULE_WIDTH) / (rows * ROW_HEIGHT);
            if (shape != null && Math.abs(newRatio - PREFERRED_RATIO) > Math.abs(ratio - PREFERRED_RATIO)) {
                continue;
            }
            ratio = newRatio;
            shape = new int[] {cols, rows};
        }
        if (shape == null) {
            int rows = calculateNumberOfRows(sourceCodeWords, ecCount, dimensions.minCols());
            if (rows < dimensions.minRows()) {
                shape = new int[] {dimensions.minCols(), dimensions.minRows()};
            }
        }
        if (shape == null) {
            throw new UnsupportedTextException("Unable to fit message in columns");
        }
        return shape;
    }

    /**
     * Appends a bar pattern to a row, alternating runs of dark and light modules.
     *
     * @param pattern the pattern, most significant bit first
     * @param len     number of significant bits in the pattern
     * @param row     the row being filled
     */
    private static void encodeChar(int pattern, int len, Pdf417BarcodeRow row) {
        int map = 1 << (len - 1);
        boolean last = (pattern & map) != 0;
        int width = 0;
        for (int i = 0; i < len; i++) {
            boolean dark = (pattern & map) != 0;
            if (last == dark) {
                width++;
            } else {
                row.addRun(last, width);
                last = dark;
                width = 1;
            }
            map >>= 1;
        }
        row.addRun(last, width);
    }

    /** Builds every row of the symbol: start pattern, row indicators, data codewords and stop. */
    private void encodeLowLevel(int[] fullCodewords, int cols, int rows, int ecLevel,
                                Pdf417BarcodeMatrix matrix) {
        int idx = 0;
        for (int y = 0; y < rows; y++) {
            int cluster = y % 3;
            matrix.startRow();
            Pdf417BarcodeRow row = matrix.currentRow();
            encodeChar(START_PATTERN, 17, row);

            int left;
            int right;
            if (cluster == 0) {
                left = (30 * (y / 3)) + ((rows - 1) / 3);
                right = (30 * (y / 3)) + (cols - 1);
            } else if (cluster == 1) {
                left = (30 * (y / 3)) + (ecLevel * 3) + ((rows - 1) % 3);
                right = (30 * (y / 3)) + ((rows - 1) / 3);
            } else {
                left = (30 * (y / 3)) + (cols - 1);
                right = (30 * (y / 3)) + (ecLevel * 3) + ((rows - 1) % 3);
            }

            encodeChar(Pdf417CodeWords.pattern(cluster, left), 17, row);

            for (int x = 0; x < cols; x++) {
                encodeChar(Pdf417CodeWords.pattern(cluster, fullCodewords[idx]), 17, row);
                idx++;
            }

            if (compact) {
                encodeChar(STOP_PATTERN, 1, row);
            } else {
                encodeChar(Pdf417CodeWords.pattern(cluster, right), 17, row);
                encodeChar(STOP_PATTERN, 18, row);
            }
        }
    }
}
