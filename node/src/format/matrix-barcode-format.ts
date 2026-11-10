import type { MatrixEncoder } from "../encode/api/matrix-encoder.ts";
import type { BarcodeRenderer } from "../render/barcode-renderer.ts";
import type { RasterImage } from "../render/raster-image.ts";
import type { RenderSpec } from "../render/render-spec.ts";
import type { MatrixSymbol } from "../symbol/matrix-symbol.ts";
import type { BarcodeFormat } from "./barcode-format.ts";
import type { FormatSpec } from "./format-spec.ts";

/**
 * A matrix (2D) format backed by the symbol/renderer pipeline (e.g. QRCode).
 *
 * Port of {@code org.bars4j.format.MatrixBarcodeFormat}.
 */
export class MatrixBarcodeFormat implements BarcodeFormat {
  private readonly encoder: MatrixEncoder;
  private readonly renderer: BarcodeRenderer;
  private readonly specValue: FormatSpec;

  constructor(encoder: MatrixEncoder, renderer: BarcodeRenderer, spec: FormatSpec) {
    this.encoder = encoder;
    this.renderer = renderer;
    this.specValue = spec;
  }

  id(): string {
    return this.specValue.id;
  }

  spec(): FormatSpec {
    return this.specValue;
  }

  toImage(text: string, renderSpec: RenderSpec): RasterImage {
    return this.renderer.toImage(this.encoder.encode(text), renderSpec);
  }

  toSvg(text: string, renderSpec: RenderSpec): string {
    const symbol: MatrixSymbol = this.encoder.encode(text);
    const fragment = this.renderer.toSvg(symbol, renderSpec);
    const total = (symbol.widthInModules() + 2 * symbol.quietZone()) * renderSpec.moduleSize;
    return `<?xml version="1.0" encoding="UTF-8" standalone="no" ?>\n`
      + `<!-- Created with Bars4J -->\n`
      + `<svg xmlns="http://www.w3.org/2000/svg" version="1.2" width="${total}" height="${total}" viewBox="0 0 ${total} ${total}">`
      + fragment
      + `</svg>`;
  }

  checkSum(text: string): string {
    return this.encoder.computeCheckSum(text);
  }
}
