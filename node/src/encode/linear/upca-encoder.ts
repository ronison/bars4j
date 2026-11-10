import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { checkSum, validateCheckDigit } from "./ean-support.ts";
import { numericOrInvalid } from "./errors.ts";
import { Ean13Encoder } from "./ean13-encoder.ts";

/**
 * Native UPC-A encoder (11/12 digits). Ported from the legacy {@code UPCAEncoder}, which reuses the
 * EAN-13 encoding of {@code "0" + text}.
 */
export class UpcaEncoder implements LinearEncoder {
  private readonly ean13 = new Ean13Encoder();

  completeLength(): number {
    return 12;
  }

  encode(text: string): LinearSymbol {
    if (text.length < 11 || text.length > 12) {
      throw new InvalidSymbolException(`[UPCA] Invalid text length (${text.length}).`);
    }
    if (text.length === 12) {
      try {
        // UPC-A shares the EAN-13 arithmetic with a leading zero.
        validateCheckDigit("UPCA", text.charAt(11), checkSum("0" + text.substring(0, 11)));
      } catch (e) {
        numericOrInvalid(e, "[UPCA] Only numbers suported.");
      }
    }
    try {
      return this.ean13.encode("0" + text);
    } catch (e) {
      if (e instanceof InvalidSymbolException) {
        throw new InvalidSymbolException("[UPCA] Only numbers suported.", { cause: e });
      }
      throw e;
    }
  }

  computeCheckSum(text: string): string {
    try {
      return checkSum("0" + text);
    } catch (e) {
      numericOrInvalid(e, "[UPCA] Only numbers suported.");
    }
  }
}
