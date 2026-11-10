import { AztecBitBuffer } from "./aztec-bit-buffer.ts";
import { AztecBitMatrix } from "./aztec-bit-matrix.ts";

/**
 * Geometry of an Aztec symbol of a given layout and layer count: how many modules it spans, where
 * the layers sit relative to the centre, and how the data bits, mode message, central bull's-eye and
 * (for full symbols) the reference grid are placed.
 *
 * <p>The data modules are laid out in four runs per layer, spiralling around the centre. Full
 * symbols interleave the reference grid between layers, so a mapping from layer coordinates to
 * module coordinates is needed; compact symbols have no grid and use the identity mapping.
 *
 * Port of {@code org.bars4j.encode.matrix.aztec.AztecLayout}.
 */
export class AztecLayout {
  private readonly compactValue: boolean;
  private readonly layersValue: number;
  /** Symbol size before the reference grid is interleaved (equals the final size when compact). */
  private readonly baseSizeValue: number;
  private readonly matrixSizeValue: number;
  private readonly moduleOfBaseValue: number[];

  private constructor(compact: boolean, layers: number) {
    this.compactValue = compact;
    this.layersValue = layers;
    this.baseSizeValue = (compact ? 11 : 14) + layers * 4;
    this.moduleOfBaseValue = new Array<number>(this.baseSizeValue).fill(0);

    if (compact) {
      this.matrixSizeValue = this.baseSizeValue;
      for (let i = 0; i < this.baseSizeValue; i++) {
        this.moduleOfBaseValue[i] = i;
      }
    } else {
      // Each group of 15 layer positions gains one reference-grid line (and one extra at the
      // centre), so the final symbol is wider than the layers alone.
      this.matrixSizeValue =
        this.baseSizeValue + 1 + 2 * Math.trunc((Math.trunc(this.baseSizeValue / 2) - 1) / 15);
      const halfBase = Math.trunc(this.baseSizeValue / 2);
      const center = Math.trunc(this.matrixSizeValue / 2);
      for (let i = 0; i < halfBase; i++) {
        const gridOffset = i + Math.trunc(i / 15);
        this.moduleOfBaseValue[halfBase - i - 1] = center - gridOffset - 1;
        this.moduleOfBaseValue[halfBase + i] = center + gridOffset + 1;
      }
    }
  }

  /** Returns the layout for the given symbol kind and layer count. */
  static of(compact: boolean, layers: number): AztecLayout {
    return new AztecLayout(compact, layers);
  }

  /** Returns the final symbol size in modules. */
  matrixSize(): number {
    return this.matrixSizeValue;
  }

  /** Builds the complete module grid for {@code dataBits} and {@code modeMessage}. */
  build(dataBits: AztecBitBuffer, modeMessage: AztecBitBuffer): AztecBitMatrix {
    const matrix = new AztecBitMatrix(this.matrixSizeValue);
    this.placeDataBits(matrix, dataBits);
    this.placeModeMessage(matrix, modeMessage);
    this.drawBullseye(matrix);
    if (!this.compactValue) {
      this.drawReferenceGrid(matrix);
    }
    return matrix;
  }

  /**
   * Places the data bits. For each layer the bits form four runs, each two modules wide: the top
   * side runs left to right, the right side top to bottom, the bottom side right to left, and the
   * left side bottom to top. Inner layers have shorter sides, so the run length shrinks towards
   * the centre.
   */
  private placeDataBits(matrix: AztecBitMatrix, dataBits: AztecBitBuffer): void {
    let cursor = 0;
    for (let layer = 0; layer < this.layersValue; layer++) {
      const sideLength = (this.layersValue - layer) * 4 + (this.compactValue ? 9 : 12);
      for (let pair = 0; pair < sideLength; pair++) {
        const offset = pair * 2;
        for (let bit = 0; bit < 2; bit++) {
          const outer = layer * 2 + bit;
          const inner = layer * 2 + pair;
          const far = this.baseSizeValue - 1 - outer;
          const near = this.baseSizeValue - 1 - inner;

          if (dataBits.get(cursor + offset + bit)) {
            matrix.set(this.moduleOfBaseValue[outer], this.moduleOfBaseValue[inner]);
          }
          if (dataBits.get(cursor + sideLength * 2 + offset + bit)) {
            matrix.set(this.moduleOfBaseValue[inner], this.moduleOfBaseValue[far]);
          }
          if (dataBits.get(cursor + sideLength * 4 + offset + bit)) {
            matrix.set(this.moduleOfBaseValue[far], this.moduleOfBaseValue[near]);
          }
          if (dataBits.get(cursor + sideLength * 6 + offset + bit)) {
            matrix.set(this.moduleOfBaseValue[near], this.moduleOfBaseValue[outer]);
          }
        }
      }
      cursor += sideLength * 8;
    }
  }

  /**
   * Places the mode message around the bull's-eye: seven (compact) or ten (full) bits per side,
   * read counter-clockwise starting on the top edge.
   */
  private placeModeMessage(matrix: AztecBitMatrix, modeMessage: AztecBitBuffer): void {
    const center = Math.trunc(this.matrixSizeValue / 2);
    if (this.compactValue) {
      for (let i = 0; i < 7; i++) {
        const offset = center - 3 + i;
        if (modeMessage.get(i)) {
          matrix.set(offset, center - 5);
        }
        if (modeMessage.get(i + 7)) {
          matrix.set(center + 5, offset);
        }
        if (modeMessage.get(20 - i)) {
          matrix.set(offset, center + 5);
        }
        if (modeMessage.get(27 - i)) {
          matrix.set(center - 5, offset);
        }
      }
    } else {
      for (let i = 0; i < 10; i++) {
        // The middle of each side is one module further out to clear the reference grid.
        const offset = center - 5 + i + Math.trunc(i / 5);
        if (modeMessage.get(i)) {
          matrix.set(offset, center - 7);
        }
        if (modeMessage.get(i + 10)) {
          matrix.set(center + 7, offset);
        }
        if (modeMessage.get(29 - i)) {
          matrix.set(offset, center + 7);
        }
        if (modeMessage.get(39 - i)) {
          matrix.set(center - 7, offset);
        }
      }
    }
  }

  /** Draws the concentric square rings of the finder, plus the three orientation marks. */
  private drawBullseye(matrix: AztecBitMatrix): void {
    const center = Math.trunc(this.matrixSizeValue / 2);
    const radius = this.compactValue ? 5 : 7;
    for (let ring = 0; ring < radius; ring += 2) {
      for (let position = center - ring; position <= center + ring; position++) {
        matrix.set(position, center - ring);
        matrix.set(position, center + ring);
        matrix.set(center - ring, position);
        matrix.set(center + ring, position);
      }
    }
    // Three dark corner modules break the ring's symmetry.
    matrix.set(center - radius, center - radius);
    matrix.set(center - radius + 1, center - radius);
    matrix.set(center - radius, center - radius + 1);
    matrix.set(center + radius, center - radius);
    matrix.set(center + radius, center - radius + 1);
    matrix.set(center + radius, center + radius - 1);
  }

  /** Draws the full-symbol reference grid: dashed lines every 16 modules, both ways. */
  private drawReferenceGrid(matrix: AztecBitMatrix): void {
    for (
      let base = 0, moduleOffset = 0;
      base < Math.trunc(this.baseSizeValue / 2) - 1;
      base += 15, moduleOffset += 16
    ) {
      const center = Math.trunc(this.matrixSizeValue / 2);
      for (let position = center & 1; position < this.matrixSizeValue; position += 2) {
        matrix.set(center - moduleOffset, position);
        matrix.set(center + moduleOffset, position);
        matrix.set(position, center - moduleOffset);
        matrix.set(position, center + moduleOffset);
      }
    }
  }
}
