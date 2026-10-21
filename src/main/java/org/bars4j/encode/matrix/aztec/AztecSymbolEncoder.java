package org.bars4j.encode.matrix.aztec;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.bars4j.encode.api.UnsupportedTextException;

/**
 * Builds a complete Aztec symbol (ISO/IEC 24778) from input text: chooses the symbol size, performs
 * bit stuffing and error correction, generates the mode message and delegates the module placement
 * to {@link AztecLayout}.
 */
public final class AztecSymbolEncoder {

    /** Largest layer count of a full symbol. */
    private static final int MAX_LAYERS = 32;
    /** Largest layer count of a compact symbol. */
    private static final int MAX_COMPACT_LAYERS = 4;

    /**
     * Codeword width in bits for each layer count: 4 bits at one layer, then 6, 8, 10 and finally
     * 12 bits as the symbol grows.
     */
    private static final int[] CODEWORD_WIDTH = {
            4, 6, 6, 8, 8, 8, 8, 8, 8, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
            12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
    };

    private AztecSymbolEncoder() {
    }

    /**
     * Encodes {@code content} as an Aztec symbol.
     *
     * @param content      text to encode; without an explicit {@code charset} it must be representable
     *                     as ISO-8859-1 (Latin-1)
     * @param eccPercent   minimum percentage of error correction words
     * @param forcedLayers {@code null} or {@code 0} for automatic sizing; a negative value forces a
     *                     compact symbol with {@code abs(value)} layers, a positive value forces a
     *                     full symbol with that many layers
     * @param charset      character set to announce with an ECI, or {@code null} for no ECI
     */
    public static AztecCode encode(String content, int eccPercent, Integer forcedLayers, Charset charset)
            throws UnsupportedTextException {
        byte[] bytes = content.getBytes(charset != null ? charset : StandardCharsets.ISO_8859_1);
        AztecBitBuffer dataBits = new AztecHighLevelEncoder(bytes, charset).encode();

        int checkBits = dataBits.size() * eccPercent / 100 + 11;

        Sizing sizing;
        if (forcedLayers != null && forcedLayers != 0) {
            sizing = forcedSize(dataBits, checkBits, forcedLayers);
        } else {
            sizing = automaticSize(dataBits, checkBits);
        }

        int dataWords = sizing.stuffed.size() / sizing.wordSize;
        AztecBitBuffer message = addCheckWords(sizing.stuffed, sizing.totalBitsInLayer, sizing.wordSize);
        AztecBitBuffer modeMessage = buildModeMessage(sizing.compact, sizing.layers, dataWords);
        AztecBitMatrix matrix = AztecLayout.of(sizing.compact, sizing.layers).build(message, modeMessage);
        return new AztecCode(sizing.compact, sizing.layers, dataWords, matrix);
    }

    /** A chosen symbol size together with its stuffed data. */
    private static final class Sizing {
        final boolean compact;
        final int layers;
        final int wordSize;
        final int totalBitsInLayer;
        final AztecBitBuffer stuffed;

        Sizing(boolean compact, int layers, int wordSize, int totalBitsInLayer, AztecBitBuffer stuffed) {
            this.compact = compact;
            this.layers = layers;
            this.wordSize = wordSize;
            this.totalBitsInLayer = totalBitsInLayer;
            this.stuffed = stuffed;
        }
    }

    /** Validates a caller-requested layer count and prepares its data. */
    private static Sizing forcedSize(AztecBitBuffer dataBits, int checkBits, int forcedLayers)
            throws UnsupportedTextException {
        boolean compact = forcedLayers < 0;
        int layers = Math.abs(forcedLayers);
        int maxLayers = compact ? MAX_COMPACT_LAYERS : MAX_LAYERS;
        if (layers > maxLayers) {
            throw new IllegalArgumentException("Illegal value " + forcedLayers + " for layers");
        }
        int wordSize = CODEWORD_WIDTH[layers];
        int totalBitsInLayer = dataCapacity(compact, layers);
        AztecBitBuffer stuffed = insertStuffing(dataBits, wordSize);
        int usableBits = totalBitsInLayer - totalBitsInLayer % wordSize;
        boolean tooLarge = stuffed.size() + checkBits > usableBits
                || (compact && stuffed.size() > wordSize * 64);
        if (tooLarge) {
            throw new UnsupportedTextException("Data too large for user specified layer");
        }
        return new Sizing(compact, layers, wordSize, totalBitsInLayer, stuffed);
    }

    /**
     * Finds the smallest symbol that fits. Compact symbols are tried first (Compact1 through
     * Compact4); the smallest full symbol is Normal4, because a smaller full symbol would have the
     * same size as a compact one with less capacity.
     */
    private static Sizing automaticSize(AztecBitBuffer dataBits, int checkBits)
            throws UnsupportedTextException {
        AztecBitBuffer stuffed = null;
        int wordSize = 0;
        for (int candidate = 0; candidate <= MAX_LAYERS; candidate++) {
            boolean compact = candidate <= 3;
            int layers = compact ? candidate + 1 : candidate;
            int totalBitsInLayer = dataCapacity(compact, layers);
            if (dataBits.size() + checkBits > totalBitsInLayer) {
                continue;
            }
            if (stuffed == null || wordSize != CODEWORD_WIDTH[layers]) {
                wordSize = CODEWORD_WIDTH[layers];
                stuffed = insertStuffing(dataBits, wordSize);
            }
            // Compact symbols can carry at most 64 data words.
            if (compact && stuffed.size() > wordSize * 64) {
                continue;
            }
            int usableBits = totalBitsInLayer - totalBitsInLayer % wordSize;
            if (stuffed.size() + checkBits <= usableBits) {
                return new Sizing(compact, layers, wordSize, totalBitsInLayer, stuffed);
            }
        }
        throw new UnsupportedTextException("Data too large for an Aztec code");
    }

    /** Number of data bits available in {@code layers} layers of the given layout. */
    private static int dataCapacity(boolean compact, int layers) {
        return ((compact ? 88 : 112) + 16 * layers) * layers;
    }

    /**
     * Splits {@code bits} into {@code wordSize}-bit words. A word whose upper bits would all be one
     * or all be zero is ambiguous, so the final bit is forced and one input bit is deferred to the
     * following word. Missing bits at the end are treated as ones.
     */
    static AztecBitBuffer insertStuffing(AztecBitBuffer bits, int wordSize) {
        AztecBitBuffer out = new AztecBitBuffer();
        int bitCount = bits.size();
        int allOne = (1 << wordSize) - 1;
        int upperMask = allOne - 1;

        int read = 0;
        while (read < bitCount) {
            int word = 0;
            for (int offset = 0; offset < wordSize; offset++) {
                int position = read + offset;
                if (position >= bitCount || bits.get(position)) {
                    word |= 1 << (wordSize - 1 - offset);
                }
            }
            int upper = word & upperMask;
            if (upper == upperMask) {
                // Force the last bit to zero and let the borrowed one open the next word.
                out.appendBits(upperMask, wordSize);
                read += wordSize - 1;
            } else if (upper == 0) {
                // Force a lone one in the last position.
                out.appendBits(1, wordSize);
                read += wordSize - 1;
            } else {
                out.appendBits(word, wordSize);
                read += wordSize;
            }
        }
        return out;
    }

    /** Builds the mode message: symbol geometry, protected by Reed-Solomon check words. */
    static AztecBitBuffer buildModeMessage(boolean compact, int layers, int dataWords) {
        AztecBitBuffer raw = new AztecBitBuffer();
        if (compact) {
            raw.appendBits(layers - 1, 2);
            raw.appendBits(dataWords - 1, 6);
            return addCheckWords(raw, 28, 4);
        }
        raw.appendBits(layers - 1, 5);
        raw.appendBits(dataWords - 1, 11);
        return addCheckWords(raw, 40, 4);
    }

    /**
     * Packs {@code dataBits} into {@code totalBits / wordSize} codewords and fills the trailing
     * slots with Reed-Solomon check words, returning the complete message as a bit buffer.
     */
    private static AztecBitBuffer addCheckWords(AztecBitBuffer dataBits, int totalBits, int wordSize) {
        int dataWords = dataBits.size() / wordSize;
        int totalWords = totalBits / wordSize;
        int[] words = new int[totalWords];
        for (int w = 0; w < dataWords; w++) {
            int value = 0;
            for (int bit = 0; bit < wordSize; bit++) {
                if (dataBits.get(w * wordSize + bit)) {
                    value |= 1 << (wordSize - 1 - bit);
                }
            }
            words[w] = value;
        }
        AztecReedSolomon.appendCheckWords(words, totalWords - dataWords, AztecField.forWordSize(wordSize));

        AztecBitBuffer message = new AztecBitBuffer();
        // Prepend the filler that aligns the codeword block inside the available bits.
        int leadingFiller = totalBits % wordSize;
        message.appendBits(0, leadingFiller);
        for (int word : words) {
            message.appendBits(word, wordSize);
        }
        return message;
    }
}
