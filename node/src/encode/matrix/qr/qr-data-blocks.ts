import { encode as reedSolomonEncode } from "../gf256/reed-solomon.ts";
import type { EcBlocks } from "./qr-version.ts";
import { numBlocks } from "./qr-version.ts";

/**
 * Distributes the data codewords over the error-correction blocks of a symbol, computes each
 * block's Reed-Solomon parity and returns the interleaved final codeword sequence.
 *
 * <p>ISO/IEC 18004 interleaves by taking the first codeword of every block, then the second of
 * every block, and so on; blocks with fewer data codewords are simply skipped once exhausted.
 * The parity codewords are interleaved the same way afterwards.
 *
 * Port of {@code org.bars4j.encode.matrix.qr.QrDataBlocks}.
 */
export function interleave(dataCodewords: number[], ecBlocks: EcBlocks): number[] {
  const parityLength = ecBlocks.ecCodewordsPerBlock;
  const blocks = splitIntoBlocks(dataCodewords, ecBlocks);

  const parity: number[][] = new Array(blocks.length);
  for (let b = 0; b < blocks.length; b++) {
    parity[b] = reedSolomonEncode(blocks[b], parityLength);
  }

  const sequence = new Array<number>(dataCodewords.length + blocks.length * parityLength);
  let written = 0;

  let longest = 0;
  for (const block of blocks) {
    longest = Math.max(longest, block.length);
  }
  for (let position = 0; position < longest; position++) {
    for (const block of blocks) {
      if (position < block.length) {
        sequence[written++] = block[position];
      }
    }
  }
  for (let position = 0; position < parityLength; position++) {
    for (let b = 0; b < parity.length; b++) {
      sequence[written++] = parity[b][position];
    }
  }
  return sequence;
}

/** Copies consecutive slices of {@code dataCodewords} into one array per block. */
function splitIntoBlocks(dataCodewords: number[], ecBlocks: EcBlocks): number[][] {
  const blocks = new Array<number[]>(numBlocks(ecBlocks));
  let start = 0;
  let b = 0;
  for (const group of ecBlocks.blocks) {
    for (let n = 0; n < group.count; n++) {
      blocks[b++] = dataCodewords.slice(start, start + group.dataCodewords);
      start += group.dataCodewords;
    }
  }
  return blocks;
}
