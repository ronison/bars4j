import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { numericOrInvalid } from "./errors.ts";
import { digit, symbol } from "./linear-bits.ts";

const CODES = [
  "11000", "00011", "00101", "00110", "01001",
  "01010", "01100", "10001", "10010", "10100",
];

const START_STOP_CHAR = "1";

/**
 * Native PostNet encoder. Ported from the legacy {@code PostNetEncoder}.
 */
export class PostNetEncoder implements LinearEncoder {
  encode(text: string): LinearSymbol {
    if (text.length < 1) {
      throw new InvalidSymbolException(`[PostNet] Tamanho de texto invalido (${text.length}).`);
    }
    let bits = "";
    try {
      bits += START_STOP_CHAR;
      for (let i = 0; i < text.length; i++) {
        bits += CODES[digit(text.charAt(i))];
      }
      bits += START_STOP_CHAR;
    } catch (e) {
      numericOrInvalid(e, "[PostNet] O padrao suporta apenas numeros.");
    }
    return symbol(bits, BitSemantics.HEIGHT_CODED);
  }

  computeCheckSum(text: string): string {
    let check = 0;
    try {
      for (let i = 0; i < text.length; i++) {
        check += digit(text.charAt(i));
      }
    } catch (e) {
      numericOrInvalid(e, "[PostNet] O padrao suporta apenas numeros.");
    }
    const result = check % 10;
    if (result === 0) {
      return "0";
    }
    return String(10 - result);
  }
}
