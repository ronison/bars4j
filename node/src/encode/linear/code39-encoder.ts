import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { symbol } from "./linear-bits.ts";

const CODES_WIDTH = [
  "000110100", "100100001", "001100001", "101100000", "000110001",
  "100110000", "001110000", "000100101", "100100100", "001100100",
  "100001001", "001001001", "101001000", "000011001", "100011000",
  "001011000", "000001101", "100001100", "001001100", "000011100",
  "100000011", "001000011", "101000010", "000010011", "100010010",
  "001010010", "000000111", "100000110", "001000110", "000010110",
  "110000001", "011000001", "111000000", "010010001", "110010000",
  "011010000", "010000101", "110000100", "011000100", "010101000",
  "010100010", "010001010", "000101010",
];

const START_STOP_CHAR = "010010100";
const INTER_CHAR = "0";

/**
 * Native Code 39 encoder. Ported from the legacy {@code Code39Encoder}.
 */
export class Code39Encoder implements LinearEncoder {
  encode(text: string): LinearSymbol {
    if (text.length < 1) {
      throw new InvalidSymbolException(`[Code39] Invalid text length (${text.length}).`);
    }
    let bits = START_STOP_CHAR;
    for (let i = 0; i < text.length; i++) {
      bits += INTER_CHAR;
      bits += CODES_WIDTH[this.getCharIndex(text.charAt(i))];
    }
    bits += INTER_CHAR;
    bits += START_STOP_CHAR;
    return symbol(bits, BitSemantics.WIDE_NARROW_ALTERNATING);
  }

  computeCheckSum(text: string): string {
    let check = 0;
    for (let i = 0; i < text.length; i++) {
      check += this.getCharIndex(text.charAt(i));
    }
    return String.fromCharCode(this.getChar(check % 43));
  }

  protected getCharIndex(c: string): number {
    const code = c.charCodeAt(0);
    if (c >= "0" && c <= "9") {
      return code - 48;
    } else if (c >= "A" && c <= "Z") {
      return code - 55;
    } else if (c === "-") {
      return 36;
    } else if (c === ".") {
      return 37;
    } else if (c === " ") {
      return 38;
    } else if (c === "$") {
      return 39;
    } else if (c === "/") {
      return 40;
    } else if (c === "+") {
      return 41;
    } else if (c === "%") {
      return 42;
    }
    throw new InvalidSymbolException("[Code39] The text contains unsuported chars.");
  }

  private getChar(c: number): number {
    if (c >= 0 && c <= 9) {
      return c + 48;
    } else if (c >= 10 && c <= 35) {
      return c + 55;
    } else if (c === 36) {
      return 45;
    } else if (c === 37) {
      return 46;
    } else if (c === 38) {
      return 32;
    } else if (c === 39) {
      return 36;
    } else if (c === 40) {
      return 47;
    } else if (c === 41) {
      return 43;
    } else if (c === 42) {
      return 37;
    }
    throw new InvalidSymbolException("[Code39] The text contains unsuported chars.");
  }
}
