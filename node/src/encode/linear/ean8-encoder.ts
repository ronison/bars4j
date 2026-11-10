import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { CENTER_GUARD, CODES, LEFT_GUARD, RIGHT_GUARD, checkSum, validateCheckDigit } from "./ean-support.ts";
import { numericOrInvalid } from "./errors.ts";
import { digit, invert, symbol } from "./linear-bits.ts";

/**
 * Native EAN-8 encoder (8 digits). Ported from the legacy {@code EAN8Encoder}.
 */
export class Ean8Encoder implements LinearEncoder {
  completeLength(): number {
    return 8;
  }

  encode(text: string): LinearSymbol {
    if (text.length !== 8) {
      throw new InvalidSymbolException(`[EAN8] Invalid text length (${text.length}).`);
    }
    let bits = "";
    try {
      validateCheckDigit("EAN8", text.charAt(7), checkSum(text.substring(0, 7)));
      bits += LEFT_GUARD;
      for (let i = 0; i < 4; i++) {
        bits += invert(CODES[digit(text.charAt(i))]);
      }
      bits += CENTER_GUARD;
      for (let i = 4; i < 8; i++) {
        bits += CODES[digit(text.charAt(i))];
      }
      bits += RIGHT_GUARD;
    } catch (e) {
      numericOrInvalid(e, "[EAN8] Only numbers suported.");
    }
    return symbol(bits, BitSemantics.MODULE_DARK);
  }

  computeCheckSum(text: string): string {
    try {
      return checkSum(text);
    } catch (e) {
      numericOrInvalid(e, "[EAN8] Only numbers suported.");
    }
  }
}
