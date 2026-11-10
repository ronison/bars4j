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

  checkSum(text: string): string;
}
