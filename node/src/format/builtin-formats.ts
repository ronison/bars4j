import { AztecEncoder } from "../encode/matrix/aztec-encoder.ts";
import { DataMatrixEncoder } from "../encode/matrix/data-matrix-encoder.ts";
import { PDF417Encoder } from "../encode/matrix/pdf417-encoder.ts";
import { QRCodeEncoder } from "../encode/matrix/qr-code-encoder.ts";
import { CodabarEncoder } from "../encode/linear/codabar-encoder.ts";
import { Code11Encoder } from "../encode/linear/code11-encoder.ts";
import { Code128Encoder } from "../encode/linear/code128-encoder.ts";
import { Code39Encoder } from "../encode/linear/code39-encoder.ts";
import { Code39ExtEncoder } from "../encode/linear/code39ext-encoder.ts";
import { Code93Encoder } from "../encode/linear/code93-encoder.ts";
import { Code93ExtEncoder } from "../encode/linear/code93ext-encoder.ts";
import { Datalogic2of5Encoder } from "../encode/linear/datalogic2of5-encoder.ts";
import { Ean5Encoder } from "../encode/linear/ean5-encoder.ts";
import { Ean8Encoder } from "../encode/linear/ean8-encoder.ts";
import { Ean13Encoder } from "../encode/linear/ean13-encoder.ts";
import { Iata2of5Encoder } from "../encode/linear/iata2of5-encoder.ts";
import { Interleaved2of5Encoder } from "../encode/linear/interleaved2of5-encoder.ts";
import { Itf14Encoder } from "../encode/linear/itf14-encoder.ts";
import { Matrix2of5Encoder } from "../encode/linear/matrix2of5-encoder.ts";
import { MsiPlesseyEncoder } from "../encode/linear/msi-plessey-encoder.ts";
import { PostNetEncoder } from "../encode/linear/postnet-encoder.ts";
import { Standard2of5Encoder } from "../encode/linear/standard2of5-encoder.ts";
import { UpcaEncoder } from "../encode/linear/upca-encoder.ts";
import { UpceEncoder } from "../encode/linear/upce-encoder.ts";
import type { LinearEncoder } from "../encode/api/barcode-encoder.ts";
import type { MatrixEncoder } from "../encode/api/matrix-encoder.ts";
import { BaseLineTextRenderer } from "../render/linear/base-line-text-renderer.ts";
import { Ean13TextRenderer } from "../render/linear/ean13-text-renderer.ts";
import { Ean8TextRenderer } from "../render/linear/ean8-text-renderer.ts";
import { HeightCodedRenderer } from "../render/linear/height-coded-renderer.ts";
import { MatrixRenderer } from "../render/matrix/matrix-renderer.ts";
import { Itf14Renderer } from "../render/linear/itf14-renderer.ts";
import { Itf14TextRenderer } from "../render/linear/itf14-text-renderer.ts";
import type { LinearRenderer } from "../render/linear/linear-renderer.ts";
import type { LinearTextRenderer } from "../render/linear/linear-text-renderer.ts";
import { UpcaTextRenderer } from "../render/linear/upca-text-renderer.ts";
import { UpceTextRenderer } from "../render/linear/upce-text-renderer.ts";
import { WideRatioCodedRenderer } from "../render/linear/wide-ratio-coded-renderer.ts";
import { WidthCodedRenderer } from "../render/linear/width-coded-renderer.ts";
import { SymbolKind } from "../symbol/symbol-kind.ts";
import type { BarcodeRegistry } from "./barcode-registry.ts";
import type { FormatSpec } from "./format-spec.ts";
import { LinearBarcodeFormat } from "./linear-barcode-format.ts";
import { MatrixBarcodeFormat } from "./matrix-barcode-format.ts";

/** x-dimension used by the EAN/UPC/Code11/MSI/Matrix/Datalogic formats (1 module). */
const XD_1 = 0.264583333;
/** x-dimension used by the wide-ratio and larger formats (2 modules). */
const XD_2 = 0.5291666;

/**
 * Extra vertical space (px) reserved below the bars for the ITF-14 human-readable text, so the
 * label does not overlap the bottom bearer bar.
 */
const TEXT_GAP = 8;

/**
 * Registers the formats shipped with the library.
 *
 * Port of {@code org.bars4j.format.BuiltinFormats}.
 */
export function registerBuiltinFormats(registry: BarcodeRegistry): void {
  registry.register(linear("EAN13", "EAN-13", new Ean13Encoder(), new WidthCodedRenderer(),
    new Ean13TextRenderer(), XD_1, 19, 2.0, true, true, true));
  registry.register(linear("EAN8", "EAN-8", new Ean8Encoder(), new WidthCodedRenderer(),
    new Ean8TextRenderer(), XD_1, 19, 2.0, true, true, true));
  registry.register(linear("EAN5", "EAN-5", new Ean5Encoder(), new WidthCodedRenderer(),
    new BaseLineTextRenderer(), XD_1, 17, 2.0, false, false, true));
  registry.register(linear("UPCA", "UPC-A", new UpcaEncoder(), new WidthCodedRenderer(),
    new UpcaTextRenderer(), XD_1, 17, 2.0, true, true, true));
  registry.register(linear("UPCE", "UPC-E", new UpceEncoder(), new WidthCodedRenderer(),
    new UpceTextRenderer(), XD_1, 17, 2.0, true, true, true));
  registry.register(linear("CODABAR", "Codabar", new CodabarEncoder(), new WideRatioCodedRenderer(),
    new BaseLineTextRenderer(), XD_2, 17, 3.0, false, false, true));
  registry.register(linear("CODE39", "Code 39", new Code39Encoder(), new WideRatioCodedRenderer(),
    new BaseLineTextRenderer(), XD_2, 17, 2.0, false, false, true));
  registry.register(linear("CODE39EXT", "Code 39 Extended", new Code39ExtEncoder(), new WideRatioCodedRenderer(),
    new BaseLineTextRenderer(), XD_2, 17, 2.0, false, false, true));
  registry.register(linear("CODE93", "Code 93", new Code93Encoder(), new WidthCodedRenderer(),
    new BaseLineTextRenderer(), XD_2, 17, 2.0, true, false, true));
  registry.register(linear("CODE93EXT", "Code 93 Extended", new Code93ExtEncoder(), new WidthCodedRenderer(),
    new BaseLineTextRenderer(), XD_2, 17, 2.0, true, false, true));
  registry.register(linear("CODE128", "Code 128", new Code128Encoder(), new WidthCodedRenderer(),
    new BaseLineTextRenderer(), XD_2, 17, 2.0, true, false, true));
  registry.register(linear("CODE11", "Code 11", new Code11Encoder(), new WidthCodedRenderer(),
    new BaseLineTextRenderer(), XD_2, 17, 2.0, true, true, true));
  registry.register(linear("MSIPLESSEY", "MSI Plessey", new MsiPlesseyEncoder(), new WidthCodedRenderer(),
    new BaseLineTextRenderer(), XD_1, 17, 2.0, true, true, true));
  registry.register(linear("STANDARD2OF5", "Standard 2 of 5", new Standard2of5Encoder(), new WideRatioCodedRenderer(),
    new BaseLineTextRenderer(), XD_2, 17, 2.0, true, false, true));
  registry.register(linear("IATA2OF5", "IATA 2 of 5", new Iata2of5Encoder(), new WideRatioCodedRenderer(),
    new BaseLineTextRenderer(), XD_2, 17, 2.0, true, false, true));
  registry.register(linear("INTERLEAVED2OF5", "Interleaved 2 of 5", new Interleaved2of5Encoder(), new WideRatioCodedRenderer(),
    new BaseLineTextRenderer(), XD_2, 17, 2.0, true, false, true));
  registry.register(new LinearBarcodeFormat(new Itf14Encoder(), new Itf14Renderer(),
    new Itf14TextRenderer(),
    linearSpec("ITF14", "ITF-14", true, true, true),
    XD_2, 20, 2.5, true, true, true,
    TEXT_GAP));
  registry.register(linear("MATRIX2OF5", "Matrix 2 of 5", new Matrix2of5Encoder(), new WideRatioCodedRenderer(),
    new BaseLineTextRenderer(), XD_1, 17, 2.0, true, false, true));
  registry.register(linear("DATALOGIC2OF5", "Datalogic 2 of 5", new Datalogic2of5Encoder(), new WideRatioCodedRenderer(),
    new BaseLineTextRenderer(), XD_1, 17, 2.0, true, true, true));
  registry.register(linear("POSTNET", "PostNet", new PostNetEncoder(), new HeightCodedRenderer(),
    new BaseLineTextRenderer(), XD_2, 2, 2.0, true, false, false));

  // Matrix (2D) formats.
  registry.register(matrix("QRCODE", "QRCode", new QRCodeEncoder()));
  registry.register(matrix("DATAMATRIX", "Data Matrix", new DataMatrixEncoder()));
  registry.register(matrix("PDF417", "PDF417", new PDF417Encoder()));
  registry.register(matrix("AZTEC", "Aztec", new AztecEncoder()));
}

function matrix(id: string, name: string, encoder: MatrixEncoder): MatrixBarcodeFormat {
  const spec: FormatSpec = {
    id,
    displayName: name,
    kind: SymbolKind.MATRIX,
    supportsCheckDigit: false,
    allowsText: false,
    allowsWideRatio: false,
  };
  return new MatrixBarcodeFormat(encoder, new MatrixRenderer(), spec);
}

function linearSpec(
  id: string,
  name: string,
  checkDigit: boolean,
  text: boolean,
  wideRatio: boolean,
): FormatSpec {
  return {
    id,
    displayName: name,
    kind: SymbolKind.LINEAR,
    supportsCheckDigit: checkDigit,
    allowsText: text,
    allowsWideRatio: wideRatio,
  };
}

function linear(
  id: string,
  name: string,
  encoder: LinearEncoder,
  renderer: LinearRenderer,
  textRenderer: LinearTextRenderer,
  xDimension: number,
  barHeight: number,
  wideRatio: number,
  checkDigit: boolean,
  showCheckDigit: boolean,
  showText: boolean,
): LinearBarcodeFormat {
  const spec = linearSpec(id, name, checkDigit, showText, renderer instanceof WideRatioCodedRenderer);
  return new LinearBarcodeFormat(encoder, renderer, textRenderer, spec,
    xDimension, barHeight, wideRatio, checkDigit, showCheckDigit, showText);
}
