import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { validateCheckDigit } from "./ean-support.ts";
import { numericOrInvalid } from "./errors.ts";
import { digit, isDigit, symbol } from "./linear-bits.ts";
import { CODES_WIDTH, checkSum } from "./tof-support.ts";

/** ITF-14 always encodes exactly 14 digits (13 data + 1 check digit). */
export const ITF14_CODE_LENGTH = 14;

/** ITF-14 uses the Interleaved 2 of 5 start pattern (4 narrow elements). */
const START_CHAR = "0000";

/** ITF-14 uses the Interleaved 2 of 5 stop pattern (2 narrow bars + 1 narrow space / bar). */
const STOP_CHAR = "100";

/**
 * ITF-14 (GS1 GTIN-14) encoder.
 *
 * Port of {@code org.bars4j.encode.linear.Itf14Encoder}.
 */
export class Itf14Encoder implements LinearEncoder {
  completeLength(): number {
    return ITF14_CODE_LENGTH;
  }

  encode(text: string): LinearSymbol {
    const digits = complete(text);
    let bits = "";
    try {
      bits += START_CHAR;
      for (let i = 0; i < Math.trunc(digits.length / 2); i++) {
        const first = CODES_WIDTH[digit(digits.charAt(2 * i))];
        const second = CODES_WIDTH[digit(digits.charAt(2 * i + 1))];
        for (let j = 0; j < first.length; j++) {
          bits += first.charAt(j);
          bits += second.charAt(j);
        }
      }
      bits += STOP_CHAR;
    } catch (e) {
      numericOrInvalid(e, "[ITF14] Only numbers suported.");
    }
    return symbol(bits, BitSemantics.WIDE_NARROW_ALTERNATING);
  }

  computeCheckSum(text: string): string {
    if (text == null || (text.length !== ITF14_CODE_LENGTH - 1 && text.length !== ITF14_CODE_LENGTH)) {
      const length = text == null ? 0 : text.length;
      throw new InvalidSymbolException(`[ITF14] Invalid text length (${length}).`);
    }
    for (let i = 0; i < text.length; i++) {
      if (!isDigit(text.charAt(i))) {
        throw new InvalidSymbolException("[ITF14] Only numbers suported.");
      }
    }
    const payload = text.substring(0, ITF14_CODE_LENGTH - 1);
    return checkSum(payload);
  }
}

/**
 * Normalises the input to the complete 14-digit code, appending the computed check digit to a
 * 13-digit payload or validating the supplied one for a 14-digit code.
 */
function complete(text: string): string {
  if (text == null || (text.length !== ITF14_CODE_LENGTH - 1 && text.length !== ITF14_CODE_LENGTH)) {
    const length = text == null ? 0 : text.length;
    throw new InvalidSymbolException(`[ITF14] Invalid text length (${length}).`);
  }
  for (let i = 0; i < text.length; i++) {
    if (!isDigit(text.charAt(i))) {
      throw new InvalidSymbolException("[ITF14] Only numbers suported.");
    }
  }
  if (text.length === ITF14_CODE_LENGTH) {
    const payload = text.substring(0, ITF14_CODE_LENGTH - 1);
    validateCheckDigit("ITF14", text.charAt(ITF14_CODE_LENGTH - 1), checkSum(payload));
    return text;
  }
  return text + checkSum(text);
}
