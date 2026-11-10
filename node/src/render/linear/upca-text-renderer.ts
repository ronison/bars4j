import { ARGB_BLACK, ARGB_WHITE, type RasterImage } from "../raster-image.ts";
import { drawText } from "../canvas-backend.ts";
import type { LinearTextRenderer } from "./linear-text-renderer.ts";

/**
 * UPC-A specific text layout (number system + two 5-digit groups + check digit).
 *
 * Port of {@code org.bars4j.render.linear.UpcaTextRenderer}.
 */
export class UpcaTextRenderer implements LinearTextRenderer {
  paintText(image: RasterImage, text: string, nWidth: number): void {
    drawText(image, (pen) => {
      pen.setFont(11 * nWidth);
      const textWidth = nWidth * 35;
      const textHeight = pen.fontHeight();
      const textXShift = Math.trunc((textWidth - pen.stringWidth("00000") + 2 * nWidth) / 2);
      const textYPos = image.height - Math.trunc(textHeight / 10);

      let x = nWidth * 20;
      const y = image.height - Math.trunc((textHeight * 4) / 5);

      pen.fillRect(0, 0, image.width, Math.trunc(image.height / 20), ARGB_WHITE);
      pen.fillRect(
        0,
        image.height - Math.trunc(textHeight / 3),
        image.width,
        Math.trunc(textHeight / 3),
        ARGB_WHITE,
      );

      pen.fillRect(x, y, textWidth, textHeight, ARGB_WHITE);
      pen.drawString(text.substring(0, 1), 8 * nWidth - pen.stringWidth("0"), textYPos, ARGB_BLACK);
      pen.drawString(text.substring(1, 6), x + textXShift, textYPos, ARGB_BLACK);

      x = nWidth * 59;
      pen.fillRect(x, y, textWidth, textHeight, ARGB_WHITE);
      pen.drawString(text.substring(6, 11), x + textXShift, textYPos, ARGB_BLACK);

      // check digit
      pen.drawString(text.substring(11), image.width - 8 * nWidth, textYPos, ARGB_BLACK);
    });
  }

  paintTextSvg(text: string, barHeight: number, totalWidth: number): string {
    const t1 = text.substring(0, 1);
    const t2 = text.substring(1, 6);
    const t3 = text.substring(6, 11);
    const t4 = text.substring(11, 12);
    const posY = Math.round(barHeight * 0.9);

    let res = "";
    res += `<path fill="#fff" d="M0 ${barHeight}h${totalWidth}v8H0z"/>`;
    res += `<path fill="#fff" d="M20 ${posY}h36v14H20zm39 0h36v14H59z"/>`;
    res += `<text x="0" y="${posY + 10}" class="small" textLength="8">${t1}</text>`;
    res += `<text x="20" y="${posY + 10}" class="small" textLength="36">${t2}</text>`;
    res += `<text x="59" y="${posY + 10}" class="small" textLength="36">${t3}</text>`;
    res += `<text x="106" y="${posY + 10}" class="small" textLength="8">${t4}</text>`;
    return res;
  }

  calcTotalHeight(barHeight: number, fontSize: number): number {
    return Math.ceil(barHeight * 0.9 + fontSize);
  }
}
