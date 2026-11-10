import { ARGB_BLACK, ARGB_WHITE, type RasterImage } from "../raster-image.ts";
import { drawText } from "../canvas-backend.ts";
import type { LinearTextRenderer } from "./linear-text-renderer.ts";

/**
 * EAN-8 specific text layout (two 4-digit groups).
 *
 * Port of {@code org.bars4j.render.linear.Ean8TextRenderer}.
 */
export class Ean8TextRenderer implements LinearTextRenderer {
  paintText(image: RasterImage, text: string, nWidth: number): void {
    drawText(image, (pen) => {
      pen.setFont(11 * nWidth);
      const textWidth = nWidth * 28;
      const textHeight = pen.fontHeight();
      const textXShift = Math.trunc((textWidth - pen.stringWidth("0000") + 2 * nWidth) / 2);
      const textYPos = image.height - Math.trunc(textHeight / 10);

      let x = nWidth * 13;
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
      pen.drawString(text.substring(0, 4), x + textXShift, textYPos, ARGB_BLACK);

      x = nWidth * 45;
      pen.fillRect(x, y, textWidth, textHeight, ARGB_WHITE);
      pen.drawString(text.substring(4), x + textXShift, textYPos, ARGB_BLACK);
    });
  }

  paintTextSvg(text: string, barHeight: number, totalWidth: number): string {
    const t1 = text.substring(0, 4);
    const t2 = text.substring(4, 8);
    const posY = Math.ceil(barHeight * 0.9);

    let res = "";
    res += `<path fill="#fff" d="M0 ${barHeight}h${totalWidth}v8H0z"/>`;
    res += `<path fill="#fff" d="M13 ${posY}h29v14H13zm32 0h29v14H45z"/>`;
    res += `<text x="13" y="${posY + 10}" class="small" textLength="29">${t1}</text>`;
    res += `<text x="45" y="${posY + 10}" class="small" textLength="29">${t2}</text>`;
    return res;
  }

  calcTotalHeight(barHeight: number, fontSize: number): number {
    return Math.ceil(barHeight * 0.9 + fontSize);
  }
}
