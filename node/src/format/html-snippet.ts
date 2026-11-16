import { escapeAttr } from "../util/escapes.ts";

/**
 * Assembles the embeddable HTML/SVG snippet shared by the barcode formats. Unlike the standalone
 * SVG document ({@code toSvg}) the snippet has no XML prologue, no generator comment and no
 * fallback text, and carries a {@code viewBox} so it scales with its container.
 *
 * Port of {@code org.bars4j.format.HtmlSnippet}.
 */

/**
 * Assembles the snippet with an explicit display size. The {@code viewBox} keeps the natural
 * geometry so the symbol scales proportionally (a display size of -1 keeps the natural size).
 */
export function htmlSnippet(
  viewBoxWidth: number,
  viewBoxHeight: number,
  displayWidth: number,
  displayHeight: number,
  ariaLabel: string,
  fragment: string,
): string {
  return "<svg xmlns=\"http://www.w3.org/2000/svg\""
    + ` viewBox="0 0 ${viewBoxWidth} ${viewBoxHeight}"`
    + ` width="${displayWidth}" height="${displayHeight}"`
    + " preserveAspectRatio=\"xMidYMid meet\""
    + ` role="img" aria-label="${escapeAttr(ariaLabel)}">`
    + "<style>text{font-family: 'Courier New', monospace;font-size: 14px;}</style>"
    + fragment
    + "</svg>";
}
