import type { BarcodeSymbol } from "../symbol/barcode-symbol.ts";
import type { RasterImage } from "./raster-image.ts";
import type { RenderSpec } from "./render-spec.ts";

/**
 * Renders a {@link BarcodeSymbol} to a raster image or an SVG fragment.
 *
 * <p>{@link #toSvg} returns only the drawing markup (paths/rects); assembling the surrounding
 * SVG document and drawing the human-readable text is the responsibility of the caller/facade.
 *
 * Port of {@code org.bars4j.render.BarcodeRenderer}.
 */
export interface BarcodeRenderer {
  /** Renders the symbol to a new {@link RasterImage}. */
  toImage(symbol: BarcodeSymbol, spec: RenderSpec): RasterImage;

  /** SVG drawing markup only (no surrounding document). */
  toSvg(symbol: BarcodeSymbol, spec: RenderSpec): string;

  /**
   * Draws the symbol into an existing target image at the given offset, without allocating an
   * intermediate image. The default behaviour is to blit {@link #toImage}.
   */
  renderInto(
    target: RasterImage,
    offsetX: number,
    offsetY: number,
    symbol: BarcodeSymbol,
    spec: RenderSpec,
  ): void;
}
