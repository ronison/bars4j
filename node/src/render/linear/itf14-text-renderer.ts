import type { RasterImage } from "../raster-image.ts";
import { BaseLineTextRenderer } from "./base-line-text-renderer.ts";
import type { LinearTextRenderer } from "./linear-text-renderer.ts";

const GROUP_SIZES = [1, 2, 5, 5, 1];

/**
 * Human-readable text renderer for ITF-14.
 *
 * <p>Formats the 14-digit GS1 Identification Key using the standard ITF-14 HRI mask
 * {@code "0 00 00000 00000 0"} (groups of 1, 2, 5, 5 and 1 digits) before delegating the actual
 * drawing to {@link BaseLineTextRenderer}.
 *
 * Port of {@code org.bars4j.render.linear.Itf14TextRenderer}.
 */
export class Itf14TextRenderer implements LinearTextRenderer {
  private readonly delegate = new BaseLineTextRenderer();

  /** Applies the {@code "0 00 00000 00000 0"} mask to a 14-digit value. */
  static format(text: string): string {
    if (text == null || text.length !== 14) {
      return text;
    }
    for (let i = 0; i < text.length; i++) {
      const c = text.charCodeAt(i);
      if (c < 48 || c > 57) {
        return text;
      }
    }
    let result = "";
    let pos = 0;
    for (let i = 0; i < GROUP_SIZES.length; i++) {
      if (i > 0) {
        result += " ";
      }
      const end = pos + GROUP_SIZES[i];
      result += text.substring(pos, end);
      pos = end;
    }
    return result;
  }

  paintText(image: RasterImage, text: string, nWidth: number): void {
    this.delegate.paintText(image, Itf14TextRenderer.format(text), nWidth);
  }

  paintTextSvg(text: string, barHeight: number, totalWidth: number): string {
    return this.delegate.paintTextSvg(Itf14TextRenderer.format(text), barHeight, totalWidth);
  }

  calcTotalHeight(barHeight: number, fontSize: number): number {
    return this.delegate.calcTotalHeight(barHeight, fontSize);
  }
}
