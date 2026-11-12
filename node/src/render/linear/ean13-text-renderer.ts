import { parseArgb, type RasterImage } from "../raster-image.ts";
import { drawText } from "../canvas-backend.ts";
import { hexColour } from "./colour-util.ts";
import { escapeText } from "../../util/escapes.ts";
import { HUMAN_TEXT_GAP_PX, type LinearTextRenderer } from "./linear-text-renderer.ts";
import type { RenderSpec } from "../render-spec.ts";

/**
 * EAN-13 specific text layout (guard bars left visible, three text groups).
 *
 * Port of {@code org.bars4j.render.linear.Ean13TextRenderer}.
 */
export class Ean13TextRenderer implements LinearTextRenderer {
  paintText(image: RasterImage, text: string, nWidth: number): void {
    this.paintTextWithSpec(image, text, nWidth, {
      barWidth: 1,
      barHeight: 70,
      wideRatio: 2.0,
      moduleSize: 1,
      foreground: "#000000",
      background: "#ffffff",
    });
  }

  paintTextWithSpec(image: RasterImage, text: string, nWidth: number, spec: RenderSpec): void {
    const background = parseArgb(spec.background);
    const foreground = parseArgb(spec.foreground);
    // The label is normally 13 digits; without the check digit it has 12. Pad defensively so a
    // short label cannot crash (mirrors paintTextSvg).
    const padded = text.length >= 13 ? text : (text + "0000000000000").substring(0, 13);
    drawText(image, (pen) => {
      pen.setFont(11 * nWidth);
      const textWidth = nWidth * 42;
      const textHeight = pen.fontHeight();
      const textXShift = Math.trunc((textWidth - pen.stringWidth("000000") + 2 * nWidth) / 2);
      const textYPos = image.height - Math.trunc(textHeight / 10);
      // Bars are shortened to leave HUMAN_TEXT_GAP_PX above the human-readable digits.
      const y = textYPos - pen.fontAscent() - HUMAN_TEXT_GAP_PX;

      let x = nWidth * 13;

      pen.fillRect(0, 0, image.width, Math.trunc(image.height / 20), background);
      pen.fillRect(
        0,
        image.height - Math.trunc(textHeight / 3),
        image.width,
        Math.trunc(textHeight / 3),
        background,
      );

      pen.fillRect(x, y, textWidth, textHeight, background);
      pen.drawString(padded.substring(0, 1), 10 * nWidth - pen.stringWidth("0"), textYPos, foreground);
      pen.drawString(padded.substring(1, 7), x + textXShift, textYPos, foreground);

      x = nWidth * 59;
      pen.fillRect(x, y, textWidth, textHeight, background);
      pen.drawString(padded.substring(7), x + textXShift, textYPos, foreground);
    });
  }

  paintTextSvg(text: string, barHeight: number, totalWidth: number): string {
    return this.paintTextSvgWithSpec(text, barHeight, totalWidth, { background: "#ffffff" } as RenderSpec);
  }

  paintTextSvgWithSpec(text: string, barHeight: number, totalWidth: number, spec: RenderSpec): string {
    // The label is normally 13 digits; without the check digit it has 12. Pad defensively so a
    // short label cannot crash.
    const padded = text.length >= 13 ? text : (text + "0000000000000").substring(0, 13);
    const t1 = padded.substring(0, 1);
    const t2 = padded.substring(1, 7);
    const t3 = padded.substring(7, 13);
    const posY = Math.ceil(barHeight * 0.9);

    let res = "";
    res += `<path fill="${hexColour(spec.background)}" d="M0 ${barHeight}h${totalWidth}v8H0z"/>`;
    res += `<path fill="${hexColour(spec.background)}" d="M13 ${posY}h43v14H13zm46 0h43v14H59z"/>`;
    res += `<text x="0" y="${posY + 10}" class="small" textLength="8">${escapeText(t1)}</text>`;
    res += `<text x="13" y="${posY + 10}" class="small" textLength="43">${escapeText(t2)}</text>`;
    res += `<text x="59" y="${posY + 10}" class="small" textLength="43">${escapeText(t3)}</text>`;
    return res;
  }

  calcTotalHeight(barHeight: number, fontSize: number): number {
    return Math.ceil(barHeight * 0.9 + fontSize);
  }
}
