import type { FormatSpec } from "../format/format-spec.ts";
import type { BarcodeFormat } from "../format/barcode-format.ts";
import { BarcodeRegistry } from "../format/barcode-registry.ts";
import type { RasterImage } from "../render/raster-image.ts";
import { SymbolKind } from "../symbol/symbol-kind.ts";
import { linearDefaults, matrixDefaults, type RenderSpec } from "../render/render-spec.ts";

/**
 * Unified entry point to generate any registered barcode format (1D or 2D).
 *
 * <pre>{@code
 * const svg = BarcodeGenerator.of("EAN13").toSvg("789100031550");
 * }</pre>
 *
 * Port of {@code org.bars4j.api.BarcodeGenerator}.
 */
export class BarcodeGenerator {
  private readonly barcodeFormat: BarcodeFormat;
  private readonly renderSpec: RenderSpec;

  private constructor(barcodeFormat: BarcodeFormat, renderSpec: RenderSpec) {
    this.barcodeFormat = barcodeFormat;
    this.renderSpec = renderSpec;
  }

  static of(formatId: string): BarcodeGenerator {
    const format = BarcodeRegistry.getInstance().get(formatId);
    const spec = format.spec().kind === SymbolKind.MATRIX
      ? matrixDefaults()
      : linearDefaults();
    return new BarcodeGenerator(format, spec);
  }

  with(renderSpec: RenderSpec): BarcodeGenerator {
    return new BarcodeGenerator(this.barcodeFormat, renderSpec);
  }

  format(): FormatSpec {
    return this.barcodeFormat.spec();
  }

  /** Raster output: renders the symbol to a {@link RasterImage} (opaque ARGB pixels). */
  toImage(text: string): RasterImage {
    return this.barcodeFormat.toImage(text, this.renderSpec);
  }

  toSvg(text: string): string {
    return this.barcodeFormat.toSvg(text, this.renderSpec);
  }

  /**
   * Returns an SVG snippet ready to be embedded inline in an HTML page, at its natural size.
   *
   * <pre>{@code
   * const html = BarcodeGenerator.of("EAN13").toHtml("789100031550");
   * // paste directly inside <body>...</body>
   * }</pre>
   */
  toHtml(text: string): string;

  /**
   * Returns an SVG snippet ready to be embedded inline in an HTML page, sized to the given
   * display dimensions in pixels. The {@code viewBox} preserves the symbol geometry, so the
   * barcode scales proportionally.
   */
  toHtml(text: string, displayWidth: number, displayHeight: number): string;

  toHtml(text: string, displayWidth?: number, displayHeight?: number): string {
    if (displayWidth !== undefined || displayHeight !== undefined) {
      if (displayWidth === undefined || displayHeight === undefined) {
        throw new Error("displayWidth and displayHeight must be given together");
      }
      if (displayWidth <= 0 || displayHeight <= 0) {
        throw new Error("displayWidth/displayHeight must be positive");
      }
      return this.barcodeFormat.toHtml(text, this.renderSpec, displayWidth, displayHeight);
    }
    return this.barcodeFormat.toHtml(text, this.renderSpec);
  }

  checkSum(text: string): string {
    return this.barcodeFormat.checkSum(text);
  }
}
