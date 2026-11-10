import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { numericOrInvalid } from "./errors.ts";
import { digit, symbol } from "./linear-bits.ts";
import { CODES_WIDTH, checkSum } from "./tof-support.ts";

/**
 * Native Standard/Industrial 2 of 5 encoder. Ported from the legacy {@code Standard2of5Encoder}.
 */
export class Standard2of5Encoder implements LinearEncoder {
  protected startChar = "101000";
  protected stopChar = "10001";

  encode(text: string): LinearSymbol {
    if (text.length < 1) {
      throw new InvalidSymbolException(`[Standard2of5] Invalid text length (${text.length}).`);
    }
    let bits = "";
    try {
      bits += this.startChar;
      for (let i = 0; i < text.length; i++) {
        const ind = digit(text.charAt(i));
        bits += expand(CODES_WIDTH[ind]);
      }
      bits += this.stopChar;
    } catch (e) {
      numericOrInvalid(e, "[Standard2of5] Only numbers suported.");
    }
    return symbol(bits, BitSemantics.WIDE_NARROW_ALTERNATING);
  }

  computeCheckSum(text: string): string {
    try {
      return checkSum(text);
    } catch (e) {
      numericOrInvalid(e, "[Standard2of5] Only numbers suported.");
    }
  }
}

/** Expands a 5-element code into the legacy 10-bit standard 2-of-5 pattern. */
function expand(code: string): string {
  let sb = "";
  for (let j = 0; j < code.length; j++) {
    sb += code.charAt(j);
    sb += "0";
  }
  return sb;
}
