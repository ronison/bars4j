import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { symbol } from "./linear-bits.ts";

const CODES_WIDTH = [
  "0000011", "0000110", "0001001", "1100000", "0010010",
  "1000010", "0100001", "0100100", "0110000", "1001000",
  "0001100", "0011000", "1000101", "1010001", "1010100",
  "0011111", "0011010", "0001011", "0101001", "0001110",
];

const INTER_CHAR = "0";
const START_CHAR_A = 16;
const STOP_CHAR_ASTERISC = 18;

/**
 * Native Codabar encoder. Ported from the legacy {@code CodabarEncoder} (default start 'A',
 * stop '*').
 */
export class CodabarEncoder implements LinearEncoder {
  encode(text: string): LinearSymbol {
    if (text.length < 1) {
      throw new InvalidSymbolException(`[Codabar] Invalid text length (${text.length}).`);
    }
    let bits = CODES_WIDTH[START_CHAR_A];
    for (let i = 0; i < text.length; i++) {
      bits += INTER_CHAR;
      bits += CODES_WIDTH[getCharIndex(text.charAt(i))];
    }
    bits += INTER_CHAR;
    bits += CODES_WIDTH[STOP_CHAR_ASTERISC];
    return symbol(bits, BitSemantics.WIDE_NARROW_ALTERNATING);
  }

  computeCheckSum(_text: string): string {
    return "";
  }
}

function getCharIndex(c: string): number {
  const code = c.charCodeAt(0);
  if (c >= "0" && c <= "9") {
    return code - 48;
  } else if (c === "-") {
    return 10;
  } else if (c === "$") {
    return 11;
  } else if (c === ":") {
    return 12;
  } else if (c === "/") {
    return 13;
  } else if (c === ".") {
    return 14;
  } else if (c === "+") {
    return 15;
  }
  throw new InvalidSymbolException(`[Codabar] The text contains not suported chars (${c}).`);
}
