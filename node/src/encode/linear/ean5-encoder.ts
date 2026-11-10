import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { CODES } from "./ean-support.ts";
import { numericOrInvalid } from "./errors.ts";
import { digit, invert, reverse, symbol } from "./linear-bits.ts";

const DIGIT_PARITY = [
  "11000", "10100", "10010", "10001", "01100",
  "00110", "00011", "01010", "01001", "00101",
];

const START = "01011";
const SEPARATOR = "01";

/**
 * Native EAN-5 (supplemental) encoder. Ported from the legacy {@code EAN5Encoder}.
 */
export class Ean5Encoder implements LinearEncoder {
  encode(text: string): LinearSymbol {
    if (text.length !== 5) {
      throw new InvalidSymbolException(`[EAN5] Invalid text length (${text.length}).`);
    }
    let bits = "";
    try {
      const checksum = parseInt(this.computeCheckSum(text), 10);
      bits += START;
      for (let i = 1; i <= 5; i++) {
        const atual = digit(text.charAt(i - 1));
        if (DIGIT_PARITY[checksum].charAt(i - 1) === "1") {
          bits += reverse(CODES[atual]);
        } else {
          bits += invert(CODES[atual]);
        }
        if (i < 5) {
          bits += SEPARATOR;
        }
      }
    } catch (e) {
      numericOrInvalid(e, "[EAN5] Only numbers suported.");
    }
    return symbol(bits, BitSemantics.MODULE_DARK);
  }

  computeCheckSum(text: string): string {
    let sum = 0;
    let odd = true;
    try {
      for (let charPos = 0; charPos < text.length; charPos++) {
        if (odd) {
          sum += 3 * digit(text.charAt(charPos));
        } else {
          sum += 9 * digit(text.charAt(charPos));
        }
        odd = !odd;
      }
    } catch (e) {
      numericOrInvalid(e, "[EAN5] Only numbers suported.");
    }
    return String(sum % 10);
  }
}
