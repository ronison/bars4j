import { ARGB_BLACK, ARGB_WHITE, type RasterImage } from "../raster-image.ts";
import { drawText } from "../canvas-backend.ts";
import type { LinearTextRenderer } from "./linear-text-renderer.ts";

const FONT_SIZE = 14;

/**
 * Draws a single centred line of text under the barcode (default for most linear formats).
 *
 * Port of {@code org.bars4j.render.linear.BaseLineTextRenderer}.
 */
export class BaseLineTextRenderer implements LinearTextRenderer {
  paintText(image: RasterImage, text: string, nWidth: number): void {
    drawText(image, (pen) => {
      pen.setFont(10 * nWidth);
      const h = pen.fontHeight();
      const center = Math.trunc((image.width - pen.stringWidth(text)) / 2);

      pen.fillRect(0, 0, image.width, Math.trunc(image.height / 20), ARGB_WHITE);
      pen.fillRect(
        0,
        image.height - Math.trunc((h * 9) / 10),
        image.width,
        Math.trunc((h * 9) / 10),
        ARGB_WHITE,
      );
      pen.drawString(text, center, image.height - Math.trunc(h / 10), ARGB_BLACK);
    });
  }

  paintTextSvg(text: string, barHeight: number, totalWidth: number): string {
    let res = "";
    // TEXT AREA
    res += `<path fill="#fff" d="M0 ${barHeight}h${totalWidth}v${FONT_SIZE}H0z"/>`;
    // TEXT
    res += `<text x="${Math.trunc(totalWidth / 2)}" y="${barHeight + FONT_SIZE - 2}" class="small" text-anchor="middle">${text}</text>`;
    return res;
  }

  calcTotalHeight(barHeight: number, fontSize: number): number {
    return barHeight + fontSize;
  }
}
