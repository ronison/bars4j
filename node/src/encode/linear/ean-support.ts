import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { digit } from "./linear-bits.ts";

/**
 * Shared EAN/UPC tables and arithmetic (ported from the legacy {@code EANEncoder}).
 */
export const CODES = [
  "1110010", "1100110", "1101100", "1000010", "1011100",
  "1001110", "1010000", "1000100", "1001000", "1110100",
];

export const LEFT_GUARD = "101";
export const CENTER_GUARD = "01010";
export const RIGHT_GUARD = "101";

export function checkSum(text: string): string {
  let sum = 0;
  const odd = text.length % 2;
  for (let charPos = 0; charPos < text.length; charPos++) {
    if ((charPos + odd) % 2 > 0) {
      sum += digit(text.charAt(charPos)) * 3;
    } else {
      sum += digit(text.charAt(charPos));
    }
  }
  const result = sum % 10;
  return result === 0 ? "0" : String(10 - result);
}

/**
 * Rejects a supplied check digit that does not match the one computed from the payload.
 */
export function validateCheckDigit(format: string, provided: string, expected: string): void {
  const expectedChar = expected.charAt(0);
  if (provided !== expectedChar) {
    throw new InvalidSymbolException(
      `[${format}] Invalid check digit '${provided}' (expected '${expectedChar}').`,
    );
  }
}
