import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { symbol } from "./linear-bits.ts";

const START_CODE_B_ALPHA = 104;
const START_CODE_C_NUMERIC = 105;
const SWITCH_TO_CODE_B_ALPHA = 100;
const SWITCH_TO_CODE_C_NUMERIC = 99;
const STOP = 106;

const BARSETS = [
  "11011001100", "11001101100", "11001100110", "10010011000", "10010001100",
  "10001001100", "10011001000", "10011000100", "10001100100", "11001001000",
  "11001000100", "11000100100", "10110011100", "10011011100", "10011001110",
  "10111001100", "10011101100", "10011100110", "11001110010", "11001011100",
  "11001001110", "11011100100", "11001110100", "11101101110", "11101001100",
  "11100101100", "11100100110", "11101100100", "11100110100", "11100110010",
  "11011011000", "11011000110", "11000110110", "10100011000", "10001011000",
  "10001000110", "10110001000", "10001101000", "10001100010", "11010001000",
  "11000101000", "11000100010", "10110111000", "10110001110", "10001101110",
  "10111011000", "10111000110", "10001110110", "11101110110", "11010001110",
  "11000101110", "11011101000", "11011100010", "11011101110", "11101011000",
  "11101000110", "11100010110", "11101101000", "11101100010", "11100011010",
  "11101111010", "11001000010", "11110001010", "10100110000", "10100001100",
  "10010110000", "10010000110", "10000101100", "10000100110", "10110010000",
  "10110000100", "10011010000", "10011000010", "10000110100", "10000110010",
  "11000010010", "11001010000", "11110111010", "11000010100", "10001111010",
  "10100111100", "10010111100", "10010011110", "10111100100", "10011110100",
  "10011110010", "11110100100", "11110010100", "11110010010", "11011011110",
  "11011110110", "11110110110", "10101111000", "10100011110", "10001011110",
  "10111101000", "10111100010", "11110101000", "11110100010", "10111011110",
  "10111101110", "11101011110", "11110101110", "11010000100", "11010010000",
  "11010011100", "11000111010",
];

const TERMINATION_BAR = "11";

/**
 * Native Code 128 encoder. Ported from the legacy {@code Code128Encoder} (character sets B and C).
 */
export class Code128Encoder implements LinearEncoder {
  encode(text: string): LinearSymbol {
    if (text == null || text.length < 1) {
      throw new InvalidSymbolException(`[Code128] Invalid text (${text}).`);
    }

    let bits = "";
    // Latin-1 matches the Java implementation (ISO-8859-1, one byte per char for 0-255).
    const bytes = Buffer.from(text, "latin1");

    let usingNumericEncoding = false;
    let code = START_CODE_B_ALPHA;

    if (bytes.length > 1 && isDigit(bytes[0]) && isDigit(bytes[1])) {
      usingNumericEncoding = true;
      code = START_CODE_C_NUMERIC;
    }

    let checksum = code;
    bits += BARSETS[code];

    let checkMultiplier = 1;

    for (let i = 0; i < bytes.length; i++) {
      if (i < bytes.length - 1 && isDigit(bytes[i]) && isDigit(bytes[i + 1])) {
        if (!usingNumericEncoding) {
          usingNumericEncoding = true;
          code = SWITCH_TO_CODE_C_NUMERIC;
          checksum += code * checkMultiplier;
          checkMultiplier++;
          bits += BARSETS[code];
        }
        code = parseInt(bytes.toString("ascii", i, i + 2), 10);
        i++;
      } else {
        if (usingNumericEncoding) {
          usingNumericEncoding = false;
          code = SWITCH_TO_CODE_B_ALPHA;
          checksum += code * checkMultiplier;
          checkMultiplier++;
          bits += BARSETS[code];
        }
        code = getCode(bytes[i]);
      }

      checksum += code * checkMultiplier;
      checkMultiplier++;

      bits += BARSETS[code];
    }

    const checkDigit = checksum % 103;
    bits += BARSETS[checkDigit];
    bits += BARSETS[STOP];
    bits += TERMINATION_BAR;

    return symbol(bits, BitSemantics.MODULE_DARK);
  }

  computeCheckSum(_text: string): string {
    return "";
  }
}

function isDigit(b: number): boolean {
  return b >= 48 && b <= 57;
}

function getCode(s: number): number {
  if (s < 32 || s > 126) {
    throw new InvalidSymbolException("[Code128] The text contains unsuported chars.");
  }
  return s - 32;
}
