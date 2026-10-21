package org.bars4j.encode.matrix.qr;

import java.util.Arrays;

import org.bars4j.encode.matrix.gf256.ReedSolomon;

/**
 * Distributes the data codewords over the error-correction blocks of a symbol, computes each
 * block's Reed-Solomon parity and returns the interleaved final codeword sequence.
 *
 * <p>ISO/IEC 18004 interleaves by taking the first codeword of every block, then the second of
 * every block, and so on; blocks with fewer data codewords are simply skipped once exhausted.
 * The parity codewords are interleaved the same way afterwards.
 */
public final class QrDataBlocks {

    private QrDataBlocks() {
    }

    public static int[] interleave(int[] dataCodewords, QrVersion.EcBlocks ecBlocks) {
        int parityLength = ecBlocks.ecCodewordsPerBlock();
        int[][] blocks = splitIntoBlocks(dataCodewords, ecBlocks);

        int[][] parity = new int[blocks.length][];
        for (int b = 0; b < blocks.length; b++) {
            parity[b] = ReedSolomon.encode(blocks[b], parityLength);
        }

        int[] sequence = new int[dataCodewords.length + blocks.length * parityLength];
        int written = 0;

        int longest = 0;
        for (int[] block : blocks) {
            longest = Math.max(longest, block.length);
        }
        for (int position = 0; position < longest; position++) {
            for (int[] block : blocks) {
                if (position < block.length) {
                    sequence[written++] = block[position];
                }
            }
        }
        for (int position = 0; position < parityLength; position++) {
            for (int b = 0; b < parity.length; b++) {
                sequence[written++] = parity[b][position];
            }
        }
        return sequence;
    }

    /** Copies consecutive slices of {@code dataCodewords} into one array per block. */
    private static int[][] splitIntoBlocks(int[] dataCodewords, QrVersion.EcBlocks ecBlocks) {
        int[][] blocks = new int[ecBlocks.numBlocks()][];
        int start = 0;
        int b = 0;
        for (QrVersion.EcBlock group : ecBlocks.blocks()) {
            for (int n = 0; n < group.count(); n++) {
                blocks[b++] = Arrays.copyOfRange(dataCodewords, start, start + group.dataCodewords());
                start += group.dataCodewords();
            }
        }
        return blocks;
    }
}
