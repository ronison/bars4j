import type { AztecBitMatrix } from "./aztec-bit-matrix.ts";

/**
 * Metadata and module grid of an encoded Aztec symbol, as produced by {@code AztecSymbolEncoder}.
 *
 * Port of {@code org.bars4j.encode.matrix.aztec.AztecCode}.
 */
export class AztecCode {
  private readonly compactValue: boolean;
  private readonly layersValue: number;
  private readonly codeWordsValue: number;
  private readonly matrixValue: AztecBitMatrix;

  constructor(compact: boolean, layers: number, codeWords: number, matrix: AztecBitMatrix) {
    this.compactValue = compact;
    this.layersValue = layers;
    this.codeWordsValue = codeWords;
    this.matrixValue = matrix;
  }

  /** Returns {@code true} if this is a compact symbol (as opposed to a full symbol). */
  compact(): boolean {
    return this.compactValue;
  }

  /** Returns the number of layers. */
  layers(): number {
    return this.layersValue;
  }

  /** Returns the number of data codewords (not counting error correction words). */
  codeWords(): number {
    return this.codeWordsValue;
  }

  /** Returns the symbol size (width and height) in modules. */
  size(): number {
    return this.matrixValue.size();
  }

  /** Returns the modules as {@code modules[row][column]}, row 0 at the top. */
  modules(): boolean[][] {
    return this.matrixValue.toModules();
  }
}
