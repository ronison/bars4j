import type { RasterImage } from "../raster-image.ts";

/**
 * Draws the human-readable text of a linear barcode, reproducing the legacy
 * {@code org.bars4j.paint.TextPainter} / {@code render.linear} behaviour.
 *
 * <p>For SVG the layout is emitted as markup; for the raster output the text is drawn onto an
 * already rendered {@link RasterImage} using the optional canvas backend (glyphs are therefore not
 * pixel-identical to the Java AWT output, while the bar geometry is).
 *
 * Port of {@code org.bars4j.render.linear.LinearTextRenderer}.
 */
export interface LinearTextRenderer {
  /** Draws the text onto an already rendered barcode image. */
  paintText(image: RasterImage, text: string, nWidth: number): void;

  /** Appends the SVG markup for the text area (may assume {@code fontSize} of 14). */
  paintTextSvg(text: string, barHeight: number, totalWidth: number): string;

  calcTotalHeight(barHeight: number, fontSize: number): number;
}
