import type { RasterImage } from "../raster-image.ts";
import type { RenderSpec } from "../render-spec.ts";

/**
 * Vertical gap (in pixels) kept between the bottom of the bars and the top of the human-readable
 * text in the raster output, so the label never touches the bars.
 */
export const HUMAN_TEXT_GAP_PX = 2;

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

  /**
   * Draws the text onto an already rendered barcode image, clearing the text area with the given
   * spec's colours. The default delegates to {@link LinearTextRenderer.paintText}
   * (colour-unaware); implementations may override to honour the spec.
   */
  paintTextWithSpec?(image: RasterImage, text: string, nWidth: number, spec: RenderSpec): void;

  /** Appends the SVG markup for the text area (may assume {@code fontSize} of 14). */
  paintTextSvg(text: string, barHeight: number, totalWidth: number): string;

  /**
   * Appends the SVG markup for the text area, clearing the label background with the given
   * spec's colours. The default ignores the spec (legacy behaviour, hardcoded {@code #fff});
   * implementations may override to honour it.
   */
  paintTextSvgWithSpec?(text: string, barHeight: number, totalWidth: number, spec: RenderSpec): string;

  calcTotalHeight(barHeight: number, fontSize: number): number;
}
