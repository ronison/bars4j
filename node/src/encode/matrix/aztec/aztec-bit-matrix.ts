/**
 * A square grid of Aztec modules addressed by {@code (x, y)} (column, row). Internally the modules
 * are kept row-major so that they can be handed to {@code MatrixSymbol} directly.
 *
 * Port of {@code org.bars4j.encode.matrix.aztec.AztecBitMatrix}.
 */
export class AztecBitMatrix {
  private readonly sizeValue: number;
  private readonly bits: boolean[];

  constructor(size: number) {
    if (size <= 0) {
      throw new RangeError("size must be positive");
    }
    this.sizeValue = size;
    this.bits = new Array<boolean>(size * size).fill(false);
  }

  /** Returns the width/height of the symbol in modules. */
  size(): number {
    return this.sizeValue;
  }

  /** Marks the module at column {@code x}, row {@code y} as dark. */
  set(x: number, y: number): void {
    this.bits[y * this.sizeValue + x] = true;
  }

  /** Returns {@code true} if the module at column {@code x}, row {@code y} is dark. */
  get(x: number, y: number): boolean {
    return this.bits[y * this.sizeValue + x];
  }

  /** Returns the modules as {@code modules[row][column]}, row 0 at the top. */
  toModules(): boolean[][] {
    const modules: boolean[][] = [];
    for (let y = 0; y < this.sizeValue; y++) {
      modules.push(this.bits.slice(y * this.sizeValue, (y + 1) * this.sizeValue));
    }
    return modules;
  }
}
