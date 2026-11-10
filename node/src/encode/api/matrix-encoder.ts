import type { MatrixSymbol } from "../../symbol/matrix-symbol.ts";
import type { BarcodeEncoder } from "./barcode-encoder.ts";

/**
 * Encoder of matrix (2D) symbols, such as QRCode.
 *
 * Port of {@code org.bars4j.encode.api.MatrixEncoder}.
 */
export interface MatrixEncoder extends BarcodeEncoder<MatrixSymbol> {}
