import { ARGB_BLACK, ARGB_WHITE, type RasterImage } from "../raster-image.ts";
import { drawText } from "../canvas-backend.ts";
import { HUMAN_TEXT_GAP_PX, type LinearTextRenderer } from "./linear-text-renderer.ts";

/**
 * EAN-13 specific text layout (guard bars left visible, three text groups).
 *
 * Port of {@code org.bars4j.render.linear.Ean13TextRenderer}.
 */
export class Ean13TextRenderer implements LinearTextRenderer {
  paintText(image: RasterImage, text: string, nWidth: number): void {
    drawText(image, (pen) => {
      pen.setFont(11 * nWidth);
      const textWidth = nWidth * 42;
      const textHeight = pen.fontHeight();
      const textXShift = Math.trunc((textWidth - pen.stringWidth("000000") + 2 * nWidth) / 2);
      const textYPos = image.height - Math.trunc(textHeight / 10);
      // Bars are shortened to leave HUMAN_TEXT_GAP_PX above the human-readable digits.
      const y = textYPos - pen.fontAscent() - HUMAN_TEXT_GAP_PX;

      let x = nWidth * 13;

      pen.fillRect(0, 0, image.width, Math.trunc(image.height / 20), ARGB_WHITE);
      pen.fillRect(
        0,
        image.height - Math.trunc(textHeight / 3),
        image.width,
        Math.trunc(textHeight / 3),
        ARGB_WHITE,
      );

      pen.fillRect(x, y, textWidth, textHeight, ARGB_WHITE);
      pen.drawString(text.substring(0, 1), 10 * nWidth - pen.stringWidth("0"), textYPos, ARGB_BLACK);
      pen.drawString(text.substring(1, 7), x + textXShift, textYPos, ARGB_BLACK);

      x = nWidth * 59;
      pen.fillRect(x, y, textWidth, textHeight, ARGB_WHITE);
      pen.drawString(text.substring(7), x + textXShift, textYPos, ARGB_BLACK);
    });
  }

  paintTextSvg(text: string, barHeight: number, totalWidth: number): string {
    const t1 = text.substring(0, 1);
    const t2 = text.substring(1, 7);
    const t3 = text.substring(7, 13);
    const posY = Math.ceil(barHeight * 0.9);

    let res = "";
    res += `<path fill="#fff" d="M0 ${barHeight}h${totalWidth}v8H0z"/>`;
    res += `<path fill="#fff" d="M13 ${posY}h43v14H13zm46 0h43v14H59z"/>`;
    res += `<text x="0" y="${posY + 10}" class="small" textLength="8">${t1}</text>`;
    res += `<text x="13" y="${posY + 10}" class="small" textLength="43">${t2}</text>`;
    res += `<text x="59" y="${posY + 10}" class="small" textLength="43">${t3}</text>`;
    res += `<text x="106" y="${posY + 10}" class="small" textLength="8">></text>`;
    return res;
  }

  calcTotalHeight(barHeight: number, fontSize: number): number {
    return Math.ceil(barHeight * 0.9 + fontSize);
  }
}
