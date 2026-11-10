import { BitSemantics } from "../../format/bit-semantics.ts";
import { barElement, moduleElement, type BarElement } from "../../symbol/bar-element.ts";
import { LinearSymbol } from "../../symbol/linear-symbol.ts";

/** Thrown by {@link digit}, mirroring the Java {@code NumberFormatException} used by the encoders. */
export class NumberFormatError extends Error {
  constructor(message = "Invalid number") {
    super(message);
    this.name = "NumberFormatError";
  }
}

/**
 * Helpers shared by the native linear encoders.
 *
 * Port of {@code org.bars4j.encode.linear.LinearBits}.
 */
export function invert(bits: string): string {
  let sb = "";
  for (let i = 0; i < bits.length; i++) {
    sb += bits.charAt(i) === "1" ? "0" : "1";
  }
  return sb;
}

export function reverse(bits: string): string {
  return [...bits].reverse().join("");
}

export function symbol(bits: string, semantics: BitSemantics): LinearSymbol {
  const elements: BarElement[] = [];
  let dark = true; // WIDE_NARROW_ALTERNATING starts dark
  for (let i = 0; i < bits.length; i++) {
    const bit = bits.charAt(i) === "1";
    if (semantics === BitSemantics.MODULE_DARK) {
      elements.push(moduleElement(bit));
    } else if (semantics === BitSemantics.WIDE_NARROW_ALTERNATING) {
      elements.push(barElement(dark, bit, 100));
    } else {
      elements.push(barElement(bit, false, bit ? 100 : 50));
    }
    dark = !dark;
  }
  return new LinearSymbol(elements);
}

export function digit(c: string): number {
  const code = c.charCodeAt(0);
  if (code < 48 || code > 57) {
    throw new NumberFormatError("Invalid number");
  }
  return code - 48;
}

/** True for the ASCII digits '0'-'9' (mirrors {@code Character.isDigit} for the supported inputs). */
export function isDigit(c: string): boolean {
  const code = c.charCodeAt(0);
  return code >= 48 && code <= 57;
}
