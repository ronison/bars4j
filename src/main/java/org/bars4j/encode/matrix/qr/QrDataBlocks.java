package org.bars4j.encode.matrix.qr;

import java.util.Arrays;

import org.bars4j.encode.matrix.gf256.ReedSolomon;

/**
 * Splits data codewords into error-correction blocks, computes their ECC and interleaves
 * everything into the final codeword sequence.
 */
public final class QrDataBlocks {

    private QrDataBlocks() {
    }

    public static int[] interleave(int[] dataCodewords, QrVersion.EcBlocks ecBlocks) {
        int numBlocks = ecBlocks.numBlocks();
        int ecPerBlock = ecBlocks.ecCodewordsPerBlock();

        int[][] data = new int[numBlocks][];
        int[][] ecc = new int[numBlocks][];

        int offset = 0;
        int blockIndex = 0;
        for (QrVersion.EcBlock group : ecBlocks.blocks()) {
            for (int b = 0; b < group.count(); b++) {
                int length = group.dataCodewords();
                data[blockIndex] = Arrays.copyOfRange(dataCodewords, offset, offset + length);
                offset += length;
                ecc[blockIndex] = ReedSolomon.encode(data[blockIndex], ecPerBlock);
                blockIndex++;
            }
        }

        int maxDataLength = 0;
        for (int[] block : data) {
            maxDataLength = Math.max(maxDataLength, block.length);
        }

        int[] result = new int[dataCodewords.length + numBlocks * ecPerBlock];
        int index = 0;
        for (int i = 0; i < maxDataLength; i++) {
            for (int b = 0; b < numBlocks; b++) {
                if (i < data[b].length) {
                    result[index++] = data[b][i];
                }
            }
        }
        for (int i = 0; i < ecPerBlock; i++) {
            for (int b = 0; b < numBlocks; b++) {
                result[index++] = ecc[b][i];
            }
        }
        return result;
    }
}
