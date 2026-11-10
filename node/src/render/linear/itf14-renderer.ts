import type { BarcodeSymbol } from "../../symbol/barcode-symbol.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import { fillRect, parseArgb, type RasterImage } from "../raster-image.ts";
import type { RenderSpec } from "../render-spec.ts";
import { WideRatioCodedRenderer } from "./wide-ratio-coded-renderer.ts";

/** Bearer-bar thickness as a fraction of the bar height. */
const BEARER_RATIO = 0.1;

/** Vertical offset used by {@link WideRatioCodedRenderer.toSvg} for the bar baseline. */
const SVG_TOP = 2;

/**
 * ITF-14 rendering strategy.
 *
 * <p>Draws the Interleaved 2 of 5 wide/narrow bars (inherited from {@link WideRatioCodedRenderer})
 * framed by the mandatory GS1 bearer bars: a horizontal bar above and below the symbol spanning the
 * whole symbol including its quiet zones.
 *
 * Port of {@code org.bars4j.render.linear.Itf14Renderer}.
 */
export class Itf14Renderer extends WideRatioCodedRenderer {
  override toImage(symbol: BarcodeSymbol, spec: RenderSpec): RasterImage {
    const image = super.toImage(symbol, spec);
    this.paintBearer(image, symbol as LinearSymbol, spec, 0, 0);
    return image;
  }

  override renderInto(
    target: RasterImage,
    offsetX: number,
    offsetY: number,
    symbol: BarcodeSymbol,
    spec: RenderSpec,
  ): void {
    super.renderInto(target, offsetX, offsetY, symbol, spec);
    this.paintBearer(target, symbol as LinearSymbol, spec, offsetX, offsetY);
  }

  toSvg(symbol: BarcodeSymbol, spec: RenderSpec): string {
    const bars = super.toSvg(symbol, spec);

    const barWidth = spec.barWidth;
    const barHeight = spec.barHeight;
    const bearer = bearerWidth(barHeight);
    const fullWidth = this.calcTotalWidth(symbol as LinearSymbol, barWidth, barHeight, spec.wideRatio);
    const bottom = SVG_TOP + barHeight - bearer;

    let frame = `<path d="`;
    // top bearer bar
    frame += rect(0, SVG_TOP, fullWidth, bearer);
    // bottom bearer bar
    frame += rect(0, bottom, fullWidth, bearer);
    frame += `"/>`;
    return bars + frame;
  }

  private paintBearer(
    target: RasterImage,
    symbol: LinearSymbol,
    spec: RenderSpec,
    offsetX: number,
    offsetY: number,
  ): void {
    const barWidth = spec.barWidth;
    const barHeight = spec.barHeight;
    const bearer = bearerWidth(barHeight);
    const fullWidth = this.calcTotalWidth(symbol, barWidth, barHeight, spec.wideRatio);
    const bottom = offsetY + barHeight - bearer;
    const foreground = parseArgb(spec.foreground);

    // GS1 ITF-14 bearer bars: a horizontal bar above and below the symbol, spanning the bars
    // plus the quiet zones (the full width of the symbol).
    fillRect(target, offsetX, offsetY, fullWidth, bearer, foreground); // top
    fillRect(target, offsetX, bottom, fullWidth, bearer, foreground); // bottom
  }
}

function rect(x: number, y: number, width: number, height: number): string {
  return `M${x} ${y}h${width}v${height}h-${width}z`;
}

function bearerWidth(barHeight: number): number {
  return Math.max(2, Math.round(barHeight * BEARER_RATIO));
}
