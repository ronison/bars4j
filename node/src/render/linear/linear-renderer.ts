import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { BarcodeRenderer } from "../barcode-renderer.ts";

/**
 * Renderer of {@link LinearSymbol}s.
 *
 * <p>Extends {@link BarcodeRenderer} with the total-width calculation that the SVG assembly needs.
 *
 * Port of {@code org.bars4j.render.linear.LinearRenderer}.
 */
export interface LinearRenderer extends BarcodeRenderer {
  calcTotalWidth(symbol: LinearSymbol, barWidth: number, barHeight: number, wideRatio: number): number;
}
