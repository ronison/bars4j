import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { symbol } from "./linear-bits.ts";

const CODES = [
  "100010100", "101001000", "101000100", "101000010", "100101000",
  "100100100", "100100010", "101010000", "100010010", "100001010",
  "110101000", "110100100", "110100010", "110010100", "110010010",
  "110001010", "101101000", "101100100", "101100010", "100110100",
  "100011010", "101011000", "101001100", "101000110", "100101100",
  "100010110", "110110100", "110110010", "110101100", "110100110",
  "110010110", "110011010", "101101100", "101100110", "100110110",
  "100111010", "100101110", "111010100", "111010010", "111001010",
  "101101110", "101110110", "110101110", "100100110", "111011010",
  "111010110", "100110010",
];

const START_STOP_CHAR = "101011110";
const TERMINATION_BAR = "1";

/**
 * Native Code 93 encoder. Ported from the legacy {@code Code93Encoder}.
 */
export class Code93Encoder implements LinearEncoder {
  encode(text: string): LinearSymbol {
    if (text.length < 1) {
      throw new InvalidSymbolException(`[Code93] Invalid text length (${text.length}).`);
    }
    let bits = START_STOP_CHAR;
    for (let i = 0; i < text.length; i++) {
      bits += CODES[this.getCharIndex(text.charAt(i))];
    }
    bits += START_STOP_CHAR;
    bits += TERMINATION_BAR;
    return symbol(bits, BitSemantics.MODULE_DARK);
  }

  computeCheckSum(text: string): string {
    // Calc 'C' check digit char
    let check = 0;
    for (let i = text.length - 1; i >= 0; i--) {
      let weight = (text.length - i) % 20;
      weight = weight === 0 ? 20 : weight;
      check += this.getCharIndex(text.charAt(i)) * weight;
    }
    let result = String.fromCharCode(this.getChar(check % 47));

    // Calc 'K' check digit char
    check = 0;
    const temp = text + result;
    for (let i = temp.length - 1; i >= 0; i--) {
      let weight = (temp.length - i) % 15;
      weight = weight === 0 ? 15 : weight;
      check += this.getCharIndex(temp.charAt(i)) * weight;
    }
    result += String.fromCharCode(this.getChar(check % 47));
    return result;
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
    } else if (c === "\u00B9") { // special char ($)
      return 43;
    } else if (c === "\u00B2") { // special char (%)
      return 44;
    } else if (c === "\u00B3") { // special char (/)
      return 45;
    } else if (c === "\u00A3") { // special char (+)
      return 46;
    }
    throw new InvalidSymbolException("[Code93] The text contains unsuported chars.");
  }

  protected getChar(c: number): number {
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
    } else if (c === 43) {
      return 0x00b9;
    } else if (c === 44) {
      return 0x00b2;
    } else if (c === 45) {
      return 0x00b3;
    } else if (c === 46) {
      return 0x00a3;
    }
    throw new InvalidSymbolException("[Code93] The text contains unsuported chars.");
  }
}
