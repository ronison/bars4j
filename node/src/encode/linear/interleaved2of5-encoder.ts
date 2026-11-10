import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { numericOrInvalid } from "./errors.ts";
import { digit, isDigit, symbol } from "./linear-bits.ts";
import { CODES_WIDTH, checkSum } from "./tof-support.ts";

const START_CHAR = "0000";
const STOP_CHAR = "100";

/**
 * Native Interleaved 2 of 5 encoder. Ported from the legacy {@code Interleaved2of5Encoder}.
 */
export class Interleaved2of5Encoder implements LinearEncoder {
  encode(text: string): LinearSymbol {
    if (text.length < 1) {
      throw new InvalidSymbolException(`[Interleaved2of5] Invalid text length (${text.length}).`);
    }
    for (let i = 0; i < text.length; i++) {
      if (!isDigit(text.charAt(i))) {
        throw new InvalidSymbolException("[Interleaved2of5] Only numbers suported.");
      }
    }
    let bits = "";
    try {
      bits += START_CHAR;
      for (let i = 0; i < Math.trunc(text.length / 2); i++) {
        const first = CODES_WIDTH[digit(text.charAt(2 * i))];
        const second = CODES_WIDTH[digit(text.charAt(2 * i + 1))];
        for (let j = 0; j < first.length; j++) {
          bits += first.charAt(j);
          bits += second.charAt(j);
        }
      }
      bits += STOP_CHAR;
    } catch (e) {
      numericOrInvalid(e, "[Interleaved2of5] Only numbers suported.");
    }
    return symbol(bits, BitSemantics.WIDE_NARROW_ALTERNATING);
  }

  computeCheckSum(text: string): string {
    try {
      return checkSum(text);
    } catch (e) {
      numericOrInvalid(e, "[Interleaved2of5] Only numbers suported.");
    }
  }
}
