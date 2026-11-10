import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { CENTER_GUARD, CODES, LEFT_GUARD, RIGHT_GUARD, checkSum, validateCheckDigit } from "./ean-support.ts";
import { numericOrInvalid } from "./errors.ts";
import { digit, invert, reverse, symbol } from "./linear-bits.ts";

const DIGIT_PARITY = [
  "000000", "001011", "001101", "001110", "010011",
  "011001", "011100", "010101", "010110", "011010",
];

/**
 * Native EAN-13 encoder (12/13 digits). Ported from the legacy {@code EAN13Encoder}.
 */
export class Ean13Encoder implements LinearEncoder {
  completeLength(): number {
    return 13;
  }

  encode(text: string): LinearSymbol {
    if (text.length < 12 || text.length > 13) {
      throw new InvalidSymbolException(`[EAN13] Invalid text length (${text.length}).`);
    }
    // A 12-digit input is the data part: complete it with its check digit. A 13-digit input is
    // already complete, so its check digit is validated and the text is encoded verbatim.
    let digits: string;
    try {
      if (text.length === 13) {
        validateCheckDigit("EAN13", text.charAt(12), checkSum(text.substring(0, 12)));
        digits = text;
      } else {
        digits = text + this.computeCheckSum(text);
      }
    } catch (e) {
      numericOrInvalid(e, "[EAN13] Only numbers suported.");
    }
    let bits = "";
    try {
      const primeiro = digit(digits.charAt(0));
      bits += LEFT_GUARD;
      for (let i = 1; i <= 6; i++) {
        const atual = digit(digits.charAt(i));
        if (DIGIT_PARITY[primeiro].charAt(i - 1) === "1") {
          bits += reverse(CODES[atual]);
        } else {
          bits += invert(CODES[atual]);
        }
      }
      bits += CENTER_GUARD;
      for (let i = 7; i <= 12; i++) {
        bits += CODES[digit(digits.charAt(i))];
      }
      bits += RIGHT_GUARD;
    } catch (e) {
      numericOrInvalid(e, "[EAN13] Only numbers suported.");
    }
    return symbol(bits, BitSemantics.MODULE_DARK);
  }

  computeCheckSum(text: string): string {
    try {
      return checkSum(text);
    } catch (e) {
      numericOrInvalid(e, "[EAN13] Only numbers suported.");
    }
  }
}
