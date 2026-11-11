/**
 * HTML/XML escaping helpers for content embedded into SVG markup (human-readable text and
 * attributes). Code 128, for instance, accepts {@code &}, {@code <} and {@code >} in its charset,
 * so the raw characters would break the surrounding markup.
 *
 * Port of {@code org.bars4j.util.Escapes}.
 */

/** Escapes a string for use as element text content ({@code & < >}). */
export function escapeText(text: string | null | undefined): string {
  if (text == null) {
    return "";
  }
  let res = "";
  for (const c of text) {
    switch (c) {
      case "&":
        res += "&amp;";
        break;
      case "<":
        res += "&lt;";
        break;
      case ">":
        res += "&gt;";
        break;
      default:
        res += c;
    }
  }
  return res;
}

/** Escapes a string for use inside a double-quoted attribute value ({@code & < > "}). */
export function escapeAttr(text: string | null | undefined): string {
  if (text == null) {
    return "";
  }
  let res = "";
  for (const c of text) {
    switch (c) {
      case "&":
        res += "&amp;";
        break;
      case "<":
        res += "&lt;";
        break;
      case ">":
        res += "&gt;";
        break;
      case '"':
        res += "&quot;";
        break;
      default:
        res += c;
    }
  }
  return res;
}
