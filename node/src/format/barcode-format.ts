import type { RasterImage } from "../render/raster-image.ts";
import type { RenderSpec } from "../render/render-spec.ts";
import type { FormatSpec } from "./format-spec.ts";

/**
 * A registered barcode format, able to render text to an image or an SVG fragment.
 *
 * Port of {@code org.bars4j.format.BarcodeFormat}.
 */
export interface BarcodeFormat {
  id(): string;

  spec(): FormatSpec;

  /** Raster output, rendered to an opaque {@link RasterImage}. */
  toImage(text: string, spec: RenderSpec): RasterImage;

  toSvg(text: string, spec: RenderSpec): string;

  /**
   * Renders the embeddable HTML/SVG snippet for web pages: no XML prologue, with a
   * {@code viewBox}, {@code role="img"} and an {@code aria-label}.
   */
  toHtml(text: string, spec: RenderSpec): string;

  /**
   * Renders the embeddable HTML/SVG snippet with an explicit display size in pixels
   * (a display size of -1 keeps the natural size).
   */
  toHtml(text: string, spec: RenderSpec, displayWidth: number, displayHeight: number): string;

  checkSum(text: string): string;
}
