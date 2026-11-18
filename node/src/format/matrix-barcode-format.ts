import type { MatrixEncoder } from "../encode/api/matrix-encoder.ts";
import type { BarcodeRenderer } from "../render/barcode-renderer.ts";
import type { RasterImage } from "../render/raster-image.ts";
import type { RenderSpec } from "../render/render-spec.ts";
import type { MatrixSymbol } from "../symbol/matrix-symbol.ts";
import type { BarcodeFormat } from "./barcode-format.ts";
import type { FormatSpec } from "./format-spec.ts";
import { htmlSnippet } from "./html-snippet.ts";

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
    // Width and height are computed separately: not every matrix symbol is square (PDF417 is
    // rectangular), and a square canvas would letterbox the symbol with dead space.
    const width = (symbol.widthInModules() + 2 * symbol.quietZone()) * renderSpec.moduleSize;
    const height = (symbol.heightInModules() + 2 * symbol.quietZone()) * renderSpec.moduleSize;
    // Document assembly mirrors the Java SvgDocument: XML prologue, fixed canvas (no viewBox)
    // and the inline-SVG fallback text.
    return `<?xml version="1.0" encoding="UTF-8" standalone="no" ?>\n`
      + `<!-- Created with Bars4J -->\n`
      + `<svg xmlns="http://www.w3.org/2000/svg" version="1.2" width="${width}" height="${height}"`
      + ` preserveAspectRatio="xMidYMid meet">`
      + fragment
      + `Sorry, your browser does not support inline SVG.</svg>`;
  }

  /**
   * Renders the embeddable HTML snippet at its natural size
   * ({@code width}/{@code height} of -1 keeps the natural size).
   */
  toHtml(text: string, renderSpec: RenderSpec): string;

  /**
   * Renders the embeddable HTML snippet with an explicit display size. The {@code viewBox}
   * keeps the natural geometry so the symbol scales proportionally.
   */
  toHtml(text: string, renderSpec: RenderSpec, displayWidth: number, displayHeight: number): string;

  toHtml(text: string, renderSpec: RenderSpec, displayWidth = -1, displayHeight = -1): string {
    const symbol: MatrixSymbol = this.encoder.encode(text);
    const fragment = this.renderer.toSvg(symbol, renderSpec);
    // Width and height are computed separately: not every matrix symbol is square (PDF417 is
    // rectangular), and a square canvas would letterbox the symbol with dead space.
    const width = (symbol.widthInModules() + 2 * symbol.quietZone()) * renderSpec.moduleSize;
    const height = (symbol.heightInModules() + 2 * symbol.quietZone()) * renderSpec.moduleSize;
    return htmlSnippet(width, height, displayWidth, displayHeight,
      this.specValue.displayName + " " + text, fragment);
  }

  checkSum(text: string): string {
    return this.encoder.computeCheckSum(text);
  }
}
