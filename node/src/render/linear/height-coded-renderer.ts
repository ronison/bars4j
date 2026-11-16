import type { BarcodeSymbol } from "../../symbol/barcode-symbol.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import { createRasterImage, fillRect, parseArgb, type RasterImage } from "../raster-image.ts";
import type { RenderSpec } from "../render-spec.ts";
import { hexColour } from "./colour-util.ts";
import { AbstractLinearRenderer } from "./abstract-linear-renderer.ts";
import { dark } from "./symbol-bits.ts";

/**
 * Height-coded rendering strategy (bit = full-height bar, 0 = short bar), reproducing the legacy
 * {@code HeightCodedPainter} (e.g. PostNet) for both raster and SVG output.
 *
 * <p>Like the other 1D renderers it reserves a 10-module quiet zone on each side, so the symbol is
 * horizontally centred within its canvas.
 *
 * Port of {@code org.bars4j.render.linear.HeightCodedRenderer}.
 */
export class HeightCodedRenderer extends AbstractLinearRenderer {
  toImage(symbol: BarcodeSymbol, spec: RenderSpec): RasterImage {
    const bits = dark(symbol as LinearSymbol);
    const barWidth = spec.barWidth;
    const barHeight = spec.barHeight;

    const width = this.calcTotalWidth(symbol as LinearSymbol, barWidth, barHeight, spec.wideRatio);
    const foreground = parseArgb(spec.foreground);
    const image = createRasterImage(width, barHeight, parseArgb(spec.background));

    let x = 10 * barWidth; // 10 x Xdimension is leading quiet zone
    for (const bit of bits) {
      const barUnit = bit ? barHeight : Math.trunc(barHeight / 2);
      fillRect(image, x, barHeight - barUnit, barWidth, barUnit, foreground);
      x += 2 * barWidth;
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

    let x = offsetX + 10 * barWidth;
    for (const bit of bits) {
      const barUnit = bit ? barHeight : Math.trunc(barHeight / 2);
      fillRect(target, x, offsetY + barHeight - barUnit, barWidth, barUnit, foreground);
      x += 2 * barWidth;
    }
  }

  toSvg(symbol: BarcodeSymbol, spec: RenderSpec): string {
    const bits = dark(symbol as LinearSymbol);
    const barWidth = spec.barWidth;
    const barHeight = spec.barHeight;

    const totalWidth = this.calcTotalWidth(symbol as LinearSymbol, barWidth, barHeight, spec.wideRatio);
    const svgY = 2; // historical 2px vertical offset of the ink inside the canvas

    // One absolute subpath per bar, bottom-aligned at the canvas baseline as in the Java
    // renderers: a bar sits svgY px from the top and ends barHeight px below the canvas top.
    let res = `<path fill="${hexColour(spec.background)}" d="M0 0h${totalWidth}v${barHeight + 4}H0z"/>`;
    res += `<path fill="${hexColour(spec.foreground)}" d="`;
    let x = 10 * barWidth;
    for (const bit of bits) {
      const barUnit = bit ? barHeight : Math.trunc(barHeight / 2);
      res += `M${x} ${svgY + barHeight - barUnit}h${barWidth}v${barUnit}h-${barWidth}z`;
      x += 2 * barWidth;
    }
    res += `"/>`;
    return res;
  }

  calcTotalWidth(symbol: LinearSymbol, barWidth: number, _barHeight: number, _wideRatio: number): number {
    return symbol.elements.length * barWidth * 2 - barWidth + 20 * barWidth;
  }
}
