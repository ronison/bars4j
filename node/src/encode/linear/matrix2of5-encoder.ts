import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { numericOrInvalid } from "./errors.ts";
import { digit, symbol } from "./linear-bits.ts";
import { Standard2of5Encoder } from "./standard2of5-encoder.ts";
import { CODES_WIDTH } from "./tof-support.ts";

/**
 * Native Matrix 2 of 5 encoder. Ported from the legacy {@code Matrix2of5Encoder}.
 */
export class Matrix2of5Encoder extends Standard2of5Encoder {
  constructor() {
    super();
    this.startChar = "100000";
    this.stopChar = "10000";
  }

  encode(text: string): LinearSymbol {
    if (text.length < 1) {
      throw new InvalidSymbolException(`[Matrix2of5] Invalid text length (${text.length}).`);
    }
    let bits = "";
    try {
      bits += this.startChar;
      for (let i = 0; i < text.length; i++) {
        const ind = digit(text.charAt(i));
        bits += CODES_WIDTH[ind];
        bits += "0";
      }
      bits += this.stopChar;
    } catch (e) {
      numericOrInvalid(e, "[Matrix2of5] Only numbers suported.");
    }
    return symbol(bits, BitSemantics.WIDE_NARROW_ALTERNATING);
  }
}
