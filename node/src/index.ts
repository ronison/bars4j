/**
 * Bars4J Node.js/TypeScript port — public API.
 *
 * Mirrors the Java public entry points: {@link BarcodeGenerator}, {@link BarcodeRegistry} and the
 * render/encode exception types.
 */

export { BarcodeGenerator } from "./api/barcode-generator.ts";
export { BarcodeRegistry } from "./format/barcode-registry.ts";
export type { BarcodeFormat } from "./format/barcode-format.ts";
export type { FormatSpec } from "./format/format-spec.ts";
export {
  DEFAULT_MAX_TEXT_LENGTH,
  LinearBarcodeFormat,
  MIN_XDIMENSION,
  maxTextLength,
  resetMaxTextLength,
  setMaxTextLength,
  type LinearOptions,
} from "./format/linear-barcode-format.ts";
export { BitSemantics } from "./format/bit-semantics.ts";

export {
  renderSpec,
  linearDefaults,
  matrixDefaults,
  type RenderSpec,
} from "./render/render-spec.ts";
export type { BarcodeRenderer } from "./render/barcode-renderer.ts";
export { MatrixRenderer } from "./render/matrix/matrix-renderer.ts";

// Raster output: ARGB image buffer and PNG export (no native dependency required).
export {
  createRasterImage,
  fingerprint,
  getArgb,
  parseArgb,
  ARGB_BLACK,
  ARGB_WHITE,
  type RasterImage,
} from "./render/raster-image.ts";
export { encodePng } from "./render/png.ts";
export { canvasAvailable } from "./render/canvas-backend.ts";

export { SymbolKind } from "./symbol/symbol-kind.ts";
export type { BarcodeSymbol } from "./symbol/barcode-symbol.ts";
export { LinearSymbol } from "./symbol/linear-symbol.ts";
export { MatrixSymbol } from "./symbol/matrix-symbol.ts";
export { barElement, moduleElement, type BarElement } from "./symbol/bar-element.ts";

export { BarcodeException } from "./encode/api/barcode-exception.ts";
export { InvalidSymbolException } from "./encode/api/invalid-symbol-exception.ts";
export { UnsupportedTextException } from "./encode/api/unsupported-text-exception.ts";
export type { BarcodeEncoder, LinearEncoder } from "./encode/api/barcode-encoder.ts";
export type { MatrixEncoder } from "./encode/api/matrix-encoder.ts";
export { MatrixBarcodeFormat } from "./format/matrix-barcode-format.ts";

// Linear encoders.
export { Ean13Encoder } from "./encode/linear/ean13-encoder.ts";
export { Ean8Encoder } from "./encode/linear/ean8-encoder.ts";
export { Ean5Encoder } from "./encode/linear/ean5-encoder.ts";
export { UpcaEncoder } from "./encode/linear/upca-encoder.ts";
export { UpceEncoder } from "./encode/linear/upce-encoder.ts";
export { Itf14Encoder, ITF14_CODE_LENGTH } from "./encode/linear/itf14-encoder.ts";
export { CodabarEncoder } from "./encode/linear/codabar-encoder.ts";
export { Code11Encoder } from "./encode/linear/code11-encoder.ts";
export { Code39Encoder } from "./encode/linear/code39-encoder.ts";
export { Code39ExtEncoder } from "./encode/linear/code39ext-encoder.ts";
export { Code93Encoder } from "./encode/linear/code93-encoder.ts";
export { Code93ExtEncoder } from "./encode/linear/code93ext-encoder.ts";
export { Code128Encoder } from "./encode/linear/code128-encoder.ts";
export { Standard2of5Encoder } from "./encode/linear/standard2of5-encoder.ts";
export { Iata2of5Encoder } from "./encode/linear/iata2of5-encoder.ts";
export { Interleaved2of5Encoder } from "./encode/linear/interleaved2of5-encoder.ts";
export { Matrix2of5Encoder } from "./encode/linear/matrix2of5-encoder.ts";
export { Datalogic2of5Encoder } from "./encode/linear/datalogic2of5-encoder.ts";
export { MsiPlesseyEncoder } from "./encode/linear/msi-plessey-encoder.ts";
export { PostNetEncoder } from "./encode/linear/postnet-encoder.ts";

// Matrix (2D) encoders.
export { QRCodeEncoder } from "./encode/matrix/qr-code-encoder.ts";
export { DataMatrixEncoder, DataMatrixShape } from "./encode/matrix/data-matrix-encoder.ts";
export { PDF417Encoder } from "./encode/matrix/pdf417-encoder.ts";
export { AztecEncoder } from "./encode/matrix/aztec-encoder.ts";
