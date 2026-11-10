import { digit } from "./linear-bits.ts";

/**
 * Shared 2-of-5 tables and arithmetic (ported from the legacy {@code ToFEncoder}).
 */
export const CODES_WIDTH = [
  "00110", "10001", "01001", "11000", "00101",
  "10100", "01100", "00011", "10010", "01010",
];

/**
 * GS1 mod-10 check digit over the given data digits.
 *
 * <p>The weights are anchored to the <em>right</em> (the rightmost data digit gets weight 3,
 * alternating 1/3 towards the left), as required by the GS1/GTIN and Interleaved 2 of 5 standards.
 */
export function checkSum(text: string): string {
  let sum = 0;
  const length = text.length;
  for (let charPos = 0; charPos < length; charPos++) {
    const d = digit(text.charAt(charPos));
    if ((length - charPos) % 2 === 1) {
      sum += d * 3;
    } else {
      sum += d;
    }
  }
  const result = sum % 10;
  return result === 0 ? "0" : String(10 - result);
}
