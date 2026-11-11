import type { BarcodeSymbol } from "../../symbol/barcode-symbol.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import { createRasterImage, fillRect, parseArgb, type RasterImage } from "../raster-image.ts";
import type { RenderSpec } from "../render-spec.ts";
import { hexColour } from "./colour-util.ts";
import { AbstractLinearRenderer } from "./abstract-linear-renderer.ts";
import { dark } from "./symbol-bits.ts";

/**
 * Width-coded rendering strategy (bit = dark module), reproducing the legacy
 * {@code WidthCodedPainter} for both raster and SVG output.
 *
 * Port of {@code org.bars4j.render.linear.WidthCodedRenderer}.
 */
export class WidthCodedRenderer extends AbstractLinearRenderer {
  toImage(symbol: BarcodeSymbol, spec: RenderSpec): RasterImage {
    const bits = dark(symbol as LinearSymbol);
    const barWidth = spec.barWidth;
    const barHeight = spec.barHeight;

    const width = bits.length * barWidth + 20 * barWidth; // 20 x Xdimension is quiet zone width
    const foreground = parseArgb(spec.foreground);
    const background = parseArgb(spec.background);
    const image = createRasterImage(width, barHeight, background);

    let x = 10 * barWidth; // 10 x Xdimension is leading quiet zone
    for (const bit of bits) {
      fillRect(image, x, 0, barWidth, barHeight, bit ? foreground : background);
      x += barWidth;
    }
    return image;
  }

  renderInto(
    target: RasterImage,
    offsetX: number,
    offsetY: number,
    symbol: BarcodeSymbol,
    spec: RenderSpec,
  ): void {
    const bits = dark(symbol as LinearSymbol);
    const barWidth = spec.barWidth;
    const barHeight = spec.barHeight;
    const foreground = parseArgb(spec.foreground);
    const background = parseArgb(spec.background);

    let x = offsetX + 10 * barWidth;
    for (const bit of bits) {
      fillRect(target, x, offsetY, barWidth, barHeight, bit ? foreground : background);
      x += barWidth;
    }
  }

  toSvg(symbol: BarcodeSymbol, spec: RenderSpec): string {
    const bits = dark(symbol as LinearSymbol);
    const barWidth = spec.barWidth;
    const barHeight = spec.barHeight;

    const totalWidth = this.calcTotalWidth(symbol as LinearSymbol, barWidth, barHeight, spec.wideRatio);

    let res = `<path fill="${hexColour(spec.background)}" d="M0 0h${totalWidth}v${barHeight + 4}H0z"/>`;
    res += `<path fill="${hexColour(spec.foreground)}" d="M${10 * barWidth}`;

    let width = 0;
    let white = 0;
    let relY = 2;
    for (const bit of bits) {
      if (!bit) {
        white += barWidth;
      } else {
        if (white > 0 && width > 0) {
          // The relY token completes the implicit relative moveto pair (m dx dy).
          res += ` ${relY}h${width}v${barHeight}h-${width}zm${white + width}`;
          relY = 0;
          width = 0;
          white = 0;
        }
        width += barWidth;
      }
    }
    if (width > 0) {
      res += ` ${relY}h${width}v${barHeight}h-${width}z"/>`;
    }
    return res;
  }

  calcTotalWidth(symbol: LinearSymbol, barWidth: number, _barHeight: number, _wideRatio: number): number {
    return symbol.elements.length * barWidth + 20 * barWidth;
  }
}
