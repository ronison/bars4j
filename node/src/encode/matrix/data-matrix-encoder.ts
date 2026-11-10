import type { MatrixEncoder } from "../api/matrix-encoder.ts";
import { UnsupportedTextException } from "../api/unsupported-text-exception.ts";
import { MatrixSymbol } from "../../symbol/matrix-symbol.ts";
import { DataMatrixShape, DataMatrixSymbolInfo } from "./datamatrix/data-matrix-symbol-info.ts";
import { encodeToCodewords, padToCapacity } from "./datamatrix/data-matrix-high-level-encoder.ts";
import { encodeECC200 } from "./datamatrix/data-matrix-error-correction.ts";
import { DataMatrixPlacement } from "./datamatrix/data-matrix-placement.ts";
import { assemble } from "./datamatrix/data-matrix-symbol-assembler.ts";

export { DataMatrixShape };

/**
 * Self-contained Data Matrix (ECC200) encoder.
 *
 * <p>Implements the ISO/IEC 16022 square and rectangular symbols. The pipeline is: ASCII
 * encodation of the ISO-8859-1 input, filler padding up to the selected symbol capacity,
 * Reed-Solomon error protection with interleaving, module placement and finally finder-pattern
 * assembly. No external library is needed at run time.
 *
 * Port of {@code org.bars4j.encode.matrix.DataMatrixEncoder}.
 */
export class DataMatrixEncoder implements MatrixEncoder {
  /** Quiet zone required by the Data Matrix standard, in modules. */
  private static readonly QUIET_ZONE = 1;

  private readonly symbolShape: DataMatrixShape;

  /** Creates an encoder that is free to pick any symbol shape. */
  constructor(shape: DataMatrixShape = DataMatrixShape.NONE) {
    this.symbolShape = shape;
  }

  encode(text: string): MatrixSymbol {
    const message = encodeToCodewords(text);
    let symbolInfo: DataMatrixSymbolInfo;
    try {
      symbolInfo = DataMatrixSymbolInfo.lookup(message.length, this.symbolShape);
    } catch (e) {
      // A too-large message is an input problem, not a configuration error: surface it as a
      // checked UnsupportedTextException like the other encoding failures.
      throw new UnsupportedTextException((e as Error).message);
    }
    const codewords = DataMatrixEncoder.protect(message, symbolInfo);
    const modules = DataMatrixEncoder.placeAndFrame(codewords, symbolInfo);
    return new MatrixSymbol(modules, DataMatrixEncoder.QUIET_ZONE);
  }

  /** Pads the message to capacity and appends the Reed-Solomon error codewords. */
  private static protect(message: number[], symbolInfo: DataMatrixSymbolInfo): number[] {
    const padded = padToCapacity(message, symbolInfo.dataCapacity());
    return encodeECC200(padded, symbolInfo);
  }

  /** Places the codewords over the data regions and wraps each region in its finder pattern. */
  private static placeAndFrame(
    codewords: number[],
    symbolInfo: DataMatrixSymbolInfo,
  ): boolean[][] {
    const placement = new DataMatrixPlacement(
      codewords,
      symbolInfo.symbolDataWidth(),
      symbolInfo.symbolDataHeight(),
    );
    placement.place();
    return assemble(placement, symbolInfo);
  }

  computeCheckSum(_text: string): string {
    // Error correction is internal to the symbol; there is no external check digit.
    return "";
  }
}
