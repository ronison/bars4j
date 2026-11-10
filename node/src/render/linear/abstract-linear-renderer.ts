import type { BarcodeSymbol } from "../../symbol/barcode-symbol.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import { blit, type RasterImage } from "../raster-image.ts";
import type { RenderSpec } from "../render-spec.ts";
import type { LinearRenderer } from "./linear-renderer.ts";

/**
 * Base class for the native linear renderers.
 *
 * <p>Implements the default {@link LinearRenderer.renderInto} (a plain blit of {@link #toImage}),
 * which concrete strategies override when they can paint straight into the destination.
 */
export abstract class AbstractLinearRenderer implements LinearRenderer {
  abstract toImage(symbol: BarcodeSymbol, spec: RenderSpec): RasterImage;

  abstract toSvg(symbol: BarcodeSymbol, spec: RenderSpec): string;

  abstract calcTotalWidth(
    symbol: LinearSymbol,
    barWidth: number,
    barHeight: number,
    wideRatio: number,
  ): number;

  renderInto(
    target: RasterImage,
    offsetX: number,
    offsetY: number,
    symbol: BarcodeSymbol,
    spec: RenderSpec,
  ): void {
    blit(target, this.toImage(symbol, spec), offsetX, offsetY);
  }
}
