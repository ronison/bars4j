import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { symbol } from "./linear-bits.ts";

const CODES_WIDTH = [
  "101011", "1101011", "1001011", "1100101", "1011011",
  "1101101", "1001101", "1010011", "1101001", "110101",
  "101101",
];

const INTER_CHAR = "0";
const START_STOP_CHAR = "1011001";

/**
 * Native Code 11 encoder. Ported from the legacy {@code Code11Encoder}.
 */
export class Code11Encoder implements LinearEncoder {
  encode(text: string): LinearSymbol {
    if (text.length < 1) {
      throw new InvalidSymbolException(`[Code11] Invalid text length (${text.length}).`);
    }
    let bits = START_STOP_CHAR;
    bits += INTER_CHAR;
    for (let i = 0; i < text.length; i++) {
      bits += CODES_WIDTH[getCharIndex(text.charAt(i))];
      bits += INTER_CHAR;
    }
    bits += START_STOP_CHAR;
    return symbol(bits, BitSemantics.MODULE_DARK);
  }

  computeCheckSum(text: string): string {
    // Calc 'C' check digit char
    let check = 0;
    for (let i = text.length - 1; i >= 0; i--) {
      let weight = (text.length - i) % 10;
      weight = weight === 0 ? 10 : weight;
      check += getCharIndex(text.charAt(i)) * weight;
    }
    let result = String(check % 11);

    if (text.length + result.length >= 10) {
      // Calc 'K' check digit char
      check = 0;
      const temp = text + result;
      for (let i = temp.length - 1; i >= 0; i--) {
        let weight = (temp.length - i) % 9;
        weight = weight === 0 ? 9 : weight;
        check += getCharIndex(temp.charAt(i)) * weight;
      }
      result += String(check % 9);
    }
    return result;
  }
}

function getCharIndex(val: string): number {
  const code = val.charCodeAt(0);
  if (val >= "0" && val <= "9") {
    return code - 48;
  } else if (val === "-") {
    return 10;
  }
  throw new InvalidSymbolException("[Code11] The text contains unsuported chars.");
}
