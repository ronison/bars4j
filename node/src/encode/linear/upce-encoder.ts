import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { CODES, LEFT_GUARD, checkSum, validateCheckDigit } from "./ean-support.ts";
import { numericOrInvalid } from "./errors.ts";
import { digit, invert, reverse, symbol } from "./linear-bits.ts";

const DIGIT_PARITY = [
  "111000", "110100", "110010", "110001", "101100",
  "100110", "100011", "101010", "101001", "100101",
];

const RIGHT_GUARD_EXT = "010101";

/**
 * Native UPC-E encoder (8 characters: 7 data + check digit). Ported from the legacy
 * {@code UPCEEncoder}.
 */
export class UpceEncoder implements LinearEncoder {
  completeLength(): number {
    return 8;
  }

  encode(text: string): LinearSymbol {
    if (text.length !== 8) {
      throw new InvalidSymbolException(`[UPCE] Invalid text length (${text.length}).`);
    }
    validateCheckDigit("UPCE", text.charAt(7), this.computeCheckSum(text.substring(0, 7)));
    let bits = "";
    try {
      const check = digit(text.charAt(7));
      const primeiro = digit(text.charAt(0));
      const par = primeiro === 0 ? DIGIT_PARITY[check] : invert(DIGIT_PARITY[check]);

      bits += LEFT_GUARD;
      for (let i = 0; i < 6; i++) {
        const atual = digit(text.charAt(i + 1));
        if (par.charAt(i) === "1") {
          bits += reverse(CODES[atual]);
        } else {
          bits += invert(CODES[atual]);
        }
      }
      bits += RIGHT_GUARD_EXT;
    } catch (e) {
      numericOrInvalid(e, "[UPCE] O padrao suporta apenas numeros.");
    }
    return symbol(bits, BitSemantics.MODULE_DARK);
  }

  computeCheckSum(text: string): string {
    if (text.length !== 7) {
      throw new InvalidSymbolException(`[UPCE] Invalid text length (${text.length}).`);
    }
    try {
      return checkSum(this.convertUPCEtoUPCA(text));
    } catch (e) {
      numericOrInvalid(e, "[UPCE] O padrao suporta apenas numeros.");
    }
  }

  convertUPCEtoUPCA(upce: string): string {
    if (upce.length !== 7 && upce.length !== 8) {
      throw new InvalidSymbolException(`[UPCE] Invalid text length (${upce.length}).`);
    }
    let result = upce.charAt(0);
    switch (upce.charAt(6)) {
      case "0":
      case "1":
      case "2":
        result += upce.charAt(1) + upce.charAt(2) + upce.charAt(6) + "0000" + upce.charAt(3) + upce.charAt(4) + upce.charAt(5);
        break;
      case "3":
        result += upce.charAt(1) + upce.charAt(2) + upce.charAt(3) + "00000" + upce.charAt(4) + upce.charAt(5);
        break;
      case "4":
        result += upce.charAt(1) + upce.charAt(2) + upce.charAt(3) + upce.charAt(4) + "00000" + upce.charAt(5);
        break;
      case "5":
      case "6":
      case "7":
      case "8":
      case "9":
        result += upce.charAt(1) + upce.charAt(2) + upce.charAt(3) + upce.charAt(4) + upce.charAt(5) + "0000" + upce.charAt(6);
        break;
      default:
        throw new InvalidSymbolException(`[UPCE] Invalid char (${upce.charAt(6)}).`);
    }
    if (upce.length === 8) {
      result += upce.charAt(7);
    }
    return result;
  }
}
