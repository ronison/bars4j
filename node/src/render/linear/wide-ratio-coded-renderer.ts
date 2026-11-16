import type { BarcodeSymbol } from "../../symbol/barcode-symbol.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import { createRasterImage, fillRect, parseArgb, type RasterImage } from "../raster-image.ts";
import type { RenderSpec } from "../render-spec.ts";
import { hexColour } from "./colour-util.ts";
import { AbstractLinearRenderer } from "./abstract-linear-renderer.ts";
import { wide } from "./symbol-bits.ts";

/**
 * Wide-to-narrow ratio rendering strategy (bit = wide element, colour alternates starting dark),
 * reproducing the legacy {@code WideRatioCodedPainter} for both raster and SVG output.
 *
 * Port of {@code org.bars4j.render.linear.WideRatioCodedRenderer}.
 */
export class WideRatioCodedRenderer extends AbstractLinearRenderer {
  toImage(symbol: BarcodeSymbol, spec: RenderSpec): RasterImage {
    const bits = wide(symbol as LinearSymbol);
    const barWidth = spec.barWidth;
    const barHeight = spec.barHeight;
    const wideRatio = spec.wideRatio;

    let width = 20 * barWidth; // 20 x Xdimension is quiet zone width
    const wideWidth = Math.round(barWidth * wideRatio);
    for (const bit of bits) {
      width += bit ? wideWidth : barWidth;
    }

    const foreground = parseArgb(spec.foreground);
    const background = parseArgb(spec.background);
    const image = createRasterImage(width, barHeight, background);

    let x = 10 * barWidth; // 10 x Xdimension is leading quiet zone
    let flag = true;
    let colour = foreground;
    for (const bit of bits) {
      const w = bit ? wideWidth : barWidth;
      fillRect(image, x, 0, w, barHeight, colour);
      colour = flag ? background : foreground;
      flag = !flag;
      x += w;
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
    const bits = wide(symbol as LinearSymbol);
    const barWidth = spec.barWidth;
    const barHeight = spec.barHeight;
    const wideWidth = Math.round(barWidth * spec.wideRatio);
    const foreground = parseArgb(spec.foreground);
    const background = parseArgb(spec.background);

    let x = offsetX + 10 * barWidth;
    let flag = true;
    let colour = foreground;
    for (const bit of bits) {
      const w = bit ? wideWidth : barWidth;
      fillRect(target, x, offsetY, w, barHeight, colour);
      colour = flag ? background : foreground;
      flag = !flag;
      x += w;
    }
  }

  toSvg(symbol: BarcodeSymbol, spec: RenderSpec): string {
    const bits = wide(symbol as LinearSymbol);
    const barWidth = spec.barWidth;
    const barHeight = spec.barHeight;
    const wideRatio = spec.wideRatio;

    const totalWidth = this.calcTotalWidth(symbol as LinearSymbol, barWidth, barHeight, wideRatio);
    const wideWidth = Math.round(barWidth * wideRatio);
    const svgY = 2; // historical 2px vertical offset of the ink inside the canvas

    // One absolute subpath per element, alternating ink starting dark, in separate paths per
    // colour — reproducing the Java renderers (one rect per element instead of the legacy
    // run-length fusion).
    let res = `<path fill="${hexColour(spec.background)}" d="M0 0h${totalWidth}v${barHeight + 4}H0z"/>`;
    let foregrounds = `<path fill="${hexColour(spec.foreground)}" d="`;
    let backgrounds = `<path fill="${hexColour(spec.background)}" d="`;
    let x = 10 * barWidth;
    let dark = true;
    for (const bit of bits) {
      const w = bit ? wideWidth : barWidth;
      const rect = `M${x} ${svgY}h${w}v${barHeight}h-${w}z`;
      if (dark) {
        foregrounds += rect;
      } else {
        backgrounds += rect;
      }
      dark = !dark;
      x += w;
    }
    res += foregrounds + `"/>` + backgrounds + `"/>`;
    return res;
  }

  calcTotalWidth(symbol: LinearSymbol, barWidth: number, _barHeight: number, wideRatio: number): number {
    let res = 20 * barWidth;
    const wideWidth = Math.round(barWidth * wideRatio);
    for (const bit of wide(symbol)) {
      res += bit ? wideWidth : barWidth;
    }
    return res;
  }
}
